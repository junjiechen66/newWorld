const { Client } = require('ssh2');
const path = require('path');

// ============ 配置 ============
const CONFIG = {
  host: '8.153.13.15',
  port: 22,
  username: 'root',
  password: '1qaz@WSX3edc',
  projectDir: '/root/newWorld'
};

// ============ SSH 执行命令 ============
function sshExec(conn, command) {
  return new Promise((resolve, reject) => {
    conn.exec(command, (err, stream) => {
      if (err) return reject(err);
      let stdout = '', stderr = '';
      stream.on('data', d => stdout += d.toString());
      stream.stderr.on('data', d => stderr += d.toString());
      stream.on('close', code => resolve({ stdout: stdout.trim(), stderr: stderr.trim(), code }));
    });
  });
}

// ============ SSH 连接 ============
function connect() {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn.on('ready', () => resolve(conn));
    conn.on('error', err => reject(err));
    conn.connect({
      host: CONFIG.host,
      port: CONFIG.port,
      username: CONFIG.username,
      password: CONFIG.password,
      readyTimeout: 30000,
      keepaliveInterval: 10000
    });
  });
}

// ============ SFTP 上传 ============
function sftpUpload(conn, localPath, remotePath) {
  return new Promise((resolve, reject) => {
    const fs = require('fs');
    conn.sftp((err, sftp) => {
      if (err) return reject(new Error('SFTP init failed: ' + err.message));
      const readStream = fs.createReadStream(localPath);
      const writeStream = sftp.createWriteStream(remotePath);
      let done = false;
      writeStream.on('close', () => { if (!done) { done = true; resolve(); } });
      writeStream.on('error', e => { if (!done) { done = true; reject(e); } });
      readStream.on('error', e => { if (!done) { done = true; reject(e); } });
      readStream.pipe(writeStream);
    });
  });
}

module.exports = { CONFIG, sshExec, connect, sftpUpload };
