# ISDD: Indian Scam Detection Dataset
## Schema, Annotation Guidelines & Data Collection Protocol

**Dataset Name:** Indian Scam Detection Dataset (ISDD)  
**Version:** 1.0  
**Created:** May 2026  
**Curator:** Ayush More (VIT Pune)  
**License:** CC-BY-4.0 (for academic research)

---

## 1. DATASET OVERVIEW

### **Purpose**
Create the first comprehensive machine learning dataset for detecting phishing, fraud, and scam messages in Indian languages and English across SMS, WhatsApp, and Email channels.

### **Key Statistics**
```
Total Messages:          8,000+
├── SMS Messages:        5,000+
├── WhatsApp Messages:   3,000+
└── Email Messages:      500

Languages Covered:       5
├── Hindi (Devanagari):  3,000+ messages
├── Marathi:             2,000+ messages
├── Hinglish (mixed):    2,000+ messages
├── Tamil:               500+ messages
└── Telugu:              500+ messages

Scam Categories:         6 primary types
├── UPI Fraud:           2,000 messages
├── KYC Fraud:           1,500 messages
├── Fake Job Offers:     1,500 messages
├── Lottery/Prize:       1,000 messages
├── Bank Impersonation:  1,000 messages
└── OTP/Password Theft:  1,000 messages

Label Distribution:
├── Scam Messages:       50% (4,000)
└── Legitimate Messages: 50% (4,000)

Languages:
├── Hindi:              35%
├── Marathi:            25%
├── Hinglish:           25%
├── Tamil:              10%
└── Telugu:             5%
```

---

## 2. DATA SCHEMA & STRUCTURE

### **A. Database Structure (JSON Format)**

```json
{
  "message_id": "ISDD_SMS_00001",
  "timestamp": "2026-05-12T14:30:00Z",
  "channel": "SMS",
  "scam_label": 1,
  "scam_type": "UPI_FRAUD",
  "confidence": 0.95,
  "language": "hindi",
  "message_content": {
    "original": "आपका UPI ब्लॉक हो गया है। तुरंत यह लिंक खोलें: http://fake-link.in",
    "transliteration_roman": "Aapka UPI block ho gaya hai. Turant yeh link kholein: http://fake-link.in",
    "english_translation": "Your UPI has been blocked. Open this link immediately: http://fake-link.in"
  },
  "metadata": {
    "sender_country_code": "91",
    "sender_type": "unknown",
    "sender_pattern": "spoofed",
    "message_length": 87,
    "contains_url": true,
    "contains_urgency_words": true,
    "contains_threat_words": true,
    "contains_financial_keywords": true
  },
  "annotations": {
    "scam_indicators": [
      "urgent_language",
      "account_threat",
      "suspicious_link",
      "impersonation",
      "banking_impersonation"
    ],
    "target_entity": "GOOGLE_PAY/PAYTM/PHONEPE",
    "attack_vector": "social_engineering",
    "risk_severity": "high",
    "annotator_id": "ANNOT_001",
    "annotation_timestamp": "2026-05-12T15:00:00Z",
    "confidence_score": 0.95,
    "notes": "Classic UPI block scam targeting Google Pay users"
  },
  "features": {
    "tfidf_keywords": [
      {"term": "UPI", "score": 0.85},
      {"term": "block", "score": 0.82},
      {"term": "turant", "score": 0.78},
      {"term": "link", "score": 0.72}
    ],
    "urgency_score": 0.92,
    "impersonation_score": 0.88,
    "linguistic_anomalies": [
      "grammatical_error",
      "non_standard_hindi"
    ]
  }
}
```

### **B. CSV Format (For Easy Use)**

