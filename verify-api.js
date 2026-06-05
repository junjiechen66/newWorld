const { Client } = require('ssh2');
const c = new Client();
c.on('ready', () => {
  const cmds = [
    'echo "=== POST /api/auth/login ==="',
    `curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>&1`,
    'echo "\\n=== POST /api/auth/register ==="',
    `curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"test","password":"test123"}' 2>&1`,
    'echo "\\n=== GET /api/auth/user-info ==="',
    `curl -s http://localhost:8080/api/auth/user-info 2>&1`,
  ];
  c.exec(cmds.join('; '), (e, s) => {
    let o = '';
    s.on('data', d => o += d.toString());
    s.stderr.on('data', d => o += d.toString());
    s.on('close', () => { console.log(o); c.end(); });
  });
});
c.on('error', e => { console.error(e.message); process.exit(1); });
c.connect({ host: '8.153.13.15', port: 22, username: 'root', password: '1qaz@WSX3edc', readyTimeout: 10000 });
