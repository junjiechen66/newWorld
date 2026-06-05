const { Client } = require('ssh2');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const HOST = '8.153.13.15';
const USER = 'root';
const PASS = '1qaz@WSX3edc';

function connect() {
  return new Promise((resolve, reject) => {
    const c = new Client();
    c.on('ready', () => resolve(c));
    c.on('error', reject);
    c.connect({ host: HOST, port: 22, username: USER, password: PASS, readyTimeout: 30000 });
  });
}

function exec(c, cmd) {
  return new Promise((resolve, reject) => {
    c.exec(cmd, (err, stream) => {
      if (err) return reject(err);
      let out = '';
      stream.on('data', d => out += d.toString());
      stream.stderr.on('data', d => out += d.toString());
      stream.on('close', code => resolve({ out, code }));
    });
  });
}

function sftpUpload(conn, localPath, remotePath) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err);
      const rs = fs.createReadStream(localPath);
      const ws = sftp.createWriteStream(remotePath);
      let done = false;
      ws.on('close', () => { if(!done){done=true;resolve();} });
      ws.on('error', e => { if(!done){done=true;reject(e);} });
      rs.on('error', e => { if(!done){done=true;reject(e);} });
      rs.pipe(ws);
    });
  });
}

async function main() {
  const c = await connect();
  console.log('SSH连接成功\n');

  // 1. Upload updated docker-compose.yml
  console.log('=== 上传 docker-compose.yml ===');
  await sftpUpload(c, path.resolve(__dirname, 'docker-compose.yml'), '/root/newWorld/docker-compose.yml');
  console.log('  完成');

  // 2. Rebuild Docker image using previously prepared local tar with updated config
  console.log('=== 重新打包源代码 ===');
  const tarFile = path.resolve(__dirname, 'deploy-tmp.tar.gz');
  execSync(`tar -czf "${tarFile}" -C "${path.dirname(path.resolve(__dirname, 'backend'))}" backend/pom.xml backend/Dockerfile backend/src backend/sql/init.sql docker-compose.yml`, { stdio: 'pipe' });
  console.log('  打包完成');
  
  await sftpUpload(c, tarFile, '/tmp/newworld-deploy.tar.gz');
  console.log('  上传完成');
  
  await exec(c, 'cd /root/newWorld && tar xzf /tmp/newworld-deploy.tar.gz && rm -f /tmp/newworld-deploy.tar.gz');
  console.log('  解压完成');
  fs.unlinkSync(tarFile);

  // 3. Rebuild
  console.log('=== 后台构建 Docker 镜像 ===');
  await exec(c, 'cd /root/newWorld && nohup docker compose build app > /tmp/build.log 2>&1 & echo started');
  console.log('  构建已启动，等待完成...');
  
  // Poll build log until done
  let done = false;
  while (!done) {
    await new Promise(r => setTimeout(r, 10000));
    const r = await exec(c, 'tail -3 /tmp/build.log 2>&1');
    if (r.out.includes('BUILD SUCCESS') || r.out.includes('BUILD FAILURE') || r.out.includes('ERROR')) {
      done = true;
      console.log('  构建结果:', r.out);
    } else {
      console.log('  仍在构建中...');
    }
  }

  // 4. Deploy
  console.log('\n=== 停止旧容器 ===');
  await exec(c, 'cd /root/newWorld && docker compose down 2>&1');
  
  console.log('=== 启动新容器 ===');
  await exec(c, 'cd /root/newWorld && docker compose up -d app 2>&1');
  
  console.log('=== 等待应用启动(15s) ===');
  await new Promise(r => setTimeout(r, 15000));
  
  // 5. Verify
  console.log('=== 容器状态 ===');
  let r = await exec(c, `docker ps --filter name=newworld-app --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"`);
  console.log(r.out);
  
  console.log('=== 应用日志(最后20行) ===');
  r = await exec(c, 'docker logs --tail 20 newworld-app 2>&1');
  console.log(r.out);
  
  console.log('=== 接口测试 ===');
  r = await exec(c, `curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>&1`);
  console.log('POST /api/auth/login:', r.out.substring(0, 200));
  
  r = await exec(c, `curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"newuser","password":"pass123"}' 2>&1`);
  console.log('POST /api/auth/register:', r.out.substring(0, 200));
  
  c.end();
}

main().catch(e => console.error('错误:', e.message));
