-- K6 트래픽 테스트용 사전 데이터 세팅 (PostgreSQL)
--
-- 실행 전:
--   1. BCrypt 해시 생성: new BCryptPasswordEncoder().encode("Test1234!")
--      → IntelliJ 콘솔에서 직접 실행하거나 아래 메모 공간에 복사
--   2. {BCRYPT_HASH} 를 실제 해시 값으로 교체 후 실행
--
-- 예시 해시 (로컬 테스트용, 실서비스 절대 사용 금지):
--   Test1234! → $2a$10$슬래시포함64자해시
-- ─────────────────────────────────────────────────────────────

-- 기존 테스트 데이터 초기화 (재실행 시)
DELETE FROM info   WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'test%@test.com');
DELETE FROM member WHERE email LIKE 'test%@test.com';

-- 테스트 멤버 200명 생성
INSERT INTO member (email, password, nickname, role, created_at, updated_at)
SELECT
    'test' || gs || '@test.com',
    '{BCRYPT_HASH}',
    'TestUser' || gs,
    'MEMBER',
    NOW(),
    NOW()
FROM generate_series(1, 200) AS gs;

-- 각 멤버에 대한 Info 레코드 생성 (req_count 0 으로 초기화)
INSERT INTO info (member_id, req_count, is_subscribe, created_at, updated_at)
SELECT id, 0, false, NOW(), NOW()
FROM member
WHERE email LIKE 'test%@test.com';

-- 테스트 데이터 확인
SELECT COUNT(*) AS member_count FROM member WHERE email LIKE 'test%@test.com';
SELECT COUNT(*) AS info_count   FROM info   WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'test%@test.com');