```csv
message_id,timestamp,channel,label,scam_type,language,message_text_original,message_text_english,sender_type,contains_url,urgency_words,threat_words,financial_keywords,confidence,annotator_id,notes

ISDD_SMS_00001,2026-05-12T14:30:00Z,SMS,1,UPI_FRAUD,hindi,आपका UPI ब्लॉक हो गया है। तुरंत यह लिंक खोलें,Your UPI has been blocked. Open this link immediately,unknown,TRUE,TRUE,TRUE,TRUE,0.95,ANNOT_001,Classic UPI block scam

ISDD_WHATSAPP_00001,2026-05-12T15:00:00Z,WHATSAPP,1,KYC_FRAUD,marathi,आपले आधार KYC अद्यतन करा. लिंक: http://kyc-fake.in,Update your Aadhar KYC. Link: http://kyc-fake.in,unknown,TRUE,TRUE,FALSE,TRUE,0.92,ANNOT_002,Aadhar KYC scam

ISDD_EMAIL_00001,2026-05-12T15:30:00Z,EMAIL,1,BANK_IMPERSONATION,hinglish,Dear Customer, Your ICICI account verify kijiye: link.icici-verify.com,Dear Customer - Verify your ICICI account,spoofed_bank,TRUE,TRUE,TRUE,TRUE,0.98,ANNOT_001,Email spoofing ICICI bank
```

### **C. Data File Structure**

```
ISDD_Dataset/
├── README.md                          # Overview and usage guide
├── SCHEMA.md                          # This file
├── LICENSE                            # CC-BY-4.0
├── raw/
│   ├── SMS/
│   │   ├── sms_hindi_raw.json        # 1,500+ Hindi SMS
│   │   ├── sms_marathi_raw.json      # 1,000+ Marathi SMS
│   │   ├── sms_hinglish_raw.json     # 1,500+ Hinglish SMS
│   │   └── sms_english_raw.json      # 1,000 English SMS
│   ├── WHATSAPP/
│   │   ├── whatsapp_hindi_raw.json
│   │   ├── whatsapp_marathi_raw.json
│   │   └── whatsapp_hinglish_raw.json
│   └── EMAIL/
│       ├── email_english_raw.json
│       └── email_hindi_raw.json
├── processed/
│   ├── ISDD_combined_v1.0.csv        # All messages combined
│   ├── ISDD_train_split.csv          # 70% for training
│   ├── ISDD_test_split.csv           # 15% for testing
│   ├── ISDD_validation_split.csv     # 15% for validation
│   └── ISDD_balanced.csv             # Balanced version
├── annotations/
│   ├── annotation_guidelines.md       # How to annotate
│   ├── annotator_guide.pdf           # Quick reference
│   └── scam_taxonomy.json            # Category definitions
├── analysis/
│   ├── dataset_statistics.json       # Stats and distributions
│   ├── language_distribution.csv     # Messages per language
│   ├── scam_type_distribution.csv    # Messages per scam type
│   └── linguistic_analysis.json      # Language patterns
└── utilities/
    ├── load_dataset.py               # Python loader
    ├── validation_script.py           # Data validation
    └── preprocessing.py              # Cleaning & preprocessing
```

---

## 3. ANNOTATION GUIDELINES (CRITICAL)

### **A. Primary Label: Scam vs Legitimate**

#### **Label 1: SCAM (Label=1)**
Message is attempting fraud/phishing/social engineering.

**Examples:**
```
Hindi:    "आपका बैंक खाता ब्लॉक हो गया। तुरंत अपडेट करें: http://fake-bank.in"
          (Your bank account is blocked. Update immediately: http://fake-bank.in)

English:  "Congratulations! You won ₹50 Lakh. Claim now: bit.ly/prize123"

Marathi:  "आपले ICICI खाते ला समस्या आहे. सत्यापित करा: icici-verify.com"
          (Problem with your ICICI account. Verify: icici-verify.com)
```

**Decision Rule:** If message attempts to:
- Get sensitive information (password, OTP, account details)
- Create false urgency/fear
- Impersonate legitimate entity
- Request money/payment
- Create false opportunity (lottery, prize)

→ Label as SCAM (1)

---

#### **Label 0: LEGITIMATE**
Message is normal communication, no scam intent.

