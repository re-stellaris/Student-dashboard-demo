# 若依教育平台 —— Docker Compose 部署与运维手册

一条命令拉起整套本地 / 准生产环境：

```bash
cd deploy && docker compose up -d --build
```

包含 **MySQL 8.0 + Redis 7 + Spring Boot 后端（Java 21）+ Nginx 前端**，
数据库初始化脚本直接挂载 `backend/sql/` 下的真实文件，不维护第二份拷贝，
统一库名 `education_db`。

---

## 1. 前置条件

| 依赖 | 版本要求 | 检查命令 |
|---|---|---|
| Docker Engine | 23+（需要 BuildKit） | `docker version` |
| Docker Compose | v2.20+（`additional_contexts` 需 2.17+） | `docker compose version` |
| 可用内存 | ≥ 4 GB（MySQL 1G + JVM 1G + 构建期 Maven/Node 峰值） | — |
| 可用磁盘 | ≥ 8 GB（Maven 依赖 + npm 依赖 + 数据卷） | — |
| 端口 | 宿主 `${HTTP_PORT}`（默认 80）空闲 | `netstat -ano \| findstr :80` |

> 首次构建需要联网下载 Maven 依赖与 npm 依赖（Maven 走 `pom.xml` 里配置的阿里云仓库）。
> 后端镜像首次构建通常 3~8 分钟，之后改 Java 代码只重跑编译层。

Windows 建议开启 WSL2 后端；Linux/macOS 无需额外配置。

---

## 2. 快速开始

```bash
cd deploy

# 1) 生成环境变量文件（.env 已被 .gitignore 忽略，不会入库）
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env

# 2) 必须修改的 4 个值（其余可按需调整）
#    DB_PASSWORD / DB_ROOT_PASSWORD / REDIS_PASSWORD / TOKEN_SECRET
#    随机密钥生成：openssl rand -base64 32
${EDITOR:-vim} .env

# 3) 构建并启动
docker compose up -d --build

# 4) 查看状态，等到 mysql/redis/backend/frontend 都是 healthy
docker compose ps
```

`docker compose` 必须在 **deploy/ 目录**下执行（compose 默认从项目目录读取 `.env`）。
如果非要在仓库根执行，请显式指定：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

启动顺序由健康检查保证：`mysql`/`redis` 就绪 → `backend` 启动 → `frontend` 启动。
后端首次启动含 JVM + Spring + Quartz 初始化，**约需 40~90 秒**，
`docker compose ps` 里 backend 显示 `(health: starting)` 属正常。

---

## 3. 目录结构

```text
deploy/
├── docker-compose.yml        # 编排（唯一入口）
├── backend.Dockerfile        # 后端多阶段构建（maven:3.9-eclipse-temurin-21 -> eclipse-temurin:21-jre）
├── frontend.Dockerfile       # 前端多阶段构建（node:24-alpine -> nginx:1.29-alpine）
├── nginx/nginx.conf          # SPA 回退 + gzip + 长缓存 + /prod-api、/api 反代
├── .env.example              # 环境变量模板（复制成 .env 使用）
├── .gitignore                # 忽略 .env 与 backups/
├── mysql/init/README.md      # 说明：初始化 SQL 来自 backend/sql，此处不放副本
└── backups/                  # 备份演练输出目录（首次 up 时自动创建，不入库）
```

两个 Dockerfile 都不在各自构建上下文的根目录，compose 里用「上下文 + 相对 dockerfile」的写法声明：

| 服务 | build.context | build.dockerfile | 实际文件 |
|---|---|---|---|
| backend | `../backend` | `../deploy/backend.Dockerfile` | `deploy/backend.Dockerfile` |
| frontend | `../frontend` | `../deploy/frontend.Dockerfile` | `deploy/frontend.Dockerfile` |

`build.dockerfile` 的相对路径按**构建上下文**解析，所以 `../deploy/backend.Dockerfile`
从 `backend/` 往上退一级再进 `deploy/`，正好指向仓库里的真实文件。
等价的手工构建命令（在 `deploy/` 下执行）：

```bash
docker build -f backend.Dockerfile  -t ruoyi-edu/backend:3.9.2  ../backend
docker build -f frontend.Dockerfile --build-context nginx_conf=nginx \
                                    -t ruoyi-edu/frontend:3.9.2 ../frontend
```

---

## 4. 访问地址与两个 API 前缀

