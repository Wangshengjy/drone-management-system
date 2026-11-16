# 无人机飞手及协作单位管理系统

面向无人机企业的前后端分离管理系统：飞手与协作单位（企业/志愿者机构）的线上报名、资料维护、**自动预审**与人工复核，以及面向公众的备案查询、**应急任务智能派单**与数据报表。

## 系统亮点

- **自动预审引擎**：多维加权评分（信息完整度/证书/备案）＋硬性条件一票否决＋阈值三档分层（自动通过/人工复核/自动驳回），预审规则存 JSON 配置表，热更新免重启——申请筛选从人工 15 分钟缩短至 2 分钟内
- **应急任务智能派单**：Haversine 球面距离（40%）＋设备状态（30%）＋证书匹配（15%）＋当前负载（15%）四维加权评分，输出 Top5 推荐序列，支撑红黄蓝三级紧急度调度
- **安全体系**：JWT 双 Token 无感续期、RBAC 四角色权限控制、BCrypt 密码哈希、审计日志记录操作前后快照
- **数据设计**：13 张表第三范式设计，status+region 复合索引优化（查询提速约 40%）；JMeter 压测公开查询接口 196 QPS、平均响应 423ms、P95 687ms、成功率 100%

## 技术栈

后端：Java 17 · Spring Boot 3 · MyBatis-Plus · JWT · BCrypt
前端：Vue 3 · Vite · TypeScript · Element Plus · ECharts
数据库：MySQL 8 · 第三范式设计 · 复合索引优化
测试：JUnit · Postman 集合 · JMeter 压测

## 架构

```
Vue3 + Element Plus + ECharts (frontend/)
   │  REST API (axios)
   ▼
Spring Boot 3 (backend/)
   ├─ Controller 层   ← JWT 认证 + RBAC 四角色鉴权（管理员/协作单位/飞手/访客）
   ├─ Service 层      ← 预审引擎（权重 JSON 热更新）/ 派单算法 / 状态机
   ├─ MyBatis-Plus    ← Mapper XML
   └─ MySQL（13 张表）
```

## 主要功能

| 模块 | 说明 |
| --- | --- |
| 飞手管理 | 飞手档案五级状态机流转；自助维护备案信息与资质证书 |
| 协作单位管理 | 线上报名通道；企业/志愿者机构资料维护与审核筛选 |
| 自动预审 | 加权综合分 + 硬性条件否决 + 三档分层，人工复核队列兜底 |
| 应急任务派单 | 任务发布 → 四维加权评分 → Top5 飞手推荐 → 红黄蓝紧急度调度 |
| 公众查询 | 已通过飞手与协作单位脱敏公示；首页统计看板与图表 |

## 快速开始

### 数据库

执行 `database/schema.sql`（业务表）与 `database/create-mission-tables.sql`（应急任务表），或直接使用 `backend/src/main/resources/db/schema.sql`。

### 后端（端口 8080）

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

数据库连接配置在 `backend/src/main/resources/application.yml`（默认 `localhost:3306/emergency_db`）。

### 前端（端口 5173）

```powershell
cd frontend
npm install
npm run dev
```

### 演示账号

- 管理员：`admin / 123456`
- 飞手：`pilot_sample_alpha / 123456`
- 协作单位：`org_sample_horizon / 123456`

## 文档

- `docs/DEVELOPMENT.md` — 环境与启动
- `docs/USER_MANUAL.md` — 按角色操作手册
- `docs/TESTING.md` — 测试记录
- `docs/postman/Drone-Collab.postman_collection.json` — Postman 接口集合