**Examples:**
```
Hindi:    "नमस्ते, कल मीटिंग 2 बजे है। कृपया आ जाइए।"
          (Hi, meeting tomorrow at 2 PM. Please come.)

English:  "Your Amazon order #123456 has been delivered. Thank you!"

Marathi:  "मी तुम्हाला आज संध्याकाळी भेटू. ठीक आहे का?"
          (I'll meet you this evening. OK?)
```

**Decision Rule:** If message is:
- Regular personal/business communication
- Legitimate service notification (actual Amazon, bank)
- No request for sensitive info
- No artificial urgency
- No false claims

→ Label as LEGITIMATE (0)

---

### **B. Secondary Label: Scam Type (If Label=1)**

Only annotate if message is classified as SCAM.

#### **1. UPI_FRAUD**
Targets UPI payment apps (Google Pay, PhonePe, Paytm, WhatsApp Pay).

**Indicators:**
- Mentions UPI, Google Pay, PhonePe, Paytm, WhatsApp Pay
- Claims account blocked/limited
- Asks for verification
- Links to fake payment apps

**Examples:**
```
"आपका गूगल पे अकाउंट सीमित हो गया। तुरंत यह लिंक खोलें"
(Your Google Pay account is limited. Open this link immediately)

"PhonePe को सत्यापित करने के लिए यहाँ क्लिक करें"
(Click here to verify PhonePe)
```

**Confidence Guide:**
- Contains UPI app name + urgency = VERY HIGH
- Contains UPI app name + verification request = HIGH

---

#### **2. KYC_FRAUD**
Targets Know-Your-Customer (KYC) verification.

**Indicators:**
- Mentions Aadhar, PAN, KYC, verification
- Fake urgency ("expire soon", "immediate action")
- Links to verification portals
- Asks for personal documents

**Examples:**
```
"आपका आधार KYC अद्यतन करना अनिवार्य है। लिंक: kyc-aadhar.in"
(Your Aadhar KYC update is mandatory. Link: kyc-aadhar.in)

"PAN verification expires tomorrow. Verify now: pan-verify.com"
```

**Confidence Guide:**
- Mentions Aadhar/PAN + false urgency = HIGH
- Suspicious KYC link = VERY HIGH

---

#### **3. FAKE_JOB_OFFER**
Fraudulent employment opportunities.

**Indicators:**
- Job offers with unrealistic salaries
- Work-from-home scams (₹50,000/day)
- No application process
- Asks for money upfront
- Vague job descriptions

**Examples:**
```
"Work from home! Earn ₹50,000 per day. No experience needed. Click: job-link.com"

"Join our company! ₹5,00,000 monthly. Send your details to: scammer@email.com"

"सरकारी नौकरी: साक्षात्कार पास! ₹2 लाख महीना। रजिस्टर करें"
(Government job: Interview passed! ₹2 lakh/month. Register)
```

**Confidence Guide:**
- Extremely high salary + no experience = VERY HIGH
- Asking for money upfront = VERY HIGH
- Generic job title + high pay = HIGH

---

#### **4. LOTTERY_PRIZE_SCAM**
False lottery/prize winning notifications.

**Indicators:**
- Claims of lottery/raffle win
- Large prize amounts
- "Congratulations" language
- Claim user never participated
- Asks for personal/bank details

**Examples:**
```
"You won ₹50 Lakh in National Lottery! Claim your prize: lottery-claim.in"

"आप ने Amazon Raffle जीता! ₹1 लाख जीतें। यहाँ क्लिक करें"
(You won Amazon Raffle! Win ₹1 lakh. Click here)

"Flipkart Prize Draw Winner! Redeem your ₹2,00,000 now"
```

**Confidence Guide:**
- Large prize amount + never participated = VERY HIGH
- Lottery message + suspicious link = HIGH
- Prize redemption request = HIGH

---

#### **5. BANK_IMPERSONATION**
Fraudulent messages pretending to be from banks.

