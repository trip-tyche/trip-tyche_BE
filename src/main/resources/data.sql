-- User 데이터 삽입
INSERT INTO user (provider, user_nick_name, user_name, user_email)
VALUES ('google', 'TestUser1', 'Mark', 'abc@ac.kr'),
       ('google', 'TestUser2', 'John', 'def@ac.kr');

-- Trip 데이터 삽입
INSERT INTO trip (user_id, trip_title, country, start_date, end_date, hashtags)
VALUES (1, 'Test Trip 1', 'South Korea', '2024-01-01', '2024-01-10', '#vacation'),
       (1, 'Test Trip 2', 'Japan', '2024-02-01', '2024-02-15', '#adventure');

-- PinPoint 데이터 삽입
INSERT INTO pin_point (trip_id, latitude, longitude, pin_point_image_date, pin_point_image_link)
VALUES (1, 37.5665, 126.9780, '2024-01-01', 'https://example.com/image1.jpg'),
       (2, 35.6762, 139.6503, '2024-02-01', 'https://example.com/image2.jpg');
