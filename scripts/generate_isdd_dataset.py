#!/usr/bin/env python3
"""
Generate ISDD v1.0 — Indian Scam Detection Dataset (schema per 2_isdd_dataset_schema_guidelines.md).
Keeps legacy backend/dataset.csv.enc separate; writes under ISDD_Dataset/.

Usage:
  python scripts/generate_isdd_dataset.py
  python scripts/generate_isdd_dataset.py --total 8000
"""
from __future__ import annotations

import argparse
import json
import random
import re
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd

REPO_ROOT = Path(__file__).resolve().parents[1]
ISDD_ROOT = REPO_ROOT / "ISDD_Dataset"

SCAM_TYPES = [
    "UPI_FRAUD",
    "KYC_FRAUD",
    "FAKE_JOB_OFFER",
    "LOTTERY_PRIZE_SCAM",
    "BANK_IMPERSONATION",
    "OTP_PASSWORD_THEFT",
]

CHANNELS = ["SMS", "WHATSAPP", "EMAIL"]
LANGUAGES = ["hindi", "marathi", "hinglish", "tamil", "telugu", "english"]

# Synthetic training domains — clearly fake (not real brands' domains)
FAKE_LINKS = [
    "http://example-training.invalid/verify",
    "http://demo-scam-lab.invalid/kyc",
    "http://sample-fraud.invalid/claim",
    "http://isdd-synthetic.invalid/update",
    "http://training-only.invalid/pay",
]

UPI_APPS = ["Google Pay", "PhonePe", "Paytm", "WhatsApp Pay", "BHIM UPI"]
BANKS = ["ICICI", "HDFC", "SBI", "Axis", "Kotak"]

# Short genuine messages — critical for avoiding false positives (names, greetings)
SHORT_LEGIT_NAMES = [
    "Ayush", "Yash", "Rahul", "Priya", "Ananya", "Vikram", "Neha", "Arjun", "Aditya",
    "Sneha", "Kavya", "Rohan", "Isha", "Dev", "Meera", "Karan", "Pooja", "Amit",
]
SHORT_LEGIT_PHRASES = [
    ("english", "Hi", "Hi", "Hi"),
    ("english", "Hello", "Hello", "Hello"),
    ("english", "Thanks", "Thanks", "Thanks"),
    ("english", "OK see you", "OK see you", "OK see you"),
    ("hindi", "नमस्ते", "Hello", "Namaste"),
    ("hindi", "ठीक है", "OK", "Theek hai"),
    ("marathi", "धन्यवाद", "Thank you", "Dhanyavaad"),
    ("hinglish", "On my way bro", "On my way bro", "On my way"),
]


def _ts(offset_min: int = 0) -> str:
    base = datetime(2026, 5, 12, 10, 0, 0, tzinfo=timezone.utc)
    return (base.replace(minute=(base.minute + offset_min) % 60)).strftime("%Y-%m-%dT%H:%M:%SZ")


def _meta(text: str, label: int) -> dict:
    t = text.lower()
    return {
        "message_length": len(text),
        "contains_url": bool(re.search(r"https?://", text, re.I)),
        "contains_urgency_words": any(
            w in t
            for w in [
                "turant",
                "urgent",
                "immediately",
                "abhi",
                "लगेच",
                "now",
                "today",
                "15 min",
            ]
        ),
        "contains_threat_words": any(
            w in t
            for w in ["block", "suspend", "compromised", "ब्लॉक", "freeze", "expire", "suspended"]
        ),
        "contains_financial_keywords": any(
            w in t
            for w in [
                "upi",
                "bank",
                "account",
                "kyc",
                "aadhar",
                "pan",
                "otp",
                "payment",
                "loan",
                "बैंक",
            ]
        ),
        "sender_country_code": "91",
        "sender_type": "unknown" if label else "personal",
        "sender_pattern": "spoofed" if label else "normal",
    }


