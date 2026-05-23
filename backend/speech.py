import os
import uuid
import shutil
import subprocess
import tempfile
import struct

ALLOWED_EXTENSIONS = {"wav", "mp3", "m4a", "ogg", "flac", "webm", "aac", "3gp", "amr"}
MIN_AUDIO_BYTES = 1_000
CALL_GUARD_MIN_PCM_MS = 400
_whisper_model = None

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


def _wav_peak_amplitude(path: str) -> int:
    """Max abs 16-bit sample value in WAV."""
    try:
        import wave

        peak = 0
        with wave.open(path, "rb") as w:
            frames = w.readframes(w.getnframes())
        for i in range(0, len(frames) - 1, 2):
            sample = struct.unpack("<h", frames[i : i + 2])[0]
            peak = max(peak, abs(sample))
        return peak
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


def amplify_wav(wav_path: str, gain_db: float = 18.0) -> str:
    """Boost quiet phone-call recordings before speech recognition."""
    out_dir = tempfile.mkdtemp(prefix="aegis_amp_")
    out = os.path.join(out_dir, f"{uuid.uuid4().hex}.wav")
    cmd = [
        _ffmpeg_executable(),
        "-y",
        "-i",
        wav_path,
        "-af",
        f"volume={gain_db}dB,highpass=f=80",
        "-ar",
        "16000",
        "-ac",
        "1",
        out,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=90)
    if proc.returncode == 0 and os.path.isfile(out) and os.path.getsize(out) > MIN_AUDIO_BYTES:
        return out
    return wav_path


def trim_wav_max_seconds(wav_path: str, max_seconds: int = 60) -> str:
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
        "thank you.",
        "thanks for watching.",
        "subtitle",
        "[music]",
        "(music)",
    }
    if t.lower() in noise:
        return ""
    if len(t) < 1:
        return ""
    return t


def _whisper_transcribe(wav_path: str, max_seconds: int | None = None) -> tuple[str, str, str]:
    global _whisper_model
    import whisper
    import warnings

    warnings.filterwarnings("ignore")
    if _whisper_model is None:
        model_name = os.getenv("WHISPER_MODEL", "tiny")
        _whisper_model = whisper.load_model(model_name)

    work = trim_wav_max_seconds(wav_path, max_seconds) if max_seconds else wav_path

    tr = _whisper_model.transcribe(
        work,
        fp16=False,
        task="translate",
        language=None,
        temperature=0.0,
        condition_on_previous_text=False,
        no_speech_threshold=0.35,
    )
    lang = tr.get("language") or ""
    en = _normalize_transcript(tr.get("text", ""))
    if en:
        return en, "Whisper (multilingual → English)", lang

    tr2 = _whisper_model.transcribe(
        work,
        fp16=False,
        task="transcribe",
        language=None,
        temperature=0.0,
        condition_on_previous_text=False,
        no_speech_threshold=0.35,
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


def _google_multilingual(wav_path: str, for_call_guard: bool = False) -> tuple[str, str]:
    import speech_recognition as sr

    recognizer = sr.Recognizer()
    if for_call_guard:
        recognizer.energy_threshold = 80
        recognizer.dynamic_energy_threshold = False
        recognizer.pause_threshold = 0.6
    else:
        recognizer.energy_threshold = 200
        recognizer.dynamic_energy_threshold = True

    with sr.AudioFile(wav_path) as source:
        if not for_call_guard:
            recognizer.adjust_for_ambient_noise(source, duration=0.2)
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


def _no_speech_message(peak: int, dur_ms: int) -> str:
    if peak < 300:
        return (
            f"Recording looks silent (peak={peak}, {dur_ms // 1000}s). "
            "Many phones block the mic during cellular calls — use speakerphone, or paste a transcript in Vishing."
        )
    return (
        f"No speech detected ({dur_ms // 1000}s audio). Speak clearly on speakerphone, "
        "or paste what was said in Vishing → Analyze transcript."
    )


def process_audio(audio_file_path: str, fast: bool = False):
    wav_path = None
    created_wav = False
    trimmed_path = None
    amplified_path = None
    whisper_error = ""

    try:
        validate_audio_file(audio_file_path)
        wav_path = convert_to_wav(audio_file_path)
        created_wav = wav_path != audio_file_path

        if fast:
            max_sec = int(os.getenv("CALL_GUARD_MAX_AUDIO_SEC", "90"))
            trimmed_path = trim_wav_max_seconds(wav_path, max_sec)
            amplified_path = amplify_wav(trimmed_path, gain_db=20.0)
            work_path = amplified_path

            dur_ms = _wav_duration_ms(work_path)
            peak = _wav_peak_amplitude(work_path)
            whisper_sec = int(os.getenv("CALL_GUARD_WHISPER_SEC", "45"))
            use_whisper = os.getenv("CALL_GUARD_WHISPER_FALLBACK", "true").lower() in (
                "1",
                "true",
                "yes",
            )

            try:
                text, method = _google_multilingual(work_path, for_call_guard=True)
                if text:
                    return text, method, ""
            except Exception as e:
                whisper_error = str(e)

            if use_whisper:
                try:
                    text, method, lang = _whisper_transcribe(work_path, max_seconds=whisper_sec)
                    if text:
                        return text, method, lang
                except Exception as e:
                    if not whisper_error:
                        whisper_error = str(e)

            msg = _no_speech_message(peak, dur_ms)
            if whisper_error:
                msg += f" (engine: {whisper_error[:120]})"
            return f"Error: {msg}", "Error", ""

        try:
            text, method = _google_multilingual(wav_path)
            if text:
                return text, method, ""
        except Exception:
            pass

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
        for path, is_temp in [(amplified_path, True), (trimmed_path, True), (wav_path, created_wav)]:
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