| 用途 | 地址 |
|---|---|
| 前端登录页 | `http://localhost:${HTTP_PORT:-80}/` |
| 健康探针（后端） | 容器内 `curl -fsS http://127.0.0.1:8080/captchaImage` |
| Swagger（**默认关闭**） | 设 `SPRINGDOC_ENABLED=true` 后：`http://localhost:${HTTP_PORT}/prod-api/swagger-ui.html` |
| Druid 监控台（**默认关闭**） | 设 `DRUID_STAT_VIEW_ENABLED=true` 后：`http://localhost:${HTTP_PORT}/prod-api/druid/` |

### 两个受支持的 API 前缀

Nginx 同时把下面两个前缀反向代理到 `http://backend:8080/`，
**只剥离前缀，其余路径原样透传**（后端 Controller 的 `/edu/**` 路径保持不变）：

| 前端 / 调用方请求 | 转发到后端 |
|---|---|
| `GET /prod-api/edu/student/list` | `GET /edu/student/list` |
| `GET /api/edu/student/list` | `GET /edu/student/list` |
| `POST /prod-api/login` | `POST /login` |
| `GET /api/system/user/getInfo` | `GET /system/user/getInfo` |

- `/prod-api`：前端 `frontend/.env.production` 里的 `VITE_APP_BASE_API = '/prod-api'`，
  构建期打包进静态资源，是浏览器实际使用的路径。
- `/api`：给外部脚本 / 第三方集成使用的等价简化前缀，行为与 `/prod-api` 完全一致。

两个前缀都带 `Host` / `X-Real-IP` / `X-Forwarded-For` / `X-Forwarded-Proto` /
`X-Forwarded-Host` / `X-Forwarded-Port` 头，后端 `server.forward-headers-strategy: native`
会采信它们，因此 `sys_logininfor` 里记录到的是真实客户端 IP。

---

## 5. 默认账号（演示数据）

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `admin123` | 超级管理员 | 若依内置，来自 `backend/sql/ry_20260417.sql` |
| `stu_chen` | `admin123` | 学生（STUDENT） | 演示学生，来自 `backend/sql/edu/V2__edu_permissions.sql` |
| `teacher_li` | `admin123` | 教师（TEACHER） | 演示教师，同上 |
| `stu_wang` / `stu_liu` / `stu_ean` / `stu_zixuan` / `stu_muyang` / `stu_zihan` / `stu_ruohan` | `admin123` | 学生 | 其余演示学生，同上 |

> ⚠️ **必须修改**：以上口令都是公开的默认值，只允许在本地演示环境使用。
> 任何对外可达的环境（内网测试机、预发、生产）上线前必须：
> 1. 登录后通过「个人中心 → 修改密码」改掉 `admin`；
> 2. 用管理员账号批量重置或禁用全部 `stu_*` / `teacher_*` 演示账号；
> 3. 或直接不导入 `V2__edu_permissions.sql` 的演示账号段（生产建议改为自建账号）。

---

## 6. 验证清单（部署后逐条执行）

```bash
cd deploy

# ① 四个容器都在运行，且 healthy
docker compose ps

# ② 后端存活（SecurityConfig 中 permitAll 的匿名接口，返回 JSON 即正常）
docker compose exec backend curl -fsS http://127.0.0.1:8080/captchaImage
# 期望：{"msg":"操作成功","img":"data:image/png;base64,...","code":200,...}

# ③ 前端首页可访问（经 nginx）
curl -I "http://localhost:${HTTP_PORT:-80}/"
# 期望：HTTP/1.1 200 OK，Content-Type: text/html

# ④ API 前缀可透传（未登录返回 401，说明已到达后端而不是 404）
curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:${HTTP_PORT:-80}/prod-api/system/user/getInfo"
curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:${HTTP_PORT:-80}/api/system/user/getInfo"
# 期望：两次都是 401（未带 token）；若为 502 说明 backend 还没起来，若为 404 说明前缀没剥对

# ⑤ 数据库初始化成功（应有 sys_* / QRTZ_* / edu_* 三类表）
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "select count(*) from information_schema.tables where table_schema=\"$MYSQL_DATABASE\";"'
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "use $MYSQL_DATABASE; show tables like \"edu%\";"'

# ⑥ 行数抽查：若依基础数据 + 教育权限数据（V3~V6 未启用时 edu_* 业务表为 0 行，属正常）
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
   select \"sys_user\" t, count(*) c from $MYSQL_DATABASE.sys_user
   union all select \"sys_role\", count(*) from $MYSQL_DATABASE.sys_role
   union all select \"sys_menu\", count(*) from $MYSQL_DATABASE.sys_menu
   union all select \"sys_dept\", count(*) from $MYSQL_DATABASE.sys_dept;"'
# 期望：sys_user >= 10（admin + 8 演示学生 + 1 演示教师），
#       sys_role >= 4，sys_dept >= 4，sys_menu 含 menu_id 2000 起的教育菜单

# ⑦ Redis 可认证
docker compose exec redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
# 期望：PONG
```

