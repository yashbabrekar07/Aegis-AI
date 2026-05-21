import os
import uuid
import tempfile
from fastapi import UploadFile

from speech import ALLOWED_EXTENSIONS, MIN_AUDIO_BYTES, validate_audio_file


def save_upload_temp(upload: UploadFile) -> tuple[str, str]:
    """
    Persist upload to a secure temp path.
    Returns (path, temp_dir) — caller should shutil.rmtree(temp_dir) when done.
    """
    filename = upload.filename or "audio.bin"
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if ext not in ALLOWED_EXTENSIONS:
        raise ValueError(
            f"Unsupported audio type '.{ext}'. Allowed: {', '.join(sorted(ALLOWED_EXTENSIONS))}"
        )

    temp_dir = tempfile.mkdtemp(prefix="aegis_upload_")
    path = os.path.join(temp_dir, f"{uuid.uuid4().hex}.{ext}")

    content = upload.file.read() if hasattr(upload, "file") else None
    if content is None:
        import asyncio

        raise RuntimeError("Use async read in route handler")

    return path, temp_dir


async def save_upload_async(upload: UploadFile) -> tuple[str, str]:
    filename = upload.filename or "audio.bin"
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if ext not in ALLOWED_EXTENSIONS:
        raise ValueError(
            f"Unsupported audio type '.{ext}'. Allowed: {', '.join(sorted(ALLOWED_EXTENSIONS))}"
        )

    temp_dir = tempfile.mkdtemp(prefix="aegis_upload_")
    path = os.path.join(temp_dir, f"{uuid.uuid4().hex}.{ext}")
    content = await upload.read()
    if len(content) < MIN_AUDIO_BYTES:
        raise ValueError(
            "Uploaded file is too small. Record or upload at least 3 seconds of clear speech."
        )
    with open(path, "wb") as f:
        f.write(content)
    validate_audio_file(path)
    return path, temp_dir


def cleanup_temp_dir(temp_dir: str) -> None:
    import shutil

    if temp_dir and os.path.isdir(temp_dir):
        shutil.rmtree(temp_dir, ignore_errors=True)
