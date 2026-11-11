FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    libheif-dev \
    libheif1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN pip install --no-cache-dir \
    redis==8.1.0 \
    boto3==1.44.0 \
    Pillow==13.0.0 \
    pillow-heif==1.8.1 \
    pymysql==1.2.4 \
    sentry-sdk==2.74.0

COPY main.py .

CMD ["python", "main.py"]
