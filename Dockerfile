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
COPY scripts/render-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENV WHISPER_MODEL=tiny
ENV PYTHONUNBUFFERED=1

EXPOSE 8000
CMD ["/entrypoint.sh"]