浏览器验证：打开 `http://localhost:${HTTP_PORT:-80}/`，
用 `admin / admin123` 登录，左侧应出现「教育平台」菜单（来自 `V2`）；
直接刷新 `http://localhost:${HTTP_PORT}/education/dashboard` 应仍能打开（SPA history 回退生效）。

---

## 7. 常用运维命令

```bash
cd deploy

# 日志（-f 跟踪；--tail=200 只看最近 200 行）
docker compose logs -f --tail=200 backend
docker compose logs -f --tail=200 frontend
docker compose logs -f --tail=200 mysql
docker compose logs -f --tail=200 redis

# 健康状态与健康检查历史
docker compose ps
docker inspect --format '{{json .State.Health}}' ruoyi-edu-backend

# 重启单个服务 / 全栈
docker compose restart backend
docker compose down && docker compose up -d

# 进容器排查
docker compose exec backend sh          # 后端（非 root 用户 ruoyi）
docker compose exec mysql bash          # MySQL
docker compose exec redis sh            # Redis
docker compose exec frontend sh         # Nginx

# 容器内直连后端调接口（绕过 nginx，验证问题是否出在网关）
docker compose exec backend curl -s -X POST http://127.0.0.1:8080/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

# 查看后端日志文件（logback 写到 /home/ruoyi/logs，已挂载 backend_logs 卷）
docker compose exec backend tail -n 200 /home/ruoyi/logs/sys-error.log

# 彻底销毁（⚠️ 含 -v 会删除 mysql/redis/上传文件的全部数据卷）
docker compose down            # 仅停容器，保留数据
docker compose down -v         # 停容器 + 删数据卷（重新初始化数据库时使用）
```

---

## 8. 重新导入初始化 SQL

**重要前提**：MySQL 官方镜像只在数据目录为空（首次初始化）时执行
`/docker-entrypoint-initdb.d/*`。已有 `mysql_data` 卷时，改 SQL 文件不会自动重放。

当前启用的是 4 个脚本（顺序即编号）：

```text
01_ry_20260417.sql        backend/sql/ry_20260417.sql        若依基础表（sys_*）
02_quartz.sql             backend/sql/quartz.sql             定时任务表（QRTZ_*）
03_V1__edu_schema.sql     backend/sql/edu/V1__edu_schema.sql  教育业务表结构（edu_*）
04_V2__edu_permissions.sql backend/sql/edu/V2__edu_permissions.sql 角色、演示账号、菜单权限
```

### 8.1 全量重来（最干净，会丢数据）

```bash
cd deploy
docker compose down -v          # 删除 mysql_data 卷
docker compose up -d --build    # 重新初始化：01 -> 02 -> 03 -> 04
```

### 8.2 只重放教育库脚本（保留 sys_* 里的账号配置等）

```bash
cd deploy

# 取出 .env 中的口令（bash / zsh）
set -a; . ./.env; set +a

# V1 是 DROP TABLE IF EXISTS + CREATE，可重复执行
docker compose exec -T mysql mysql -uroot -p"${DB_ROOT_PASSWORD}" "${DB_NAME}" \
  < ../backend/sql/edu/V1__edu_schema.sql

# V2 是纯 INSERT（带固定主键），重复执行会主键冲突；请先跑 V1（会重建 edu_* 表）再跑 V2。
docker compose exec -T mysql mysql -uroot -p"${DB_ROOT_PASSWORD}" "${DB_NAME}" \
  < ../backend/sql/edu/V2__edu_permissions.sql
```

Windows PowerShell 等价写法（用管道代替 `<` 重定向，库名取 `.env` 里的 `DB_NAME`）：

