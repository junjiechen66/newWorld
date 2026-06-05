const { Client } = require('ssh2');

const conn = new Client();
const host = '8.153.13.15';
const username = 'root';
const password = '1qaz@WSX3edc';

conn.on('ready', () => {
  console.log('SSH连接成功！');
  conn.exec('docker --version && docker compose version 2>/dev/null || docker-compose --version 2>/dev/null', (err, stream) => {
    if (err) {
      console.error('执行命令失败:', err.message);
      conn.end();
      return;
    }
    let output = '';
    stream.on('data', (data) => { output += data.toString(); });
    stream.stderr.on('data', (data) => { output += data.toString(); });
    stream.on('close', (code) => {
      console.log('命令输出:');
      console.log(output);
      console.log('退出码:', code);
      conn.end();
    });
  });
});

conn.on('error', (err) => {
  console.error('SSH连接失败:', err.message);
  process.exit(1);
});

conn.connect({
  host,
  port: 22,
  username,
  password,
  readyTimeout: 10000
});
