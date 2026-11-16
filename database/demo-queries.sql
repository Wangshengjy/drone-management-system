-- 本地快速演示：与公开接口数据一致（mysql 客户端执行）
USE emergency_db;

SELECT '已通过备案飞手' AS section;
SELECT id, full_name, record_no, region,
  CONCAT(LEFT(phone,3),'****',RIGHT(phone,4)) AS phone_masked
FROM pilot WHERE status = 'APPROVED';

SELECT '协作单位（已通过）' AS section;
SELECT id, name, org_type, address FROM organization WHERE status = 'APPROVED';

SELECT '待人工复核队列' AS section;
SELECT 'PILOT' AS kind, id, full_name, status FROM pilot WHERE status = 'PENDING_REVIEW'
UNION ALL
SELECT 'ORG', id, name, status FROM organization WHERE status = 'PENDING_REVIEW';