**Indicators:**
- Fake bank names or URL spoofing
- Account security threats
- Money transfer requests
- Password/OTP requests
- Loan/credit offers

**Examples:**
```
"ICICI Bank Alert: Unusual activity detected. Verify: icici-verify.com"

"Your HDFC account has been compromised. Update password: hdfc-secure.in"

"SBI Personal Loan: ₹5 Lakh instant approval! Apply: sbi-loan-fake.com"

"आपके बैंक खाते में संदिग्ध लेनदेन। तुरंत सत्यापित करें"
(Suspicious transaction in your bank account. Verify immediately)
```

**Confidence Guide:**
- Bank name + fake URL = VERY HIGH
- Account threat + password request = VERY HIGH
- Spoofed bank domain = HIGH

---

#### **6. OTP_PASSWORD_THEFT**
Direct requests for passwords, OTPs, or authentication codes.

**Indicators:**
- Explicitly asks for OTP
- Requests passwords
- PIN/CVV requests
- Account credential requests
- Claims system needs verification

**Examples:**
```
"Your account needs verification. Send us your 6-digit OTP"

"Click link and enter your password: verify-account.com"

"Bank Alert: Send your CVV and Expiry date to confirm transaction"

"अपना 4 अंकों का PIN भेजें: account.verify@email.com"
(Send your 4-digit PIN: account.verify@email.com)
```

**Confidence Guide:**
- Direct OTP/password request = VERY HIGH
- CVV request = VERY HIGH
- Verification code request = HIGH

---

### **C. Additional Annotations (Metadata)**

For each message, annotate these additional fields:

#### **1. Scam Indicators (Multi-select)**
```
SCAM INDICATORS (select all that apply):
☐ urgent_language         ("तुरंत", "immediately", "अभी")
☐ account_threat          ("blocked", "suspended", "compromised")
☐ suspicious_link         (Shortened URLs, fake domains)
☐ impersonation           (Pretending to be bank/company)
☐ banking_impersonation   (Specifically bank-related)
☐ urgency_with_fear       ("act now or lose", "immediate action")
☐ financial_keywords      ("money", "loan", "investment")
☐ personal_info_request   (Asking for sensitive data)
☐ unusual_grammar         (Grammatical errors, non-native language)
☐ character_substitution  ("@" for "a", "0" for "O")
☐ url_with_embedded_text  (URLs containing usernames/suspicious text)
```

#### **2. Target Entity** (If applicable)
```
TYPE: DROPDOWN
- GOOGLE_PAY
- PAYTM
- PHONEPE
- WHATSAPP_PAY
- ICICI_BANK
- HDFC_BANK
- SBI_BANK
- AXIS_BANK
- KOTAK_BANK
- AMAZON
- FLIPKART
- AADHAR/UIDAI
- INCOME_TAX
- POLICE/CYBERCRIME
- LOTTERY_ORGANIZATION
- EMPLOYER/JOB_SITE
- OTHER
- NONE
```

#### **3. Linguistic Anomalies**
```
ANOMALIES (multi-select):
☐ grammatical_error       ("are sent" instead of "is sent")
☐ spelling_mistakes       (Common typos in scam messages)
☐ non_standard_hindi      (Awkward phrasing in Hindi)
☐ english_in_hindi        (Code-switching without purpose)
☐ auto_translate_quality  (Obvious machine translation)
☐ excessive_punctuation   (!!!???, multiple symbols)
☐ repetitive_words        (Same word used many times)
☐ unnatural_formality     (Overly formal tone)
```

#### **4. Risk Severity**
```
Severity: LOW / MEDIUM / HIGH / CRITICAL

LOW:
- Message easily identifiable as scam
- Clear grammatical errors
- Obvious impersonation
- Detection confidence: 90%+

MEDIUM:
- Requires some analysis to identify
- May fool some users
- Professional-looking message
- Detection confidence: 70-89%

HIGH:
- Well-crafted scam message
- Mimics legitimate communication
- Uses social engineering
- Detection confidence: 50-69%

CRITICAL:
- Highly convincing
- Minimal red flags
- Targets specific person/organization
- Detection confidence: <50%
```

