import io
import logging
import os
import re
import signal
import sys
import time

import boto3
import pymysql
import redis
import sentry_sdk
from botocore.config import Config
from botocore.exceptions import ClientError
from PIL import Image, ImageOps
from pillow_heif import register_heif_opener

register_heif_opener()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger(__name__)

EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")


def _mask(text):
    return EMAIL_RE.sub("[email]", text) if isinstance(text, str) else text


def _scrub(event, hint):
    if event.get("message"):
        event["message"] = _mask(event["message"])
    logentry = event.get("logentry")
    if logentry:
        logentry["message"] = _mask(logentry.get("message"))
        params = logentry.get("params")
        if isinstance(params, (list, tuple)):
            logentry["params"] = [_mask(p) for p in params]
        elif isinstance(params, dict):
            logentry["params"] = {k: _mask(v) for k, v in params.items()}
    for exc in event.get("exception", {}).get("values", []):
        exc["value"] = _mask(exc.get("value"))
    return event


def _scrub_breadcrumb(crumb, hint):
    if crumb.get("message"):
        crumb["message"] = _mask(crumb["message"])
    return crumb


sentry_sdk.init(
    dsn=os.environ.get("SENTRY_DSN") or None,
    environment="prod",
    release=os.environ.get("APP_VERSION"),
    send_default_pii=False,
    traces_sample_rate=0,
    before_send=_scrub,
    before_breadcrumb=_scrub_breadcrumb,
)

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
PROCESSED_STREAM_KEY = "media-processed-stream"
CONSUMER_GROUP = "image-workers"
CONSUMER_NAME = "worker-1"
MAX_RETRY = 3
MAX_IMAGE_SIZE = 1920
WEBP_QUALITY = 85
# 표지용 축소본. 프론트가 카드 비율로 잘라(cover crop) 최대 750px 폭으로 그리므로 짧은 변 기준으로 줄인다.
THUMB_SHORT_SIDE = 800
THUMB_QUALITY = 80
THUMB_SUFFIX = "_thumb.webp"
# 객체 키에 UUID가 들어 있어 내용이 바뀌지 않는다. 재처리해도 같은 원본에서 같은 결과가 나온다.
# private: CDN 같은 공유 캐시가 보관하면 사진 삭제·공유 해제 뒤에도 계속 내보낼 수 있다. 브라우저 캐시만 허용한다.
CACHE_CONTROL = "private, max-age=31536000, immutable"

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
        # redis-py 8부터 기본 socket_timeout이 5초라 XREADGROUP block(5초)과 같아 대기 중 타임아웃이 난다.
        socket_timeout=15,
    )


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=OCI_S3_ENDPOINT,
        aws_access_key_id=OCI_ACCESS_KEY,
        aws_secret_access_key=OCI_SECRET_KEY,
        config=Config(signature_version="s3v4"),
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

        if fields.get("flow") == "v2" and "mediaFileId" in fields:
            try:
                publish_failed_result(r, int(fields["mediaFileId"]), reason)
            except Exception as ex:
                log.error("[v2] DLQ FAILED 결과 발행 실패 — error: %s", ex, exc_info=True)
    except Exception as e:
        log.error("DLQ 이동 실패 — message_id: %s, error: %s", message_id, e, exc_info=True)


def decode_image(image_bytes: bytes) -> Image.Image:
    """회전(EXIF)과 RGB 변환까지 끝낸 이미지. 48MP 사진도 있으므로 한 번만 디코딩해 여러 출력에 쓴다."""
    with Image.open(io.BytesIO(image_bytes)) as img:
        img = ImageOps.exif_transpose(img)
        return img.convert("RGB")


def fit_long_side(img: Image.Image, max_size: int) -> Image.Image:
    w, h = img.size
    if max(w, h) <= max_size:
        return img
    ratio = max_size / max(w, h)
    return img.resize((int(w * ratio), int(h * ratio)), Image.LANCZOS)


def fit_short_side(img: Image.Image, short_size: int) -> Image.Image:
    w, h = img.size
    if min(w, h) <= short_size:
        return img
    ratio = short_size / min(w, h)
    # 짧은 변이 정확히 short_size가 되도록 그 변은 반올림 오차 없이 고정한다.
    new_size = (short_size, round(h * ratio)) if w <= h else (round(w * ratio), short_size)
    return img.resize(new_size, Image.LANCZOS)


def encode_webp(img: Image.Image, quality: int) -> bytes:
    output = io.BytesIO()
    img.save(output, format="WEBP", quality=quality)
    return output.getvalue()


def resize_and_convert(image_bytes: bytes) -> bytes:
    return encode_webp(fit_long_side(decode_image(image_bytes), MAX_IMAGE_SIZE), WEBP_QUALITY)


def build_thumb_key(final_key: str) -> str:
    # processed/{tripId}/{uuid}.webp → processed/{tripId}/{uuid}_thumb.webp
    base = final_key[:-len(".webp")] if final_key.endswith(".webp") else final_key.rsplit(".", 1)[0]
    return base + THUMB_SUFFIX


def put_webp(s3, key: str, body: bytes):
    s3.put_object(
        Bucket=OCI_BUCKET_NAME,
        Key=key,
        Body=body,
        ContentType="image/webp",
        ContentLength=len(body),
        CacheControl=CACHE_CONTROL,
    )


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
                "UPDATE media_file SET media_link = %s WHERE media_file_id = %s",
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


