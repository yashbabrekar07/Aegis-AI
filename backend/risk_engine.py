"""
Calibrated risk scoring — avoids flagging short neutral text (names, greetings) as SCAM.
Thresholds are tuned during train_model() and saved to model_config.json.
"""
import json
import os
import re

CONFIG_FILE = "model_config.json"

DEFAULT_CONFIG = {
    "scam_ml_confidence": 0.78,
    "suspicious_ml_confidence": 0.62,
    "short_text_max_words": 5,
    "min_words_for_ml_scam": 6,
    "scam_keyword_count": 2,
}

_config_cache = None


def load_config() -> dict:
    global _config_cache
    if _config_cache is not None:
        return _config_cache
    cfg = dict(DEFAULT_CONFIG)
    if os.path.isfile(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, encoding="utf-8") as f:
                cfg.update(json.load(f))
        except Exception:
            pass
    _config_cache = cfg
    return cfg


def save_config(cfg: dict) -> None:
    global _config_cache
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2)
    _config_cache = cfg


def _word_count(text: str) -> int:
    return len(re.findall(r"\w+", text, flags=re.UNICODE))


def _is_neutral_short(text: str) -> bool:
    """Names, greetings, acknowledgements — no scam signals."""
    t = text.strip()
    if not t:
        return True
    if re.search(r"https?://|www\.|@\w", t, re.I) or re.search(r"\d{4,}", t):
        return False
    words = re.findall(r"\w+", t, flags=re.UNICODE)
    if len(words) > 5:
        return False
    lower = t.lower()
    scam_hints = (
        "otp", "bank", "upi", "kyc", "lottery", "winner", "verify", "block",
        "urgent", "pin", "cvv", "aadhar", "suspend", "click here", "claim prize",
    )
    if any(kw in lower for kw in scam_hints):
        return False
    greetings = {
        "hi", "hello", "hey", "ok", "okay", "yes", "no", "thanks", "thank",
        "you", "namaste", "good", "morning", "night", "see", "tomorrow",
    }
    if len(words) == 1:
        w = words[0]
        if re.match(r"^[A-Za-z]{2,25}$", w) or re.match(r"^[\u0900-\u097F]{2,20}$", w):
            return True
    if len(words) <= 3 and all(w.lower() in greetings for w in words):
        return True
    if len(words) <= 2 and not re.search(r"\d", t):
        return True
    return False


def classify_risk(
    prediction_label: str,
    confidence_pct: float,
    detected_keywords: list,
    suspicious_links: list,
    scan_links: bool,
    original_text: str,
    english_text: str,
) -> tuple[str, str, str]:
    """
    Returns (risk, reason, display_label).
    risk: SAFE | SUSPICIOUS | SCAM
    """
    cfg = load_config()
    conf = confidence_pct / 100.0
    text_for_heuristics = english_text or original_text
    n_words = _word_count(text_for_heuristics)
    n_kw = len(detected_keywords or [])
    n_links = len(suspicious_links or []) if scan_links else 0

    if _is_neutral_short(text_for_heuristics) and n_kw == 0 and n_links == 0:
        return (
            "SAFE",
            "Short personal message or name with no fraud indicators.",
            "safe",
        )

    # Strong rule-based scam signals
    if n_links > 0 and n_kw >= 1:
        return (
            "SCAM",
            "Suspicious links combined with high-risk scam phrases detected.",
            "phishing",
        )
    if n_kw >= cfg["scam_keyword_count"]:
        return (
            "SCAM",
            "Multiple high-risk fraud signatures identified including predatory phrasing.",
            "phishing",
        )

    # ML — require confidence + enough context (reduces 'Ayush' false positives)
    ml_phishing = prediction_label == "phishing"
    min_words = cfg["min_words_for_ml_scam"]

    if ml_phishing and conf >= cfg["scam_ml_confidence"] and (n_words >= min_words or n_kw >= 1):
        return (
            "SCAM",
            "AI model identified strong phishing patterns in this message.",
            "phishing",
        )

    if ml_phishing and conf >= cfg["suspicious_ml_confidence"]:
        return (
            "SUSPICIOUS",
            "Some phishing-like patterns detected. Verify through official channels.",
            "phishing",
        )

    if n_kw == 1:
        return (
            "SUSPICIOUS",
            f"Contains caution word: '{detected_keywords[0]}'. May be legitimate context — verify sender.",
            prediction_label,
        )

    if conf < cfg["suspicious_ml_confidence"] and n_kw == 0:
        return (
            "SAFE",
            "No malicious patterns or external links detected. Message appears genuine.",
            "safe" if prediction_label != "phishing" or conf < 0.55 else prediction_label,
        )

    return (
        "SAFE",
        "No malicious patterns or external links detected. Message appears genuine.",
        "safe",
    )
