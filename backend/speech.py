import os
import uuid
import shutil
import subprocess
import tempfile

ALLOWED_EXTENSIONS = {"wav", "mp3", "m4a", "ogg", "flac", "webm", "aac", "3gp", "amr"}
MIN_AUDIO_BYTES = 2_000
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


def validate_audio_file(path: str) -> None:
    if not os.path.isfile(path):
        raise ValueError("Audio file was not saved correctly. Please try again.")
    size = os.path.getsize(path)
    if size < MIN_AUDIO_BYTES:
        raise ValueError(
            "Audio is too short or empty. Record at least 3–5 seconds of clear speech "
            "(use speakerphone for calls)."
        )


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
                "Audio file appears truncated or corrupt. Re-export as WAV/MP3 or record again."
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


def process_audio(audio_file_path: str):
    """
    Returns (transcript_text, method, detected_language).
    """
    wav_path = None
    created_wav = False
    whisper_error = ""

    try:
        validate_audio_file(audio_file_path)
        wav_path = convert_to_wav(audio_file_path)
        created_wav = wav_path != audio_file_path

        try:
            text, method, lang = _whisper_transcribe(wav_path)
            if text:
                return text, method, lang
        except Exception as e:
            whisper_error = str(e)

        try:
            text, method = _google_multilingual(wav_path)
            if text:
                return text, method, ""
        except Exception as sr_e:
            sr_error = str(sr_e)
            if "end of input" in sr_error.lower():
                return (
                    "Error: Audio file is empty or unreadable. Record 3–5 seconds with clear speech.",
                    "Error",
                    "",
                )
            combined = f"Whisper: {whisper_error}; Google: {sr_error}" if whisper_error else sr_error
            return f"Error: Transcription failed ({combined}).", "Error", ""

        return (
            "Error: No speech detected. Speak clearly for at least 3–5 seconds (speakerphone helps).",
            "Error",
            "",
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
