import os
import uuid
import shutil
import subprocess
import tempfile

ALLOWED_EXTENSIONS = {"wav", "mp3", "m4a", "ogg", "flac", "webm", "aac", "3gp", "amr"}
MIN_AUDIO_BYTES = 2_000
_whisper_model = None


def _ensure_ffmpeg_on_path():
    """Whisper and conversion need ffmpeg; imageio-ffmpeg bundle on Windows/local dev."""
    try:
        import imageio_ffmpeg

        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
        ffmpeg_dir = os.path.dirname(ffmpeg_exe)
        target_exe = os.path.join(ffmpeg_dir, "ffmpeg.exe")
        if not os.path.exists(target_exe) and os.name == "nt":
            shutil.copy(ffmpeg_exe, target_exe)
        if ffmpeg_dir not in os.environ.get("PATH", ""):
            os.environ["PATH"] = ffmpeg_dir + os.pathsep + os.environ.get("PATH", "")
    except Exception as e:
        print(f"ffmpeg path setup note: {e}")


_ensure_ffmpeg_on_path()


def _ffmpeg_executable():
    return shutil.which("ffmpeg") or "ffmpeg"


def validate_audio_file(path: str) -> None:
    if not os.path.isfile(path):
        raise ValueError("Audio file was not saved correctly. Please try again.")
    size = os.path.getsize(path)
    if size < MIN_AUDIO_BYTES:
        raise ValueError(
            "Audio is too short or empty. Record at least a few seconds of clear speech "
            "(use speakerphone for calls)."
        )


def convert_to_wav(input_path: str) -> str:
    """Normalize any supported format to 16 kHz mono WAV for Whisper / SpeechRecognition."""
    ext = os.path.splitext(input_path)[1].lower().lstrip(".")
    if ext == "wav":
        validate_audio_file(input_path)
        return input_path

    out_dir = tempfile.mkdtemp(prefix="aegis_audio_")
    wav_path = os.path.join(out_dir, f"{uuid.uuid4().hex}.wav")
    cmd = [
        _ffmpeg_executable(),
        "-y",
        "-i",
        input_path,
        "-ar",
        "16000",
        "-ac",
        "1",
        "-vn",
        wav_path,
    ]
    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=120,
        )
    except subprocess.TimeoutExpired as e:
        raise ValueError("Audio conversion timed out. Try a shorter clip or WAV format.") from e
    except FileNotFoundError as e:
        raise ValueError(
            "Server cannot convert this audio format (ffmpeg missing). "
            "Upload WAV or MP3, or retry in a moment."
        ) from e

    if proc.returncode != 0 or not os.path.isfile(wav_path) or os.path.getsize(wav_path) < MIN_AUDIO_BYTES:
        err = (proc.stderr or proc.stdout or "").strip()
        if "end of file" in err.lower() or "end of input" in err.lower():
            raise ValueError(
                "Audio file appears truncated or corrupt. Re-export as WAV/MP3 or record again."
            )
        snippet = err[-400:] if err else "unknown conversion error"
        raise ValueError(f"Could not read audio file. {snippet}")

    validate_audio_file(wav_path)
    return wav_path


def _normalize_transcript(text: str) -> str:
    t = (text or "").strip()
    if not t:
        return ""
    if t.lower() in ("you", ".", "...", "thank you.", "thanks for watching."):
        return ""
    return t


def process_audio(audio_file_path: str):
    """
    Transcribe audio via Whisper, with Google Speech fallback.
    All inputs are converted to WAV first (fixes m4a/mp3 'end of input' errors).
    """
    global _whisper_model
    wav_path = None
    created_wav = False

    try:
        validate_audio_file(audio_file_path)
        wav_path = convert_to_wav(audio_file_path)
        created_wav = wav_path != audio_file_path

        whisper_error = ""
        try:
            import whisper
            import warnings

            warnings.filterwarnings("ignore")
            if _whisper_model is None:
                model_name = os.getenv("WHISPER_MODEL", "tiny")
                _whisper_model = whisper.load_model(model_name)

            result = _whisper_model.transcribe(
                wav_path,
                fp16=False,
                language=None,
                task="transcribe",
            )
            text = _normalize_transcript(result.get("text", ""))
            if text:
                return text, "Whisper (Multilingual)"
        except Exception as whisper_err:
            whisper_error = str(whisper_err)

        import speech_recognition as sr

        try:
            recognizer = sr.Recognizer()
            with sr.AudioFile(wav_path) as source:
                recognizer.adjust_for_ambient_noise(source, duration=0.2)
                audio_data = recognizer.record(source)
            text = recognizer.recognize_google(audio_data, language="en-IN")
            text = _normalize_transcript(text)
            if text:
                return text, "Google Speech (Fallback)"
        except sr.UnknownValueError:
            return (
                "Error: No speech detected in the audio. Speak clearly for at least 3–5 seconds.",
                "Error",
            )
        except Exception as sr_e:
            sr_error = str(sr_e)
            if "end of input" in sr_error.lower() or "end of file" in sr_error.lower():
                return (
                    "Error: Audio file is empty or unreadable. Use WAV/MP3 with clear speech.",
                    "Error",
                )
            combined = (
                f"Whisper: {whisper_error}; Fallback: {sr_error}"
                if whisper_error
                else sr_error
            )
            return f"Error: Transcription failed ({combined}).", "Error"

        return (
            "Error: No speech detected. Use a longer recording with clear audio.",
            "Error",
        )
    finally:
        if created_wav and wav_path and os.path.isfile(wav_path):
            try:
                os.remove(wav_path)
                parent = os.path.dirname(wav_path)
                if parent and os.path.isdir(parent) and "aegis_audio_" in parent:
                    os.rmdir(parent)
            except OSError:
                pass


def save_uploaded_audio(uploaded_file):
    """Save uploaded file securely into temp_audio (Streamlit / legacy)."""
    temp_dir = "temp_audio"
    os.makedirs(temp_dir, exist_ok=True)

    ext = uploaded_file.name.split(".")[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise ValueError("Invalid file extension. Use WAV, MP3, M4A, OGG, or FLAC.")

    safe_filename = f"{uuid.uuid4().hex}.{ext}"
    file_path = os.path.join(temp_dir, safe_filename)

    with open(file_path, "wb") as f:
        f.write(uploaded_file.getbuffer())
    return file_path
