# ---- 构建阶段：npm ci + vite build ----
FROM node:20-alpine AS builder
WORKDIR /build

# 先拷锁文件装依赖（层缓存：源码改动不触发重装 node_modules）
COPY package.json package-lock.json ./
RUN npm ci

# 再拷源码构建
COPY . .
RUN npm run build

# ---- 运行阶段：nginx 托管静态产物 + 反代后端 ----
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]