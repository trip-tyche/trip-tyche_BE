CREATE TABLE guest_session
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_user_id BIGINT     NOT NULL,
    share_sent    TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_user_id) REFERENCES `user` (user_id) ON DELETE CASCADE
);