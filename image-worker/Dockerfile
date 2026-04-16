FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    libheif-dev \
    libheif1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN pip install --no-cache-dir \
    redis \
    boto3 \
    Pillow \
    pillow-heif \
    pymysql

COPY main.py .

CMD ["python", "main.py"]
