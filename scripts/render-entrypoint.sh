#!/bin/sh
set -e

# Optional: download a custom model (any size) from your own URL — not Render Secret Files.
if [ -n "$PHISHING_MODEL_URL" ] && [ ! -f /app/phishing_model.pkl ]; then
  echo "Downloading phishing model from PHISHING_MODEL_URL..."
  wget -q -O /app/phishing_model.pkl "$PHISHING_MODEL_URL" || \
    curl -fsSL -o /app/phishing_model.pkl "$PHISHING_MODEL_URL" || true
fi

# Fallback if image was built without a model (should not happen normally).
if [ ! -f /app/phishing_model.pkl ]; then
  echo "Training phishing model on startup..."
  python scripts/build_model.py || true
fi

# Optional secrets for retraining only (keep under 500KiB each on Render).
if [ -f /etc/secrets/secret.key ]; then
  cp /etc/secrets/secret.key /app/secret.key
fi
if [ -f /etc/secrets/dataset.csv.enc ]; then
  cp /etc/secrets/dataset.csv.enc /app/dataset.csv.enc
fi

exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8000}"
