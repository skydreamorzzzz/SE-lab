// Exercise the real launcher in Windows PowerShell, including secure password prompts.
// Credentials and databases are isolated test fixtures; no existing app data is touched.
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const runId = Date.now().toString(36);
const output = path.join(root, 'target/launcher-compatibility', runId);
fs.mkdirSync(output, { recursive: true });
const wrapper = path.join(output, 'launch.ps1');
fs.writeFileSync(wrapper, `
$ErrorActionPreference = 'Stop'
foreach ($name in @('MARKET_ADMIN_USER', 'MARKET_ADMIN_PASSWORD', 'MARKET_SUPER_ADMIN_USER', 'MARKET_SUPER_ADMIN_PASSWORD')) {
    Remove-Item ('Env:' + $name) -ErrorAction SilentlyContinue
}
function Read-Host {
    param([string]$Prompt, [switch]$AsSecureString)
    if ($AsSecureString) {
        $secret = New-Object System.Security.SecureString
        foreach ($character in $env:LAUNCHER_TEST_PASSWORD.ToCharArray()) {
            $secret.AppendChar($character)
        }
        $secret.MakeReadOnly()
        return $secret
    }
    return $env:LAUNCHER_TEST_USER
}
$launchArguments = @()
$launchArguments += '--server.port=' + $env:LAUNCHER_TEST_PORT
$launchArguments += '--spring.datasource.url=jdbc:h2:file:' + $env:LAUNCHER_TEST_DATABASE + ';WRITE_DELAY=0'
if ($env:LAUNCHER_TEST_SUPER -eq 'true') {
    & (Join-Path $env:LAUNCHER_TEST_ROOT 'run-local.ps1') -SetupSuperAdmin @launchArguments
} else {
    & (Join-Path $env:LAUNCHER_TEST_ROOT 'run-local.ps1') @launchArguments
}
`, 'ascii');

async function freePort() {
  const listener = net.createServer();
  await new Promise(resolve => listener.listen(0, '127.0.0.1', resolve));
  const port = listener.address().port;
  await new Promise(resolve => listener.close(resolve));
  return port;
}

async function scenario(shell, superAdmin, index) {
  const port = await freePort();
  const username = 'compat_' + runId + '_' + index;
  const password = 'Compat!9aA' + crypto.randomBytes(12).toString('hex');
  const server = spawn(shell, ['-NoProfile', '-ExecutionPolicy', 'RemoteSigned', '-File', wrapper], {
    cwd: root, windowsHide: true,
    env: {
      ...process.env, LAUNCHER_TEST_ROOT: root, LAUNCHER_TEST_PORT: String(port),
      LAUNCHER_TEST_USER: username, LAUNCHER_TEST_PASSWORD: password,
      LAUNCHER_TEST_DATABASE: path.join(output, 'market-' + index),
      LAUNCHER_TEST_SUPER: String(superAdmin)
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  let log = '';
  server.stdout.on('data', data => { log += data.toString(); });
  server.stderr.on('data', data => { log += data.toString(); });
  const exited = new Promise(resolve => server.once('close', resolve));
  const base = 'http://127.0.0.1:' + port;
  try {
    let ready = false;
    for (let attempt = 0; attempt < 120; attempt++) {
      if (server.exitCode !== null) throw Error('Launcher exited early: ' + log);
      try {
        const response = await fetch(base, { signal: AbortSignal.timeout(1500) });
        if (response.ok) { ready = true; break; }
      } catch {}
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    assert(ready, 'Application failed to start: ' + log);
    const login = await fetch(base + '/login');
    const html = await login.text();
    const csrf = html.match(/name="_csrf"[^>]*value="([^"]+)"/);
    assert(csrf, 'Missing CSRF token');
    const cookies = login.headers.getSetCookie().map(value => value.split(';')[0]).join('; ');
    const result = await fetch(base + '/login', {
      method: 'POST', redirect: 'manual',
      headers: { Cookie: cookies, 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username, password, _csrf: csrf[1] })
    });
    assert.equal(result.status, 302);
    assert.equal(new URL(result.headers.get('location'), base).pathname, '/');
    const evidence = { shell: path.basename(shell), setup: superAdmin ? 'super-admin' : 'admin', startup: true, login: true };
    console.log(JSON.stringify(evidence));
    return evidence;
  } finally {
    if (server.exitCode === null) {
      // Stop only the JVM started by this isolated test. The shell then runs its own finally block.
      const javaPid = log.match(/Starting MarketApplication.*with PID (\d+)/);
      if (javaPid) {
        try { process.kill(Number(javaPid[1])); } catch (error) { if (error.code !== 'ESRCH') throw error; }
      } else {
        server.kill();
      }
    }
    await Promise.race([
      exited,
      new Promise((_, reject) => setTimeout(() => reject(Error('Launcher cleanup timed out')), 10000))
    ]);
    fs.writeFileSync(path.join(output, 'application-' + index + '.log'), log);
  }
}

async function run() {
  const shells = ['powershell.exe'];
  if (process.env.POWERSHELL7_PATH) shells.push(process.env.POWERSHELL7_PATH);
  const evidence = [];
  for (const shell of shells) {
    for (const superAdmin of [false, true]) {
      evidence.push(await scenario(shell, superAdmin, evidence.length));
    }
  }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ passed: true, scenarios: evidence }, null, 2) + '\n');
}

run().catch(error => { console.error(error); process.exitCode = 1; });
