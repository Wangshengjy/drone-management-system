# 开发文档

## 技术栈

- **后端**：JDK 17、Spring Boot 3.2、Spring Security、JWT（jjwt）、MyBatis-Plus、MySQL 8
- **前端**：Node 18+、Vite 5、Vue 3、TypeScript、Vue Router、Pinia、Element Plus、ECharts、Axios

## 环境要求

| 组件 | 版本建议 |
|------|----------|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

## 数据库

1. 创建库并执行脚本：`backend/src/main/resources/db/schema.sql`
2. （可选）导入演示数据：`backend/src/main/resources/db/seed.sql`。其中演示账号明文密码均为 `password`（BCrypt 已写入），详见该文件头部说明；建议在首次启动应用前执行，避免与自动创建的 `sys_user` 主键策略冲突
3. 修改 `backend/src/main/resources/application.yml` 中的 `spring.datasource.url`、`username`、`password`
4. 首次启动后端时，`DataInitializer` 会创建默认管理员 `admin`（密码见配置项 `app.bootstrap.admin-password`，默认 `123456`），并写入默认预审配置（若库中尚无）
5. 启动后若未导入 `seed.sql`，系统仍会自动补充一组演示账号：飞手 `pilot_sample_alpha / 123456`、协作单位 `org_sample_horizon / 123456`

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

服务默认端口：`8080`。JWT 密钥与过期时间在 `application.yml` 的 `app.jwt` 中配置。

## 安装 Node.js（Windows）

若本机尚未安装，可在 **PowerShell（管理员）** 中执行：

```powershell
winget install OpenJS.NodeJS.LTS --accept-package-agreements --accept-source-agreements
```

安装完成后 **关闭并重新打开** 终端（或注销登录），使 `PATH` 生效。验证：

```powershell
node -v
npm -v
```

## 启动前端

**须先启动后端**（默认 `http://localhost:8080`），再启动前端。

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认端口：`5173`，通过 Vite 代理将 `/api` 与 `/uploads` 转发到 `http://localhost:8080`。浏览器访问：`http://localhost:5173`。

Windows 也可在项目根目录执行：`powershell -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1`

生产构建：`cd frontend` → `npm run build`，产物在 `frontend/dist/`。

## 架构说明

- **统一响应**：`ApiResponse<T>`，`code=0` 表示成功，`data` 为载荷
- **认证**：登录接口返回 JWT，前端在请求头携带 `Authorization: Bearer <token>`
- **权限**：业务层结合角色（`ADMIN` / `ORG` / `PILOT`）与资源归属（`pilotId` / `orgId`）校验；管理类接口要求管理员角色
- **文件**：上传目录由 `app.upload-dir` 指定，静态访问路径为 `/uploads/**`

## 主要包结构（后端）

- `controller`：REST 接口
- `service`：业务与自动预审
- `mapper`：MyBatis-Plus Mapper
- `entity`：数据库实体
- `security`：JWT 过滤器与安全配置
- `config`：Web、MyBatis、跨域、启动数据

## 接口索引（摘要）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register` | 注册飞手、协作单位或访客（`PILOT`/`ORG`/`GUEST`） |
| GET/POST/PUT/DELETE | `/api/pilots` 等 | 飞手 CRUD、提交审核、证书 |
| GET/POST/PUT/DELETE | `/api/organizations` 等 | 单位 CRUD、提交审核 |
| GET | `/api/public/pilots`、`/api/public/organizations` | 公开查询（脱敏） |
| GET | `/api/public/stats/dashboard` | 报表聚合 |
| GET | `/api/admin/review/**` | 审核队列与人工复核 |
| PUT | `/api/admin/review-config` | 预审权重与阈值 |
| GET | `/api/admin/audit-logs` | 审核与操作日志 |
| POST | `/api/files/upload` | 文件上传 |

完整请求示例见 `docs/postman/Drone-Collab.postman_collection.json`。

## 部署提示

- 生产环境请更换 JWT 密钥、数据库账号，并配置 HTTPS 与对象存储（若不用本地 `uploads/`）
- 身份证等敏感字段仅用于演示；展示层公开接口已做手机脱敏
