# =====================================================================
# 若依前端镜像（多阶段构建：Node 构建 -> Nginx 运行）
# ---------------------------------------------------------------------
# 构建上下文必须是 frontend/ 目录本身（Vite 需要在 /app 下看到
# package.json、vite.config.ts、.env.production、src/ 等）。
# deploy/docker-compose.yml 里已显式声明：
#   context:    ../frontend
#   dockerfile: ../deploy/frontend.Dockerfile
#   additional_contexts.nginx_conf: ../deploy/nginx
#
# 手动构建（在 deploy/ 目录下）：
#   docker build -f frontend.Dockerfile \
#     --build-context nginx_conf=nginx \
#     -t ruoyi-edu/frontend:3.9.2 ../frontend
# =====================================================================

# ---------------------------------------------------------------------
# 阶段 1：npm 构建
# ---------------------------------------------------------------------
FROM node:24-alpine AS build

WORKDIR /app

# 依赖安装层：有 package-lock.json 就走 npm ci（可复现、更快、不会改版本），
# 没有则回退到 npm install，避免镜像构建直接失败（若依前端仓库历史上有无 lockfile 的状态）。
COPY package.json package-lock.json* ./

RUN if [ -f package-lock.json ]; then \
      echo "[build] package-lock.json found -> npm ci"; \
      npm ci --no-audit --no-fund; \
    else \
      echo "[build] no package-lock.json -> npm install (fallback)"; \
      npm install --no-audit --no-fund; \
    fi

# 构建期需要的其余源码（含 .env.production：VITE_APP_BASE_API=/prod-api，
# 该前缀由 nginx 反向代理剥离，见 deploy/nginx/nginx.conf）
COPY . .

# build:prod == vite build（mode=production，读取 .env.production）
# 产物目录：/app/dist（vite.config.ts -> build.outDir=dist）
RUN npm run build:prod

# ---------------------------------------------------------------------
# 阶段 2：Nginx 静态托管
# ---------------------------------------------------------------------
FROM nginx:1.29-alpine AS runtime

ENV TZ=Asia/Shanghai

# 用自定义配置替换镜像自带的 default.conf（SPA 回退 + gzip + 缓存 + API 反代）
RUN rm -f /etc/nginx/conf.d/default.conf

# 命名构建上下文 nginx_conf -> deploy/nginx（见文件头说明）
COPY --from=nginx_conf nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# nginx:alpine 自带 busybox wget，用它做存活探测（/ 返回 index.html 即 200）
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

# 镜像自带的 entrypoint 会先处理 /etc/nginx/templates/*.template，
# 这里没有模板，等价于直接启动 nginx。
CMD ["nginx", "-g", "daemon off;"]