---

## 4. ANNOTATION PROCESS & QUALITY CONTROL

### **A. Annotation Workflow**

```
Step 1: Pre-annotation Review
├─ Read annotation guidelines (15 minutes)
├─ Review 5 training examples
├─ Take small quiz on label definitions
└─ Confirm understanding (>80% accuracy required)

Step 2: Batch Annotation
├─ Assign 100-200 messages per annotator
├─ Set deadline (typically 1 week)
├─ Allow clarification questions
└─ Track progress

Step 3: Quality Control
├─ Random sampling: 10% of annotations reviewed
├─ Inter-annotator agreement calculated (target: >90%)
├─ Discrepancies resolved through discussion
└─ Revision if needed

Step 4: Final Approval
├─ Supervisor reviews all annotations
├─ Data validation checks
├─ Statistical quality checks
└─ Data release
```

### **B. Quality Metrics**

**Cohen's Kappa (Inter-Annotator Agreement)**
```
Target: κ > 0.90 (Nearly perfect agreement)

Interpretation:
κ > 0.81 = Almost perfect agreement
κ = 0.61-0.80 = Substantial agreement
κ = 0.41-0.60 = Moderate agreement
κ < 0.40 = Fair agreement
```

**Formula:**
```
κ = (p_o - p_e) / (1 - p_e)

Where:
p_o = observed agreement
p_e = expected agreement by chance
```

**Implementation:**
```python
from sklearn.metrics import cohen_kappa_score
kappa = cohen_kappa_score(annotator1_labels, annotator2_labels)
```

### **C. Handling Disagreements**

**When two annotators disagree:**

```
Option 1: Third Annotator Review (if kappa <0.90)
├─ Third independent annotator labels message
├─ Majority vote determines label
└─ If 2/3 disagree, escalate to supervisor

Option 2: Supervisor Review (if persistent disagreement)
├─ Supervisor analyzes message
├─ Makes final determination
├─ Documents reasoning
└─ Uses for annotator feedback

Option 3: Remove Ambiguous Cases (if >3 disagreements)
├─ Message too ambiguous for clear classification
├─ Remove from training data
├─ Mark as "AMBIGUOUS" in dataset
└─ Document for future analysis
```

---

## 5. DATA COLLECTION SOURCES

### **A. Public Sources (Ethical)**

```
✓ ALLOWED - Public Data:
├─ Twitter/X: #PhishingAlert, #CyberFraud, #ScamAlert hashtags
├─ Reddit: r/IndiaLoans, r/IndianPersonalFinance scam warnings
├─ Police Cyber Crime Portals: Scam examples from reports
├─ CERT-IN alerts: Government cybersecurity notifications
├─ News articles: Documented scam attempts
├─ Cybersecurity blogs: Scam pattern documentation
└─ User-submitted examples: With explicit consent

✗ PROHIBITED - Private Data:
├─ Personal WhatsApp/SMS messages (without consent)
├─ Hacked/leaked databases
├─ Confidential bank records
└─ Non-public threat intelligence
```

### **B. Synthetic Data Generation**

For increasing dataset size and diversity:

```python
# Example: Generate synthetic scam messages

scam_templates = {
    "UPI_FRAUD": [
        "आपका {UPI_APP} सीमित हो गया। {LINK} खोलें",
        "Your {UPI_APP} account is blocked. Update now: {LINK}",
        "{UPI_APP} सत्यापन अनिवार्य है: {LINK}"
    ],
    "KYC_FRAUD": [
        "आपका आधार KYC {ACTION} है। {LINK} जाएं",
        "Your PAN verification has {STATUS}. Click: {LINK}"
    ]
}

substitutions = {
    "UPI_APP": ["Google Pay", "PhonePe", "Paytm", "WhatsApp Pay"],
    "LINK": ["bit.ly/update", "http://verify-now.in", "secure.pay.verify.com"],
    "ACTION": ["expire हो रहा है", "update की जरूरत है", "verify करना है"],
    "STATUS": ["expired", "needs update", "pending verification"]
}

# Generate combinations
for template in scam_templates["UPI_FRAUD"]:
    for upi_app in substitutions["UPI_APP"]:
        for link in substitutions["LINK"]:
            message = template.replace("{UPI_APP}", upi_app).replace("{LINK}", link)
            dataset.append({
                "message": message,
                "label": 1,
                "scam_type": "UPI_FRAUD",
                "language": detect_language(message),
                "source": "synthetic"
            })
```

