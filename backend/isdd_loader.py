"""
Load ISDD (Indian Scam Detection Dataset) for model training.
Legacy Kaggle SMS dataset remains in model.py — use DATASET_SOURCE=isdd to train on ISDD.
"""
from io import StringIO

import pandas as pd

from paths import isdd_processed_dir

ISDD_PROCESSED = isdd_processed_dir()
ISDD_COMBINED = ISDD_PROCESSED / "ISDD_combined_v1.0.csv"
ISDD_TRAIN = ISDD_PROCESSED / "ISDD_train_split.csv"


def isdd_available() -> bool:
    return ISDD_COMBINED.is_file() or ISDD_TRAIN.is_file()


def load_isdd_training_frame() -> pd.DataFrame:
    """Returns DataFrame with columns: text, label (safe|phishing)."""
    path = ISDD_TRAIN if ISDD_TRAIN.is_file() else ISDD_COMBINED
    if not path.is_file():
        raise FileNotFoundError(
            f"ISDD dataset not found at {path}. Run: python scripts/generate_isdd_dataset.py"
        )

    df = pd.read_csv(path)
    text_col = "message_text_english" if "message_text_english" in df.columns else "message_text_original"
    if text_col not in df.columns and "message_content" in df.columns:
        # JSON-derived fallback
        df["message_text_english"] = df["message_content"]
        text_col = "message_text_english"

    label_col = "label" if "label" in df.columns else "scam_label"
    out = pd.DataFrame()
    out["text"] = df[text_col].fillna("").astype(str)
    out["label"] = df[label_col].map({1: "phishing", 0: "safe", "1": "phishing", "0": "safe"})
    out = out[out["text"].str.len() > 2]
    out = out.dropna(subset=["label"])
    return out
