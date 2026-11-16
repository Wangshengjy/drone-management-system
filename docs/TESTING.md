# 测试记录

## 功能测试（手工）

| 编号 | 场景 | 步骤摘要 | 预期 | 结果 |
|------|------|----------|------|------|
| F1 | 管理员登录 | 使用 `admin` 登录 | 返回 JWT，可访问 `/api/admin/*` | 待执行 |
| F2 | 飞手注册与资料 | 注册 PILOT，填写资料与证书，提交审核 | 状态变为 `PENDING_REVIEW` 或 `REJECTED`，产生 `audit_log` | 待执行 |
| F3 | 单位注册与提交 | 注册 ORG，填写 18 位信用代码，提交 | 自动预审通过规则校验 | 待执行 |
| F4 | 人工审核 | 管理员在待审列表通过/驳回 | 状态变为 `APPROVED`/`REJECTED`，写日志 | 待执行 |
| F5 | 公开查询 | 无 Token 访问 `/api/public/pilots` | 仅 `APPROVED`，手机脱敏 | 待执行 |
| F6 | 统计接口 | GET `/api/public/stats/dashboard` | 返回月度与分布等聚合字段 | 待执行 |
| F7 | 权限校验 | 飞手 Token 访问其他飞手 ID | 返回 403 或业务错误 | 待执行 |

*说明：环境未预装 Maven/Node 时，可在本机安装依赖后按 `docs/DEVELOPMENT.md` 启动服务并勾选上表「结果」。*

## 接口自动化建议

- 使用 Postman Collection（`docs/postman/Drone-Collab.postman_collection.json`）覆盖登录、CRUD、审核与公开接口
- 可将 `baseUrl`、`token` 设为集合变量，在 Pre-request 中从登录接口写入 `token`

## 性能测试（建议）

| 场景 | 工具 | 指标建议 |
|------|------|----------|
| 公开查询并发 | JMeter / wrk | 采样 QPS、P95 延迟 |
| 统计看板 | JMeter | 聚合 SQL 在数据量 1w+ 行时的响应时间 |

*记录方式：保留工具报告截图与结论段落，写入论文「测试」章节。*