def _scam_record(
    mid: str,
    channel: str,
    scam_type: str,
    language: str,
    original: str,
    english: str,
    roman: str,
    indicators: list[str],
    target: str,
    severity: str,
    confidence: float,
) -> dict:
    return {
        "message_id": mid,
        "timestamp": _ts(random.randint(0, 5000)),
        "channel": channel,
        "scam_label": 1,
        "scam_type": scam_type,
        "confidence": confidence,
        "language": language,
        "message_content": {
            "original": original,
            "transliteration_roman": roman,
            "english_translation": english,
        },
        "metadata": _meta(original, 1),
        "annotations": {
            "scam_indicators": indicators,
            "target_entity": target,
            "attack_vector": "social_engineering",
            "risk_severity": severity,
            "annotator_id": "SYNTH_ISDD",
            "annotation_timestamp": _ts(),
            "confidence_score": confidence,
            "notes": f"Synthetic {scam_type} sample for ISDD v1.0",
        },
        "features": {
            "urgency_score": 0.85 if "urgent_language" in indicators else 0.4,
            "impersonation_score": 0.9 if "impersonation" in indicators else 0.3,
            "linguistic_anomalies": ["non_standard_hindi"] if language == "hinglish" else [],
        },
    }


def _legit_record(
    mid: str, channel: str, language: str, original: str, english: str, roman: str
) -> dict:
    return {
        "message_id": mid,
        "timestamp": _ts(random.randint(0, 5000)),
        "channel": channel,
        "scam_label": 0,
        "scam_type": "",
        "confidence": 0.98,
        "language": language,
        "message_content": {
            "original": original,
            "transliteration_roman": roman,
            "english_translation": english,
        },
        "metadata": _meta(original, 0),
        "annotations": {
            "scam_indicators": [],
            "target_entity": "NONE",
            "attack_vector": "",
            "risk_severity": "low",
            "annotator_id": "SYNTH_ISDD",
            "annotation_timestamp": _ts(),
            "confidence_score": 0.98,
            "notes": "Legitimate communication sample",
        },
        "features": {"urgency_score": 0.1, "impersonation_score": 0.0, "linguistic_anomalies": []},
    }


