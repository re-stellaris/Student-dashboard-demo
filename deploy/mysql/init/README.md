# deploy/mysql/init —— 故意保持为空

## 为什么这个目录里没有 SQL 文件

数据库初始化脚本的**唯一来源**是 `backend/sql/`：

| 挂载到容器内的顺序 | 真实文件（仓库内） |
|---|---|
| `/docker-entrypoint-initdb.d/01_ry_20260417.sql` | `backend/sql/ry_20260417.sql` |
| `/docker-entrypoint-initdb.d/02_quartz.sql` | `backend/sql/quartz.sql` |
| `/docker-entrypoint-initdb.d/03_V1__edu_schema.sql` | `backend/sql/edu/V1__edu_schema.sql` |
| `/docker-entrypoint-initdb.d/04_V2__edu_permissions.sql` | `backend/sql/edu/V2__edu_permissions.sql` |
| `/docker-entrypoint-initdb.d/05_V3__edu_seed_course.sql` | `backend/sql/edu/V3__edu_seed_course.sql`（待生成，见下） |
| `/docker-entrypoint-initdb.d/06_V4__edu_seed_curriculum.sql` | `backend/sql/edu/V4__edu_seed_curriculum.sql`（待生成） |
| `/docker-entrypoint-initdb.d/07_V5__edu_seed_student.sql` | `backend/sql/edu/V5__edu_seed_student.sql`（待生成） |
| `/docker-entrypoint-initdb.d/08_V6__edu_seed_competency.sql` | `backend/sql/edu/V6__edu_seed_competency.sql`（待生成） |

这些文件在 `deploy/docker-compose.yml` 里以**单文件 bind mount**（`type: bind` +
`read_only: true` + `create_host_path: false`）的方式挂载进 MySQL 容器，
挂载顺序即 `versions.md` 第 4 节规定的迁移顺序。

## 强制规则

1. **禁止在本目录放 SQL 副本、软链接或 `COPY` 出来的版本。**
   一旦出现第二份拷贝，`backend/sql` 的改动就不会反映到容器里，
   最终会出现「本地库结构对、容器库结构旧」这类极难排查的问题。
2. 需要改表结构时，改 `backend/sql/` 下的文件（或新增 `V7__...`），
   然后按 `deploy/README.md`「重新导入初始化 SQL」重跑。
3. MySQL 官方镜像只在数据目录**为空**（即首次初始化）时执行
   `/docker-entrypoint-initdb.d/*`。已有 `mysql_data` 卷时，改 SQL 文件不会自动重放。

## 为什么 V3~V6 目前在 compose 里是注释状态

仓库里目前只有 `V1__edu_schema.sql` 和 `V2__edu_permissions.sql`；
`V3~V6` 的演示数据以及生成器 `pipeline/course_import/build_edu_seed.py` 尚未提交。
把不存在的文件挂进容器会导致 `docker compose up` 直接失败（`create_host_path: false`
就是为了让这种缺失立刻报错，而不是静默创建空目录）。

生成出 V3~V6 之后，取消 `deploy/docker-compose.yml` 中 05~08 四段挂载的注释，
并清空数据卷重新初始化：

```bash
cd deploy
docker compose down -v
docker compose up -d --build
```
