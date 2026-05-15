#!/bin/sh
set -e

# Render Secret Files (Dashboard → Secret Files) — do not commit these
if [ -f /etc/secrets/phishing_model.pkl ]; then
  cp /etc/secrets/phishing_model.pkl /app/phishing_model.pkl
fi
if [ -f /etc/secrets/secret.key ]; then
  cp /etc/secrets/secret.key /app/secret.key
fi
if [ -f /etc/secrets/dataset.csv.enc ]; then
  cp /etc/secrets/dataset.csv.enc /app/dataset.csv.enc
fi

exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8000}"
