-- user 테이블 생성
CREATE TABLE IF NOT EXISTS user
(
    userId       BIGINT AUTO_INCREMENT PRIMARY KEY, -- Primary Key
    userName     VARCHAR(255) NOT NULL,             -- 이름
    userNickName VARCHAR(255),                      -- 닉네임
    userEmail    VARCHAR(255) NOT NULL UNIQUE,      -- 이메일 (UNIQUE 제약 조건)
    provider     VARCHAR(255) NOT NULL              -- 제공자 (google, kakao 등)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- user 테이블 초기 데이터 삽입
INSERT INTO user (userId, provider, userName, userEmail, userNickName)
VALUES (4, 'google', 'Mark Kwon', 'redhero8830@gmail.com', '나는야혁준'),
       (5, 'kakao', '권혁준', 'redhero8830@naver.com', '테스트')
ON DUPLICATE KEY UPDATE userName     = VALUES(userName),
                        userEmail    = VALUES(userEmail),
                        userNickName = VALUES(userNickName),
                        provider     = VALUES(provider);


-- Trip 테이블 생성
CREATE TABLE IF NOT EXISTS trip
(
    tripId     BIGINT AUTO_INCREMENT PRIMARY KEY, -- 기본 키
    user_id    BIGINT NOT NULL,                   -- 사용자 ID (외래 키)
    trip_title VARCHAR(255),                      -- 여행 제목
    country    VARCHAR(255),                      -- 국가
    start_date DATE,                              -- 여행 시작 날짜
    end_date   DATE,                              -- 여행 종료 날짜
    hashtags   VARCHAR(255),                      -- 해시태그 문자열
    CONSTRAINT fk_trip_user FOREIGN KEY (user_id) REFERENCES user (userId) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Trip 공유 사용자 관계 테이블 생성
CREATE TABLE IF NOT EXISTS trip_shared_users
(
    trip_id BIGINT NOT NULL,        -- Trip ID (외래 키)
    user_id BIGINT NOT NULL,        -- 공유된 사용자 ID (외래 키)
    PRIMARY KEY (trip_id, user_id), -- 복합 기본 키
    CONSTRAINT fk_trip_shared_trip FOREIGN KEY (trip_id) REFERENCES trip (tripId) ON DELETE CASCADE,
    CONSTRAINT fk_trip_shared_user FOREIGN KEY (user_id) REFERENCES user (userId) ON DELETE CASCADE
);

-- trip 테이블 초기 데이터 삽입
INSERT INTO trip (trip_id, country, hashtags, end_date, user_id, start_date, trip_title)
VALUES (2, '🇳🇱/네덜란드/NETHERLANDS', '우리끼리,버킷리스트,취향저격', '2023-07-25', 4, '2023-07-19', '왜애'),
       (3, '🇬🇷/그리스/GREECE', '우리끼리', '2023-07-25', 5, '2023-07-19', '왜애2');

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
    media_file_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 기본 키
    trip_id       BIGINT       NOT NULL,             -- Trip ID (외래 키)
    pin_point_id  BIGINT       NOT NULL,             -- PinPoint ID (외래 키)
    media_type    VARCHAR(50),                       -- 미디어 타입
    media_link    VARCHAR(255),                      -- 미디어 링크
    record_date   DATETIME,                          -- 기록 날짜/시간
    latitude      DOUBLE,                            -- 위도
    longitude     DOUBLE,                            -- 경도
    media_key     VARCHAR(255) NOT NULL,             -- 미디어 키 (고유 값)
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