### **C. Collection Demographics**

```
Target Distribution:
├─ Regional Representation:
│  ├─ North India: 40% (Hindi, Hinglish)
│  ├─ West India: 35% (Marathi, Hindi, Hinglish)
│  ├─ South India: 15% (Tamil, Telugu)
│  └─ Other Regions: 10%
│
├─ User Demographics:
│  ├─ Age 18-30: 30%
│  ├─ Age 30-45: 40%
│  ├─ Age 45-60: 20%
│  └─ Age 60+: 10%
│
└─ Scam Sophistication:
   ├─ Obvious scams (many red flags): 30%
   ├─ Moderate scams (some red flags): 40%
   └─ Sophisticated scams (few red flags): 30%
```

---

## 6. FEATURE ENGINEERING FOR DATASET

### **A. Automatic Feature Extraction**

Once messages are collected and labeled, extract these features:

```python
def extract_features(message, language='hindi'):
    features = {}
    
    # 1. Basic Statistics
    features['message_length'] = len(message)
    features['word_count'] = len(message.split())
    features['unique_words'] = len(set(message.split()))
    features['avg_word_length'] = features['message_length'] / features['word_count']
    
    # 2. URL Detection
    import re
    urls = re.findall(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', message)
    features['contains_url'] = len(urls) > 0
    features['num_urls'] = len(urls)
    features['url_count'] = len(urls)
    
    # 3. Special Characters
    features['num_special_chars'] = len(re.findall(r'[!@#$%^&*()_+=\[\]{};:\'",.<>?/\\`~]', message))
    features['num_digits'] = len(re.findall(r'\d', message))
    features['has_all_caps'] = any(c.isupper() for c in message)
    
    # 4. Language-Specific Features
    if language == 'hindi':
        hindi_urgency_words = ['तुरंत', 'अभी', 'जल्दी', 'तुरंत ही', 'अभी की']
        marathi_urgency_words = ['लगेच', 'ताडकन', 'लगेच करा']
    
    features['contains_urgency'] = any(word in message.lower() for word in urgency_words)
    
    # 5. Financial Keywords
    financial_keywords = ['बैंक', 'खाता', 'पैसे', 'क्रेडिट', 'डेबिट', 'लोन', 'ऋण', 
                         'bank', 'account', 'money', 'credit', 'payment', 'loan']
    features['financial_keyword_count'] = sum(1 for keyword in financial_keywords if keyword in message.lower())
    
    # 6. Threat Words
    threat_words = ['ब्लॉक', 'suspend', 'freeze', 'limited', 'delete', 'close',
                   'रोक', 'सीमित', 'बंद']
    features['threat_word_count'] = sum(1 for word in threat_words if word in message.lower())
    
    # 7. TF-IDF Scores
    from sklearn.feature_extraction.text import TfidfVectorizer
    vectorizer = TfidfVectorizer(max_features=100)
    tfidf_matrix = vectorizer.fit_transform([message])
    features['tfidf_max'] = tfidf_matrix.max()
    features['tfidf_mean'] = tfidf_matrix.mean()
    
    return features
