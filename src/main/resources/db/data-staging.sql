-- user 테이블 생성
CREATE TABLE IF NOT EXISTS user
(
    user_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name      VARCHAR(255) NOT NULL,
    user_nick_name VARCHAR(255),
    user_email     VARCHAR(255) NOT NULL UNIQUE,
    provider       VARCHAR(255) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- user 테이블 초기 데이터 삽입
INSERT INTO user (user_id, provider, user_name, user_email, user_nick_name)
VALUES (4, 'google', 'Mark Kwon', 'redhero8830@gmail.com', '나는야혁준'),
       (5, 'kakao', '권혁준', 'redhero8830@naver.com', '테스트')
ON DUPLICATE KEY UPDATE user_name      = VALUES(user_name),
                        user_email     = VALUES(user_email),
                        user_nick_name = VALUES(user_nick_name),
                        provider       = VALUES(provider);


-- Trip 테이블 생성
CREATE TABLE IF NOT EXISTS trip
(
    trip_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    trip_title VARCHAR(255),
    country    VARCHAR(255),
    start_date DATE,
    end_date   DATE,
    hashtags   VARCHAR(255),
    CONSTRAINT fk_trip_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Trip 공유 사용자 관계 테이블 생성
CREATE TABLE IF NOT EXISTS trip_shared_users
(
    trip_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (trip_id, user_id),
    CONSTRAINT fk_trip_shared_trip FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_shared_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

-- trip 테이블 초기 데이터 삽입
INSERT INTO trip (trip_id, country, hashtags, end_date, user_id, start_date, trip_title)
VALUES (2, '🇳🇱/네덜란드/NETHERLANDS', '우리끼리,버킷리스트,취향저격', '2023-07-25', 4, '2023-07-19', '왜애'),
       (3, '🇬🇷/그리스/GREECE', '우리끼리', '2023-07-25', 5, '2023-07-19', '왜애2');

-- PinPoint 테이블 생성
CREATE TABLE IF NOT EXISTS pin_point
(
    pin_point_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- PK
    trip_id      BIGINT NOT NULL,                   -- Trip과의 외래키
    latitude     DOUBLE,
    longitude    DOUBLE,
    CONSTRAINT fk_pinpoint_trip FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- pin_point 테이블 초기 데이터 삽입
INSERT INTO pin_point (pin_point_id, latitude, trip_id, longitude)
VALUES (3, 41.390299999999996, 2, 2.1676444444444445),
       (4, 41.40457222222222, 2, 2.1758416666666665),
       (5, 41.389377777777774, 2, 2.1863777777777775),
       (6, 41.390299999999996, 3, 2.1676444444444445),
       (7, 41.40457222222222, 3, 2.1758416666666665),
       (8, 41.389377777777774, 3, 2.1863777777777775);

-- media_file 테이블 생성
CREATE TABLE IF NOT EXISTS media_file
(
    media_file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id       BIGINT       NOT NULL,
    pin_point_id  BIGINT       NOT NULL,
    media_type    VARCHAR(50),
    media_link    VARCHAR(255),
    record_date   DATETIME,
    latitude      DOUBLE,
    longitude     DOUBLE,
    media_key     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_media_file_trip FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_media_file_pinpoint FOREIGN KEY (pin_point_id) REFERENCES pin_point (pin_point_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- media_file 테이블에 인덱스 추가
CREATE INDEX idx_media_file_trip_id ON media_file (trip_id);
CREATE INDEX idx_media_file_pin_point_id ON media_file (pin_point_id);
CREATE INDEX idx_media_file_trip_id_record_date ON media_file (trip_id, record_date);

-- media_file 테이블 초기 데이터 삽입
INSERT INTO media_file (media_file_id, latitude, media_key, longitude, pin_point_id, record_date,
                        media_link, media_type, trip_id)
VALUES (6, 41.390299999999996, 'upload/2/IMG#_2121.webp', 2.1676444444444445, 3,
        '2023-07-19 23:48:16.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_2121.webp',
        'image/webp', 2),
       (7, 41.39061111111111, 'upload/2/IMG#_2127.webp', 2.166447222222222, 3,
        '2023-07-19 23:50:46.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_2127.webp',
        'image/webp', 2),
       (8, 41.39180833333333, 'upload/2/IMG#_2162.webp', 2.1651249999999997, 3,
        '2023-07-19 23:55:06.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_2162.webp',
        'image/webp', 2),
       (9, 41.40457222222222, 'upload/2/IMG#_2946.webp', 2.1758416666666665, 4,
        '2023-07-25 23:56:40.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_2946.webp',
        'image/webp', 2),
       (10, 41.391844444444445, 'upload/2/IMG#_4275.webp', 2.1650638888888887, 3,
        '2023-07-22 21:47:01.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_4275.webp',
        'image/webp', 2),
       (11, 41.40114166666667, 'upload/2/IMG#_6074.webp', 2.18645, 4, '2023-07-22 14:45:43.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6074.webp',
        'image/webp', 2),
       (12, 41.401180555555555, 'upload/2/IMG#_6077.webp', 2.1864555555555554, 4,
        '2023-07-22 14:45:49.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6077.webp',
        'image/webp', 2),
       (13, 41.40105555555555, 'upload/2/IMG#_6119.webp', 2.186330555555555, 4,
        '2023-07-22 15:13:03.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6119.webp',
        'image/webp', 2),
       (14, 41.401019444444444, 'upload/2/IMG#_6124.webp', 2.1863527777777776, 4,
        '2023-07-22 15:13:18.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6124.webp',
        'image/webp', 2),
       (15, 41.401022222222224, 'upload/2/IMG#_6125.webp', 2.186330555555555, 4,
        '2023-07-22 15:13:19.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6125.webp',
        'image/webp', 2),
       (16, 41.40102777777778, 'upload/2/IMG#_6126.webp', 2.186319444444444, 4,
        '2023-07-22 15:13:20.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6126.webp',
        'image/webp', 2),
       (17, 41.389377777777774, 'upload/2/IMG#_6208.webp', 2.1863777777777775, 5,
        '2023-07-22 16:26:53.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6208.webp',
        'image/webp', 2),
       (18, 41.38937222222222, 'upload/2/IMG#_6212.webp', 2.186372222222222, 5,
        '2023-07-22 16:26:59.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/2/IMG#_6212.webp',
        'image/webp', 2),
       (19, 41.390299999999996, 'upload/3/IMG#_2121.webp', 2.1676444444444445, 6,
        '2023-07-19 23:48:16.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_2121.webp',
        'image/webp', 3),
       (20, 41.39061111111111, 'upload/3/IMG#_2127.webp', 2.166447222222222, 6,
        '2023-07-19 23:50:46.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_2127.webp',
        'image/webp', 3),
       (21, 41.39180833333333, 'upload/3/IMG#_2162.webp', 2.1651249999999997, 6,
        '2023-07-19 23:55:06.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_2162.webp',
        'image/webp', 3),
       (22, 41.40457222222222, 'upload/3/IMG#_2946.webp', 2.1758416666666665, 7,
        '2023-07-25 23:56:40.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_2946.webp',
        'image/webp', 3),
       (23, 41.391844444444445, 'upload/3/IMG#_4275.webp', 2.1650638888888887, 6,
        '2023-07-22 21:47:01.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_4275.webp',
        'image/webp', 3),
       (24, 41.40114166666667, 'upload/3/IMG#_6074.webp', 2.18645, 7, '2023-07-22 14:45:43.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6074.webp',
        'image/webp', 3),
       (25, 41.401180555555555, 'upload/3/IMG#_6077.webp', 2.1864555555555554, 7,
        '2023-07-22 14:45:49.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6077.webp',
        'image/webp', 3),
       (26, 41.40105555555555, 'upload/3/IMG#_6119.webp', 2.186330555555555, 7,
        '2023-07-22 15:13:03.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6119.webp',
        'image/webp', 3),
       (27, 41.401019444444444, 'upload/3/IMG#_6124.webp', 2.1863527777777776, 7,
        '2023-07-22 15:13:18.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6124.webp',
        'image/webp', 3),
       (28, 41.401022222222224, 'upload/3/IMG#_6125.webp', 2.186330555555555, 7,
        '2023-07-22 15:13:19.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6125.webp',
        'image/webp', 3),
       (29, 41.40102777777778, 'upload/3/IMG#_6126.webp', 2.186319444444444, 7,
        '2023-07-22 15:13:20.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6126.webp',
        'image/webp', 3),
       (30, 41.389377777777774, 'upload/3/IMG#_6208.webp', 2.1863777777777775, 8,
        '2023-07-22 16:26:53.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6208.webp',
        'image/webp', 3),
       (31, 41.38937222222222, 'upload/3/IMG#_6212.webp', 2.186372222222222, 8,
        '2023-07-22 16:26:59.000000',
        'https://staging-trip-tyche.s3.ap-northeast-2.amazonaws.com/upload/3/IMG#_6212.webp',
        'image/webp', 3);

