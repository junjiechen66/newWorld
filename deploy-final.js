const { Client } = require('ssh2');

async function main() {
  const c = new Client();
  await new Promise((resolve, reject) => {
    c.on('ready', resolve);
    c.on('error', reject);
    c.connect({ host: '8.153.13.15', port: 22, username: 'root', password: '1qaz@WSX3edc', readyTimeout: 30000 });
  });
  console.log('SSH连接成功');

  function exec(cmd) {
    return new Promise((resolve, reject) => {
      c.exec(cmd, (err, stream) => {
        if (err) return reject(err);
        let out = '';
        stream.on('data', d => out += d.toString());
        stream.stderr.on('data', d => out += d.toString());
        stream.on('close', code => resolve(out));
      });
    });
  }

  console.log('\n=== 1. 停止旧容器 ===');
  console.log(await exec('cd /root/newWorld && docker compose down 2>&1'));

  console.log('=== 2. 启动新容器 ===');
  console.log(await exec('cd /root/newWorld && docker compose up -d app 2>&1'));

  console.log('=== 3. 等待启动(15s) ===');
  await new Promise(r => setTimeout(r, 15000));

  console.log('=== 4. 容器状态 ===');
  console.log(await exec(`docker ps --filter name=newworld-app --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"`));

  console.log('=== 5. 应用日志(最后25行) ===');
  console.log(await exec('docker logs --tail 25 newworld-app 2>&1'));

  console.log('=== 6. API测试 ===');
  let r = await exec(`curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>&1`);
  console.log('POST /api/auth/login ->', r.substring(0, 300));

  r = await exec(`curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"testuser","password":"test123"}' 2>&1`);
  console.log('POST /api/auth/register ->', r.substring(0, 300));

  r = await exec(`curl -s http://localhost:8080/api/auth/user-info 2>&1`);
  console.log('GET /api/auth/user-info ->', r.substring(0, 200));

  c.end();
}

main().catch(e => console.error('失败:', e.message));
