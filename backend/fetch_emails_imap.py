import imaplib
import email
from email.header import decode_header
import sys

# Import our AI model
from main import analyze_text

def fetch_and_scan_emails(username, app_password):
    # Connect to the Gmail IMAP server
    try:
        mail = imaplib.IMAP4_SSL("imap.gmail.com")
        mail.login(username, app_password)
    except Exception as e:
        print(f"Failed to login: {e}")
        return

    # Select the mailbox (Inbox)
    mail.select("inbox")

    # Search for all emails
    status, messages = mail.search(None, "ALL")
    email_ids = messages[0].split()

    if not email_ids:
        print("No emails found.")
        return

    # Fetch the last 5 emails
    latest_email_ids = email_ids[-5:]

    for e_id in latest_email_ids:
        # Fetch the email by ID
        res, msg_data = mail.fetch(e_id, "(RFC822)")
        for response_part in msg_data:
            if isinstance(response_part, tuple):
                msg = email.message_from_bytes(response_part[1])
                
                # Decode subject
                subject, encoding = decode_header(msg["Subject"])[0]
                if isinstance(subject, bytes):
                    subject = subject.decode(encoding if encoding else "utf-8")
                
                # Get the email body
                body = ""
                if msg.is_multipart():
                    for part in msg.walk():
                        content_type = part.get_content_type()
                        content_disposition = str(part.get("Content-Disposition"))
                        if content_type == "text/plain" and "attachment" not in content_disposition:
                            body = part.get_payload(decode=True).decode("utf-8", errors="ignore")
                            break
                else:
                    body = msg.get_payload(decode=True).decode("utf-8", errors="ignore")

                print(f"--- Scanning Email: {subject} ---")
                
                # Combine subject and body and pass to our AI Model
                full_text = f"{subject}\n{body}"
                result = analyze_text(full_text, scan_links=True)
                
                print(f"Result: {result['risk']} ({result['confidence']*100}% confidence)")
                print(f"Reason: {result['reason']}\n")

    mail.close()
    mail.logout()

if __name__ == "__main__":
    print("Email Scanner Tool")
    print("You must use a Google 'App Password' if you use Gmail with 2FA enabled.")
    user_email = input("Enter your email: ")
    user_pass = input("Enter your App Password: ")
    fetch_and_scan_emails(user_email, user_pass)
