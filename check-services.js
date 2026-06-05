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
          resolve({ stdout: o.trim(), code });
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
  // 检查MySQL数据库
  console.log('=== 检查 MySQL 数据库 ===');
  let r = await sshExec('/usr/local/mysql/mysql-8.0/bin/mysql -uroot -p1qaz@WSX3edc -e "SHOW DATABASES;" 2>&1');
  console.log(r.stdout);

  // 创建数据库如果不存在
  console.log('\\n=== 创建数据库(如果需要) ===');
  r = await sshExec('/usr/local/mysql/mysql-8.0/bin/mysql -uroot -p1qaz@WSX3edc -e "CREATE DATABASE IF NOT EXISTS newworld DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1');
  console.log(r.stdout);

  // 检查Redis
  console.log('\\n=== 检查 Redis 连接 ===');
  r = await sshExec('redis-cli -a 1qaz@WSX3edc PING 2>&1');
  console.log(r.stdout);

  // 检查Redis是否有密码
  console.log('\\n=== 检查 Redis 配置 ===');
  r = await sshExec('redis-cli -a 1qaz@WSX3edc CONFIG GET requirepass 2>&1');
  console.log(r.stdout);
}

main().catch(e => { console.error(e); process.exit(1); });
