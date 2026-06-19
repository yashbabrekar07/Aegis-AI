"""Language detection helpers for call transcripts and multilingual SMS."""
import re

_SUPPORTED = {
    "en": "English",
    "hi": "Hindi",
    "mr": "Marathi",
    "hinglish": "Hinglish",
}


def detect_text_language(text: str) -> str:
    """
    Detect primary language of text.
    Returns: english | hindi | marathi | hinglish | unknown
    """
    if not text or not text.strip():
        return "unknown"

    t = text.strip()
    has_devanagari = bool(re.search(r"[\u0900-\u097F]", t))
    has_latin = bool(re.search(r"[A-Za-z]{2,}", t))

    # Code-mixed Hindi + English
    if has_devanagari and has_latin:
        return "hinglish"

    if has_devanagari:
        # Marathi-specific markers (rough heuristic)
        marathi_markers = (
            "आहे", "नाही", "तुम्ही", "माझ", "करा", "होते", "होत", "पैसे",
            "खाते", "बँक", "पोलिस", "OTP", "व्हेरिफाय",
        )
        if any(m in t for m in marathi_markers):
            return "marathi"
        return "hindi"

    if has_latin:
        lower = t.lower()
        hinglish_markers = (
            "karo", "karein", "bhejo", "bhej", "turant", "abhi", "paisa",
            "account block", "verify karo", "otp bhejo", "lottery jeet",
            "bank wale", "police wale", "kyc update", "link pe click",
        )
        if any(m in lower for m in hinglish_markers):
            return "hinglish"
        return "english"

    return "unknown"


def normalize_stt_language(code: str | None, transcript: str | None = None) -> str:
    """Map Whisper/Google codes to our display labels."""
    if transcript:
        detected = detect_text_language(transcript)
        if detected != "unknown":
            return detected

    if not code:
        return "unknown"

    c = code.lower().replace("_", "-")
    if c.startswith("en"):
        return "english"
    if c.startswith("hi"):
        return "hindi"
    if c.startswith("mr"):
        return "marathi"
    if "hinglish" in c:
        return "hinglish"
    return c.split("-")[0] if c else "unknown"


def language_display_name(lang_key: str) -> str:
    return _SUPPORTED.get(lang_key, lang_key.replace("_", " ").title())
