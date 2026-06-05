const { Client } = require('ssh2');
c = new Client();
c.on('ready', () => {
  c.exec("ip route show default 2>/dev/null || route -n 2>/dev/null | head -5; echo ===; docker run --rm alpine sh -c 'ip route | grep default' 2>&1 || echo alpine-no-ip; echo ===; cat /etc/hosts | head -5", (e, s) => {
    let o = '';
    s.on('data', d => o += d.toString());
    s.stderr.on('data', d => o += d.toString());
    s.on('close', () => { console.log(o); c.end(); });
  });
});
c.on('error', e => { console.error(e.message); process.exit(1); });
c.connect({ host: '8.153.13.15', port: 22, username: 'root', password: '1qaz@WSX3edc', readyTimeout: 10000 });
