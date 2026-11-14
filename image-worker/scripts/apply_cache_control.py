"""기존 사진 객체에 Cache-Control을 붙이는 일회성 스크립트.

객체를 자기 자신에게 복사(copy_object + MetadataDirective=REPLACE)해 메타데이터를 바꾼다.
REPLACE는 기존 메타데이터를 모두 지우므로 ContentType을 반드시 다시 지정한다.

필요 환경변수: OCI_ACCESS_KEY, OCI_SECRET_KEY, OCI_S3_ENDPOINT, OCI_BUCKET_NAME

사용 순서:
  1) 객체 하나로 시험한다. 적용 전후 헤더를 출력한다.
     python apply_cache_control.py --key processed/12/xxxx.webp
     curl -sI <mediaLink> | grep -i -E 'cache-control|content-type'
     → GET 응답에 Cache-Control이 없으면 OCI가 저장하지 않는 것이다. 여기서 중단한다.
  2) 대상 목록만 확인한다(기본값은 dry-run).
     python apply_cache_control.py
  3) 전체에 적용한다.
     python apply_cache_control.py --apply
"""

import argparse
import os
import sys

import boto3
from botocore.config import Config

CACHE_CONTROL = "private, max-age=31536000, immutable"
CONTENT_TYPE = "image/webp"
PREFIXES = ("processed/", "images/")


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=os.environ["OCI_S3_ENDPOINT"],
        aws_access_key_id=os.environ["OCI_ACCESS_KEY"],
        aws_secret_access_key=os.environ["OCI_SECRET_KEY"],
        config=Config(signature_version="s3v4"),
    )


def head(s3, bucket, key):
    meta = s3.head_object(Bucket=bucket, Key=key)
    return meta.get("CacheControl"), meta.get("ContentType")


def apply(s3, bucket, key):
    s3.copy_object(
        Bucket=bucket,
        Key=key,
        CopySource={"Bucket": bucket, "Key": key},
        MetadataDirective="REPLACE",
        ContentType=CONTENT_TYPE,
        CacheControl=CACHE_CONTROL,
    )


def iter_target_keys(s3, bucket):
    paginator = s3.get_paginator("list_objects_v2")
    for prefix in PREFIXES:
        for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
            for obj in page.get("Contents", []):
                key = obj["Key"]
                # 변환본은 모두 WebP다. 다른 형식이 섞여 있으면 ContentType을 잘못 덮어쓰므로 건너뛴다.
                if key.endswith(".webp"):
                    yield key


def run_single(s3, bucket, key):
    cache_control, content_type = head(s3, bucket, key)
    print(f"[before] {key} cache-control={cache_control!r} content-type={content_type!r}")
    apply(s3, bucket, key)
    cache_control, content_type = head(s3, bucket, key)
    print(f"[after ] {key} cache-control={cache_control!r} content-type={content_type!r}")
    if cache_control != CACHE_CONTROL:
        print("Cache-Control이 저장되지 않았다. OCI S3 호환 API가 지원하지 않는 것으로 보고 중단한다.")
        return 1
    if content_type != CONTENT_TYPE:
        print("Content-Type이 바뀌었다. 전체 적용 전에 확인이 필요하다.")
        return 1
    print("HEAD 기준 정상. GET 응답 헤더도 curl로 확인한 뒤 전체 적용을 진행한다.")
    return 0


def run_all(s3, bucket, do_apply):
    total = skipped = done = failed = 0
    for key in iter_target_keys(s3, bucket):
        total += 1
        try:
            cache_control, _ = head(s3, bucket, key)
            if cache_control == CACHE_CONTROL:
                skipped += 1
                continue
            if do_apply:
                apply(s3, bucket, key)
            done += 1
        except Exception as e:  # noqa: BLE001 — 한 객체 실패로 전체를 멈추지 않는다
            failed += 1
            print(f"[fail] {key}: {e}", file=sys.stderr)
        if total % 500 == 0:
            print(f"... {total}개 확인 (적용 {done}, 건너뜀 {skipped}, 실패 {failed})")

    mode = "적용" if do_apply else "적용 대상(dry-run)"
    print(f"완료 — 전체 {total}, {mode} {done}, 이미 적용됨 {skipped}, 실패 {failed}")
    return 1 if failed else 0


def main():
    parser = argparse.ArgumentParser(description="기존 사진 객체에 Cache-Control 적용")
    parser.add_argument("--key", help="이 객체 하나만 적용하고 전후 헤더를 출력한다")
    parser.add_argument("--apply", action="store_true", help="실제로 적용한다. 없으면 dry-run")
    args = parser.parse_args()

    s3 = get_s3_client()
    bucket = os.environ["OCI_BUCKET_NAME"]

    if args.key:
        return run_single(s3, bucket, args.key)
    return run_all(s3, bucket, args.apply)


if __name__ == "__main__":
    sys.exit(main())