```

### **B. Feature List for ML Model**

```
NUMERICAL FEATURES (45 total):
├─ Length Features (3):
│  ├─ message_length
│  ├─ word_count
│  └─ avg_word_length
│
├─ URL Features (2):
│  ├─ contains_url (binary)
│  └─ num_urls
│
├─ Character Features (4):
│  ├─ num_special_chars
│  ├─ num_digits
│  ├─ has_all_caps (binary)
│  └─ punctuation_density
│
├─ Linguistic Features (5):
│  ├─ contains_urgency
│  ├─ financial_keyword_count
│  ├─ threat_word_count
│  ├─ impersonation_language
│  └─ grammatical_error_count
│
├─ Language-Specific (8):
│  ├─ hindi_urgency_words
│  ├─ marathi_urgency_words
│  ├─ tamil_phonetic_anomalies
│  ├─ code_switching_presence
│  ├─ script_mixing
│  ├─ transliteration_errors
│  ├─ non_standard_grammar
│  └─ character_substitution
│
└─ N-gram Features (20):
   ├─ TF-IDF bigrams
   ├─ Character n-grams (2-3)
   └─ Most common word pairs
```

---

## 7. DATASET VALIDATION CHECKLIST

Before releasing dataset, verify:

```
✓ Data Integrity
  ☐ No duplicate messages (>95% similarity)
  ☐ No corrupted text files
  ☐ All JSON valid and parseable
  ☐ No missing required fields
  ☐ Label consistency (all labels 0 or 1)

✓ Annotation Quality
  ☐ Inter-annotator agreement κ > 0.90
  ☐ No annotator has >30% disagreement
  ☐ All disagreements documented
  ☐ Random samples verified by supervisor

✓ Language Quality
  ☐ All Hindi text is valid Devanagari
  ☐ All Marathi text verified for accuracy
  ☐ Hinglish mix is natural/realistic
  ☐ Translations are accurate
  ☐ Transliterations are consistent

✓ Dataset Balance
  ☐ 50% scam, 50% legitimate ± 5%
  ☐ Scam types distributed evenly
  ☐ Language distribution matches target
  ☐ No class imbalance in train/test

✓ Privacy & Ethics
  ☐ No real personal information (names, addresses, phone)
  ☐ No identifying details
  ☐ All synthetic data clearly marked
  ☐ Licensed for research use
  ☐ Attribution requirements clear

✓ Documentation
  ☐ README.md complete with usage examples
  ☐ SCHEMA.md explains all fields
  ☐ Annotation guidelines documented
  ☐ Statistics/analysis included
  ☐ Citation format provided
```

---

## 8. DATASET USAGE EXAMPLES

### **A. Loading Dataset in Python**

```python
import pandas as pd
import json

# Load CSV version
df = pd.read_csv('ISDD_combined_v1.0.csv')

# Quick statistics
print(f"Total messages: {len(df)}")
print(f"Scam messages: {len(df[df['label']==1])}")
print(f"Legitimate messages: {len(df[df['label']==0])}")
print(f"\nLanguage distribution:\n{df['language'].value_counts()}")
print(f"\nScam type distribution:\n{df['scam_type'].value_counts()}")

# Train-test split
from sklearn.model_selection import train_test_split

X = df['message_text_original']
y = df['label']

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, stratify=y, random_state=42
)

print(f"Train set: {len(X_train)} messages")
print(f"Test set: {len(X_test)} messages")
```

### **B. Feature Extraction & Model Training**

```python
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score

# Vectorization
vectorizer = TfidfVectorizer(max_features=1000, language='english')
X_train_tfidf = vectorizer.fit_transform(X_train)
X_test_tfidf = vectorizer.transform(X_test)

# Model training
clf = RandomForestClassifier(n_estimators=100, random_state=42)
clf.fit(X_train_tfidf, y_train)

# Evaluation
y_pred = clf.predict(X_test_tfidf)

