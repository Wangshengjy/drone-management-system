CREATE TABLE IF NOT EXISTS `mission` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  `title` VARCHAR(128) NOT NULL COMMENT '任务标题',
  `description` TEXT DEFAULT NULL COMMENT '任务描述',
  `mission_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
  `urgency` VARCHAR(16) NOT NULL DEFAULT 'BLUE' COMMENT '紧急程度',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态',
  `latitude` DOUBLE DEFAULT NULL COMMENT '事发地纬度',
  `longitude` DOUBLE DEFAULT NULL COMMENT '事发地经度',
  `location_name` VARCHAR(255) DEFAULT NULL COMMENT '事发地名称',
  `weather_info` VARCHAR(512) DEFAULT NULL COMMENT '气象信息',
  `required_cert_types` VARCHAR(255) DEFAULT NULL COMMENT '需要的资质类型',
  `required_drone_type` VARCHAR(64) DEFAULT NULL COMMENT '需要的无人机类型',
  `creator_id` BIGINT NOT NULL COMMENT '创建人用户ID',
  `assigned_org_id` BIGINT DEFAULT NULL COMMENT '指派的协作单位ID',
  `assigned_pilot_id` BIGINT DEFAULT NULL COMMENT '指派的飞手ID',
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

CREATE TABLE IF NOT EXISTS `mission_assignment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '派单ID',
  `mission_id` BIGINT NOT NULL COMMENT '任务ID',
  `assignee_type` VARCHAR(16) NOT NULL COMMENT '被派对象类型',
  `assignee_id` BIGINT NOT NULL COMMENT '被派对象ID',
  `match_score` DOUBLE DEFAULT NULL COMMENT '匹配度评分',
  `match_detail` TEXT DEFAULT NULL COMMENT '匹配详情JSON',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `responded_at` DATETIME DEFAULT NULL COMMENT '响应时间',
  `rank_order` INT NOT NULL DEFAULT 1 COMMENT '派单顺位',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_ma_mission` (`mission_id`),
  KEY `idx_ma_assignee` (`assignee_type`, `assignee_id`),
  KEY `idx_ma_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务派单记录表';

CREATE TABLE IF NOT EXISTS `mission_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `mission_id` BIGINT NOT NULL COMMENT '任务ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `comment_text` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `attachment_url` VARCHAR(512) DEFAULT NULL COMMENT '附件',
  `detail_json` TEXT DEFAULT NULL COMMENT '详细信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_ml_mission` (`mission_id`),
  KEY `idx_ml_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

CREATE TABLE IF NOT EXISTS `pilot_status` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `pilot_id` BIGINT NOT NULL UNIQUE COMMENT '飞手ID',
  `availability` VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '状态',
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

CREATE TABLE IF NOT EXISTS `org_status` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `org_id` BIGINT NOT NULL UNIQUE COMMENT '单位ID',
  `availability` VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '状态',
  `latitude` DOUBLE DEFAULT NULL COMMENT '单位纬度',
  `longitude` DOUBLE DEFAULT NULL COMMENT '单位经度',
  `mission_count` INT NOT NULL DEFAULT 0 COMMENT '累计完成任务数',
  `active_mission_id` BIGINT DEFAULT NULL COMMENT '当前执行的任务ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_os_availability` (`availability`),
  KEY `idx_os_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作单位实时状态表';
