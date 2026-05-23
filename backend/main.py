from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import os
import io
import hashlib
import random
import time

from preprocess import translate_to_english
from model import predict_text
from utils import detect_scam_keywords, extract_links, flag_suspicious_links
from speech import process_audio
from audio_io import save_upload_async, cleanup_temp_dir
from risk_engine import classify_risk

app = FastAPI(title="Aegis AI Backend")

_origins_raw = os.getenv("ALLOWED_ORIGINS", "*")
_allow_origins = (
    ["*"]
    if _origins_raw.strip() == "*"
    else [o.strip() for o in _origins_raw.split(",") if o.strip()]
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_allow_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ScanRequest(BaseModel):
    text: str


class VishingTranscriptRequest(BaseModel):
    transcript: str
    phone_number: Optional[str] = None


class PhoneOtpSendRequest(BaseModel):
    phone: str
    email: Optional[str] = None


class PhoneOtpVerifyRequest(BaseModel):
    phone: str
    otp: str
    email: Optional[str] = None


# In-memory OTP store (use Redis/Supabase in production)
_phone_otp_store: dict[str, tuple[str, float]] = {}


@app.get("/")
def read_root():
    return {"status": "Aegis AI Backend is running securely."}

@app.get("/api/user/profile")
def get_user_profile(email: str = "guest@example.com", phone: Optional[str] = None):
    hash_obj = hashlib.md5(email.encode())
    hash_int = int(hash_obj.hexdigest(), 16)
    user_id = str(hash_int)[:9]

    if phone and phone.strip():
        display_phone = phone.strip()
    else:
        phone_part = str(hash_int)[9:19]
        if len(phone_part) < 10:
            phone_part = phone_part.ljust(10, "0")
        display_phone = f"(+1) {phone_part[:3]}-{phone_part[3:6]}-{phone_part[6:10]}"

    return {"user_id": user_id, "phone": display_phone}


@app.post("/api/auth/send-phone-otp")
def send_phone_otp(req: PhoneOtpSendRequest):
    phone = (req.phone or "").strip()
    if len(phone) < 10:
        return {"ok": False, "error": "Invalid phone number"}

    code = f"{random.randint(100000, 999999)}"
    _phone_otp_store[phone] = (code, time.time() + 600)

    # TODO: integrate Twilio / MSG91 when credentials are set
    twilio_sid = os.getenv("TWILIO_ACCOUNT_SID")
    if twilio_sid:
        pass  # send_sms(phone, f"Your Aegis AI code is {code}")

    resp = {"ok": True, "message": "OTP sent"}
    if os.getenv("OTP_DEV_MODE", "true").lower() in ("1", "true", "yes"):
        resp["dev_otp"] = code
    return resp


@app.post("/api/auth/verify-phone-otp")
def verify_phone_otp(req: PhoneOtpVerifyRequest):
    phone = (req.phone or "").strip()
    otp = (req.otp or "").strip()
    stored = _phone_otp_store.get(phone)
    if not stored:
        return {"ok": False, "error": "No OTP requested for this number"}
    code, expires = stored
    if time.time() > expires:
        del _phone_otp_store[phone]
        return {"ok": False, "error": "OTP expired"}
    if otp != code:
        return {"ok": False, "error": "Invalid OTP"}
    del _phone_otp_store[phone]
    return {"ok": True, "message": "Phone verified", "phone": phone}

@app.post("/api/scan")
def scan_text(req: ScanRequest):
    try:
        return analyze_text(req.text, scan_links=True)
    except Exception as e:
        return {"error": f"Scan failed: {e}", "risk": "ERROR", "label": "error", "confidence": 0}

@app.post("/api/vishing/analyze-transcript")
def analyze_vishing_transcript(req: VishingTranscriptRequest):
    """Analyze call transcript for vishing (mobile-friendly, no large audio upload)."""
    t = (req.transcript or "").strip()
    if len(t) < 3:
        return {"error": "Transcript too short", "status": 400}
    result = analyze_text(t, scan_links=False)
    result["phone_number"] = req.phone_number
    result["source"] = "vishing_transcript"
    return result


async def _transcribe_upload(file: UploadFile):
    import asyncio

    temp_dir = None
    temp_path = None
    try:
        temp_path, temp_dir = await save_upload_async(file)
        loop = asyncio.get_running_loop()
        audio_result = await loop.run_in_executor(None, process_audio, temp_path)
        text = audio_result[0]
        method = audio_result[1] if len(audio_result) > 1 else ""
        lang = audio_result[2] if len(audio_result) > 2 else ""
        return text, method, lang, None
    except ValueError as e:
        return None, None, None, str(e)
    except Exception as e:
        return None, None, None, f"Audio processing failed: {e}"
    finally:
        if temp_dir:
            cleanup_temp_dir(temp_dir)


@app.post("/api/transcribe-audio")
async def transcribe_audio(file: UploadFile = File(...)):
    """Transcribe only — used by Call Guard before scam analysis."""
    text, method, lang, err = await _transcribe_upload(file)
    if err:
        return {"error": err, "transcription": None, "status": 400}
    if text and not str(text).startswith("Error"):
        out = {"transcription": text, "method": method}
        if lang:
            out["detected_language"] = lang
        return out
    err_msg = text or "No speech detected. Use speakerphone during the call."
    if str(err_msg).startswith("Error:"):
        err_msg = str(err_msg)[6:].strip()
    return {"error": err_msg, "transcription": None, "status": 400}


@app.post("/api/scan-audio")
async def scan_audio(file: UploadFile = File(...)):
    transcribed_text, method, detected_lang, err = await _transcribe_upload(file)
    if err:
        return {
            "error": err,
            "risk": "ERROR",
            "label": "error",
            "confidence": 0,
            "status": 400,
        }

    if transcribed_text and not str(transcribed_text).startswith("Error"):
        try:
            result = analyze_text(transcribed_text, scan_links=False)
        except Exception as e:
            return {
                "error": f"Analysis failed after transcription: {e}",
                "transcription": transcribed_text,
                "method": method,
                "risk": "ERROR",
                "label": "error",
                "confidence": 0,
            }
        result["transcription"] = transcribed_text
        result["method"] = method
        if detected_lang:
            result["detected_language"] = detected_lang
        return result

    err_msg = transcribed_text or "No speech detected in audio."
    if str(err_msg).startswith("Error:"):
        err_msg = str(err_msg)[6:].strip()
    return {
        "error": err_msg,
        "risk": "ERROR",
        "label": "error",
        "confidence": 0,
        "status": 400,
    }

        

def analyze_text(text: str, scan_links=False):
    if not text or not str(text).strip():
        return {"error": "Empty text", "risk": "ERROR", "label": "error", "confidence": 0}

    english_text = translate_to_english(text)
    is_translated = (english_text != text)

    if not english_text or not str(english_text).strip():
        return {"error": "Could not process text", "risk": "ERROR", "label": "error", "confidence": 0}

    prediction_label, confidence = predict_text(english_text)
    detected_keywords = detect_scam_keywords(english_text)

    suspicious_links = []
    if scan_links:
        all_links = extract_links(english_text)
        suspicious_links = flag_suspicious_links(all_links)

    final_status, reason, display_label = classify_risk(
        prediction_label,
        confidence,
        detected_keywords,
        suspicious_links,
        scan_links,
        text,
        english_text,
    )

    return {
        "label": display_label,
        "risk": final_status,
        "confidence": confidence / 100.0,
        "reason": reason,
        "detected_keywords": detected_keywords,
        "suspicious_links": suspicious_links,
        "is_translated": is_translated,
        "english_text": english_text,
    }
