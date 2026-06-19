"""Extra training rows for short legitimate messages (names, greetings) — reduces false positives."""
import pandas as pd

# Names, greetings, casual chat — must stay safe
SHORT_SAFE_TEXTS = [
    "Ayush",
    "Yash",
    "Rahul",
    "Priya",
    "Ananya",
    "Vikram",
    "Neha",
    "Arjun",
    "Hi",
    "Hello",
    "Hey",
    "OK",
    "Okay",
    "Thanks",
    "Thank you",
    "Namaste",
    "Good morning",
    "Good night",
    "See you tomorrow",
    "On my way",
    "Reached safely",
    "Call me later",
    "Yes",
    "No problem",
    "Got it",
    "Sure",
    "How are you",
    "I am fine",
    "Happy birthday",
    "Congratulations",
    "Well done",
    "Meeting at 3pm",
    "Lunch tomorrow?",
    "नमस्ते",
    "ठीक है",
    "धन्यवाद",
    "कल मिलते हैं",
    "मी बरोबर आहे",
    "धन्यवाद भाऊ",
    "நன்றி",
    "நாளை சந்திப்போம்",
    "ధన్యవాదాలు",
    "రేపు కలుద్దాం",
]

# Clearly scam — short but high signal (for contrast)
SHORT_SCAM_TEXTS = [
    "Send OTP now",
    "Share your UPI PIN",
    "Your bank account blocked verify link",
    "You won lottery claim prize click",
    "KYC expired update Aadhar immediately",
    "अपना OTP भेजें",
    "UPI block turant verify karo",
]

# Legitimate carrier / telecom daily messages — reduce false positives
TELECOM_SAFE_TEXTS = [
    "Alert!! 50% of daily high speed data is consumed. Get 3GB per day for 3 days at Rs. 39. Recharge now i.airtel.in/dtpck-pre",
    "Dear Customer, 90% of your daily data is used. Recharge with Rs. 299 for 1.5GB/day. Validity 28 days. -AIRTEL",
    "Hi! Your Jio number data balance is 500MB. Recharge now at jio.com/rch",
    "Vi Alert: Your postpaid bill of Rs.599 is due on 25th. Pay now to avoid disconnection.",
    "BSNL: Your plan validity expires tomorrow. Recharge Rs.99 for 28 days.",
    "Airtel Thanks! Recharge of Rs.299 successful. 1.5GB/day, 28 days validity.",
    "Jio: Your data pack will expire in 2 days. Renew your plan now.",
    "Alert: 80% daily data consumed. Add 2GB at Rs.25. i.airtel.in/data",
    "Vi: Special offer! Get unlimited calls + 2GB/day at Rs.349. Limited time.",
    "Your prepaid balance is low. Recharge now to continue services.",
    "Data usage alert: You have used 1.2GB of 1.5GB daily limit.",
    "Plan activated: Unlimited 5G data for 28 days. Enjoy!",
    # Hindi / Hinglish carrier
    "Aapka daily data 90% use ho chuka hai. Recharge karein abhi.",
    "Aapke number par data khatam ho gaya hai. Recharge karo jio.com par.",
    "Aapka pack kal expire hoga. Recharge kar lo.",
    # Marathi carrier
    "तुमचा दैनिक डेटा ८०% वापरला आहे. रिचार्ज करा.",
    "तुमच्या नंबरवर डेटा संपला आहे. लवकर रिचार्ज करा.",
]


def augment_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    rows = [{"text": t, "label": "safe"} for t in SHORT_SAFE_TEXTS]
    rows += [{"text": t, "label": "safe"} for t in TELECOM_SAFE_TEXTS]
    rows += [{"text": t, "label": "phishing"} for t in SHORT_SCAM_TEXTS]
    extra = pd.DataFrame(rows)
    combined = pd.concat([df, extra], ignore_index=True)
    return combined.drop_duplicates(subset=["text"], keep="first")
