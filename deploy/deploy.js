#!/usr/bin/env node
/**
 * NewWorld 一键部署脚本
 *
 * 用法:
 *   node deploy/deploy.js             完整部署（前端构建→上传→构建镜像→启动→验证）
 *   node deploy/deploy.js quick       快速部署（仅上传→构建镜像→启动，跳过前端构建）
 *   node deploy/deploy.js build       仅远程构建镜像（不上传源码）
 *   node deploy/deploy.js restart     仅重启容器
 *   node deploy/deploy.js sql         仅执行初始化 SQL
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { CONFIG, sshExec, connect, sftpUpload } = require('./ssh');

const ROOT_DIR = path.resolve(__dirname, '..');
const TAR_FILE = path.resolve(__dirname, 'deploy-tmp.tar.gz');

// ============ 日志工具 ============
const LOG = {
  step: (n, msg) => console.log(`\n\x1b[36m[${n}/5] ${msg}\x1b[0m`),
  info: msg => console.log(`  ${msg}`),
  ok: msg => console.log(`  \x1b[32m✓ ${msg}\x1b[0m`),
  fail: msg => console.log(`  \x1b[31m✗ ${msg}\x1b[0m`),
  title: msg => console.log(`\n\x1b[1;35m${'='.repeat(50)}\n  ${msg}\n${'='.repeat(50)}\x1b[0m`)
};

// ============ 步骤1: 构建前端 ============
async function buildFrontend() {
  LOG.step(1, '构建前端');
  const frontendDir = path.resolve(ROOT_DIR, 'frontend');
  if (!fs.existsSync(path.resolve(frontendDir, 'node_modules'))) {
    LOG.info('安装前端依赖...');
    execSync('npm install', { cwd: frontendDir, stdio: 'pipe' });
  }
  LOG.info('执行 npm run build...');
  execSync('npm run build', { cwd: frontendDir, stdio: 'pipe' });
  const distSize = (fs.statSync(path.resolve(frontendDir, 'dist')).size / 1024).toFixed(0);
  LOG.ok(`前端构建完成 (${distSize} KB)`);
}

// ============ 步骤2: 上传源码 ============
async function uploadSource(conn, includeFrontend = true) {
  LOG.step(2, '打包并上传项目文件');

  // 构建 tar 包
  LOG.info('本地打包...');
  const files = [
    'backend/pom.xml', 'backend/Dockerfile', 'backend/src',
    'docker-compose.yml', 'backend/sql/init.sql',
    'nginx/nginx.conf', 'nginx/Dockerfile'
  ];
  if (includeFrontend) files.push('frontend/dist');

  execSync(
    `tar -czf "${TAR_FILE}" -C "${ROOT_DIR}" ${files.join(' ')}`,
    { stdio: 'pipe' }
  );
  const stats = fs.statSync(TAR_FILE);
  LOG.info(`打包完成: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);

  // 上传
  LOG.info('上传到服务器...');
  await sshExec(conn, `mkdir -p ${CONFIG.projectDir}`);
  await sftpUpload(conn, TAR_FILE, '/tmp/newworld-deploy.tar.gz');

  // 解压
  LOG.info('服务器解压...');
  const r = await sshExec(conn,
    `cd ${CONFIG.projectDir} && tar xzf /tmp/newworld-deploy.tar.gz --warning=no-timestamp && rm -f /tmp/newworld-deploy.tar.gz`
  );
  if (r.stderr && !r.stderr.includes('tar: Removing leading')) {
    LOG.fail('解压警告: ' + r.stderr);
  }
  LOG.ok('上传完成');

  // 清理本地临时文件
  fs.unlinkSync(TAR_FILE);
}

// ============ 步骤3: 构建 Docker 镜像 ============
async function buildImages(conn) {
  LOG.step(3, '构建 Docker 镜像');

  LOG.info('构建后端镜像 newworld-app...');
  const r1 = await sshExec(conn, `cd ${CONFIG.projectDir} && docker compose build app 2>&1`);
  if (r1.stdout) r1.stdout.split('\n').slice(-5).forEach(l => LOG.info(l));
  if (r1.code !== 0) throw new Error('后端镜像构建失败');
  LOG.ok('后端镜像构建完成');

  LOG.info('构建 Nginx 镜像 newworld-nginx...');
  const r2 = await sshExec(conn, `cd ${CONFIG.projectDir} && docker compose build nginx 2>&1`);
  if (r2.stdout) r2.stdout.split('\n').slice(-5).forEach(l => LOG.info(l));
  if (r2.code !== 0) throw new Error('Nginx 镜像构建失败');
  LOG.ok('Nginx 镜像构建完成');
}

// ============ 步骤4: 部署容器 ============
async function deployContainers(conn) {
  LOG.step(4, '部署容器');

  LOG.info('停止旧容器...');
  await sshExec(conn, `cd ${CONFIG.projectDir} && docker compose down 2>&1`);

  LOG.info('启动新容器...');
  const r = await sshExec(conn, `cd ${CONFIG.projectDir} && docker compose up -d 2>&1`);
  if (r.stdout) LOG.info(r.stdout);
  if (r.stderr) LOG.info(r.stderr);

  LOG.info('等待应用启动...');
  await new Promise(r => setTimeout(r, 12000));
  LOG.ok('容器已启动');
}

// ============ 步骤5: 验证部署 ============
async function verifyDeployment(conn) {
  LOG.step(5, '验证部署');

  // 容器状态
  LOG.info('容器状态:');
  const ps = await sshExec(conn,
    `docker ps --filter name=newworld --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"`
  );
  ps.stdout.split('\n').forEach(l => LOG.info(l));

  // 检查前端
  const test1 = await sshExec(conn, `curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>&1`);
  LOG.info(`前端页面 (http://localhost/): ${test1.stdout === '200' ? '\x1b[32m✓ 200 OK\x1b[0m' : '\x1b[31m' + test1.stdout + '\x1b[0m'}`);

  // 检查 API
  const test2 = await sshExec(conn,
    `curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>&1`
  );
  LOG.info(`API接口 (POST /api/auth/login): ${test2.stdout === '200' ? '\x1b[32m✓ 200 OK\x1b[0m' : '\x1b[31m' + test2.stdout + '\x1b[0m'}`);

  if (test1.stdout === '200' && test2.stdout === '200') {
    LOG.ok('所有服务正常！');
  } else {
    LOG.fail('部分服务异常，请查看详细日志');
    const logs = await sshExec(conn, 'docker logs --tail 30 newworld-app 2>&1');
    LOG.info('后端最新日志:');
    logs.stdout.split('\n').slice(-10).forEach(l => LOG.info('  ' + l));
  }
}

// ============ 执行 SQL ============
async function runSql(conn) {
  LOG.info('执行初始化 SQL...');
  const sqlPath = path.resolve(ROOT_DIR, 'backend/sql/init.sql');
  if (!fs.existsSync(sqlPath)) {
    LOG.fail('SQL 文件不存在: ' + sqlPath);
    return;
  }
  const sqlContent = fs.readFileSync(sqlPath, 'utf-8');
  await sshExec(conn, `cat > /tmp/init.sql << 'SQLEOF'\n${sqlContent}\nSQLEOF`);
  const r = await sshExec(conn,
    `/usr/local/mysql/mysql-8.0/bin/mysql -uroot -p1qaz@WSX3edc < /tmp/init.sql 2>&1`
  );
  if (r.stderr && !r.stderr.includes('Warning')) {
    LOG.fail('SQL 错误: ' + r.stderr);
  } else {
    LOG.ok('SQL 执行完成');
  }
}

// ============ 主流程 ============
async function main() {
  const command = process.argv[2] || 'full';

  LOG.title('NewWorld 部署工具');

  if (command === 'restart') {
    // 仅重启
    const conn = await connect();
    try {
      await deployContainers(conn);
      await verifyDeployment(conn);
    } finally {
      conn.end();
    }
    LOG.ok('重启完成！');
    return;
  }

  if (command === 'build') {
    // 仅远程构建
    const conn = await connect();
    try {
      await buildImages(conn);
    } finally {
      conn.end();
    }
    LOG.ok('镜像构建完成！');
    return;
  }

  if (command === 'sql') {
    const conn = await connect();
    try {
      await runSql(conn);
    } finally {
      conn.end();
    }
    LOG.ok('SQL 执行完成！');
    return;
  }

  if (command === 'quick') {
    // 快速部署（跳过前端构建）
    const conn = await connect();
    try {
      await uploadSource(conn, false);
      await buildImages(conn);
      await deployContainers(conn);
      await verifyDeployment(conn);
    } finally {
      conn.end();
    }
    LOG.ok('快速部署完成！');
    return;
  }

  // 完整部署
  const conn = await connect();
  try {
    await buildFrontend();
    await uploadSource(conn, true);
    await buildImages(conn);
    await deployContainers(conn);
    await verifyDeployment(conn);
    LOG.title('部署成功！ 访问地址: http://8.153.13.15');
  } catch (e) {
    LOG.fail('部署失败: ' + e.message);
    process.exit(1);
  } finally {
    conn.end();
  }
}

main();