```powershell
Get-Content ..\backend\sql\edu\V1__edu_schema.sql -Raw |
  docker compose exec -T mysql mysql -uroot -p"$env:DB_ROOT_PASSWORD" education_db
```

### 8.3 启用 V3~V6 演示数据（目前仍是注释状态）

`backend/sql/edu/V3__edu_seed_course.sql` ~ `V6__edu_seed_competency.sql` 目前**尚未生成**
（生成器 `pipeline/course_import/build_edu_seed.py` 也还不存在），
因此 `docker-compose.yml` 里 05~08 四段 seed 挂载保持注释，
以保证 `docker compose up` 开箱可用（空库也能跑通登录与菜单）。

生成出 V3~V6 之后按以下步骤启用：

1. 打开 `deploy/docker-compose.yml`，取消 `volumes:` 下 05~08 四段注释
   （`05_V3__edu_seed_course.sql` … `08_V6__edu_seed_competency.sql`，
   源路径分别为 `../backend/sql/edu/V3__edu_seed_course.sql` … `V6__edu_seed_competency.sql`）；
2. 因为初始化脚本只在空库时执行，必须重置数据卷：

```bash
cd deploy
docker compose down -v          # ⚠️ 删除 mysql_data（含历史数据）
docker compose up -d --build    # 重新初始化：01 -> 02 -> 03 -> 04 -> 05 -> 06 -> 07 -> 08
```

3. 再跑一遍第 6 节的验证清单，`edu_course` / `edu_student` / `edu_competency_tag`
   等表应出现非 0 行数。

> 如果暂时不想删卷，也可以手工导入（顺序必须 V3→V4→V5→V6，后一个依赖前一个的主键）：
> `docker compose exec -T mysql mysql -uroot -p"${DB_ROOT_PASSWORD}" "${DB_NAME}" < ../backend/sql/edu/V3__edu_seed_course.sql`

---

## 9. 备份与恢复演练

备份目录 `deploy/backups/` 已挂载进 MySQL 容器（容器内 `/backups`），
`mysqldump` 直接写进该目录即可落到宿主，无需在宿主装 mysql 客户端。

### 9.1 备份（逻辑备份）

```bash
cd deploy
mkdir -p backups

# 在容器内执行，$MYSQL_ROOT_PASSWORD / $MYSQL_DATABASE 是 mysql 容器的环境变量
# （MYSQL_DATABASE 即 education_db）
docker compose exec mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
     --single-transaction --quick --routines --triggers --events \
     --default-character-set=utf8mb4 \
     "$MYSQL_DATABASE" > /backups/education_db-$(date +%Y%m%d-%H%M).sql'

# 校验产物
ls -lh backups/ && head -20 backups/education_db-*.sql
```

说明：
- `--single-transaction`：InnoDB 在线一致性快照，不锁表；
- 不加 `--databases`：备份文件里不含 `CREATE DATABASE`/`USE`，方便恢复到任意库名（见 9.2）；
- 建议 cron / 计划任务每天跑一次，并把 `deploy/backups/` 同步到另一台机器或对象存储
  （同一块盘上的备份不算备份）。

### 9.2 恢复演练（恢复到临时库，不动业务库）

```bash
cd deploy

DUMP=$(ls -t backups/education_db-*.sql | head -1)
echo "即将演练恢复：$DUMP"

# ① 建临时库
docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE IF EXISTS education_restore_test; \
   CREATE DATABASE education_restore_test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"'

# ② 导入
docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" education_restore_test < /backups/'"$(basename "$DUMP")"''

# ③ 校验（行数应与备份时一致；未启用 V3~V6 时 edu_* 业务表为 0 属正常）
docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
     select \"sys_user\" t, count(*) c from education_restore_test.sys_user
     union all select \"sys_menu\", count(*) from education_restore_test.sys_menu
     union all select \"edu_student\", count(*) from education_restore_test.edu_student;"'

# ④ 清理临时库
docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE education_restore_test;"'
```

### 9.3 真·灾难恢复（整库回滚）

```bash
cd deploy
docker compose stop backend                      # 先停应用，避免半写
DUMP=$(ls -t backups/education_db-*.sql | head -1)
set -a; . ./.env; set +a                         # 取 DB_NAME（默认 education_db）

docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE IF EXISTS '"$DB_NAME"'; \
   CREATE DATABASE '"$DB_NAME"' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"'

docker compose exec mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" '"$DB_NAME"' < /backups/'"$(basename "$DUMP")"''

docker compose start backend
docker compose logs --tail=100 backend
```

