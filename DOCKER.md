# data-agent 本地数据库（Docker）启动说明

本工程使用 Docker 启动一份带 [pgvector](https://github.com/pgvector/pgvector) 的 PostgreSQL，
作为 `data-agent-backend` 的本地开发数据库（含业务表 + 向量存储）。

编排文件：[docker-compose.yml](./docker-compose.yml)
初始化 SQL：
- [init-pgvector.sql](./data-agent-backend/src/main/resources/init-pgvector.sql) —— 启用 `vector` 扩展
- [database.sql](./data-agent-backend/src/main/resources/database.sql) —— 业务表 DDL

## 0. 前置条件

- 已安装并启动 **Docker Desktop**
  - macOS：菜单栏小鲸鱼图标处于运行态
  - Windows 11：任务栏小鲸鱼图标处于运行态（建议开启 WSL2 后端）
- 本机 `5432` 端口未被占用（如果占用，可改 `docker-compose.yml` 里的 `ports`）

## 1. 启动

> Mac / Win 通用，命令一字不差。Win 上把 `cd` 后面换成你本机的工程路径即可。

```bash
cd /Users/yulong/yulongWorkSpace/data-agent
docker compose up -d
```

首次启动会自动完成：
1. 拉取 `pgvector/pgvector:pg16` 镜像
2. 创建数据库 `data_agent`（用户 `postgres` / 密码 `postgres`）
3. 按文件名顺序执行初始化脚本：
   - `00-pgvector.sql` → `CREATE EXTENSION vector`
   - `10-schema.sql`   → 建业务表

## 2. 查看启动日志

```bash
docker compose logs -f postgres
```

看到 `database system is ready to accept connections` 即为成功，
按 `Ctrl + C` 退出日志查看（不会停止容器）。

## 3. 验证

```bash
# 验证 pgvector 扩展已启用
docker exec -it data-agent-pg psql -U postgres -d data_agent -c "SELECT extname, extversion FROM pg_extension WHERE extname='vector';"

# 验证业务表已创建（应有 5 张表）
docker exec -it data-agent-pg psql -U postgres -d data_agent -c "\dt"
```

预期能看到：
- 扩展：`vector`
- 表：`db_column`、`db_foreign_key`、`db_table`、`glossary_knowledge`、`question_knowledge`

## 4. 连接信息

| 项 | 值 |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `data_agent` |
| Username | `postgres` |
| Password | `postgres` |

后端 [application.yml](./data-agent-backend/src/main/resources/application.yml) 已按此配置接入。

## 5. 常用运维命令

```bash
docker compose ps            # 查看容器状态
docker compose stop          # 停止（保留数据）
docker compose start         # 再次启动
docker compose restart       # 重启
docker compose down          # 删除容器（保留数据卷）
docker compose down -v       # 删除容器 + 数据卷（彻底重置，下次启动会重新执行初始化 SQL）

# 进入数据库交互终端
docker exec -it data-agent-pg psql -U postgres -d data_agent
```

## 6. 重要提醒

`init-pgvector.sql` 和 `database.sql` **只在数据卷 `pgdata` 为空时执行一次**。
若你修改了这两个 SQL，已存在的库不会自动同步。如需让改动生效：

```bash
docker compose down -v
docker compose up -d
```

> 注意：`down -v` 会清空数据库里所有数据，请谨慎执行。

## 7. 常见问题

- **端口冲突 `port is already allocated`**
  本机已有 PostgreSQL 在跑。改 `docker-compose.yml` 的 `ports` 为 `"15432:5432"`，
  同时把 [application.yml](./data-agent-backend/src/main/resources/application.yml) 里的端口改成 `15432`。

- **Windows 上路径或换行符报错**
  确保本仓库的 SQL 文件不被 `git` 自动转换成 CRLF。可在仓库根目录新增/确认 `.gitattributes`：
  ```
  *.sql text eol=lf
  ```

- **想看挂载的 SQL 是否进了容器**
  ```bash
  docker exec -it data-agent-pg ls /docker-entrypoint-initdb.d
  ```
  应看到 `00-pgvector.sql` 和 `10-schema.sql`。
