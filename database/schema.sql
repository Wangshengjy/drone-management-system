-- 应急协同平台数据库
-- MySQL 8+

CREATE DATABASE IF NOT EXISTS emergency_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE emergency_db;

-- =====================================================
-- 1. 用户表（系统登录使用）
-- 采用模板风格，并保留当前系统所需的 pilot_id / org_id 关联字段
-- =====================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码（明文存储）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `role` VARCHAR(20) NOT NULL DEFAULT 'PILOT' COMMENT '角色: ADMIN/PILOT/ORG/GUEST',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `pilot_id` BIGINT DEFAULT NULL COMMENT '关联飞手ID',
  `org_id` BIGINT DEFAULT NULL COMMENT '关联单位ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_users_role` (`role`),
  KEY `idx_users_pilot_id` (`pilot_id`),
  KEY `idx_users_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- 2. 飞手表
-- =====================================================
DROP TABLE IF EXISTS `pilot`;
CREATE TABLE `pilot` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '飞手ID',
  `user_id` BIGINT DEFAULT NULL UNIQUE COMMENT '关联用户ID',
  `full_name` VARCHAR(64) DEFAULT NULL COMMENT '姓名',
  `id_card` VARCHAR(32) DEFAULT NULL COMMENT '身份证号',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `record_no` VARCHAR(64) DEFAULT NULL COMMENT '备案号',
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/FROZEN',
  `auto_result` VARCHAR(32) DEFAULT NULL COMMENT '自动预审结果',
  `auto_score` INT DEFAULT NULL COMMENT '自动预审分数',
  `auto_reason` TEXT DEFAULT NULL COMMENT '自动预审原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_pilot_status` (`status`),
  KEY `idx_pilot_region` (`region`),
  KEY `idx_pilot_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞手表';

-- =====================================================
-- 3. 飞手资质证书表
-- =====================================================
DROP TABLE IF EXISTS `pilot_certificate`;
CREATE TABLE `pilot_certificate` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '证书ID',
  `pilot_id` BIGINT NOT NULL COMMENT '飞手ID',
  `cert_type` VARCHAR(64) NOT NULL COMMENT '证书类型',
  `cert_no` VARCHAR(128) DEFAULT NULL COMMENT '证书编号',
  `valid_until` DATE DEFAULT NULL COMMENT '有效期至',
  `attachment_url` VARCHAR(512) DEFAULT NULL COMMENT '附件地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_pilot_certificate_pilot_id` (`pilot_id`),
  KEY `idx_pilot_certificate_type` (`cert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞手资质证书表';

-- =====================================================
-- 4. 协作单位表
-- =====================================================
DROP TABLE IF EXISTS `organization`;
CREATE TABLE `organization` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '单位ID',
  `user_id` BIGINT DEFAULT NULL UNIQUE COMMENT '关联用户ID',
  `name` VARCHAR(128) NOT NULL COMMENT '单位名称',
  `org_type` VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '单位类型: ENTERPRISE/VOLUNTEER/OTHER',
  `credit_code` VARCHAR(32) DEFAULT NULL COMMENT '统一社会信用代码',
  `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '联系地址',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/FROZEN',
  `auto_result` VARCHAR(32) DEFAULT NULL COMMENT '自动预审结果',
  `auto_score` INT DEFAULT NULL COMMENT '自动预审分数',
  `auto_reason` TEXT DEFAULT NULL COMMENT '自动预审原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_organization_status` (`status`),
  KEY `idx_organization_org_type` (`org_type`),
  KEY `idx_organization_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作单位表';

-- =====================================================
-- 5. 审核日志表
-- =====================================================
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `entity_type` VARCHAR(16) NOT NULL COMMENT '对象类型: PILOT/ORG',
  `entity_id` BIGINT NOT NULL COMMENT '对象ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `operator_user_id` BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  `comment_text` VARCHAR(512) DEFAULT NULL COMMENT '审核意见',
  `detail_json` TEXT DEFAULT NULL COMMENT '详细信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_audit_entity` (`entity_type`, `entity_id`),
  KEY `idx_audit_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核日志表';

-- =====================================================
-- 6. 预审规则配置表
-- =====================================================
DROP TABLE IF EXISTS `review_rule_config`;
CREATE TABLE `review_rule_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(64) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值(JSON)',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预审规则配置表';

-- =====================================================
-- 7. 飞手申请记录表
-- =====================================================
DROP TABLE IF EXISTS `pilot_application`;
CREATE TABLE `pilot_application` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
  `pilot_id` BIGINT NOT NULL COMMENT '飞手ID',
  `apply_type` VARCHAR(32) NOT NULL DEFAULT 'SUBMIT' COMMENT '申请类型',
  `status` VARCHAR(32) NOT NULL COMMENT '当前状态',
  `auto_result` VARCHAR(32) DEFAULT NULL COMMENT '自动预审结果',
  `auto_score` INT DEFAULT NULL COMMENT '自动预审分数',
  `auto_reason` TEXT DEFAULT NULL COMMENT '自动预审原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_pilot_application_pilot_id` (`pilot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞手申请记录表';

-- =====================================================
-- 8. 单位申请记录表
-- =====================================================
DROP TABLE IF EXISTS `org_application`;
CREATE TABLE `org_application` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
  `org_id` BIGINT NOT NULL COMMENT '单位ID',
  `apply_type` VARCHAR(32) NOT NULL DEFAULT 'SUBMIT' COMMENT '申请类型',
  `status` VARCHAR(32) NOT NULL COMMENT '当前状态',
  `auto_result` VARCHAR(32) DEFAULT NULL COMMENT '自动预审结果',
  `auto_score` INT DEFAULT NULL COMMENT '自动预审分数',
  `auto_reason` TEXT DEFAULT NULL COMMENT '自动预审原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_org_application_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单位申请记录表';

-- =====================================================
-- 9. 应急任务表
-- =====================================================
DROP TABLE IF EXISTS `mission`;
CREATE TABLE `mission` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  `title` VARCHAR(128) NOT NULL COMMENT '任务标题',
  `description` TEXT DEFAULT NULL COMMENT '任务描述',
  `mission_type` VARCHAR(32) NOT NULL COMMENT '任务类型: FIRE_FIGHTING/FLOOD_RESCUE/ACCIDENT_SURVEY/OTHER',
  `urgency` VARCHAR(16) NOT NULL DEFAULT 'BLUE' COMMENT '紧急程度: RED/YELLOW/BLUE',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态: PUBLISHED/DISPATCHED/ACCEPTED/IN_PROGRESS/COMPLETED/CANCELLED',
  `latitude` DOUBLE DEFAULT NULL COMMENT '事发地纬度',
  `longitude` DOUBLE DEFAULT NULL COMMENT '事发地经度',
  `location_name` VARCHAR(255) DEFAULT NULL COMMENT '事发地名称',
  `weather_info` VARCHAR(512) DEFAULT NULL COMMENT '气象信息',
  `required_cert_types` VARCHAR(255) DEFAULT NULL COMMENT '需要的资质类型(逗号分隔)',
  `required_drone_type` VARCHAR(64) DEFAULT NULL COMMENT '需要的无人机类型',
  `creator_id` BIGINT NOT NULL COMMENT '创建人用户ID',
  `assigned_org_id` BIGINT DEFAULT NULL COMMENT '指派的协作单位ID',
  `assigned_pilot_id` BIGINT DEFAULT NULL COMMENT '指派的飞手ID',
  `required_pilot_count` INT DEFAULT 1 COMMENT '需要飞手数量',
  `required_org_count` INT DEFAULT 1 COMMENT '需要协作单位数量',
  `confirm_deadline` DATETIME DEFAULT NULL COMMENT '接单截止时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_mission_status` (`status`),
  KEY `idx_mission_type` (`mission_type`),
  KEY `idx_mission_urgency` (`urgency`),
  KEY `idx_mission_creator` (`creator_id`),
  KEY `idx_mission_assigned_org` (`assigned_org_id`),
  KEY `idx_mission_assigned_pilot` (`assigned_pilot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急任务表';

-- =====================================================
-- 10. 任务派单记录表
-- =====================================================
DROP TABLE IF EXISTS `mission_assignment`;
CREATE TABLE `mission_assignment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '派单ID',
  `mission_id` BIGINT NOT NULL COMMENT '任务ID',
  `assignee_type` VARCHAR(16) NOT NULL COMMENT '被派对象类型: PILOT/ORG',
  `assignee_id` BIGINT NOT NULL COMMENT '被派对象ID(飞手ID或单位ID)',
  `match_score` DOUBLE DEFAULT NULL COMMENT '匹配度评分',
  `match_detail` TEXT DEFAULT NULL COMMENT '匹配详情JSON',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ACCEPTED/REJECTED/TIMEOUT/COMPLETED',
  `responded_at` DATETIME DEFAULT NULL COMMENT '响应时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `rank_order` INT NOT NULL DEFAULT 1 COMMENT '派单顺位',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_ma_mission` (`mission_id`),
  KEY `idx_ma_assignee` (`assignee_type`, `assignee_id`),
  KEY `idx_ma_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务派单记录表';

-- =====================================================
-- 11. 任务日志表
-- =====================================================
DROP TABLE IF EXISTS `mission_log`;
CREATE TABLE `mission_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `mission_id` BIGINT NOT NULL COMMENT '任务ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `comment_text` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `attachment_url` VARCHAR(512) DEFAULT NULL COMMENT '附件(现场照片等)',
  `detail_json` TEXT DEFAULT NULL COMMENT '详细信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_ml_mission` (`mission_id`),
  KEY `idx_ml_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

-- =====================================================
-- 12. 飞手实时状态表
-- =====================================================
DROP TABLE IF EXISTS `pilot_status`;
CREATE TABLE `pilot_status` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `pilot_id` BIGINT NOT NULL UNIQUE COMMENT '飞手ID',
  `availability` VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '状态: IDLE/ON_MISSION/LEAVE/OFFLINE',
  `current_latitude` DOUBLE DEFAULT NULL COMMENT '当前纬度',
  `current_longitude` DOUBLE DEFAULT NULL COMMENT '当前经度',
  `drone_type` VARCHAR(64) DEFAULT NULL COMMENT '当前无人机类型',
  `mission_count` INT NOT NULL DEFAULT 0 COMMENT '累计完成任务数',
  `active_mission_id` BIGINT DEFAULT NULL COMMENT '当前执行的任务ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_ps_availability` (`availability`),
  KEY `idx_ps_pilot` (`pilot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞手实时状态表';

-- =====================================================
-- 13. 协作单位实时状态表
-- =====================================================
DROP TABLE IF EXISTS `org_status`;
CREATE TABLE `org_status` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `org_id` BIGINT NOT NULL UNIQUE COMMENT '单位ID',
  `availability` VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '状态: IDLE/ON_MISSION/OFFLINE',
  `latitude` DOUBLE DEFAULT NULL COMMENT '单位纬度',
  `longitude` DOUBLE DEFAULT NULL COMMENT '单位经度',
  `mission_count` INT NOT NULL DEFAULT 0 COMMENT '累计完成任务数',
  `active_mission_id` BIGINT DEFAULT NULL COMMENT '当前执行的任务ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_os_availability` (`availability`),
  KEY `idx_os_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作单位实时状态表';

-- 14. 飞手-单位关联表
DROP TABLE IF EXISTS `pilot_org`;
CREATE TABLE `pilot_org` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  `pilot_id` BIGINT NOT NULL COMMENT '飞手ID',
  `org_id` BIGINT NOT NULL COMMENT '单位ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待确认)/ACTIVE(已加入)/REJECTED(已拒绝)/LEFT(已退出)',
  `initiator` VARCHAR(16) NOT NULL DEFAULT 'ORG' COMMENT '发起方: ORG(单位邀请)/PILOT(飞手申请)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_pilot_org` (`pilot_id`, `org_id`),
  KEY `idx_pilot_org_pilot` (`pilot_id`),
  KEY `idx_pilot_org_org` (`org_id`),
  KEY `idx_pilot_org_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞手-单位关联表';
