@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo === NewWorld 启动脚本 ===
echo.
echo 确保本地的 MySQL (3306) 和 Redis (6379) 已启动
echo.
docker compose up -d
echo.
echo 启动完成！
echo 访问地址: http://localhost
echo API文档: http://localhost/doc.html
echo.
echo 容器状态:
docker ps --filter name=newworld --format "table {{.Names}}	{{.Status}}	{{.Ports}}"
pause
