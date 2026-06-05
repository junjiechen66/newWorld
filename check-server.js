const { Client } = require('ssh2');

function sshExec(commands) {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn.on('ready', () => {
      conn.exec(commands, (err, stream) => {
        if (err) { conn.end(); return reject(err); }
        let o = '';
        stream.on('data', d => o += d.toString());
        stream.stderr.on('data', d => o += d.toString());
        stream.on('close', (code) => {
          conn.end();
          resolve({ stdout: o, code });
        });
      });
    });
    conn.on('error', e => reject(e));
    conn.connect({
      host: '8.153.13.15', port: 22,
      username: 'root', password: '1qaz@WSX3edc',
      readyTimeout: 10000
    });
  });
}

async function main() {
  console.log('=== 检查服务器已有项目目录 ===');
  let r = await sshExec('ls -la /root/newWorld/backend/src/main/resources/ 2>/dev/null || echo "no project"');
  console.log(r.stdout);

  console.log('=== 检查已运行的容器 ===');
  r = await sshExec('docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null');
  console.log(r.stdout);

  console.log('=== 检查端口占用 ===');
  r = await sshExec('ss -tlnp | grep -E "8080|3306|6379" || echo "no specific ports in use"');
  console.log(r.stdout);

  console.log('=== 检查现有配置文件 ===');
  r = await sshExec('cat /root/newWorld/backend/src/main/resources/application-prod.yml 2>/dev/null || echo "no config"');
  console.log(r.stdout);

  console.log('=== 检查MySQL连接 ===');
  r = await sshExec('docker exec newworld-mysql mysql -uroot -p1qaz@WSX3edc -e "SELECT 1 AS test;" 2>/dev/null || echo "mysql container not found or not running"');
  console.log(r.stdout);

  console.log('=== 检查Redis连接 ===');
  r = await sshExec('docker exec newworld-redis redis-cli -a 1qaz@WSX3edc PING 2>/dev/null || echo "redis container not found or not running"');
  console.log(r.stdout);
}

main().catch(e => { console.error(e); process.exit(1); });
