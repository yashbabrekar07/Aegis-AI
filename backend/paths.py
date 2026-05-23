"""Resolve repo root for dev (repo/backend/) and Docker (flat /app/)."""
import os
from pathlib import Path


def repo_root() -> Path:
    env = os.getenv("AEGIS_REPO_ROOT")
    if env:
        return Path(env)

    here = Path(__file__).resolve().parent
    if (here / "ISDD_Dataset").is_dir():
        return here
    if (here.parent / "ISDD_Dataset").is_dir():
        return here.parent
    # Dev default: backend/ -> parent repo
    return here.parent


def isdd_processed_dir() -> Path:
    return repo_root() / "ISDD_Dataset" / "processed"