def expand_templates() -> list[dict]:
    """Template pools per scam type and language — expanded combinatorially."""
    records: list[dict] = []
    seq = [0]

    def next_id(channel: str) -> str:
        seq[0] += 1
        return f"ISDD_{channel}_{seq[0]:05d}"

    # --- UPI_FRAUD ---
    upi_scams = [
        (
            "hindi",
            "आपका {app} अकाउंट सीमित हो गया। तुरंत सत्यापित करें: {link}",
            "Your {app} account is limited. Verify immediately: {link}",
            "Aapka {app} account seemit ho gaya. Turant satyapit karein: {link}",
        ),
        (
            "hinglish",
            "URGENT: Your {app} UPI is blocked. Update now {link} else account close.",
            "URGENT: Your {app} UPI is blocked. Update now {link} else account close.",
            "URGENT: Your {app} UPI blocked. Update now {link}",
        ),
        (
            "marathi",
            "तुमचे {app} खाते ब्लॉक झाले. लगेच लिंक उघडा: {link}",
            "Your {app} account is blocked. Open link immediately: {link}",
            "Tumche {app} khate block zale. Lagech link ughda: {link}",
        ),
        (
            "english",
            "Alert: {app} payment failed. Re-verify UPI at {link}",
            "Alert: {app} payment failed. Re-verify UPI at {link}",
            "Alert: {app} payment failed. Re-verify at {link}",
        ),
    ]
    for lang, orig, eng, rom in upi_scams:
        for app in UPI_APPS:
            for link in FAKE_LINKS:
                o = orig.format(app=app, link=link)
                records.append(
                    _scam_record(
                        next_id("SMS"),
                        random.choice(["SMS", "WHATSAPP"]),
                        "UPI_FRAUD",
                        lang,
                        o,
                        eng.format(app=app, link=link),
                        rom.format(app=app, link=link),
                        ["urgent_language", "account_threat", "suspicious_link", "impersonation"],
                        app.upper().replace(" ", "_"),
                        "high",
                        0.92,
                    )
                )

    # --- KYC_FRAUD ---
    kyc_templates = [
        (
            "hindi",
            "आपका आधार KYC {status} है। अपडेट करें: {link}",
            "Your Aadhar KYC {status}. Update: {link}",
            "Aapka Aadhar KYC {status} hai. Update karein: {link}",
        ),
        (
            "marathi",
            "PAN verification {status}. लगेच: {link}",
            "PAN verification {status}. Immediately: {link}",
            "PAN verification {status}. Lagech: {link}",
        ),
        (
            "hinglish",
            "Dear user, KYC pending for bank account. Complete on {link} before midnight.",
            "Dear user, KYC pending for bank account. Complete on {link} before midnight.",
            "KYC pending. Complete on {link} before midnight",
        ),
    ]
    statuses = ["expire हो रहा है", "pending", "needs update", "अनिवार्य है"]
    for lang, orig, eng, rom in kyc_templates:
        for status in statuses:
            for link in FAKE_LINKS:
                records.append(
                    _scam_record(
                        next_id("SMS"),
                        "SMS",
                        "KYC_FRAUD",
                        lang,
                        orig.format(status=status, link=link),
                        eng.format(status=status, link=link),
                        rom.format(status=status, link=link),
                        ["urgent_language", "personal_info_request", "suspicious_link"],
                        "AADHAR/UIDAI",
                        "high",
                        0.9,
                    )
                )

    # --- FAKE_JOB_OFFER ---
    job_templates = [
        (
            "english",
            "Work from home! Earn ₹{salary} per day. No experience. Register: {link}",
            "Work from home! Earn ₹{salary} per day. No experience. Register: {link}",
            "WFH earn Rs {salary}/day. Register: {link}",
        ),
        (
            "hindi",
            "सरकारी नौकरी: ₹{salary} महीना। तुरंत रजिस्टर: {link}",
            "Government job: ₹{salary}/month. Register now: {link}",
            "Sarkari naukri Rs {salary}/month. Register: {link}",
        ),
    ]
    for lang, orig, eng, rom in job_templates:
        for salary in ["50,000", "2,00,000", "5,00,000", "10,000"]:
            for link in FAKE_LINKS:
                records.append(
                    _scam_record(
                        next_id("WHATSAPP"),
                        "WHATSAPP",
                        "FAKE_JOB_OFFER",
                        lang,
                        orig.format(salary=salary, link=link),
                        eng.format(salary=salary, link=link),
                        rom.format(salary=salary, link=link),
                        ["financial_keywords", "suspicious_link", "urgent_language"],
                        "EMPLOYER/JOB_SITE",
                        "high",
                        0.88,
                    )
                )

    # --- LOTTERY ---
    for lang, orig, eng, rom in [
        (
            "english",
            "Congratulations! You won ₹{amt} in lucky draw. Claim: {link}",
            "Congratulations! You won ₹{amt} in lucky draw. Claim: {link}",
            "You won Rs {amt}. Claim: {link}",
        ),
        (
            "hinglish",
            "Amazon raffle winner! ₹{amt} prize. Click {link} to redeem.",
            "Amazon raffle winner! ₹{amt} prize. Click {link} to redeem.",
            "Amazon raffle winner Rs {amt}. Click {link}",
        ),
    ]:
        for amt in ["50 Lakh", "1,00,000", "25,000"]:
            for link in FAKE_LINKS:
                records.append(
                    _scam_record(
                        next_id("SMS"),
                        random.choice(["SMS", "WHATSAPP", "EMAIL"]),
                        "LOTTERY_PRIZE_SCAM",
                        lang,
                        orig.format(amt=amt, link=link),
                        eng.format(amt=amt, link=link),
                        rom.format(amt=amt, link=link),
                        ["urgent_language", "suspicious_link", "impersonation"],
                        "LOTTERY_ORGANIZATION",
                        "medium",
                        0.87,
                    )
                )

    # --- BANK_IMPERSONATION ---
    for bank in BANKS:
        for link in FAKE_LINKS:
            records.append(
                _scam_record(
                    next_id("EMAIL"),
                    random.choice(["SMS", "EMAIL"]),
                    "BANK_IMPERSONATION",
                    random.choice(["english", "hindi", "hinglish"]),
                    f"{bank} Alert: Unusual activity. Verify account: {link}",
                    f"{bank} Alert: Unusual activity. Verify account: {link}",
                    f"{bank} alert verify: {link}",
                    ["banking_impersonation", "account_threat", "suspicious_link"],
                    f"{bank}_BANK",
                    "high",
                    0.94,
                )
            )
            records.append(
                _scam_record(
                    next_id("SMS"),
                    "SMS",
                    "BANK_IMPERSONATION",
                    "hindi",
                    f"आपके {bank} खाते में संदिग्ध लेनदेन। तुरंत सत्यापित करें: {link}",
                    f"Suspicious transaction in your {bank} account. Verify immediately: {link}",
                    f"{bank} khate me sandigdh len den. Turant verify: {link}",
                    ["banking_impersonation", "urgency_with_fear", "suspicious_link"],
                    f"{bank}_BANK",
                    "critical",
                    0.96,
                )
            )

    # --- OTP / PASSWORD ---
    otp_msgs = [
        (
            "english",
            "Send your 6-digit OTP to confirm refund. Reply to this SMS.",
            "Send your 6-digit OTP to confirm refund. Reply to this SMS.",
            "Send OTP to confirm refund",
        ),
        (
            "hindi",
            "अपना 4 अंकों का PIN भेजें: verify@example-training.invalid",
            "Send your 4-digit PIN: verify@example-training.invalid",
            "Apna PIN bhejein",
        ),
        (
            "hinglish",
            "Bank needs your CVV and expiry to unblock card. Share now.",
            "Bank needs your CVV and expiry to unblock card. Share now.",
            "Share CVV and expiry to unblock card",
        ),
    ]
    for lang, orig, eng, rom in otp_msgs:
        records.append(
            _scam_record(
                next_id("SMS"),
                "SMS",
                "OTP_PASSWORD_THEFT",
                lang,
                orig,
                eng,
                rom,
                ["personal_info_request", "banking_impersonation", "urgent_language"],
                "OTHER",
                "critical",
                0.97,
            )
        )

    # --- LEGITIMATE ---
    for name in SHORT_LEGIT_NAMES:
        for ch in ("SMS", "WHATSAPP"):
            records.append(
                _legit_record(next_id(ch), ch, "english", name, name, name)
            )
    for lang, orig, eng, rom in SHORT_LEGIT_PHRASES:
        records.append(
            _legit_record(next_id("WHATSAPP"), "WHATSAPP", lang, orig, eng, rom)
        )

    legit_pool = [
        ("hindi", "नमस्ते, कल मीटिंग 2 बजे है। कृपया आ जाइए।", "Hi, meeting tomorrow at 2 PM. Please come.", "Namaste, kal meeting 2 baje hai."),
        ("marathi", "मी तुम्हाला संध्याकाळी भेटू. ठीक आहे का?", "I will meet you this evening. OK?", "Mi tumhala sandhyakali bhetu."),
        ("english", "Your order #48291 has been delivered. Thank you for shopping.", "Your order #48291 has been delivered.", "Order delivered thank you"),
        ("hinglish", "Bro send me the project file when free, no rush.", "Bro send me the project file when free, no rush.", "Send project file when free"),
        ("tamil", "நாளை கூட்டம் 3 மணிக்கு. வருவீர்களா?", "Meeting tomorrow at 3 PM. Will you come?", "Naalai meeting 3 mani"),
        ("telugu", "రేపు లంచ్ కలుద్దామా?", "Shall we meet for lunch tomorrow?", "Repu lunch kaluddama"),
        ("english", "Reminder: Team standup at 10 AM in Room 204.", "Reminder: Team standup at 10 AM in Room 204.", "Standup 10 AM room 204"),
        ("hindi", "माँ, मैं होस्टल पहुँच गया। एक घंटे बाद कॉल करूँगा।", "Mom, I reached the hostel. Will call in an hour.", "Maa, hostel pahunch gaya."),
        ("marathi", "तुझा प्रोजेक्ट मी पाहिला. छान आहे!", "I saw your project. It looks great!", "Tujha project changla aahe"),
        ("english", "OTP for login to your trading app is 839201. Do not share with anyone.", "OTP for login is 839201. Do not share.", "OTP 839201 do not share"),
    ]
    for i in range(1500):
        lang, orig, eng, rom = legit_pool[i % len(legit_pool)]
        suffix = f" ({i % 50})" if i >= len(legit_pool) else ""
        records.append(
            _legit_record(
                next_id(random.choice(CHANNELS)),
                random.choice(CHANNELS),
                lang,
                orig + suffix,
                eng + suffix,
                rom + suffix,
            )
        )

    return records


