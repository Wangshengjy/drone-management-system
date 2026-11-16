-- =============================================================================
-- 模拟数据（演示 / 答辩用，含 20 条审核队列样本）
-- 执行顺序：先 schema.sql 建表，再执行本脚本
-- 登录口令（明文）：123456
-- =============================================================================

SET NAMES utf8mb4;
USE emergency_db;

SET @pwd := '123456';

-- 可重复导入：先清理演示数据（保留 review_rule_config）
DELETE FROM audit_log;
DELETE FROM pilot_application;
DELETE FROM org_application;
DELETE FROM pilot_certificate;
DELETE FROM users WHERE id >= 10;
DELETE FROM pilot;
DELETE FROM organization;

-- -----------------------------------------------------------------------------
-- 飞手（18 条，其中待审 10 条）
-- -----------------------------------------------------------------------------
INSERT INTO pilot (id, user_id, full_name, id_card, phone, email, record_no, region, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
VALUES
(1, NULL, '张伟', '110101199003151234', '13800138001', 'zhangwei@example.com', 'UAS-BJ-2024-0101', '北京市', 'APPROVED', 'PASS', 88, '["加权分通过阈值","certCount=2, score=88"]', '2024-06-01 09:00:00', '2024-06-15 14:30:00'),
(2, NULL, '李娜', '310115198807203456', '13900139002', 'lina@example.com', 'UAS-SH-2024-0208', '上海市', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["分数处于人工复核区间"]', '2024-08-10 11:20:00', '2025-03-01 10:00:00'),
(3, NULL, '王强', '440106199512055678', '13700137003', 'wangqiang@example.com', 'UAS-GD-2024-0306', '广东省', 'REJECTED', 'REJECT', 45, '["证件照片模糊，备案号与地区不一致"]', '2024-03-20 08:00:00', '2024-04-01 16:00:00'),
(4, NULL, '刘洋', '330106200001016789', '13600136004', 'liuyang@example.com', 'UAS-ZJ-2025-0402', '浙江省', 'DRAFT', NULL, NULL, NULL, '2025-01-05 12:00:00', '2025-01-05 12:00:00'),
(5, NULL, '陈静', '510104199211227890', '13500135005', 'chenjing@example.com', 'UAS-SC-2024-0303', '四川省', 'FROZEN', 'PASS', 75, '["演示冻结账号"]', '2024-09-12 15:00:00', '2024-11-01 09:00:00'),
(6, NULL, '赵磊', '420106199507168765', '13400134006', 'zhaolei@example.com', 'UAS-HB-2024-0616', '湖北省', 'APPROVED', 'PASS', 85, '["材料完备，历史记录良好"]', '2024-06-18 10:10:00', '2024-06-25 18:20:00'),
(7, NULL, '孙悦', '320104199811054321', '13300133007', 'sunyue@example.com', 'UAS-JS-2024-0711', '江苏省', 'APPROVED', 'PASS', 91, '["资质齐全，评分优秀"]', '2024-07-20 09:40:00', '2024-07-28 14:15:00'),
(8, NULL, '周凯', '120105199304236666', '13200132008', 'zhoukai@example.com', 'UAS-TJ-2025-0803', '天津市', 'PENDING_REVIEW', 'NEED_MANUAL', 66, '["材料有效但分值接近阈值"]', '2025-02-08 13:00:00', '2025-03-12 17:05:00'),
(9, NULL, '吴敏', '350203199612129999', '13100131009', 'wumin@example.com', 'UAS-FJ-2024-0922', '福建省', 'APPROVED', 'PASS', 79, '["基础项通过，待年度复核"]', '2024-10-02 08:35:00', '2024-10-19 11:50:00'),
(10, NULL, '郑航', '610104199901018888', '13000130010', 'zhenghang@example.com', 'UAS-SN-2024-1010', '陕西省', 'REJECTED', 'REJECT', 50, '["联系人核验失败"]', '2024-10-15 16:00:00', '2024-10-20 10:30:00'),
(11, NULL, '许辰', '370102199302163214', '13911002011', 'xuchen@example.com', 'UAS-SD-2025-1101', '山东省', 'PENDING_REVIEW', 'NEED_MANUAL', 63, '["飞行项目说明待补充"]', '2025-03-18 09:15:00', '2025-03-18 09:15:00'),
(12, NULL, '何雨', '340103199511183526', '13911002012', 'heyu@example.com', 'UAS-AH-2025-1102', '安徽省', 'PENDING_REVIEW', 'NEED_MANUAL', 61, '["执照附件待人工核验"]', '2025-03-18 11:20:00', '2025-03-18 11:20:00'),
(13, NULL, '唐峻', '430104199110125718', '13911002013', 'tangjun@example.com', 'UAS-HN-2025-1103', '湖南省', 'PENDING_REVIEW', 'NEED_MANUAL', 59, '["过往任务记录需要补录"]', '2025-03-19 10:10:00', '2025-03-19 10:10:00'),
(14, NULL, '马岚', '450105199407086932', '13911002014', 'malan@example.com', 'UAS-GX-2025-1104', '广西壮族自治区', 'PENDING_REVIEW', 'NEED_MANUAL', 65, '["证照清晰但地区证明未上传"]', '2025-03-19 14:25:00', '2025-03-19 14:25:00'),
(15, NULL, '沈拓', '210102199612243417', '13911002015', 'shentuo@example.com', 'UAS-LN-2025-1105', '辽宁省', 'PENDING_REVIEW', 'NEED_MANUAL', 60, '["保险材料待补充"]', '2025-03-20 09:40:00', '2025-03-20 09:40:00'),
(16, NULL, '顾川', '530102199801176821', '13911002016', 'guchuan@example.com', 'UAS-YN-2025-1106', '云南省', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["备案号需人工比对"]', '2025-03-20 13:30:00', '2025-03-20 13:30:00'),
(17, NULL, '梁可', '610103199408284915', '13911002017', 'liangke@example.com', 'UAS-SX-2025-1107', '陕西省', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["飞行资历说明待复核"]', '2025-03-21 08:55:00', '2025-03-21 08:55:00'),
(18, NULL, '冉溪', '520103199706115624', '13911002018', 'ranxi@example.com', 'UAS-GZ-2025-1108', '贵州省', 'PENDING_REVIEW', 'NEED_MANUAL', 67, '["附件完整但评分接近阈值"]', '2025-03-21 16:05:00', '2025-03-21 16:05:00');

-- -----------------------------------------------------------------------------
-- 协作单位（18 条，其中待审 10 条）
-- -----------------------------------------------------------------------------
INSERT INTO organization (id, user_id, name, org_type, credit_code, contact_name, phone, address, status,
  auto_result, auto_score, auto_reason, created_at, updated_at)
VALUES
(1, NULL, '京测测绘科技有限公司', 'ENTERPRISE', '91110105MA01ABCD12', '赵敏', '01088880001', '北京市朝阳区建国路 88 号', 'APPROVED', 'PASS', 92, '["score=92"]', '2024-05-01 10:00:00', '2024-05-20 11:00:00'),
(2, NULL, '蓝天应急救援志愿服务中心', 'VOLUNTEER', '52100000MJY12345X1', '孙伟', '02166660002', '上海市浦东新区世纪大道 1 号', 'APPROVED', 'PASS', 90, '["score=90"]', '2024-07-01 09:30:00', '2024-07-18 15:00:00'),
(3, NULL, '江南无人机应用促进会', 'OTHER', '53330100MJKL6789XY', '周芳', '057188880003', '杭州市西湖区文三路 259 号', 'PENDING_REVIEW', 'NEED_MANUAL', 58, '["需人工复核"]', '2025-02-01 14:00:00', '2025-03-10 10:00:00'),
(4, NULL, '华北航测服务有限公司', 'ENTERPRISE', '91110000MA00ZZZZ9X', '钱七', '01012345678', '北京市丰台区汽车博物馆东路 6 号', 'REJECTED', 'REJECT', 0, '["命中黑名单关键词"]', '2024-11-11 11:11:00', '2024-11-12 09:00:00'),
(5, NULL, '岭南低空巡检有限公司', 'ENTERPRISE', '91440101MA5ABCDE34', '林涛', '02088885555', '广州市天河区黄埔大道西 120 号', 'APPROVED', 'PASS', 87, '["资质与合同完整"]', '2024-08-08 09:10:00', '2024-08-22 16:45:00'),
(6, NULL, '西部灾害监测志愿联盟', 'VOLUNTEER', '52610000MJY77889A2', '高洁', '02966667777', '西安市雁塔区科技二路 72 号', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["需补充活动证明"]', '2025-01-12 11:30:00', '2025-03-03 09:05:00'),
(7, NULL, '中原农业植保服务站', 'OTHER', '52410000MJT22334B8', '韩冰', '037166668888', '郑州市金水区农业路 36 号', 'APPROVED', 'PASS', 81, '["历史服务记录良好"]', '2024-06-06 08:20:00', '2024-06-18 10:40:00'),
(8, NULL, '北方智慧巡检科技有限公司', 'ENTERPRISE', '91120116MA07QWER56', '吕航', '02255556666', '天津市滨海新区中新生态城创新大道 66 号', 'APPROVED', 'PASS', 89, '["评分达标"]', '2024-09-09 13:13:00', '2024-09-19 17:25:00'),
(9, NULL, '东海无人机科普中心', 'VOLUNTEER', '52330000MJU55667C3', '冯蕾', '057188889999', '宁波市鄞州区首南西路 88 号', 'DRAFT', NULL, NULL, NULL, '2025-02-20 10:10:00', '2025-02-20 10:10:00'),
(10, NULL, '川渝联合空域服务社', 'OTHER', '52510000MJK88776D4', '唐宁', '02877778888', '成都市高新区天府大道北段 1700 号', 'FROZEN', 'PASS', 76, '["演示冻结单位"]', '2024-12-01 14:25:00', '2025-01-08 09:00:00'),
(11, NULL, '华东电网空巡中心', 'ENTERPRISE', '91310115MA11HTRA01', '曹越', '02160010011', '上海市浦东新区张江路 188 号', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["合同附件待核实"]', '2025-03-18 09:00:00', '2025-03-18 09:00:00'),
(12, NULL, '海西低空应急协作站', 'VOLUNTEER', '52350000MJU11223A4', '梁雪', '05920010012', '厦门市思明区演武路 72 号', 'PENDING_REVIEW', 'NEED_MANUAL', 60, '["活动记录待补充"]', '2025-03-18 15:20:00', '2025-03-18 15:20:00'),
(13, NULL, '华中巡测服务有限公司', 'ENTERPRISE', '91420100MA4PXYZA77', '彭浩', '02770010013', '武汉市东湖高新区光谷大道 99 号', 'PENDING_REVIEW', 'NEED_MANUAL', 65, '["营业执照与联系人待复核"]', '2025-03-19 09:35:00', '2025-03-19 09:35:00'),
(14, NULL, '渤海空域保障协会', 'OTHER', '52120000MJT66543B2', '侯晨', '02270010014', '天津市河西区友谊路 120 号', 'PENDING_REVIEW', 'NEED_MANUAL', 59, '["章程附件不完整"]', '2025-03-19 16:00:00', '2025-03-19 16:00:00'),
(15, NULL, '赣北农业飞防联盟', 'OTHER', '52360000MJT99871C8', '盛楠', '07910010015', '南昌市红谷滩区会展路 66 号', 'PENDING_REVIEW', 'NEED_MANUAL', 63, '["服务案例需要补录"]', '2025-03-20 08:40:00', '2025-03-20 08:40:00'),
(16, NULL, '西南测绘联合工坊', 'ENTERPRISE', '91510100MA6ABCDX09', '罗川', '02870010016', '成都市武侯区天府三街 188 号', 'PENDING_REVIEW', 'NEED_MANUAL', 66, '["设备台账需人工确认"]', '2025-03-20 14:10:00', '2025-03-20 14:10:00'),
(17, NULL, '北疆无人机志愿团', 'VOLUNTEER', '52650000MJY33445D6', '吴昊', '09910010017', '乌鲁木齐市水磨沟区红光山路 88 号', 'PENDING_REVIEW', 'NEED_MANUAL', 61, '["负责人资质待复核"]', '2025-03-21 10:20:00', '2025-03-21 10:20:00'),
(18, NULL, '长江流域空巡协作平台', 'OTHER', '52430000MJT44556E1', '姜珂', '07310010018', '长沙市岳麓区潇湘大道 520 号', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["历史项目清单待确认"]', '2025-03-21 15:30:00', '2025-03-21 15:30:00');

-- -----------------------------------------------------------------------------
-- 飞手资质证书（10 条）
-- -----------------------------------------------------------------------------
INSERT INTO pilot_certificate (pilot_id, cert_type, cert_no, valid_until, attachment_url, created_at)
VALUES
(1, 'AOPA 多旋翼', 'AOPA-D-2023-8899', '2027-12-31', '/uploads/demo-cert-1.pdf', '2024-06-01 09:30:00'),
(1, 'UTC 测绘', 'UTC-SURVEY-2024-001', '2026-06-30', '/uploads/demo-cert-2.pdf', '2024-06-02 10:00:00'),
(2, 'AOPA 多旋翼', 'AOPA-D-2024-2200', '2028-01-01', '/uploads/demo-cert-3.pdf', '2024-08-10 11:25:00'),
(3, '民航局执照', 'CAAC-UAV-2024-0306', '2027-03-01', '/uploads/demo-cert-4.pdf', '2024-03-20 09:00:00'),
(4, '警航协作培训', 'POL-UAV-2025-0402', '2028-04-01', '/uploads/demo-cert-5.pdf', '2025-01-05 12:10:00'),
(5, 'AOPA 多旋翼', 'AOPA-D-2023-3305', '2026-12-31', '/uploads/demo-cert-6.pdf', '2024-09-12 15:10:00'),
(6, 'UTC 巡检', 'UTC-INSP-2024-0616', '2027-06-15', '/uploads/demo-cert-7.pdf', '2024-06-18 10:20:00'),
(7, 'AOPA 多旋翼', 'AOPA-D-2024-0711', '2028-07-10', '/uploads/demo-cert-8.pdf', '2024-07-20 10:00:00'),
(8, '民航局执照', 'CAAC-UAV-2025-0803', '2029-08-02', '/uploads/demo-cert-9.pdf', '2025-02-08 13:20:00'),
(9, '植保专项', 'AGRI-UAV-2024-0922', '2027-09-21', '/uploads/demo-cert-10.pdf', '2024-10-02 08:55:00');

-- -----------------------------------------------------------------------------
-- 系统用户（18 飞手 + 18 单位 + 1 访客）
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, password, role, pilot_id, org_id, status, create_time)
VALUES
(10, 'pilot_zhangwei', @pwd, 'PILOT', 1, NULL, 1, '2024-06-01 09:05:00'),
(11, 'pilot_lina', @pwd, 'PILOT', 2, NULL, 1, '2024-08-10 11:21:00'),
(12, 'pilot_wangqiang', @pwd, 'PILOT', 3, NULL, 1, '2024-03-20 08:05:00'),
(13, 'pilot_liuyang', @pwd, 'PILOT', 4, NULL, 1, '2025-01-05 12:01:00'),
(14, 'pilot_chenjing', @pwd, 'PILOT', 5, NULL, 1, '2024-09-12 15:05:00'),
(15, 'pilot_zhaolei', @pwd, 'PILOT', 6, NULL, 1, '2024-06-18 10:15:00'),
(16, 'pilot_sunyue', @pwd, 'PILOT', 7, NULL, 1, '2024-07-20 09:45:00'),
(17, 'pilot_zhoukai', @pwd, 'PILOT', 8, NULL, 1, '2025-02-08 13:05:00'),
(18, 'pilot_wumin', @pwd, 'PILOT', 9, NULL, 1, '2024-10-02 08:40:00'),
(19, 'pilot_zhenghang', @pwd, 'PILOT', 10, NULL, 1, '2024-10-15 16:05:00'),
(20, 'org_jingce', @pwd, 'ORG', NULL, 1, 1, '2024-05-01 10:05:00'),
(21, 'org_lantian', @pwd, 'ORG', NULL, 2, 1, '2024-07-01 09:35:00'),
(22, 'org_jiangnan', @pwd, 'ORG', NULL, 3, 1, '2025-02-01 14:05:00'),
(23, 'org_huabei', @pwd, 'ORG', NULL, 4, 1, '2024-11-11 11:12:00'),
(24, 'org_lingnan', @pwd, 'ORG', NULL, 5, 1, '2024-08-08 09:12:00'),
(25, 'org_xibu', @pwd, 'ORG', NULL, 6, 1, '2025-01-12 11:35:00'),
(26, 'org_zhongyuan', @pwd, 'ORG', NULL, 7, 1, '2024-06-06 08:25:00'),
(27, 'org_beifang', @pwd, 'ORG', NULL, 8, 1, '2024-09-09 13:18:00'),
(28, 'org_donghai', @pwd, 'ORG', NULL, 9, 1, '2025-02-20 10:15:00'),
(29, 'org_chuanyu', @pwd, 'ORG', NULL, 10, 1, '2024-12-01 14:30:00'),
(31, 'pilot_xuchen', @pwd, 'PILOT', 11, NULL, 1, '2025-03-18 09:18:00'),
(32, 'pilot_heyu', @pwd, 'PILOT', 12, NULL, 1, '2025-03-18 11:25:00'),
(33, 'pilot_tangjun', @pwd, 'PILOT', 13, NULL, 1, '2025-03-19 10:12:00'),
(34, 'pilot_malan', @pwd, 'PILOT', 14, NULL, 1, '2025-03-19 14:28:00'),
(35, 'pilot_shentuo', @pwd, 'PILOT', 15, NULL, 1, '2025-03-20 09:45:00'),
(36, 'pilot_guchuan', @pwd, 'PILOT', 16, NULL, 1, '2025-03-20 13:32:00'),
(37, 'pilot_liangke', @pwd, 'PILOT', 17, NULL, 1, '2025-03-21 09:00:00'),
(38, 'pilot_ranxi', @pwd, 'PILOT', 18, NULL, 1, '2025-03-21 16:10:00'),
(39, 'org_huadong', @pwd, 'ORG', NULL, 11, 1, '2025-03-18 09:05:00'),
(40, 'org_haixi', @pwd, 'ORG', NULL, 12, 1, '2025-03-18 15:25:00'),
(41, 'org_huazhong', @pwd, 'ORG', NULL, 13, 1, '2025-03-19 09:38:00'),
(42, 'org_bohai', @pwd, 'ORG', NULL, 14, 1, '2025-03-19 16:05:00'),
(43, 'org_ganbei', @pwd, 'ORG', NULL, 15, 1, '2025-03-20 08:45:00'),
(44, 'org_xinan', @pwd, 'ORG', NULL, 16, 1, '2025-03-20 14:15:00'),
(45, 'org_beijiang', @pwd, 'ORG', NULL, 17, 1, '2025-03-21 10:25:00'),
(46, 'org_changjiang', @pwd, 'ORG', NULL, 18, 1, '2025-03-21 15:35:00'),
(30, 'guest_demo', @pwd, 'GUEST', NULL, NULL, 1, '2025-01-01 00:00:00');

UPDATE pilot SET user_id = 10 WHERE id = 1;
UPDATE pilot SET user_id = 11 WHERE id = 2;
UPDATE pilot SET user_id = 12 WHERE id = 3;
UPDATE pilot SET user_id = 13 WHERE id = 4;
UPDATE pilot SET user_id = 14 WHERE id = 5;
UPDATE pilot SET user_id = 15 WHERE id = 6;
UPDATE pilot SET user_id = 16 WHERE id = 7;
UPDATE pilot SET user_id = 17 WHERE id = 8;
UPDATE pilot SET user_id = 18 WHERE id = 9;
UPDATE pilot SET user_id = 19 WHERE id = 10;
UPDATE pilot SET user_id = 31 WHERE id = 11;
UPDATE pilot SET user_id = 32 WHERE id = 12;
UPDATE pilot SET user_id = 33 WHERE id = 13;
UPDATE pilot SET user_id = 34 WHERE id = 14;
UPDATE pilot SET user_id = 35 WHERE id = 15;
UPDATE pilot SET user_id = 36 WHERE id = 16;
UPDATE pilot SET user_id = 37 WHERE id = 17;
UPDATE pilot SET user_id = 38 WHERE id = 18;

UPDATE organization SET user_id = 20 WHERE id = 1;
UPDATE organization SET user_id = 21 WHERE id = 2;
UPDATE organization SET user_id = 22 WHERE id = 3;
UPDATE organization SET user_id = 23 WHERE id = 4;
UPDATE organization SET user_id = 24 WHERE id = 5;
UPDATE organization SET user_id = 25 WHERE id = 6;
UPDATE organization SET user_id = 26 WHERE id = 7;
UPDATE organization SET user_id = 27 WHERE id = 8;
UPDATE organization SET user_id = 28 WHERE id = 9;
UPDATE organization SET user_id = 29 WHERE id = 10;
UPDATE organization SET user_id = 39 WHERE id = 11;
UPDATE organization SET user_id = 40 WHERE id = 12;
UPDATE organization SET user_id = 41 WHERE id = 13;
UPDATE organization SET user_id = 42 WHERE id = 14;
UPDATE organization SET user_id = 43 WHERE id = 15;
UPDATE organization SET user_id = 44 WHERE id = 16;
UPDATE organization SET user_id = 45 WHERE id = 17;
UPDATE organization SET user_id = 46 WHERE id = 18;

-- 自增指针后移，避免与手工 id 冲突
ALTER TABLE users AUTO_INCREMENT = 100;
ALTER TABLE pilot AUTO_INCREMENT = 100;
ALTER TABLE organization AUTO_INCREMENT = 100;

-- -----------------------------------------------------------------------------
-- 申请记录（飞手 18 条，单位 18 条）
-- -----------------------------------------------------------------------------
INSERT INTO pilot_application (pilot_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
VALUES
(1, 'SUBMIT', 'APPROVED', 'PASS', 88, '["加权分通过阈值"]', '2024-06-10 09:00:00'),
(2, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["分数处于人工复核区间"]', '2025-03-01 10:00:00'),
(3, 'SUBMIT', 'REJECTED', 'REJECT', 45, '["关键字段校验未通过"]', '2024-03-25 09:30:00'),
(4, 'SAVE_DRAFT', 'DRAFT', NULL, NULL, NULL, '2025-01-05 12:00:00'),
(5, 'SUBMIT', 'APPROVED', 'PASS', 75, '["通过后人工冻结"]', '2024-09-20 16:00:00'),
(6, 'SUBMIT', 'APPROVED', 'PASS', 85, '["材料完备"]', '2024-06-20 11:00:00'),
(7, 'SUBMIT', 'APPROVED', 'PASS', 91, '["评分优秀"]', '2024-07-24 13:30:00'),
(8, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 66, '["人工复核区间"]', '2025-03-12 17:05:00'),
(9, 'SUBMIT', 'APPROVED', 'PASS', 79, '["达标"]', '2024-10-10 10:10:00'),
(10, 'SUBMIT', 'REJECTED', 'REJECT', 50, '["联系人核验失败"]', '2024-10-19 09:40:00'),
(11, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 63, '["飞行项目说明待补充"]', '2025-03-18 09:15:00'),
(12, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 61, '["执照附件待人工核验"]', '2025-03-18 11:20:00'),
(13, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 59, '["过往任务记录需要补录"]', '2025-03-19 10:10:00'),
(14, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 65, '["证照清晰但地区证明未上传"]', '2025-03-19 14:25:00'),
(15, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 60, '["保险材料待补充"]', '2025-03-20 09:40:00'),
(16, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["备案号需人工比对"]', '2025-03-20 13:30:00'),
(17, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["飞行资历说明待复核"]', '2025-03-21 08:55:00'),
(18, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 67, '["附件完整但评分接近阈值"]', '2025-03-21 16:05:00');

INSERT INTO org_application (org_id, apply_type, status, auto_result, auto_score, auto_reason, created_at)
VALUES
(1, 'SUBMIT', 'APPROVED', 'PASS', 92, '["score=92"]', '2024-05-15 11:00:00'),
(2, 'SUBMIT', 'APPROVED', 'PASS', 90, '["score=90"]', '2024-07-10 10:30:00'),
(3, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 58, '["需人工复核"]', '2025-03-10 10:00:00'),
(4, 'SUBMIT', 'REJECTED', 'REJECT', 0, '["命中黑名单关键词"]', '2024-11-12 09:00:00'),
(5, 'SUBMIT', 'APPROVED', 'PASS', 87, '["资质完整"]', '2024-08-15 15:00:00'),
(6, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["需补充证明"]', '2025-02-28 14:00:00'),
(7, 'SUBMIT', 'APPROVED', 'PASS', 81, '["通过"]', '2024-06-14 09:50:00'),
(8, 'SUBMIT', 'APPROVED', 'PASS', 89, '["通过"]', '2024-09-15 16:20:00'),
(9, 'SAVE_DRAFT', 'DRAFT', NULL, NULL, NULL, '2025-02-20 10:10:00'),
(10, 'SUBMIT', 'APPROVED', 'PASS', 76, '["通过后人工冻结"]', '2024-12-10 11:10:00'),
(11, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 62, '["合同附件待核实"]', '2025-03-18 09:00:00'),
(12, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 60, '["活动记录待补充"]', '2025-03-18 15:20:00'),
(13, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 65, '["营业执照与联系人待复核"]', '2025-03-19 09:35:00'),
(14, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 59, '["章程附件不完整"]', '2025-03-19 16:00:00'),
(15, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 63, '["服务案例需要补录"]', '2025-03-20 08:40:00'),
(16, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 66, '["设备台账需人工确认"]', '2025-03-20 14:10:00'),
(17, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 61, '["负责人资质待复核"]', '2025-03-21 10:20:00'),
(18, 'SUBMIT', 'PENDING_REVIEW', 'NEED_MANUAL', 64, '["历史项目清单待确认"]', '2025-03-21 15:30:00');

-- -----------------------------------------------------------------------------
-- 审核 / 操作日志（10 条）
-- -----------------------------------------------------------------------------
INSERT INTO audit_log (entity_type, entity_id, action, operator_user_id, comment_text, detail_json, created_at)
VALUES
('PILOT', 1, 'MANUAL_REVIEW', 1, '资料齐全，予以公示', '{"decision":"APPROVE"}', '2024-06-15 14:30:00'),
('PILOT', 3, 'MANUAL_REVIEW', 1, '材料不完整，驳回', '{"decision":"REJECT"}', '2024-04-01 16:00:00'),
('ORG', 1, 'MANUAL_REVIEW', 1, '企业信用良好', '{"decision":"APPROVE"}', '2024-05-20 11:00:00'),
('PILOT', 2, 'SUBMIT_AUTO', 11, '自动预审: NEED_MANUAL', '["分数处于人工复核区间"]', '2025-03-01 10:00:00'),
('ORG', 3, 'SUBMIT_AUTO', 22, '自动预审: NEED_MANUAL', '["需人工复核"]', '2025-03-10 10:00:00'),
('PILOT', 5, 'FREEZE', 1, '冻结演示账号', '{"reason":"risk_control"}', '2024-11-01 09:00:00'),
('ORG', 4, 'MANUAL_REVIEW', 1, '命中黑名单，驳回', '{"decision":"REJECT"}', '2024-11-12 09:00:00'),
('PILOT', 7, 'MANUAL_REVIEW', 1, '评分优秀，快速通过', '{"decision":"APPROVE"}', '2024-07-28 14:15:00'),
('ORG', 10, 'FREEZE', 1, '配合检查，临时冻结', '{"reason":"manual_hold"}', '2025-01-08 09:00:00'),
('PILOT', 8, 'MANUAL_REVIEW', 1, '补充材料后再复核', '{"decision":"PENDING"}', '2025-03-12 17:10:00');

-- 可选：补充预审配置（若表中尚无 default，应用启动也会写入）
-- INSERT IGNORE INTO review_rule_config (config_key, config_value, updated_at)
-- VALUES ('default', '{"passThreshold":70,...}', NOW());
