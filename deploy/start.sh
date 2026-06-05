#!/bin/bash
# NewWorld 本地开发启动
# 需要先启动 MySQL 和 Redis，然后启动 Docker 服务

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== NewWorld 启动脚本 ==="
echo ""
echo "确保本地的 MySQL (3306) 和 Redis (6379) 已启动"
echo ""

cd "$PROJECT_DIR"

# 构建后端和 Nginx 容器
docker compose up -d

echo ""
echo "启动完成！"
echo "访问地址: http://localhost"
echo "API文档: http://localhost/doc.html"
echo ""
echo "容器状态:"
docker ps --filter name=newworld --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
