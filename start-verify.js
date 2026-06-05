const { Client } = require('ssh2');

function connect() {
  return new Promise((resolve, reject) => {
    const c = new Client();
    c.on('ready', () => resolve(c));
    c.on('error', reject);
    c.connect({ host: '8.153.13.15', port: 22, username: 'root', password: '1qaz@WSX3edc', readyTimeout: 30000 });
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

async function main() {
  const c = await connect();
  console.log('SSH连接成功\n');

  // 1. Stop any old container
  console.log('=== 停止旧容器 ===');
  let r = await exec(c, 'cd /root/newWorld && docker compose down 2>&1');
  console.log(r.out);

  // 2. Start new container
  console.log('=== 启动新容器 ===');
  r = await exec(c, 'cd /root/newWorld && docker compose up -d app 2>&1');
  console.log(r.out);

  // 3. Wait and check status
  console.log('=== 等待应用启动(15s) ===');
  await new Promise(r => setTimeout(r, 15000));

  // 4. Container status
  console.log('=== 容器状态 ===');
  r = await exec(c, `docker ps --filter name=newworld-app --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"`);
  console.log(r.out);

  // 5. Check logs
  console.log('=== 应用日志(最近30行) ===');
  r = await exec(c, 'docker logs --tail 30 newworld-app 2>&1');
  console.log(r.out);

  // 6. Test API
  console.log('=== 接口测试 ===');
  
  // Test root
  r = await exec(c, `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ 2>&1`);
  console.log('GET / : ' + r.out);

  // Test login API
  r = await exec(c, `curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>&1`);
  console.log('POST /auth/login:');
  console.log('  ' + r.out.substring(0, 300));

  // Test user info
  r = await exec(c, `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/user/info 2>&1`);
  console.log('GET /user/info: ' + r.out);

  c.end();
}

main().catch(e => console.error('错误:', e.message));