def upsample_to_target(records: list[dict], total_target: int) -> list[dict]:
    """Duplicate with light paraphrase until we reach total_target (for v1.0 scale)."""
    scams = [r for r in records if r["scam_label"] == 1]
    legit = [r for r in records if r["scam_label"] == 0]
    out: list[dict] = []
    seq = max(
        int(re.search(r"(\d+)$", r["message_id"]).group(1))
        for r in records
        if re.search(r"(\d+)$", r["message_id"])
    )

    def clone(base: dict, n: int) -> dict:
        c = json.loads(json.dumps(base))
        seq_id = n
        c["message_id"] = re.sub(r"\d{5}$", f"{seq_id:05d}", c["message_id"])
        o = c["message_content"]["original"]
        if n > 0 and not o.endswith("."):
            c["message_content"]["original"] = f"{o} [v{n}]"
            c["message_content"]["english_translation"] = (
                c["message_content"]["english_translation"] + f" [v{n}]"
            )
        return c

    half = total_target // 2
    si = 0
    while len([x for x in out if x["scam_label"] == 1]) < half and scams:
        out.append(clone(scams[si % len(scams)], si))
        si += 1
    li = 0
    while len(out) < total_target and legit:
        out.append(clone(legit[li % len(legit)], li))
        li += 1
    random.shuffle(out)
    return out[:total_target]


