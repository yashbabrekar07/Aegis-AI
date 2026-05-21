"""Text cleaning helpers for ISDD (mirrors backend preprocess)."""
import re
import string


def clean_for_ml(text: str) -> str:
    if not isinstance(text, str):
        return ""
    text = text.lower()
    text = text.translate(str.maketrans("", "", string.punctuation))
    text = re.sub(r"\s+", " ", text).strip()
    return text
