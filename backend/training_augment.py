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


def augment_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    rows = [{"text": t, "label": "safe"} for t in SHORT_SAFE_TEXTS]
    rows += [{"text": t, "label": "phishing"} for t in SHORT_SCAM_TEXTS]
    extra = pd.DataFrame(rows)
    combined = pd.concat([df, extra], ignore_index=True)
    return combined.drop_duplicates(subset=["text"], keep="first")
