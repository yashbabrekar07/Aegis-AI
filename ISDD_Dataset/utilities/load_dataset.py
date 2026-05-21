"""Load ISDD splits for research / training."""
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
PROCESSED = ROOT / "processed"


def load_dataset(split: str = "train") -> pd.DataFrame:
    files = {
        "train": PROCESSED / "ISDD_train_split.csv",
        "test": PROCESSED / "ISDD_test_split.csv",
        "validation": PROCESSED / "ISDD_validation_split.csv",
        "combined": PROCESSED / "ISDD_combined_v1.0.csv",
        "balanced": PROCESSED / "ISDD_balanced.csv",
    }
    path = files.get(split, files["combined"])
    if not path.is_file():
        raise FileNotFoundError(f"Missing {path}. Run scripts/generate_isdd_dataset.py")
    return pd.read_csv(path)


if __name__ == "__main__":
    df = load_dataset("train")
    print(f"Train rows: {len(df)}")
    print(df["label"].value_counts())
