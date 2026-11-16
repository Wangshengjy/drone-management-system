-- 追加演示数据（在 seed.sql 已执行后运行）
-- 幂等：按唯一业务键（备案号/信用代码等）判断，可重复执行不会重复插入主数据。
SET NAMES utf8mb4;
USE emergency_db;

SET @pwd := '123456';

INSERT INTO pilot (user_id, full_name, id_card, phone, email, record_no, region, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
SELECT NULL, '赵磊', '370102199305201111', '13700137001', 'zhaolei@example.com', 'UAS-SD-2025-0401', '山东省', 'APPROVED',
  'PASS', 85, '["追加样例：预审通过"]', '2025-04-01 10:00:00', '2025-04-10 15:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM pilot WHERE record_no = 'UAS-SD-2025-0401');

INSERT INTO pilot (user_id, full_name, id_card, phone, email, record_no, region, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
SELECT NULL, '孙悦', '320106199801022222', '13700137002', 'sunyue@example.com', NULL, '江苏省', 'PENDING_REVIEW',
  'NEED_MANUAL', 55, '["追加样例：待人工复核"]', '2025-04-12 09:30:00', '2025-04-12 09:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM pilot WHERE phone = '13700137002' AND full_name = '孙悦');

INSERT INTO organization (user_id, name, org_type, credit_code, contact_name, phone, address, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
SELECT NULL, '齐鲁无人机巡检服务部', 'ENTERPRISE', '91370100MA3ABCDEF0', '吴刚', '053188880001', '济南市历下区经十路 100 号', 'APPROVED',
  'PASS', 91, '["追加样例"]', '2025-03-15 11:00:00', '2025-03-28 14:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE credit_code = '91370100MA3ABCDEF0');

INSERT INTO organization (user_id, name, org_type, credit_code, contact_name, phone, address, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
SELECT NULL, '苏南航拍爱好者协会', 'OTHER', '52320100MJKL9999AB', '郑华', '02588880002', '南京市鼓楼区中山路 200 号', 'PENDING_REVIEW',
  'NEED_MANUAL', 60, '["追加样例"]', '2025-04-05 16:00:00', '2025-04-05 16:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE credit_code = '52320100MJKL9999AB');

SET @p1 := (SELECT id FROM pilot WHERE record_no = 'UAS-SD-2025-0401' LIMIT 1);
SET @p2 := (SELECT id FROM pilot WHERE phone = '13700137002' AND full_name = '孙悦' LIMIT 1);
SET @o1 := (SELECT id FROM organization WHERE credit_code = '91370100MA3ABCDEF0' LIMIT 1);
SET @o2 := (SELECT id FROM organization WHERE credit_code = '52320100MJKL9999AB' LIMIT 1);

INSERT INTO users (id, username, password, role, pilot_id, org_id, status, create_time)
VALUES
(101, 'pilot_zhaolei', @pwd, 'PILOT', @p1, NULL, 1, '2025-04-01 10:05:00'),
(102, 'pilot_sunyue', @pwd, 'PILOT', @p2, NULL, 1, '2025-04-12 09:31:00'),
(103, 'org_qilu', @pwd, 'ORG', NULL, @o1, 1, '2025-03-15 11:05:00'),
(104, 'org_sunan', @pwd, 'ORG', NULL, @o2, 1, '2025-04-05 16:05:00')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  pilot_id = VALUES(pilot_id),
  org_id = VALUES(org_id),
  enabled = VALUES(enabled);

UPDATE pilot SET user_id = 101 WHERE id = @p1 AND @p1 IS NOT NULL;
UPDATE pilot SET user_id = 102 WHERE id = @p2 AND @p2 IS NOT NULL;
UPDATE organization SET user_id = 103 WHERE id = @o1 AND @o1 IS NOT NULL;
UPDATE organization SET user_id = 104 WHERE id = @o2 AND @o2 IS NOT NULL;

INSERT INTO pilot_certificate (pilot_id, cert_type, cert_no, valid_until, attachment_url, created_at)
SELECT @p1, 'CAAC 小型旋翼', 'CAAC-M-2024-5566', '2028-12-31', '/uploads/demo-cert-extra-1.pdf', '2025-04-01 10:10:00'
FROM DUAL
WHERE @p1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pilot_certificate WHERE pilot_id = @p1 AND cert_no = 'CAAC-M-2024-5566');

INSERT INTO pilot_certificate (pilot_id, cert_type, cert_no, valid_until, attachment_url, created_at)
SELECT @p2, 'AOPA 多旋翼', 'AOPA-D-2025-3300', '2029-01-01', NULL, '2025-04-12 09:35:00'
FROM DUAL
WHERE @p2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pilot_certificate WHERE pilot_id = @p2 AND cert_no = 'AOPA-D-2025-3300');

INSERT INTO pilot_application (pilot_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
SELECT @p2, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 55, '["追加样例"]', '2025-04-12 09:30:00'
FROM DUAL
WHERE @p2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM pilot_application WHERE pilot_id = @p2 AND created_at = '2025-04-12 09:30:00' AND status = 'PENDING_REVIEW');

INSERT INTO pilot_application (pilot_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
SELECT @p1, 'SUBMIT', 'APPROVED', 'PASS', 85, '["追加样例"]', '2025-04-08 11:00:00'
FROM DUAL
WHERE @p1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM pilot_application WHERE pilot_id = @p1 AND created_at = '2025-04-08 11:00:00' AND status = 'APPROVED');

INSERT INTO org_application (org_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
SELECT @o2, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 60, '["追加样例"]', '2025-04-05 16:00:00'
FROM DUAL
WHERE @o2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM org_application WHERE org_id = @o2 AND created_at = '2025-04-05 16:00:00' AND status = 'PENDING_REVIEW');

INSERT INTO org_application (org_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
SELECT @o1, 'SUBMIT', 'APPROVED', 'PASS', 91, '["追加样例"]', '2025-03-20 10:00:00'
FROM DUAL
WHERE @o1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM org_application WHERE org_id = @o1 AND created_at = '2025-03-20 10:00:00' AND status = 'APPROVED');

INSERT INTO audit_log (entity_type, entity_id, action, operator_user_id, comment_text, detail_json, created_at)
SELECT 'PILOT', @p1, 'MANUAL_REVIEW', 100, '追加样例：公示通过', '{"decision":"APPROVE","batch":"seed_extra"}', '2025-04-10 15:00:00'
FROM DUAL
WHERE @p1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM audit_log WHERE entity_type = 'PILOT' AND entity_id = @p1 AND action = 'MANUAL_REVIEW' AND created_at = '2025-04-10 15:00:00');

INSERT INTO audit_log (entity_type, entity_id, action, operator_user_id, comment_text, detail_json, created_at)
SELECT 'ORG', @o1, 'MANUAL_REVIEW', 100, '追加样例：单位审核通过', '{"decision":"APPROVE"}', '2025-03-28 14:00:00'
FROM DUAL
WHERE @o1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM audit_log WHERE entity_type = 'ORG' AND entity_id = @o1 AND action = 'MANUAL_REVIEW' AND created_at = '2025-03-28 14:00:00');

INSERT INTO audit_log (entity_type, entity_id, action, operator_user_id, comment_text, detail_json, created_at)
SELECT 'PILOT', @p2, 'SUBMIT_AUTO', 102, '自动预审: NEED_MANUAL', '["追加样例"]', '2025-04-12 09:30:00'
FROM DUAL
WHERE @p2 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM audit_log WHERE entity_type = 'PILOT' AND entity_id = @p2 AND action = 'SUBMIT_AUTO' AND created_at = '2025-04-12 09:30:00');

ALTER TABLE users AUTO_INCREMENT = 200;
