const { Client } = require('ssh2');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const HOST = '8.153.13.15';
const USER = 'root';
const PASS = '1qaz@WSX3edc';
const PROJECT_DIR = '/root/newWorld';
const BACKEND_DIR = path.resolve(__dirname, 'backend');

function sshExec(conn, command) {
  return new Promise((resolve, reject) => {
    conn.exec(command, (err, stream) => {
      if (err) return reject(err);
      let stdout = '', stderr = '';
      stream.on('data', d => stdout += d.toString());
      stream.stderr.on('data', d => stderr += d.toString());
      stream.on('close', (code) => resolve({ stdout: stdout.trim(), stderr: stderr.trim(), code }));
    });
  });
}

function connect() {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn.on('ready', () => resolve(conn));
    conn.on('error', e => reject(e));
    conn.connect({ host: HOST, port: 22, username: USER, password: PASS, readyTimeout: 30000, keepaliveInterval: 10000 });
  });
}

async function sftpUpload(conn, localPath, remotePath) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(new Error('SFTP init failed: ' + err.message));
      const readStream = fs.createReadStream(localPath);
      const writeStream = sftp.createWriteStream(remotePath);
      let done = false;
      writeStream.on('close', () => { if (!done) { done = true; resolve(); }});
      writeStream.on('error', (e) => { if (!done) { done = true; reject(e); }});
      readStream.on('error', (e) => { if (!done) { done = true; reject(e); }});
      readStream.pipe(writeStream);
    });
  });
}

async function runInitSql(conn) {
  console.log('[1/4] 初始化数据库表结构...');
  // Write SQL file to server first, then execute
  const sqlContent = fs.readFileSync(path.resolve(__dirname, 'backend/sql/init.sql'), 'utf-8');
  const r1 = await sshExec(conn, `cat > /tmp/init.sql << 'SQLEOF'\n${sqlContent}\nSQLEOF`);
  const r = await sshExec(conn, `/usr/local/mysql/mysql-8.0/bin/mysql -uroot -p1qaz@WSX3edc < /tmp/init.sql 2>&1`);
  if (r.stderr && !r.stderr.includes('Warning')) {
    console.log('  SQL错误:', r.stderr);
  } else {
    console.log('  SQL执行完成');
  }
  if (r.stdout) console.log('  ', r.stdout);
}

async function uploadFiles(conn) {
  console.log('[2/4] 打包并上传项目文件...');

  // Create tar on local machine (exclude target dir and node_modules)
  const tarFile = path.resolve(__dirname, 'deploy-tmp.tar.gz');
  console.log('  本地打包...');
  execSync(`tar -czf "${tarFile}" -C "${path.dirname(BACKEND_DIR)}" backend/pom.xml backend/Dockerfile backend/src docker-compose.yml backend/sql/init.sql nginx/nginx.conf nginx/Dockerfile frontend/dist`, { stdio: 'pipe' });
  const stats = fs.statSync(tarFile);
  console.log(`  打包完成: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);

  // Upload via SFTP
  console.log('  上传到服务器...');
  await sshExec(conn, `mkdir -p ${PROJECT_DIR}`);
  await sftpUpload(conn, tarFile, `/tmp/newworld-deploy.tar.gz`);

  // Extract on server
  console.log('  解压...');
  const r = await sshExec(conn, `cd ${PROJECT_DIR} && tar xzf /tmp/newworld-deploy.tar.gz && rm -f /tmp/newworld-deploy.tar.gz && ls -la`);
  console.log('  解压完成');
  if (r.stdout) console.log('  ', r.stdout);

  // Clean up local tar
  fs.unlinkSync(tarFile);
}

async function buildDocker(conn) {
  console.log('[3/4] 构建 Docker 镜像...');

  // 构建后端镜像
  console.log('  构建后端镜像...');
  const r1 = await sshExec(conn, `cd ${PROJECT_DIR} && docker compose build app 2>&1`);
  if (r1.stdout) {
    r1.stdout.split('\n').slice(-10).forEach(l => console.log('  ' + l));
  }
  if (r1.stderr) {
    r1.stderr.split('\n').slice(-5).forEach(l => console.log('  ' + l));
  }

  // 构建 Nginx 镜像
  console.log('  构建 Nginx 镜像...');
  const r2 = await sshExec(conn, `cd ${PROJECT_DIR} && docker compose build nginx 2>&1`);
  if (r2.stdout) {
    r2.stdout.split('\n').slice(-10).forEach(l => console.log('  ' + l));
  }
  if (r2.stderr) {
    r2.stderr.split('\n').slice(-5).forEach(l => console.log('  ' + l));
  }
}

async function runContainer(conn) {
  console.log('[4/4] 启动容器并验证...');

  // Stop old container
  console.log('  停止旧容器...');
  await sshExec(conn, `cd ${PROJECT_DIR} && docker compose down 2>&1`);

  // Start new
  console.log('  启动新容器...');
  const start = await sshExec(conn, `cd ${PROJECT_DIR} && docker compose up -d 2>&1`);
  if (start.stdout) console.log('  ' + start.stdout);
  if (start.stderr) console.log('  ' + start.stderr);

  // Wait for startup
  console.log('  等待应用启动...');
  await new Promise(r => setTimeout(r, 10000));

  // Check container status
  const ps = await sshExec(conn, `docker ps --filter name=newworld --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"`);
  console.log('  容器状态:');
  ps.stdout.split('\n').forEach(l => console.log('    ' + l));
  
  // Check backend logs
  const logs = await sshExec(conn, `docker logs --tail 40 newworld-app 2>&1`);
  console.log('  后端日志:');
  logs.stdout.split('\n').slice(-40).forEach(l => console.log('    ' + l));
  
  // Test API through Nginx
  console.log('  --- 接口测试 ---');
  const test1 = await sshExec(conn, `curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>&1`);
  console.log(`  GET / (前端页面) HTTP状态码: ${test1.stdout}`);
  
  const test2 = await sshExec(conn, `curl -s http://localhost/ 2>&1 | head -5`);
  if (test2.stdout) console.log(`  响应: ${test2.stdout.substring(0, 300)}`);
  
  // Test login API through Nginx
  const test3 = await sshExec(conn, `curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"test"}' 2>&1`);
  console.log(`  POST /api/auth/login HTTP状态码: ${test3.stdout}`);
}

async function main() {
  console.log('=== NewWorld 部署开始 ===\n');
  const conn = await connect();
  console.log('SSH 连接成功\n');

  try {
    await runInitSql(conn);
    await uploadFiles(conn);
    await buildDocker(conn);
    await runContainer(conn);
    console.log('\n=== 部署完成 ===');
  } catch (e) {
    console.error('\n部署失败:', e.message);
  } finally {
    conn.end();
  }
}

main();
