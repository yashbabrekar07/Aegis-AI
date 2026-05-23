FROM python:3.11-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY backend/requirements.txt .

# CPU-only PyTorch first (avoids huge CUDA wheels and Render build OOM)
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir -r requirements.txt \
    && python -c "import nltk; nltk.download('stopwords', quiet=True); nltk.download('punkt', quiet=True); nltk.download('punkt_tab', quiet=True)"

COPY backend/ .
COPY scripts/ /app/scripts/
COPY ISDD_Dataset/ /app/ISDD_Dataset/

ENV AEGIS_REPO_ROOT=/app
ENV DATASET_SOURCE=isdd
ENV WHISPER_MODEL=tiny
ENV CALL_GUARD_WHISPER_FALLBACK=true
ENV CALL_GUARD_MAX_AUDIO_SEC=90
ENV CALL_GUARD_WHISPER_SEC=45
ENV PYTHONUNBUFFERED=1

# Use committed ISDD CSVs when present; otherwise generate (dev/CI fallback)
RUN if [ ! -f /app/ISDD_Dataset/processed/ISDD_train_split.csv ]; then \
      python /app/scripts/generate_isdd_dataset.py --total 4000; \
    else \
      echo "Using pre-built ISDD processed splits from repo"; \
    fi

RUN python -c "from model import train_model; train_model()"

COPY scripts/render-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8000
CMD ["/entrypoint.sh"]
