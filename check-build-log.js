const { Client } = require('ssh2');
const c = new Client();
c.on('ready', () => {
  c.exec('cat /tmp/build.log 2>&1 | tail -40', (e, s) => {
    let o = '';
    s.on('data', d => o += d.toString());
    s.stderr.on('data', d => o += d.toString());
    s.on('close', () => { console.log(o); c.end(); });
  });
});
c.on('error', e => { console.error(e.message); process.exit(1); });
c.connect({ host: '8.153.13.15', port: 22, username: 'root', password: '1qaz@WSX3edc', readyTimeout: 10000 });
