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
  console.log('SSH连接成功');

  // Run build in background, redirect output to log file
  console.log('启动后台构建...');
  const cmd = 'cd /root/newWorld && nohup docker compose build app > /tmp/build.log 2>&1 & echo PID=$!';
  const r = await exec(c, cmd);
  console.log(r.out);
  console.log('构建任务已在后台启动，PID如上');

  // Wait a moment then check progress
  await new Promise(r => setTimeout(r, 5000));
  const r2 = await exec(c, 'cat /tmp/build.log 2>&1 | tail -30');
  console.log('\\n当前日志:');
  console.log(r2.out);

  c.end();
}

main().catch(e => console.error('错误:', e.message));
