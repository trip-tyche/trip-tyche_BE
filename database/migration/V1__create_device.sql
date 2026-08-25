CREATE TABLE device (
    device_id   BIGINT                NOT NULL AUTO_INCREMENT,
    user_id     BIGINT                NOT NULL,
    token       VARCHAR(255)          NOT NULL,
    platform    ENUM('ANDROID','IOS') NOT NULL,
    app_version VARCHAR(20)           NULL,
    created_at  DATETIME(6)           NOT NULL,
    updated_at  DATETIME(6)           NOT NULL,
    PRIMARY KEY (device_id),
    UNIQUE KEY uq_device_token (token),
    KEY idx_device_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