> 生产环境建议额外做：每日全量 `mysqldump` + binlog 归档（`--flush-logs` + `mysqlbinlog` 时点恢复），
> 以及「备份文件能在干净环境恢复」的定期演练记录。

---

## 10. 数据持久化

| 卷名 | 挂载点 | 内容 |
|---|---|---|
| `ruoyi-edu-mysql-data` | `mysql:/var/lib/mysql` | 全部业务数据（`education_db`） |
| `ruoyi-edu-redis-data` | `redis:/data` | AOF（登录 token、缓存；丢了只是全员重登） |
| `ruoyi-edu-uploads` | `backend:/app/data` | 上传文件（`ruoyi.profile=/app/data/uploadPath`） |
| `ruoyi-edu-logs` | `backend:/home/ruoyi/logs` | logback 日志文件 |

```bash
docker volume ls | grep ruoyi-edu
docker volume inspect ruoyi-edu-mysql-data
```

> 注意：`docker compose down -v` 会删除上述全部卷（含上传文件），只在确认要重建时使用。

---

## 11. 改代码后如何重建

```bash
cd deploy
docker compose build backend        # 只重建后端镜像
docker compose build frontend       # 只重建前端镜像
docker compose up -d                # 用新镜像滚动重启
docker compose up -d --build        # 构建 + 启动一步到位
```

后端 Dockerfile 分了「依赖缓存层」（只 COPY 各 pom 后跑 `dependency:go-offline`）与「源码层」，
只改 Java 代码时不会重下 Maven 依赖；前端只有改 `package.json` 才会重新 `npm ci`/`npm install`。

> 重建后端后如果页面变 502：`docker compose restart frontend`
> （nginx 只在启动时解析一次 `backend` 域名，旧 IP 会一直留在内存里）。

---

## 12. 安全基线开关（Druid 监控台 / Swagger 文档）

容器化部署默认关闭这两个「内部调试入口」（基线见
`docs/architecture/adr/ADR-0006-security-baseline.md`）：

| 开关 | 默认值 | 作用 |
|---|---|---|
| `DRUID_STAT_VIEW_ENABLED` | `false` | Druid 监控台 `/prod-api/druid/`（`statViewServlet.enabled`，在 `application-docker.yml` 覆盖） |
| `DRUID_LOGIN_USERNAME` | `ruoyi` | 监控台账号（`application-druid.yml` 已改为 `${DRUID_LOGIN_USERNAME:ruoyi}`） |
| `DRUID_LOGIN_PASSWORD` | `123456`（原值） | 监控台口令（`application-druid.yml` 已改为 `${DRUID_LOGIN_PASSWORD:123456}`） |
| `DRUID_ALLOW` | 空 = 允许所有来源 | 监控台白名单网段，建议 `172.16.0.0/12` |
| `SPRINGDOC_ENABLED` | `false` | Swagger UI 与 `/v3/api-docs`（`springdoc.*.enabled`，在 `application-docker.yml` 覆盖） |

临时排查性能问题时：

```bash
cd deploy
# 编辑 .env
#   DRUID_STAT_VIEW_ENABLED=true
#   DRUID_LOGIN_PASSWORD=<强口令>
#   DRUID_ALLOW=172.16.0.0/12
docker compose up -d backend        # 只重建后端容器，读取新的环境变量
```

看完立刻改回 `false` 并再次 `docker compose up -d backend`。

> 注意：`DRUID_ALLOW` 为空字符串时 Druid 表示「允许所有来源」。
> 打开监控台时务必同时设置强口令和白名单，排查结束后立即关闭。

---

## 13. 故障排查

