const http = require('http');
const { spawn, execSync } = require('child_process');

const BASE_URL = process.env.BASE_URL || 'http://localhost:30080';
let pfProcess = null;

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE_URL);
    const reqOptions = {
      hostname: url.hostname,
      port: url.port || 80,
      path: url.pathname + url.search,
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    };

    const req = http.request(reqOptions, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try {
          json = JSON.parse(data);
        } catch (_) {
          json = data;
        }
        resolve({ status: res.statusCode, headers: res.headers, data: json, text: data });
      });
    });

    req.on('error', reject);

    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function ensureConnectivity() {
  try {
    await request('/');
    return;
  } catch (_) {
    console.log('[SmokeTest] Initial direct connection to port 30080 failed. Starting kubectl port-forward svc/frontend 30080:80...');
    pfProcess = spawn('kubectl', ['port-forward', 'svc/frontend', '30080:80', '-n', 'cloud-ide'], {
      detached: false,
      stdio: 'ignore'
    });
    await sleep(2500);
  }
}

async function runSmokeTests() {
  console.log('============================================================');
  console.log(' CLOUD IDE — KUBERNETES DEPLOYMENT SMOKE & HEALTH TESTS');
  console.log(` Target Base URL: ${BASE_URL}`);
  console.log('============================================================\n');

  await ensureConnectivity();

  let passed = 0;
  let failed = 0;

  // 1. Frontend Web Service
  try {
      const res = await request('/');
      if (res.status === 200 && (res.text.includes('<html') || res.text.includes('<!doctype html>'))) {
        console.log('✓ [1/8] Frontend UI Accessibility: PASS (HTTP 200 OK)');
        passed++;
      } else {
        throw new Error(`Unexpected status ${res.status}`);
      }
    } catch (err) {
      console.error('✗ [1/8] Frontend UI Accessibility: FAIL', err.message);
      failed++;
    }

  // 2. Backend Health Endpoint
  try {
    const res = await request('/api/health');
    if (res.status === 200 && res.data && res.data.status === 'UP') {
      console.log('✓ [2/8] Backend Service Health: PASS (Status: UP)');
      passed++;
    } else {
      throw new Error(`Health check returned: ${JSON.stringify(res.data)}`);
    }
  } catch (err) {
    console.error('✗ [2/8] Backend Service Health: FAIL', err.message);
    failed++;
  }

  // 3. User Registration & JWT Authentication
  let token = null;
  const testEmail = `ci_smoke_${Date.now()}@example.com`;
  try {
    const regRes = await request('/api/auth/register', {
      method: 'POST',
      body: { name: 'CI Smoke Tester', email: testEmail, password: 'Password123!' }
    });
    if (regRes.status !== 200 && regRes.status !== 201) {
      throw new Error(`Registration failed with status ${regRes.status}`);
    }

    const loginRes = await request('/api/auth/login', {
      method: 'POST',
      body: { email: testEmail, password: 'Password123!' }
    });
    if (loginRes.status === 200 && loginRes.data && loginRes.data.token) {
      token = loginRes.data.token;
      console.log('✓ [3/8] User Registration & JWT Auth: PASS (Token acquired)');
      passed++;
    } else {
      throw new Error(`Login failed with status ${loginRes.status}`);
    }
  } catch (err) {
    console.error('✗ [3/8] User Registration & JWT Auth: FAIL', err.message);
    failed++;
  }

  // 4. Project CRUD Operations
  let projectId = null;
  if (token) {
    try {
      const createRes = await request('/api/projects', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: { name: 'Smoke Test Project', description: 'Automated CI test project', language: 'PYTHON' }
      });
      if (createRes.status === 200 || createRes.status === 201) {
        projectId = createRes.data.id;
        const listRes = await request('/api/projects', {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (listRes.status === 200 && Array.isArray(listRes.data) && listRes.data.some(p => p.id === projectId)) {
          console.log(`✓ [4/8] Project CRUD Operations: PASS (Created project id: ${projectId})`);
          passed++;
        } else {
          throw new Error('Project not found in project listing');
        }
      } else {
        throw new Error(`Project creation failed with status ${createRes.status}`);
      }
    } catch (err) {
      console.error('✗ [4/8] Project CRUD Operations: FAIL', err.message);
      failed++;
    }
  } else {
    console.error('✗ [4/8] Project CRUD Operations: SKIPPED (No auth token)');
    failed++;
  }

  // 5. Execution Worker Connectivity & Code Execution
  if (token) {
    try {
      const execRes = await request('/api/execute', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: {
          language: 'PYTHON',
          code: 'print("CI Smoke Test Success")',
          stdin: ''
        }
      });
      const execId = execRes.data && (execRes.data.executionId || execRes.data.id);
      if (execRes.status === 200 && execId) {
        let finalStatus = null;
        let attempts = 0;
        while (attempts < 20) {
          await sleep(500);
          attempts++;
          const poll = await request(`/api/executions/${execId}`, {
            headers: { Authorization: `Bearer ${token}` }
          });
          if (poll.status === 200 && poll.data) {
            if (['SUCCESS', 'FAILED', 'TIMED_OUT', 'STOPPED', 'COMPILATION_ERROR', 'RUNTIME_ERROR'].includes(poll.data.status)) {
              finalStatus = poll.data;
              break;
            }
          }
        }
        if (finalStatus && finalStatus.status === 'SUCCESS' && (finalStatus.stdout || '').includes('CI Smoke Test Success')) {
          console.log('✓ [5/8] Code Execution & Worker Connectivity: PASS (Status: SUCCESS)');
          passed++;
        } else {
          throw new Error(`Execution did not succeed: ${JSON.stringify(finalStatus)}`);
        }
      } else {
        throw new Error(`Execution submission returned status ${execRes.status}`);
      }
    } catch (err) {
      console.error('✗ [5/8] Code Execution & Worker Connectivity: FAIL', err.message);
      failed++;
    }
  } else {
    console.error('✗ [5/8] Code Execution & Worker Connectivity: SKIPPED (No auth token)');
    failed++;
  }

  // 6. Prometheus Target Health
  try {
    const promOut = execSync('kubectl exec deployment/prometheus -n cloud-ide -c prometheus -- wget -qO- http://127.0.0.1:9090/api/v1/targets', { encoding: 'utf8' });
    const promJson = JSON.parse(promOut);
    const backendTarget = (promJson.data && promJson.data.activeTargets || []).find(t => t.labels && t.labels.job === 'cloud-ide-backend');
    if (backendTarget && backendTarget.health === 'up') {
      console.log('✓ [6/8] Prometheus Observability: PASS (Target backend:8088 is UP)');
      passed++;
    } else {
      throw new Error(`Prometheus target backend not up: ${JSON.stringify(backendTarget)}`);
    }
  } catch (err) {
    console.error('✗ [6/8] Prometheus Observability: FAIL', err.message);
    failed++;
  }

  // 7. Loki Ingestion Readiness
  try {
    const lokiOut = execSync('kubectl exec deployment/loki -n cloud-ide -c loki -- wget -qO- http://127.0.0.1:3100/ready', { encoding: 'utf8' });
    if (lokiOut.trim() === 'ready') {
      console.log('✓ [7/8] Loki Logging Service: PASS (/ready returned ready)');
      passed++;
    } else {
      throw new Error(`Loki returned: ${lokiOut}`);
    }
  } catch (err) {
    console.error('✗ [7/8] Loki Logging Service: FAIL', err.message);
    failed++;
  }

  // 8. Grafana Dashboard Readiness
  try {
    const grafOut = execSync('kubectl exec deployment/grafana -n cloud-ide -c grafana -- wget -qO- http://127.0.0.1:3000/api/health', { encoding: 'utf8' });
    const grafJson = JSON.parse(grafOut);
    if (grafJson.database === 'ok') {
      console.log('✓ [8/8] Grafana Dashboard Service: PASS (database: ok)');
      passed++;
    } else {
      throw new Error(`Grafana returned: ${grafOut}`);
    }
  } catch (err) {
    console.error('✗ [8/8] Grafana Dashboard Service: FAIL', err.message);
    failed++;
  } finally {
    if (pfProcess) {
      try {
        pfProcess.kill();
      } catch (_) {}
    }
  }

  console.log('\n============================================================');
  console.log(` SMOKE TEST SUMMARY: ${passed}/8 PASSED, ${failed} FAILED`);
  console.log('============================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runSmokeTests().catch((err) => {
  if (pfProcess) {
    try { pfProcess.kill(); } catch (_) {}
  }
  console.error('Fatal test error:', err);
  process.exit(1);
});
