import re
import os
import requests
import base64
from cryptography.fernet import Fernet
from dotenv import load_dotenv
from message_patterns import filter_keywords_for_context, is_legitimate_service_message

load_dotenv()
VT_API_KEY = os.getenv("VIRUSTOTAL_API_KEY")

# --- Encryption Utils ---
# We use cryptography.fernet to encrypt our dummy dataset so it cannot be easily "Ctrl+F"ed by a hacker

KEY_FILE = "secret.key"

def generate_key():
    """Generates a key and saves it into a file"""
    if not os.path.exists(KEY_FILE):
        key = Fernet.generate_key()
        with open(KEY_FILE, "wb") as key_file:
            key_file.write(key)

def load_key():
    """Loads the key from the current directory named `secret.key`"""
    return open(KEY_FILE, "rb").read()

def encrypt_file(filename, output_filename=None):
    """Encrypts a file explicitly"""
    key = load_key()
    f = Fernet(key)
    with open(filename, "rb") as file:
        file_data = file.read()
    encrypted_data = f.encrypt(file_data)
    out = output_filename if output_filename else filename
    with open(out, "wb") as file:
        file.write(encrypted_data)

def decrypt_file_to_memory(filename):
    """Decrypts a file and returns its content in memory as string"""
    key = load_key()
    f = Fernet(key)
    with open(filename, "rb") as file:
        encrypted_data = file.read()
    decrypted_data = f.decrypt(encrypted_data)
    return decrypted_data.decode('utf-8')

# --- Keyword & Link Extraction Utils ---

# High-confidence scam phrases (substring match)
HIGH_RISK_PHRASES = [
    "share your otp", "send otp", "send your otp", "tell me your otp",
    "reply with otp", "forward the otp", "verify now", "click here immediately",
    "you have won", "lottery winner", "claim your prize", "claim prize",
    "free money", "act now", "limited time offer", "gift card",
    "verification fee", "pay to unlock", "pay to open account",
    "enter pin to receive", "scan to receive payment", "qr code verification",
    "cashback claim", "refund failure", "merchant payment failed",
    "account suspended", "account blocked", "legal action",
    "customs seized", "arrest warrant", "press 1 to",
    "remote access", "anydesk", "teamviewer",
    "bitcoin transfer", "crypto wallet",
    "bank account details", "share your pin", "share your cvv",
    "transfer immediately", "wire transfer urgently",
]

# Secondary signals — only counted when paired with another signal or strong ML
CONTEXT_KEYWORDS = [
    "kyc update", "update kyc", "verify account", "verify identity",
    "urgent action", "immediate action", "winner", "congratulations you won",
]

SUSPICIOUS_DOMAINS = ["bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd"]

def detect_scam_keywords(text):
    """Detect high-risk scam phrases; skip legitimate OTP/transactional messages."""
    if not text or not str(text).strip():
        return []
    if is_legitimate_service_message(text):
        return []

    text_lower = text.lower()
    detected = []

    for phrase in HIGH_RISK_PHRASES:
        if phrase in text_lower:
            detected.append(phrase)

    # "share otp" only when NOT a bank warning (never/do not share)
    if re.search(r"share\s+(your\s+)?otp", text_lower):
        is_bank_warning = bool(re.search(
            r"(never share|do not share|don't share|not share|never ask|do not disclose).{0,20}otp|"
            r"otp.{0,40}(never share|do not share|don't share|not share|confidential)",
            text_lower,
        ))
        if not is_bank_warning and "share otp" not in detected and "share your otp" not in detected:
            detected.append("share otp")

    for phrase in CONTEXT_KEYWORDS:
        if phrase in text_lower and phrase not in detected:
            detected.append(phrase)

    # Standalone "otp" only when message asks for it (not official notification)
    if "otp" in text_lower and not detected:
        asks_for_otp = re.search(
            r"(share|send|reply|forward|tell|give|enter|provide|bhejo|bhej).{0,25}otp|"
            r"otp.{0,25}(share|send|reply|forward|bhejo|bhej)",
            text_lower,
        )
        is_bank_warning = re.search(
            r"(never share|do not share|don't share|not share).{0,20}otp|"
            r"otp.{0,40}(never share|do not share|don't share|valid for)",
            text_lower,
        )
        if asks_for_otp and not is_bank_warning:
            detected.append("otp request")

    return filter_keywords_for_context(text, detected)

def extract_links(text):
    """Extract URLs from text using regex."""
    url_pattern = re.compile(r'https?://(?:[-\w.]|(?:%[\da-fA-F]{2}))+')
    links = url_pattern.findall(text)
    return links

def flag_suspicious_links(links):
    """Check if links belong to known suspicious domains / link shorteners, or flag via VirusTotal."""
    suspicious = []
    
    # Fast check known dangerous/shortened domains
    for link in links:
        if any(domain in link for domain in SUSPICIOUS_DOMAINS):
            if link not in suspicious:
                suspicious.append(link)
                
    # Use VirusTotal if API key is provided and links exist
    if VT_API_KEY and links:
        headers = {"x-apikey": VT_API_KEY}
        for link in links:
            if link in suspicious:
                continue
            
            try:
                # VirusTotal requires URL-safe base64 encoded strings without padding
                url_id = base64.urlsafe_b64encode(link.encode()).decode().strip("=")
                vt_url = f"https://www.virustotal.com/api/v3/urls/{url_id}"
                
                resp = requests.get(vt_url, headers=headers)
                if resp.status_code == 200:
                    data = resp.json()
                    analysis = data.get("data", {}).get("attributes", {}).get("last_analysis_stats", {})
                    if analysis.get("malicious", 0) > 0 or analysis.get("suspicious", 0) > 1:
                        suspicious.append(link)
            except Exception as e:
                print(f"VT API Error for {link}: {e}")
                
    return suspicious

# --- Social Engineering Susceptibility Utils ---

def calculate_susceptibility_score(scores):
    """
    scores: list of 7 risk values chosen in the survey.
    Returns: Score 0-100 and level (str)
    """
    total_risk = sum(scores)
    
    # max possible from new questions: 30(q1) + 40(q2) + 40(q3) + 30(q4) + 30(q5) + 30(q6) + 30(q7) = 230
    max_possible_risk = 230 
    
    risk_score = (total_risk / max_possible_risk) * 100
    
    if risk_score <= 15:
        level = "LOW RISK"
    elif risk_score <= 40:
        level = "MEDIUM RISK"
    elif risk_score <= 75:
        level = "HIGH RISK"
    else:
        level = "CRITICAL RISK"
        
    return int(risk_score), level
