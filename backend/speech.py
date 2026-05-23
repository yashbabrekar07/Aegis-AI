import os
import uuid
import shutil
import subprocess
import tempfile

ALLOWED_EXTENSIONS = {"wav", "mp3", "m4a", "ogg", "flac", "webm", "aac", "3gp", "amr"}
MIN_AUDIO_BYTES = 1_000
CALL_GUARD_MIN_PCM_MS = 500  # minimum ~0.5s of audio
_whisper_model = None

# Google Speech language hints for Indian languages (fallback chain)
_GOOGLE_LANG_CHAIN = ["hi-IN", "mr-IN", "ta-IN", "te-IN", "en-IN", "en-US"]


def _ensure_ffmpeg_on_path():
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


def _wav_duration_ms(path: str) -> int:
    try:
        import wave

        with wave.open(path, "rb") as w:
            frames = w.getnframes()
            rate = w.getframerate() or 16000
            return int(frames * 1000 / rate)
    except Exception:
        return 0


def validate_audio_file(path: str) -> None:
    if not os.path.isfile(path):
        raise ValueError("Audio file was not saved correctly. Please try again.")
    size = os.path.getsize(path)
    if size < MIN_AUDIO_BYTES:
        raise ValueError(
            "Audio is too short or empty. Record at least 3–5 seconds of clear speech "
            "(use speakerphone for calls)."
        )
    if path.lower().endswith(".wav"):
        dur = _wav_duration_ms(path)
        if dur < CALL_GUARD_MIN_PCM_MS:
            raise ValueError(
                f"Recording is only {dur}ms long. Use speakerphone and speak for at least 5 seconds."
            )