| 现象 | 可能原因 | 排查 / 解决 |
|---|---|---|
| `docker compose up` 报 `required variable DB_PASSWORD is missing a value` | 没有 `.env`，或 `.env` 里该值为空 | `cp .env.example .env` 并填值；确认在 `deploy/` 目录执行 |
| `bind source path does not exist`（初始化 SQL） | `backend/sql/...` 被改名/删除 | 确认 `backend/sql/ry_20260417.sql`、`quartz.sql`、`edu/V1__..`、`edu/V2__..` 存在；缺文件时 compose 会直接报错（刻意设置 `create_host_path: false`） |
| 报 `bind source path does not exist` 且路径是 `V3__..`/`V4__..` | 取消了 05~08 注释但 V3~V6 还没生成 | 先跑生成器生成 `backend/sql/edu/V3~V6`，或重新把这四段注释掉 |
| 后端容器反复重启，日志 `Communications link failure` / `Connection refused` | MySQL 未就绪，或 `DB_HOST` 被写成 `localhost` | `DB_HOST` 必须是 `mysql`；`docker compose ps` 确认 mysql healthy；`docker compose logs mysql` |
| 后端日志 `Access denied for user 'ruoyi'@'...'` | `.env` 里 `DB_PASSWORD` 与数据卷里已初始化的口令不一致（改过 `.env` 但没重建卷） | 要么改回旧口令，要么 `docker compose down -v` 重建；或进 mysql 执行 `ALTER USER 'ruoyi'@'%' IDENTIFIED BY '新口令';` |
| 后端日志 `Unknown database 'education_db'` | 数据卷是旧的（初始化时库名还是 ry-vue），或手工改了 `DB_NAME` | `docker compose down -v` 重建；或 `CREATE DATABASE education_db CHARACTER SET utf8mb4;` 后手工导入 `backend/sql` 的脚本 |
| 后端日志 `Public Key Retrieval is not allowed` | JDBC 用了 MySQL 8 的 `caching_sha2_password` 但未允许取公钥 | `application-docker.yml` 已内置 `allowPublicKeyRetrieval=true`；若自行改过 URL 请补回 |
| 后端启动失败：`Could not resolve placeholder 'TOKEN_SECRET'` | `.env` 里 `TOKEN_SECRET` 为空 | 填一个 ≥32 位的随机串，`docker compose up -d` 重建容器 |
| 打开页面是 502 Bad Gateway | backend 还在启动 / 已崩溃 / 重建 backend 后 nginx 缓存了旧 IP | `docker compose ps` 看 backend 健康状态；`docker compose logs --tail=200 backend`；若是刚重建过后端：`docker compose restart frontend` |
| 页面能开但接口全 404 | nginx 前缀没剥对，或 `VITE_APP_BASE_API` 不是 `/prod-api` | 检查 `frontend/.env.production` 与 `deploy/nginx/nginx.conf`；`docker compose exec frontend nginx -T \| grep -A3 'location /prod-api'` |
| 页面能开但接口 401 且登录也失败 | Redis 口令不一致 / Redis 未起 | `docker compose exec redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'`；核对 `.env` |
| 访问 `/prod-api/druid/` 或 `/prod-api/swagger-ui.html` 返回 404 | **预期行为**：安全基线默认关闭这两个入口 | 需要时按第 12 节设 `DRUID_STAT_VIEW_ENABLED=true` / `SPRINGDOC_ENABLED=true` 后 `docker compose up -d backend` |
| 打开了 Druid 监控台但登录被拒 / 提示无权限 | `DRUID_LOGIN_USERNAME` / `DRUID_LOGIN_PASSWORD` 与容器当前环境变量不一致 | `docker compose exec backend env \| grep DRUID` 核对；改完 `.env` 需 `docker compose up -d backend`（不是 restart 才能确保变量刷新） |
| 上传文件 413 | 请求体超过 `client_max_body_size 10m` | 大文件上传时把 nginx 的该值调到 `25m`（后端 `max-request-size` 默认 20MB） |
| 登录后刷新页面 404 | SPA 回退没生效 | 确认 `location /` 有 `try_files $uri $uri/ /index.html`；`docker compose exec frontend nginx -t` |
| 前端构建失败 `npm ci` 报 `can only install with an existing package-lock.json` / lockfile 与 package.json 不一致 | `frontend/` 下缺 lockfile 或 lockfile 过期 | Dockerfile 已做回退（无 lockfile 时改用 `npm install`）；若 lockfile 与 package.json 不同步，请在 `frontend/` 执行 `npm install --package-lock-only` 重新生成并提交 |
| 前端构建阶段 `sass-embedded` 报错 | Alpine(musl) 下的原生二进制问题 | 把 `frontend.Dockerfile` 构建阶段基础镜像换成 `node:24-bookworm-slim` 后重试 |
| 编译报 `Could not resolve dependencies ... com.ruoyi:*` | 依赖缓存层预热失败（不影响真实构建） | 看构建日志中第二个 `mvn package` 阶段的报错；必要时 `docker compose build --no-cache backend` |
| 构建报 `failed to compute cache key` / 找不到 `nginx_conf` | Docker 版本过旧，未启用 BuildKit / 不支持命名构建上下文 | 升级 Docker Desktop（23+）；或按附录 A 改成 bind mount 方案 |
| 端口 80 被占用 `bind: address already in use` | 宿主已有 IIS/nginx/Apache | 改 `.env` 的 `HTTP_PORT=8081`，`docker compose up -d` |
| MySQL 起不来，日志 `[ERROR] --initialize specified but the data directory has files` | 数据卷里有旧的/损坏的数据目录 | `docker compose down -v` 重建（会丢数据，先备份） |

