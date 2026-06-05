const { Client } = require('ssh2');

const HOST = '8.153.13.15';
const USER = 'root';
const PASS = '1qaz@WSX3edc';

function execCmd(conn, cmd) {
  return new Promise((resolve, reject) => {
    conn.exec(cmd, (err, stream) => {
      if (err) return reject(err);
      let stdout = '', stderr = '';
      stream.on('data', d => stdout += d.toString());
      stream.stderr.on('data', d => stderr += d.toString());
      stream.on('close', (code) => resolve({ stdout: stdout.trim(), stderr: stderr.trim(), code }));
    });
  });
}

async function main() {
  const conn = new Client();
  await new Promise((resolve, reject) => {
    conn.on('ready', resolve);
    conn.on('error', reject);
    conn.connect({ host: HOST, port: 22, username: USER, password: PASS, readyTimeout: 30000 });
  });
  console.log('SSH连接成功');

  console.log('\n=== 开始构建 Docker 镜像 ===');
  const r = await execCmd(conn, 'cd /root/newWorld && docker compose build app 2>&1');
  console.log('构建输出:');
  if (r.stdout) console.log(r.stdout);
  if (r.stderr) console.log(r.stderr);
  console.log('退出码:', r.code);

  conn.end();
}

main().catch(e => { console.error('错误:', e.message); process.exit(1); });