print(f"Accuracy: {accuracy_score(y_test, y_pred):.3f}")
print(f"Precision: {precision_score(y_test, y_pred):.3f}")
print(f"Recall: {recall_score(y_test, y_pred):.3f}")
print(f"F1-Score: {f1_score(y_test, y_pred):.3f}")
```

### **C. Comparison with Existing Systems**

```python
# Compare ISDD performance vs POSTER dataset
results = {
    'POSTER_SMISHING': {'precision': 0.9789, 'recall': 0.956, 'f1': 0.9673},
    'SECURENET': {'precision': 0.92, 'recall': 0.91, 'f1': 0.915},
    'AEGIS_ON_ISDD': {'precision': 0.94, 'recall': 0.93, 'f1': 0.935}  # Your model
}

# Show comparison
import pandas as pd
comparison_df = pd.DataFrame(results).T
print("\nCompare ISDD-trained model vs existing solutions:")
print(comparison_df)
print("\nImprovement: ", )
print(f"  vs POSTER: {(comparison_df.loc['AEGIS_ON_ISDD', 'f1'] / comparison_df.loc['POSTER_SMISHING', 'f1'] - 1)*100:.1f}%")
print(f"  vs SecureNet: {(comparison_df.loc['AEGIS_ON_ISDD', 'f1'] / comparison_df.loc['SECURENET', 'f1'] - 1)*100:.1f}%")
```

---

## 9. PUBLISHING THE DATASET

### **A. Release Strategy**

```
Phase 1: Private Release (May 2026)
├─ Version 1.0 with 5,000 messages
├─ Share with 5-10 research groups for validation
├─ Collect feedback
└─ Fix issues

Phase 2: Public Beta (June 2026)
├─ Release on Zenodo/GitHub
├─ Minimum 8,000 messages
├─ Documentation complete
├─ Research paper submitted to ACL/EMNLP
└─ Invite community contributions

Phase 3: Version 2.0 (August 2026)
├─ Expand to 15,000 messages
├─ Add more languages (Kannada, Gujarati, Punjabi)
├─ Community-contributed samples
└─ Formal dataset paper published
```

### **B. Citation Format**

```
Recommended Citation (once published):

BibTeX:
@dataset{more2026isdd,
  title={ISDD: Indian Scam Detection Dataset},
  author={More, Ayush},
  year={2026},
  publisher={Zenodo},
  url={https://zenodo.org/record/XXXXX}
}

APA:
More, A. (2026). ISDD: Indian Scam Detection Dataset [Data set]. 
Zenodo. https://doi.org/10.5281/zenodo.XXXXX

IEEE:
[1] A. More, "ISDD: Indian Scam Detection Dataset," 
Zenodo, 2026. [Online]. Available: https://zenodo.org/record/XXXXX
```

### **C. GitHub Repository Structure**

```
https://github.com/ayush-more/ISDD
```

---

## 10. QUICK START GUIDE

**For someone using your dataset:**

```markdown
# Quick Start: Using ISDD

## Installation
git clone https://github.com/ayush-more/ISDD
cd ISDD
pip install -r requirements.txt

## Load Dataset
from isdd_utils import load_dataset

train_df = load_dataset('train')
test_df = load_dataset('test')

print(f"Train: {len(train_df)} messages")
print(f"Test: {len(test_df)} messages")

## Basic Classification
from isdd_utils import train_classifier

model = train_classifier(train_df)
predictions = model.predict(test_df['message_text'])

## Evaluate
from sklearn.metrics import classification_report

print(classification_report(test_df['label'], predictions))
```

---

## SUMMARY

**What you now have:**

✅ Complete dataset schema (JSON + CSV)
✅ Detailed annotation guidelines
✅ Quality control procedures
✅ Feature engineering templates
✅ Publishing & citation format
✅ Python loading & training code

**Next steps:**
1. Start collecting/synthesizing 5,000+ SMS scam messages
2. Create annotation interface (Google Forms, Label Studio, or custom tool)
3. Recruit 3-5 annotators fluent in Hindi/Marathi/Hinglish
4. Complete annotation with quality checks
5. Release on GitHub + Zenodo

---

*Dataset Schema Version 1.0*  
*Created: May 11, 2026*  
*For: Aegis AI Patent Documentation*
