import os
import uuid

try:
    import imageio_ffmpeg
    import shutil
    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    ffmpeg_dir = os.path.dirname(ffmpeg_exe)
    
    # Whisper explicitly looks for "ffmpeg.exe" in the PATH, but imageio_ffmpeg 
    # names it something like "ffmpeg-win-x86_64-v7.1.exe". 
    # We must create a copy named exactly "ffmpeg.exe" so Whisper can find it.
    target_exe = os.path.join(ffmpeg_dir, "ffmpeg.exe")
    if not os.path.exists(target_exe):
        shutil.copy(ffmpeg_exe, target_exe)
        
    if ffmpeg_dir not in os.environ.get("PATH", ""):
        os.environ["PATH"] += os.pathsep + ffmpeg_dir
except Exception as e:
    print(f"Failed to setup ffmpeg: {e}")

ALLOWED_EXTENSIONS = {'wav', 'mp3', 'm4a', 'ogg', 'flac'}
_whisper_model = None

def process_audio(audio_file_path):
    """
    Converts audio file to text using Whisper directly.
    We fallback to standard Google Web Speech API if Whisper fails.
    """
    global _whisper_model
    
    try:
        import whisper
        import warnings
        warnings.filterwarnings("ignore")
        if _whisper_model is None:
            # Switched from "base.en" (English only) to "base" (Multilingual)
            _whisper_model = whisper.load_model("base")
            
        # fp16=False is CRITICAL for preventing silent freezing on non-CUDA CPUs
        # task="translate" forces Whisper to detect Hindi/Marathi and output English text
        result = _whisper_model.transcribe(audio_file_path, fp16=False, task="translate")
        return result["text"], "Whisper (Multilingual)"
            
    except Exception as whisper_err:
        import speech_recognition as sr
        try:
            recognizer = sr.Recognizer()
            with sr.AudioFile(audio_file_path) as source:
                recognizer.adjust_for_ambient_noise(source)
                audio_data = recognizer.record(source)
                text = recognizer.recognize_google(audio_data)
                return text, "Google Web API (Fallback)"
        except Exception as sr_e:
            return f"Error: Whisper failed ({str(whisper_err)}) AND Google fallback failed ({str(sr_e)}). Ensure ffmpeg is installed for non-WAV files.", "Error"

def save_uploaded_audio(uploaded_file):
    """Save Streamlit uploaded file securely into an isolated temp dir, blocking path traversal."""
    temp_dir = "temp_audio"
    os.makedirs(temp_dir, exist_ok=True)
    
    # Verify extension securely
    ext = uploaded_file.name.split('.')[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise ValueError("Invalid file extension detected. Access blocked.")
        
    # Generate isolated UUID
    safe_filename = f"{uuid.uuid4().hex}.{ext}"
    file_path = os.path.join(temp_dir, safe_filename)
    
    with open(file_path, "wb") as f:
        f.write(uploaded_file.getbuffer())
    return file_path