def trim_wav_max_seconds(wav_path: str, max_seconds: int = 60) -> str:
    """Keep first N seconds — Call Guard clips stay under Render/time limits."""
    dur_ms = _wav_duration_ms(wav_path)
    if dur_ms <= 0 or dur_ms <= max_seconds * 1000:
        return wav_path
    out_dir = tempfile.mkdtemp(prefix="aegis_trim_")
    trimmed = os.path.join(out_dir, f"{uuid.uuid4().hex}.wav")
    cmd = [
        _ffmpeg_executable(),
        "-y",
        "-i",
        wav_path,
        "-t",
        str(max_seconds),
        "-ar",
        "16000",
        "-ac",
        "1",
        trimmed,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    if proc.returncode == 0 and os.path.isfile(trimmed) and os.path.getsize(trimmed) > MIN_AUDIO_BYTES:
        return trimmed
    return wav_path


def convert_to_wav(input_path: str) -> str:
    ext = os.path.splitext(input_path)[1].lower().lstrip(".")
    if ext == "wav":
        validate_audio_file(input_path)
        return input_path

    out_dir = tempfile.mkdtemp(prefix="aegis_audio_")
    wav_path = os.path.join(out_dir, f"{uuid.uuid4().hex}.wav")
    cmd = [
        _ffmpeg_executable(),
        "-y",
        "-err_detect",
        "ignore_err",
        "-i",
        input_path,
        "-ar",
        "16000",
        "-ac",
        "1",
        "-vn",
        wav_path,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if proc.returncode != 0 or not os.path.isfile(wav_path) or os.path.getsize(wav_path) < MIN_AUDIO_BYTES:
        err = (proc.stderr or proc.stdout or "").strip()
        if "end of file" in err.lower() or "end of input" in err.lower():
            raise ValueError(
                "Recording was empty or cut off. Turn on speakerphone during the call."
            )
        raise ValueError(f"Could not read audio file. {(err or '')[-400:]}")
    validate_audio_file(wav_path)
    return wav_path


def _normalize_transcript(text: str) -> str:
    t = (text or "").strip()
    if not t:
        return ""
    noise = {
        "you",
        ".",
        "...",
        "thank you.",
        "thanks for watching.",
        "subtitle",
        "music",
        "[music]",
        "(music)",
    }
    if t.lower() in noise or len(t) < 2:
        return ""
    return t


def _whisper_transcribe(wav_path: str) -> tuple[str, str, str]:
    """
    Multilingual path: translate to English first (Hindi/Marathi/Tamil/Telugu),
    then transcribe in native script if translate is empty.
    Returns (english_text, method_label, detected_language).
    """
    global _whisper_model
    import whisper
    import warnings

    warnings.filterwarnings("ignore")
    if _whisper_model is None:
        model_name = os.getenv("WHISPER_MODEL", "tiny")
        _whisper_model = whisper.load_model(model_name)

    # 1) Translate → English (best for scam detection pipeline)
    tr = _whisper_model.transcribe(
        wav_path,
        fp16=False,
        task="translate",
        language=None,
        temperature=0.0,
        condition_on_previous_text=False,
    )
    lang = tr.get("language") or ""
    en = _normalize_transcript(tr.get("text", ""))
    if en and len(en) >= 3:
        return en, "Whisper (multilingual → English)", lang

    # 2) Native script transcription
    tr2 = _whisper_model.transcribe(
        wav_path,
        fp16=False,
        task="transcribe",
        language=None,
        temperature=0.0,
        condition_on_previous_text=False,
    )
    lang2 = tr2.get("language") or lang
    native = _normalize_transcript(tr2.get("text", ""))
    if native:
        from preprocess import translate_to_english

        english = translate_to_english(native)
        if english and english.strip():
            return english.strip(), f"Whisper ({lang2 or 'auto'}) + translation", lang2
        return native, f"Whisper ({lang2 or 'auto'})", lang2

    return "", "", lang2 or lang


def _google_multilingual(wav_path: str) -> tuple[str, str]:
    import speech_recognition as sr

    recognizer = sr.Recognizer()
    recognizer.energy_threshold = 200
    recognizer.dynamic_energy_threshold = True
    with sr.AudioFile(wav_path) as source:
        recognizer.adjust_for_ambient_noise(source, duration=0.3)
        audio_data = recognizer.record(source)

    for lang_code in _GOOGLE_LANG_CHAIN:
        try:
            text = recognizer.recognize_google(audio_data, language=lang_code)
            text = _normalize_transcript(text)
            if text:
                if lang_code.startswith("en"):
                    return text, f"Google Speech ({lang_code})"
                from preprocess import translate_to_english

                en = translate_to_english(text)
                return (en or text).strip(), f"Google Speech ({lang_code}) + translation"
        except sr.UnknownValueError:
            continue
        except sr.RequestError:
            break
    return "", ""


def process_audio(audio_file_path: str, fast: bool = False):
    """
    Returns (transcript_text, method, detected_language).
    fast=True: trim clip, Google Speech first (Call Guard / quick scans on Render free tier).
    """
    wav_path = None
    created_wav = False
    trimmed_path = None
    whisper_error = ""

    try:
        validate_audio_file(audio_file_path)
        wav_path = convert_to_wav(audio_file_path)
        created_wav = wav_path != audio_file_path

        if fast:
            max_sec = int(os.getenv("CALL_GUARD_MAX_AUDIO_SEC", "60"))
            trimmed_path = trim_wav_max_seconds(wav_path, max_sec)
            work_path = trimmed_path

            text, method = _google_multilingual(work_path)
            if text:
                return text, method, ""

            allow_whisper = os.getenv("CALL_GUARD_WHISPER_FALLBACK", "0").lower() in (
                "1",
                "true",
                "yes",
            )
            if allow_whisper:
                try:
                    text, method, lang = _whisper_transcribe(work_path)
                    if text:
                        return text, method, lang
                except Exception as e:
                    whisper_error = str(e)

            return (
                "Error: No speech detected in the call clip. Keep speakerphone on and talk for at least 5 seconds.",
                "Error",
                "",
            )

        # Standard path: Google first (faster), Whisper fallback (better for long/multilingual uploads)
        try:
            text, method = _google_multilingual(wav_path)
            if text:
                return text, method, ""
        except Exception as sr_e:
            sr_error = str(sr_e)
            if "end of input" in sr_error.lower() or "end of file" in sr_error.lower():
                return (
                    "Error: Recording was empty or cut off. Use speakerphone during the call.",
                    "Error",
                    "",
                )

        try:
            text, method, lang = _whisper_transcribe(wav_path)
            if text:
                return text, method, lang
        except Exception as e:
            whisper_error = str(e)

        return (
            "Error: No speech detected. Speak clearly for at least 3–5 seconds (speakerphone helps).",
            "Error",
            "",
        )
    finally:
        for path, is_temp in [(trimmed_path, True), (wav_path, created_wav)]:
            if is_temp and path and os.path.isfile(path):
                try:
                    parent = os.path.dirname(path)
                    os.remove(path)
                    if parent and os.path.isdir(parent) and "aegis_" in parent:
                        os.rmdir(parent)
                except OSError:
                    pass


def save_uploaded_audio(uploaded_file):
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
