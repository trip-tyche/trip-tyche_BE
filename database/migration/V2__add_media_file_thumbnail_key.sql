-- 표지용 축소본 키. API 배포 전에 운영 DB에 먼저 적용한다(prod ddl-auto: validate).
ALTER TABLE media_file
    ADD COLUMN thumbnail_key VARCHAR(255) NULL;
