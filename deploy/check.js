#!/usr/bin/env node
/**
 * NewWorld 服务检查工具
 *
 * 用法:
 *   node deploy/check.js            完整检查（容器+日志+API+数据库）
 *   node deploy/check.js containers 查看容器状态
 *   node deploy/check.js logs       查看后端日志（默认最近40行）
 *   node deploy/check.js logs 100   查看后端日志（指定行数）
 *   node deploy/check.js api        测试 API 接口
 *   node deploy/check.js db         检查 MySQL + Redis
 *   node deploy/check.js server     检查服务器环境
 */

const { CONFIG, sshExec, connect } = require('./ssh');

const LOG = {
  title: msg => console.log(`\n\x1b[1;36m${msg}\x1b[0m`),
  info: msg => console.log(`  ${msg}`),
  ok: msg => console.log(`  \x1b[32m✓ ${msg}\x1b[0m`),
  fail: msg => console.log(`  \x1b[31m✗ ${msg}\x1b[0m`)
};

async function checkContainers(conn) {
  LOG.title('=== 容器状态 ===');
  const r = await sshExec(conn,
    `docker ps --filter name=newworld --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"`
  );
  console.log(r.stdout || '  无运行中的容器');

  const r2 = await sshExec(conn,
    `docker images --filter reference="newworld*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"`
  );
  if (r2.stdout) {
    LOG.title('=== 项目镜像 ===');
    console.log(r2.stdout);
  }
}

async function checkLogs(conn, lines = 40) {
  LOG.title(`=== 后端日志 (最近${lines}行) ===`);
  const r = await sshExec(conn, `docker logs --tail ${lines} newworld-app 2>&1`);
  console.log(r.stdout || '  (无日志)');
}

async function checkApi(conn) {
  LOG.title('=== API 接口测试 ===');

  const tests = [
    { name: '前端页面', cmd: 'curl -s -o /dev/null -w "%{http_code}" http://localhost/' },
    { name: 'POST /api/auth/login', cmd: `curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'` },
  ];

  let allOk = true;
  for (const t of tests) {
    const r = await sshExec(conn, t.cmd + ' 2>&1');
    const ok = r.stdout === '200';
    if (ok) LOG.ok(`${t.name} → 200 OK`);
    else { LOG.fail(`${t.name} → ${r.stdout || '连接失败'}${r.stderr ? ' (' + r.stderr + ')' : ''}`); allOk = false; }
  }

  // 如果 API 失败，尝试直接访问后端
  if (!allOk) {
    LOG.title('=== 尝试直接访问后端 ===');
    const r = await sshExec(conn, 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ 2>&1');
    LOG.info(`后端直连 (8080): ${r.stdout}`);
  }
}

async function checkDb(conn) {
  LOG.title('=== MySQL 检查 ===');
  const r1 = await sshExec(conn,
    `/usr/local/mysql/mysql-8.0/bin/mysql -uroot -p1qaz@WSX3edc -e "SELECT 1 AS ping; SHOW DATABASES LIKE 'newworld';" 2>&1`
  );
  if (r1.stderr && !r1.stderr.includes('Warning')) {
    LOG.fail('MySQL 连接失败: ' + r1.stderr.substring(0, 200));
  } else {
    LOG.ok('MySQL 连接正常');
    console.log(r1.stdout);
  }

  LOG.title('=== Redis 检查 ===');
  const r2 = await sshExec(conn, 'redis-cli -a 1qaz@WSX3edc PING 2>&1');
  if (r2.stdout === 'PONG') {
    LOG.ok('Redis 连接正常');
  } else {
    LOG.fail('Redis 连接失败: ' + (r2.stderr || r2.stdout || '未知错误'));
  }
}

async function checkServer(conn) {
  LOG.title('=== 服务器信息 ===');
  const r1 = await sshExec(conn, 'cat /etc/os-release 2>/dev/null | head -3; echo ---; free -h | grep Mem; echo ---; df -h / | tail -1');
  console.log(r1.stdout);

  LOG.title('=== Docker 环境 ===');
  const r2 = await sshExec(conn, 'docker --version 2>&1; docker compose version 2>/dev/null || docker-compose --version 2>/dev/null');
  console.log(r2.stdout);

  LOG.title('=== 项目目录 ===');
  const r3 = await sshExec(conn, `ls -la ${CONFIG.projectDir}/ 2>/dev/null || echo "项目目录不存在"`);
  console.log(r3.stdout);

  LOG.title('=== 端口占用 ===');
  const r4 = await sshExec(conn, 'ss -tlnp | grep -E "80|8080|3306|6379" || echo "无相关端口占用"');
  console.log(r4.stdout);
}

async function main() {
  const command = process.argv[2] || 'all';

  console.log('\x1b[1;35m=== NewWorld 服务检查 ===\x1b[0m\n');

  let conn;
  try {
    conn = await connect();
    LOG.ok('SSH 连接成功\n');
  } catch (e) {
    LOG.fail('SSH 连接失败: ' + e.message);
    process.exit(1);
  }

  try {
    switch (command) {
      case 'containers':
        await checkContainers(conn);
        break;
      case 'logs':
        await checkLogs(conn, parseInt(process.argv[3]) || 40);
        break;
      case 'api':
        await checkApi(conn);
        break;
      case 'db':
        await checkDb(conn);
        break;
      case 'server':
        await checkServer(conn);
        break;
      default:
        await checkContainers(conn);
        await checkLogs(conn, 20);
        await checkApi(conn);
        await checkDb(conn);
    }
  } finally {
    conn.end();
  }
}

main().catch(e => { console.error('\n错误:', e.message); process.exit(1); });
