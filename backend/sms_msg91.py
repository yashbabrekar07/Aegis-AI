"""MSG91 OTP SMS delivery for phone verification."""
import os
import re
import logging

import requests

logger = logging.getLogger("aegis.sms")

MSG91_OTP_URL = "https://control.msg91.com/api/v5/otp"
MSG91_FLOW_URL = "https://control.msg91.com/api/v5/flow"


def normalize_phone_key(phone: str) -> str:
    """Canonical key for OTP store — digits only, India numbers as 91XXXXXXXXXX."""
    digits = re.sub(r"\D", "", phone or "")
    if len(digits) == 10:
        return "91" + digits
    if digits.startswith("0") and len(digits) == 11:
        return "91" + digits[1:]
    return digits


def format_mobile_for_msg91(phone: str) -> str:
    """MSG91 expects country code + number without + prefix."""
    return normalize_phone_key(phone)


def send_otp_sms(phone: str, otp: str) -> tuple[bool, str | None]:
    """
    Send OTP via MSG91. Tries OTP API first, then Flow API.
    Returns (success, error_message).
    """
    auth_key = (os.getenv("MSG91_AUTH_KEY") or "").strip()
    template_id = (os.getenv("MSG91_TEMPLATE_ID") or "").strip()
    sender_id = (os.getenv("MSG91_SENDER_ID") or "AEGISAI").strip()

    if not auth_key:
        return False, "MSG91 is not configured on the server."
    if not template_id:
        return False, "MSG91 template ID is missing."

    mobile = format_mobile_for_msg91(phone)
    if len(mobile) < 12 or not mobile.startswith("91"):
        return False, "Enter a valid 10-digit Indian mobile number."

    headers = {
        "authkey": auth_key,
        "Content-Type": "application/json",
        "accept": "application/json",
    }

    # OTP API — pass our generated code into DLT template (##OTP##)
    otp_payload = {
        "template_id": template_id,
        "mobile": mobile,
        "otp": otp,
    }
    ok, err = _post_msg91(MSG91_OTP_URL, headers, otp_payload)
    if ok:
        return True, None

    logger.warning("MSG91 OTP API failed (%s), trying Flow API", err)

    # Flow API fallback — variable names depend on your MSG91 template
    flow_payload = {
        "template_id": template_id,
        "short_url": "0",
        "sender": sender_id,
        "recipients": [
            {
                "mobiles": mobile,
                "OTP": otp,
                "otp": otp,
                "VAR1": otp,
            }
        ],
    }
    ok2, err2 = _post_msg91(MSG91_FLOW_URL, headers, flow_payload)
    if ok2:
        return True, None

    return False, err2 or err or "Could not send OTP SMS."


def _post_msg91(url: str, headers: dict, payload: dict) -> tuple[bool, str | None]:
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=20)
        data = {}
        try:
            data = resp.json() if resp.content else {}
        except Exception:
            data = {"message": resp.text[:200]}

        if resp.status_code in (200, 201):
            if isinstance(data, dict):
                msg_type = str(data.get("type", "")).lower()
                if msg_type == "success" or data.get("request_id") or data.get("message") == "OTP sent successfully":
                    return True, None
                if msg_type == "error":
                    return False, data.get("message") or str(data)
            return True, None

        message = ""
        if isinstance(data, dict):
            message = data.get("message") or data.get("errors") or str(data)
        else:
            message = str(data)
        return False, message or f"MSG91 HTTP {resp.status_code}"
    except requests.RequestException as e:
        logger.exception("MSG91 request failed")
        return False, f"SMS service unreachable: {e}"