def to_csv_row(r: dict) -> dict:
    mc = r["message_content"]
    meta = r["metadata"]
    ann = r["annotations"]
    return {
        "message_id": r["message_id"],
        "timestamp": r["timestamp"],
        "channel": r["channel"],
        "label": r["scam_label"],
        "scam_type": r.get("scam_type") or "",
        "language": r["language"],
        "message_text_original": mc["original"],
        "message_text_english": mc["english_translation"],
        "message_text_roman": mc.get("transliteration_roman", ""),
        "sender_type": meta.get("sender_type", ""),
        "contains_url": meta.get("contains_url", False),
        "urgency_words": meta.get("contains_urgency_words", False),
        "threat_words": meta.get("contains_threat_words", False),
        "financial_keywords": meta.get("contains_financial_keywords", False),
        "confidence": r.get("confidence", ann.get("confidence_score", 0.9)),
        "annotator_id": ann.get("annotator_id", ""),
        "notes": ann.get("notes", ""),
        "scam_indicators": "|".join(ann.get("scam_indicators", [])),
        "target_entity": ann.get("target_entity", ""),
        "risk_severity": ann.get("risk_severity", ""),
    }


def write_outputs(records: list[dict], total_target: int) -> None:
    random.seed(42)
    random.shuffle(records)

    scams = [r for r in records if r["scam_label"] == 1]
    legit = [r for r in records if r["scam_label"] == 0]
    half = total_target // 2
    n_scam = min(len(scams), half)
    n_legit = min(len(legit), total_target - n_scam)
    selected = scams[:n_scam] + legit[:n_legit]
    random.shuffle(selected)

    # Raw JSON by channel + language
    raw_dir = ISDD_ROOT / "raw"
    buckets: dict[str, list] = {}
    for r in selected:
        ch = r["channel"].lower()
        lang = r["language"]
        key = f"{ch}_{lang}"
        buckets.setdefault(key, []).append(r)

    for key, items in buckets.items():
        parts = key.split("_", 1)
        ch, lang = parts[0], parts[1] if len(parts) > 1 else "english"
        folder = raw_dir / ch.upper()
        folder.mkdir(parents=True, exist_ok=True)
        out = folder / f"{ch}_{lang}_raw.json"
        with open(out, "w", encoding="utf-8") as f:
            json.dump(items, f, ensure_ascii=False, indent=2)

    # Combined CSV + splits
    rows = [to_csv_row(r) for r in selected]
    df = pd.DataFrame(rows)
    proc = ISDD_ROOT / "processed"
    proc.mkdir(parents=True, exist_ok=True)
    combined_path = proc / "ISDD_combined_v1.0.csv"
    df.to_csv(combined_path, index=False, encoding="utf-8")

    from sklearn.model_selection import train_test_split

    train_df, temp_df = train_test_split(df, test_size=0.30, stratify=df["label"], random_state=42)
    test_df, val_df = train_test_split(temp_df, test_size=0.50, stratify=temp_df["label"], random_state=42)
    train_df.to_csv(proc / "ISDD_train_split.csv", index=False)
    test_df.to_csv(proc / "ISDD_test_split.csv", index=False)
    val_df.to_csv(proc / "ISDD_validation_split.csv", index=False)
    df.to_csv(proc / "ISDD_balanced.csv", index=False)

    # Stats
    stats = {
        "total": len(df),
        "scam": int((df["label"] == 1).sum()),
        "legitimate": int((df["label"] == 0).sum()),
        "by_language": df["language"].value_counts().to_dict(),
        "by_channel": df["channel"].value_counts().to_dict(),
        "by_scam_type": df[df["label"] == 1]["scam_type"].value_counts().to_dict(),
        "generated_at": datetime.now(timezone.utc).isoformat(),
    }
    analysis = ISDD_ROOT / "analysis"
    analysis.mkdir(parents=True, exist_ok=True)
    with open(analysis / "dataset_statistics.json", "w", encoding="utf-8") as f:
        json.dump(stats, f, indent=2)

    print(f"ISDD v1.0 written: {len(df)} messages -> {combined_path}")
    print(json.dumps(stats, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--total", type=int, default=4000, help="Target message count (50/50 balance)")
    args = parser.parse_args()

    ISDD_ROOT.mkdir(parents=True, exist_ok=True)
    (ISDD_ROOT / "annotations").mkdir(exist_ok=True)

    taxonomy = {
        "version": "1.0",
        "scam_types": {t: {"description": f"Indian {t} scam patterns"} for t in SCAM_TYPES},
        "languages": LANGUAGES,
    }
    with open(ISDD_ROOT / "annotations" / "scam_taxonomy.json", "w", encoding="utf-8") as f:
        json.dump(taxonomy, f, indent=2)

    records = expand_templates()
    if len(records) < args.total:
        records = upsample_to_target(records, args.total)
    write_outputs(records, args.total)


if __name__ == "__main__":
    main()