常用诊断命令：

```bash
docker compose config           # 渲染后的最终编排（检查变量替换是否正确）
docker compose exec frontend nginx -t          # nginx 配置语法检查
docker compose exec frontend nginx -T          # 打印最终生效配置
docker compose exec backend env | grep -E 'DB_|REDIS_|DRUID_|SPRINGDOC_|TOKEN_'  # 核对注入的变量
docker inspect --format '{{json .State.Health.Log}}' ruoyi-edu-backend | head -c 2000
```

---

## 14. 上线前安全清单

- [ ] `DB_PASSWORD` / `DB_ROOT_PASSWORD` / `REDIS_PASSWORD` / `TOKEN_SECRET` 已改为强随机值；
- [ ] `deploy/.env` 没有被提交（`git status` 确认）；
- [ ] 修改 `admin` 及全部演示账号口令，或删除演示账号；
- [ ] 确认 `DRUID_STAT_VIEW_ENABLED=false`；确需打开时同时设置 `DRUID_LOGIN_PASSWORD` 强口令
      与 `DRUID_ALLOW` 内网白名单（默认口令 `ruoyi/123456` 绝不能上线）；
- [ ] 确认 `SPRINGDOC_ENABLED=false`（默认已关闭；联调后要改回 false）；
- [ ] `logging.level.com.ruoyi` 从 `debug` 调到 `info`（可用环境变量 `LOGGING_LEVEL_COM_RUOYI=info` 覆盖）；
- [ ] 对外只暴露 `${HTTP_PORT}`（80/443）；`mysql` / `redis` / `backend` 不要发布端口；
- [ ] 在 nginx 之前再加一层 TLS（证书/Caddy/云负载均衡），或给 nginx 加 443 server 块；
- [ ] 配置 `deploy/backups` 的定时备份与异地同步，并做一次恢复演练；
- [ ] `sys_config` 的 `sys.account.captchaEnabled` 保持 `true`，`ruoyi.captchaType` 建议 `char`。

---

## 附录 A：BuildKit / 命名构建上下文不可用时的兜底

`frontend` 的构建上下文必须是 `frontend/` 目录，而 nginx 配置在 `deploy/nginx/`，
因此 `docker-compose.yml` 用 `additional_contexts: nginx_conf: ../deploy/nginx`
配合 `COPY --from=nginx_conf` 注入配置（需要 BuildKit，Docker Desktop 默认已启用；
Compose 侧要求 v2.17+）。

如果构建报 `failed to compute cache key` / `no such context`，改用运行时挂载：

1. 注释掉 `docker-compose.yml` 中 `frontend.build.additional_contexts` 两行，
   并注释掉 `frontend.Dockerfile` 里
   `COPY --from=nginx_conf nginx.conf /etc/nginx/conf.d/default.conf` 一行；
2. 在 `frontend` 服务下加：

```yaml
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
```

3. `docker compose up -d --build`，之后改 nginx 配置只需 `docker compose restart frontend`。

## 附录 B：为什么后端没有 `/actuator/health`

若依 v3.9.2 没有引入 `spring-boot-starter-actuator`（`backend/pom.xml` 与
`backend/ruoyi-admin/pom.xml` 中都没有该依赖，`application.yml` 里也没有 `management.*` 配置），
所以健康检查用的是 `SecurityConfig` 中 `permitAll` 的匿名接口 `/captchaImage`
（`backend/ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`）。
如果以后引入了 actuator，可把 `deploy/backend.Dockerfile` 的 HEALTHCHECK 改成
`curl -fsS http://127.0.0.1:8080/actuator/health`，并记得在配置里只暴露 `health`。
