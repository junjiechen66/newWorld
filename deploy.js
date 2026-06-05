#!/usr/bin/env node
/**
 * NewWorld 部署入口
 *
 * 用法:
 *   node deploy.js               完整部署
 *   node deploy.js quick         快速部署（跳过前端构建）
 *   node deploy.js restart       仅重启容器
 *   node deploy.js build         仅构建镜像
 *   node deploy.js sql           仅执行 SQL
 *   node deploy.js check         检查服务状态
 *   node deploy.js check logs    查看日志
 *   node deploy.js check api     测试 API
 *   node deploy.js check db      检查数据库
 */

const command = process.argv[2];

if (command === 'check') {
  // 检查类命令传递给 check.js
  process.argv.splice(2, 1);
  require('./deploy/check');
} else if (command === 'restart' || command === 'build' || command === 'sql' || command === 'quick' || command === undefined) {
  // 部署类命令传递给 deploy.js
  require('./deploy/deploy');
} else {
  console.log(`
未知命令: "${command}"

用法:
  node deploy.js               完整部署（前端构建→上传→构建镜像→启动→验证）
  node deploy.js quick         快速部署（跳过前端构建）
  node deploy.js restart       仅重启容器
  node deploy.js build         仅远程构建镜像（不上传源码）
  node deploy.js sql           仅执行 SQL
  node deploy.js check         检查服务状态
  node deploy.js check logs    查看日志
  node deploy.js check api     测试 API
  node deploy.js check db      检查数据库
  `);
}

