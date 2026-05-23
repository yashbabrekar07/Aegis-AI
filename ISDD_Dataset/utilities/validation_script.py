"""Validate ISDD processed files against schema checklist."""
import json
import sys
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
COMBINED = ROOT / "processed" / "ISDD_combined_v1.0.csv"


def validate() -> bool:
    ok = True
    if not COMBINED.is_file():
        print(f"FAIL: missing {COMBINED}")
        return False

    df = pd.read_csv(COMBINED)
    required = [
        "message_id",
        "label",
        "language",
        "message_text_original",
        "message_text_english",
    ]
    for col in required:
        if col not in df.columns:
            print(f"FAIL: missing column {col}")
            ok = False

    if df["label"].isin([0, 1]).sum() != len(df):
        print("FAIL: labels must be 0 or 1")
        ok = False

    dup = df["message_text_original"].duplicated().sum()
    if dup > len(df) * 0.05:
        print(f"WARN: {dup} duplicate originals (>5%)")

    scam_pct = (df["label"] == 1).mean()
    if not 0.45 <= scam_pct <= 0.55:
        print(f"WARN: scam ratio {scam_pct:.2%} (target 50% ±5%)")

    empty = df["message_text_original"].astype(str).str.strip().eq("").sum()
    if empty:
        print(f"FAIL: {empty} empty messages")
        ok = False

    print(f"OK: {len(df)} rows, scam={scam_pct:.1%}, languages={df['language'].nunique()}")
    return ok


if __name__ == "__main__":
    sys.exit(0 if validate() else 1)
