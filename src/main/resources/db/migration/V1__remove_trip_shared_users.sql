-- trip_shared_users 조인 테이블 제거
-- 공유 관계는 share 테이블의 share_status = 'APPROVED'로 단일 관리
DROP TABLE IF EXISTS trip_shared_users;