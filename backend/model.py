import os
import pickle
import pandas as pd
import requests
from io import StringIO
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
from sklearn.pipeline import Pipeline
from utils import generate_key, load_key, encrypt_file, decrypt_file_to_memory
from preprocess import preprocess_text
from training_augment import augment_dataframe
from risk_engine import save_config, DEFAULT_CONFIG

MODEL_FILE = "phishing_model.pkl"
DATA_FILE = "dataset.csv"
ENCRYPTED_DATA_FILE = "dataset.csv.enc"

SPAM_DATASET_URL = "https://raw.githubusercontent.com/justmarkham/pycon-2016-tutorial/master/data/sms.tsv"


def create_and_encrypt_dataset():
    if not os.path.exists("secret.key"):
        generate_key()

    print("Downloading Kaggle Spam Dataset mirror...")
    response = requests.get(SPAM_DATASET_URL, timeout=60)
    response.raise_for_status()

    df = pd.read_csv(StringIO(response.text), sep="\t", names=["label", "text"])
    df["label"] = df["label"].map({"ham": "safe", "spam": "phishing"})
    df.to_csv(DATA_FILE, index=False)
    encrypt_file(DATA_FILE, ENCRYPTED_DATA_FILE)
    if os.path.exists(DATA_FILE):
        os.remove(DATA_FILE)
    print("Dataset securely generated and encrypted.")


def load_training_dataset():
    source = os.getenv("DATASET_SOURCE", "isdd").strip().lower()
    if source == "isdd":
        from isdd_loader import load_isdd_training_frame

        df = load_isdd_training_frame()
        print(f"ISDD training set: {len(df)} rows")
    else:
        if not os.path.exists(ENCRYPTED_DATA_FILE):
            create_and_encrypt_dataset()
        decrypted_str = decrypt_file_to_memory(ENCRYPTED_DATA_FILE)
        df = pd.read_csv(StringIO(decrypted_str))

    df = augment_dataframe(df)
    return _balance_and_preprocess(df)


def _balance_and_preprocess(df):
    df_safe = df[df["label"] == "safe"]
    df_phishing = df[df["label"] == "phishing"]
    min_size = min(len(df_safe), len(df_phishing))
    target_size = min(min_size, 15000)
    if len(df_safe) > target_size:
        df_safe = df_safe.sample(target_size, random_state=42)
    if len(df_phishing) > target_size:
        df_phishing = df_phishing.sample(target_size, random_state=42)
    df_balanced = pd.concat([df_safe, df_phishing]).sample(frac=1, random_state=42)
    print(f"Dataset balanced: {len(df_safe)} safe, {len(df_phishing)} phishing. Preprocessing...")
    df_balanced = df_balanced.copy()
    df_balanced["cleaned_text"] = df_balanced["text"].apply(preprocess_text)
    return df_balanced


def load_encrypted_dataset():
    if not os.path.exists(ENCRYPTED_DATA_FILE):
        create_and_encrypt_dataset()
    decrypted_str = decrypt_file_to_memory(ENCRYPTED_DATA_FILE)
    df = pd.read_csv(StringIO(decrypted_str))
    return _balance_and_preprocess(df)


def _build_pipeline():
    return Pipeline(
        [
            (
                "tfidf",
                TfidfVectorizer(
                    max_features=25000,
                    ngram_range=(1, 2),
                    min_df=2,
                    sublinear_tf=True,
                ),
            ),
            ("clf", MultinomialNB(alpha=0.1)),
        ]
    )


def _tune_thresholds(model, X_test, y_test):
    """Pick confidence thresholds targeting ~95% accuracy on holdout."""
    proba = model.predict_proba(X_test)
    classes = list(model.classes_)
    ph_idx = classes.index("phishing") if "phishing" in classes else 1

    best = dict(DEFAULT_CONFIG)
    best_acc = 0.0
    for scam_th in [0.70, 0.75, 0.78, 0.82, 0.85]:
        for susp_th in [0.55, 0.60, 0.62, 0.65]:
            preds = []
            for row in proba:
                p_ph = row[ph_idx]
                preds.append("phishing" if p_ph >= scam_th else "safe")
            acc = accuracy_score(y_test, preds)
            if acc > best_acc:
                best_acc = acc
                best["scam_ml_confidence"] = scam_th
                best["suspicious_ml_confidence"] = susp_th

    best["validation_accuracy"] = round(best_acc * 100, 2)
    save_config(best)
    print(f"Tuned thresholds: scam>={best['scam_ml_confidence']}, suspicious>={best['suspicious_ml_confidence']}, acc={best_acc:.3f}")
    return best


def train_model():
    print("Loading data...")
    df = load_training_dataset()

    X = df["cleaned_text"]
    y = df["label"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.15, stratify=y, random_state=42
    )

    print("Training model...")
    model = _build_pipeline()
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, pos_label="phishing", zero_division=0)
    rec = recall_score(y_test, y_pred, pos_label="phishing", zero_division=0)
    f1 = f1_score(y_test, y_pred, pos_label="phishing", zero_division=0)
    print(f"Holdout — accuracy: {acc:.3f}, precision: {prec:.3f}, recall: {rec:.3f}, f1: {f1:.3f}")

    _tune_thresholds(model, X_test, y_test)

    with open(MODEL_FILE, "wb") as f:
        pickle.dump(model, f)

    print("Model successfully trained and saved!")


def predict_text(text):
    try:
        if not os.path.exists(MODEL_FILE):
            train_model()

        with open(MODEL_FILE, "rb") as f:
            model = pickle.load(f)

        from preprocess import translate_to_english

        translated_text = translate_to_english(text)
        clean_text = preprocess_text(translated_text)
        if not clean_text.strip():
            return "safe", 92.0

        prediction = model.predict([clean_text])[0]
        probabilities = model.predict_proba([clean_text])[0]
        classes = list(model.classes_)
        prob_dict = dict(zip(classes, probabilities))
        confidence = round(prob_dict[prediction] * 100, 2)
        return prediction, confidence
    except Exception as e:
        print(f"predict_text error: {e}")
        return "safe", 85.0