def publish_processed_result(r: redis.Redis, media_file_id: int, final_key: str, processed_at: str,
                             thumb_key: str = None):
    fields = {
        "mediaFileId": str(media_file_id),
        "finalKey": final_key,
        "processedAt": processed_at,
    }
    if thumb_key:
        fields["thumbKey"] = thumb_key
    try:
        r.xadd(PROCESSED_STREAM_KEY, fields)
        log.info("[v2] 결과 발행 완료 — mediaFileId: %d", media_file_id)
    except Exception as e:
        log.error("[v2] 결과 발행 실패 — mediaFileId: %d, error: %s", media_file_id, e, exc_info=True)


def publish_failed_result(r: redis.Redis, media_file_id: int, reason: str):
    try:
        r.xadd(PROCESSED_STREAM_KEY, {
            "mediaFileId": str(media_file_id),
            "status": "FAILED",
            "reason": reason,
        })
        log.warning("[v2] 실패 결과 발행 — mediaFileId: %d, reason: %s", media_file_id, reason)
    except Exception as e:
        log.error("[v2] 실패 결과 발행 실패 — mediaFileId: %d, error: %s", media_file_id, e, exc_info=True)


def create_thumbnail(s3, media_file_id: int, img: Image.Image, final_key: str):
    """썸네일은 보조 자료다. 실패해도 본 처리를 실패시키지 않고 None을 돌려준다."""
    thumb_key = build_thumb_key(final_key)
    try:
        # 파노라마처럼 극단적인 비율은 짧은 변 기준이면 본보다 커지므로 긴 변도 본과 같은 한도로 묶는다.
        thumb = fit_long_side(fit_short_side(img, THUMB_SHORT_SIDE), MAX_IMAGE_SIZE)
        thumb_bytes = encode_webp(thumb, THUMB_QUALITY)
        put_webp(s3, thumb_key, thumb_bytes)
        return thumb_key
    except Exception as e:
        log.warning("[v2] 썸네일 생성 실패 (무시) — mediaFileId: %d, key: %s, error: %s",
                    media_file_id, thumb_key, e)
        return None


def process_message_v2(s3, fields: dict) -> tuple:
    """v2 흐름. 변환본·썸네일 PUT만 수행 — DB/원본 삭제는 BE 위임.
    반환: (media_file_id, final_key, processed_at_iso, thumb_key 또는 None)
    """
    from datetime import datetime, timezone

    media_file_id = int(fields["mediaFileId"])
    temp_key = fields["tempKey"]
    final_key = fields["finalKey"]

    log.info("[v2] 처리 시작 — mediaFileId: %d, tempKey: %s", media_file_id, temp_key)

    response = s3.get_object(Bucket=OCI_BUCKET_NAME, Key=temp_key)
    image_bytes = response["Body"].read()
    img = decode_image(image_bytes)
    del image_bytes, response

    webp_bytes = encode_webp(fit_long_side(img, MAX_IMAGE_SIZE), WEBP_QUALITY)
    put_webp(s3, final_key, webp_bytes)

    thumb_key = create_thumbnail(s3, media_file_id, img, final_key)

    processed_at = datetime.now(timezone.utc).isoformat()

    log.info("[v2] 변환 완료 — mediaFileId: %d, finalKey: %s, thumbKey: %s", media_file_id, final_key, thumb_key)
    return media_file_id, final_key, processed_at, thumb_key


def process_message(s3, message_id: str, fields: dict):
    media_file_id = int(fields["mediaFileId"])
    original_key = fields["originalKey"]

    log.info("처리 시작 — mediaFileId: %d, key: %s", media_file_id, original_key)

    response = s3.get_object(Bucket=OCI_BUCKET_NAME, Key=original_key)
    image_bytes = response["Body"].read()

    webp_bytes = resize_and_convert(image_bytes)

    webp_key = build_webp_key(original_key)
    s3.put_object(
        Bucket=OCI_BUCKET_NAME,
        Key=webp_key,
        Body=webp_bytes,
        ContentType="image/webp",
        ContentLength=len(webp_bytes),
        CacheControl=CACHE_CONTROL,
    )

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
                            flow = fields.get("flow", "v1")
                            if flow == "v2":
                                media_file_id, final_key, processed_at, thumb_key = process_message_v2(s3, fields)
                                r.xack(STREAM_KEY, CONSUMER_GROUP, message_id)
                                publish_processed_result(r, media_file_id, final_key, processed_at, thumb_key)
                            else:
                                original_key = process_message(s3, message_id, fields)
                                r.xack(STREAM_KEY, CONSUMER_GROUP, message_id)
                                # XACK 이후 원본 삭제
                                delete_original(s3, original_key)

                        except ClientError as e:
                            log.error("OCI 오류 — message_id: %s, error: %s", message_id, e, exc_info=True)
                        except pymysql.Error as e:
                            log.error("DB 오류 — message_id: %s, error: %s", message_id, e, exc_info=True)
                        except Exception as e:
                            log.error("처리 실패 — message_id: %s, error: %s", message_id, e, exc_info=True)

        except redis.exceptions.ConnectionError as e:
            # 5초마다 재시도하므로 error로 남기면 장애 동안 Sentry 이벤트가 쏟아진다.
            log.warning("Redis 연결 오류: %s — 5초 후 재연결", e)
            time.sleep(5)
        except Exception as e:
            log.error("예상치 못한 오류: %s — 5초 후 재시작", e, exc_info=True)
            time.sleep(5)

    log.info("Image Worker 종료")


if __name__ == "__main__":
    main()
