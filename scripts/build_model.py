#!/usr/bin/env python3
"""Train phishing_model.pkl for production (Docker build / startup fallback)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from model import train_model  # noqa: E402

if __name__ == "__main__":
    train_model()
