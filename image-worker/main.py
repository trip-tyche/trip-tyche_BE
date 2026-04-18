import io
import logging
import os
import signal
import sys
import time

import boto3
import pymysql
import redis
from botocore.exceptions import ClientError
from PIL import Image
from pillow_heif import register_heif_opener

register_heif_opener()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger(__name__)

REDIS_HOST = os.environ["REDIS_HOST"]
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
REDIS_PASSWORD = os.environ.get("REDIS_PASSWORD", "")

DB_HOST = os.environ["DB_HOST"]
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_NAME = os.environ["DB_NAME"]
DB_USER = os.environ["DB_USER"]
DB_PASSWORD = os.environ["DB_PASSWORD"]

OCI_ACCESS_KEY = os.environ["OCI_ACCESS_KEY"]
OCI_SECRET_KEY = os.environ["OCI_SECRET_KEY"]
OCI_S3_ENDPOINT = os.environ["OCI_S3_ENDPOINT"]
OCI_BUCKET_NAME = os.environ["OCI_BUCKET_NAME"]

STREAM_KEY = "image-processing-stream"
DLQ_KEY = "image-processing-dlq"
CONSUMER_GROUP = "image-workers"
CONSUMER_NAME = "worker-1"
MAX_RETRY = 3
MAX_IMAGE_SIZE = 1920
WEBP_QUALITY = 85

running = True


def handle_sigterm(signum, frame):
    global running
    log.info("SIGTERM 수신 — 현재 작업 완료 후 종료합니다.")
    running = False


signal.signal(signal.SIGTERM, handle_sigterm)


def get_redis_client():
    return redis.Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        password=REDIS_PASSWORD if REDIS_PASSWORD else None,
        decode_responses=True,
    )


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=OCI_S3_ENDPOINT,
        aws_access_key_id=OCI_ACCESS_KEY,
        aws_secret_access_key=OCI_SECRET_KEY,
    )


def get_db_connection():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        db=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def ensure_consumer_group(r: redis.Redis):
    try:
        r.xgroup_create(STREAM_KEY, CONSUMER_GROUP, id="0", mkstream=True)
        log.info("Consumer Group 생성: %s", CONSUMER_GROUP)
    except redis.exceptions.ResponseError as e:
        if "BUSYGROUP" in str(e):
            log.info("Consumer Group 이미 존재: %s", CONSUMER_GROUP)
        else:
            raise


def get_delivery_count(r: redis.Redis, message_id: str) -> int:
    try:
        pending = r.xpending_range(STREAM_KEY, CONSUMER_GROUP, message_id, message_id, 1)
        if pending:
            return pending[0].get("times_delivered", 1)
    except Exception:
        pass
    return 1


def move_to_dlq(r: redis.Redis, message_id: str, fields: dict, reason: str):
    try:
        r.xadd(DLQ_KEY, {**fields, "failed_reason": reason, "original_id": message_id})
        r.xack(STREAM_KEY, CONSUMER_GROUP, message_id)
        log.error("DLQ 이동 — mediaFileId: %s, reason: %s", fields.get("mediaFileId"), reason)
    except Exception as e:
        log.error("DLQ 이동 실패 — message_id: %s, error: %s", message_id, e)


def resize_and_convert(image_bytes: bytes) -> bytes:
    with Image.open(io.BytesIO(image_bytes)) as img:
        img = img.convert("RGB")
        w, h = img.size
        if max(w, h) > MAX_IMAGE_SIZE:
            ratio = MAX_IMAGE_SIZE / max(w, h)
            new_size = (int(w * ratio), int(h * ratio))
            img = img.resize(new_size, Image.LANCZOS)

        output = io.BytesIO()
        img.save(output, format="WEBP", quality=WEBP_QUALITY)
        return output.getvalue()


def build_webp_key(original_key: str) -> str:
    # originals/{tripKey}/{filename}.ext → images/{tripKey}/{filename}.webp
    parts = original_key.split("/", 1)  # ["originals", "{tripKey}/{filename}.ext"]
    rest = parts[1]                     # "{tripKey}/{filename}.ext"
    base = rest.rsplit(".", 1)[0]       # "{tripKey}/{filename}"
    return f"images/{base}.webp"


def update_media_link(media_file_id: int, new_url: str):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE MediaFile SET media_link = %s WHERE media_file_id = %s",
                (new_url, media_file_id),
            )
        conn.commit()
    finally:
        conn.close()


def delete_original(s3, original_key: str):
    try:
        s3.delete_object(Bucket=OCI_BUCKET_NAME, Key=original_key)
        log.info("원본 삭제 완료 — key: %s", original_key)
    except Exception as e:
        log.warning("원본 삭제 실패 (무시) — key: %s, error: %s", original_key, e)


def process_message(s3, message_id: str, fields: dict):
    media_file_id = int(fields["mediaFileId"])
    original_key = fields["originalKey"]

    log.info("처리 시작 — mediaFileId: %d, key: %s", media_file_id, original_key)

    # 1. 원본 다운로드
    response = s3.get_object(Bucket=OCI_BUCKET_NAME, Key=original_key)
    image_bytes = response["Body"].read()

    # 2. 리사이징 + WebP 변환
    webp_bytes = resize_and_convert(image_bytes)

    # 3. 변환본 업로드
    webp_key = build_webp_key(original_key)
    s3.put_object(
        Bucket=OCI_BUCKET_NAME,
        Key=webp_key,
        Body=webp_bytes,
        ContentType="image/webp",
        ContentLength=len(webp_bytes),
    )

    # 4. DB UPDATE
    webp_url = f"{OCI_S3_ENDPOINT}/{OCI_BUCKET_NAME}/{webp_key}"
    update_media_link(media_file_id, webp_url)

    log.info("처리 완료 — mediaFileId: %d, webp_url: %s", media_file_id, webp_url)

    return original_key


def main():
    log.info("Image Worker 시작")
    s3 = get_s3_client()

    while running:
        r = None
        try:
            r = get_redis_client()
            ensure_consumer_group(r)

            log.info("메시지 대기 중...")

            while running:
                messages = r.xreadgroup(
                    CONSUMER_GROUP,
                    CONSUMER_NAME,
                    {STREAM_KEY: ">"},
                    count=1,
                    block=5000,
                )

                if not messages:
                    continue

                for stream_name, entries in messages:
                    for message_id, fields in entries:
                        delivery_count = get_delivery_count(r, message_id)

                        if delivery_count > MAX_RETRY:
                            move_to_dlq(r, message_id, fields, f"최대 재시도 초과 ({delivery_count}회)")
                            continue

                        try:
                            original_key = process_message(s3, message_id, fields)
                            r.xack(STREAM_KEY, CONSUMER_GROUP, message_id)
                            # XACK 이후 원본 삭제
                            delete_original(s3, original_key)

                        except ClientError as e:
                            log.error("OCI 오류 — message_id: %s, error: %s", message_id, e)
                        except pymysql.Error as e:
                            log.error("DB 오류 — message_id: %s, error: %s", message_id, e)
                        except Exception as e:
                            log.error("처리 실패 — message_id: %s, error: %s", message_id, e)

        except redis.exceptions.ConnectionError as e:
            log.error("Redis 연결 오류: %s — 5초 후 재연결", e)
            time.sleep(5)
        except Exception as e:
            log.error("예상치 못한 오류: %s — 5초 후 재시작", e)
            time.sleep(5)

    log.info("Image Worker 종료")


if __name__ == "__main__":
    main()
