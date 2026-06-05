#!/bin/bash
echo "=== NewWorld 启动脚本 ==="
echo "正在启动 MySQL + Redis + 应用..."
docker-compose up -d
echo "启动完成！"
echo "访问地址: http://localhost:8080"
echo "Swagger文档: http://localhost:8080/doc.html"
