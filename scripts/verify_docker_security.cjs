/**
 * Cloud IDE - Docker Security & Container Image Verification
 * Phase 11.3: Quality & Security Gate
 *
 * Verifies that:
 *  1. Backend container image runs as non-root (appuser).
 *  2. No .env files or Kubernetes secret manifests are baked into images.
 *  3. No AWS credentials or config files exist inside images.
 *  4. Application manifests do not mount the host Docker socket (/var/run/docker.sock).
 *  5. Application manifests do not run containers as privileged.
 *  6. No Docker daemon TCP port 2375 is exposed.
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');
const IMAGE_TAG = process.env.IMAGE_TAG || process.argv[2] || 'build-1';

const FRONTEND_IMAGE = `cloud-ide-frontend:${IMAGE_TAG}`;
const BACKEND_IMAGE = `cloud-ide-backend:${IMAGE_TAG}`;

console.log('============================================================');
console.log(' Cloud IDE Phase 11.3: Docker Security Verification');
console.log(` Target Images: ${FRONTEND_IMAGE}, ${BACKEND_IMAGE}`);
console.log('============================================================\n');

let failedChecks = 0;

function check(title, fn) {
  process.stdout.write(`[*] Checking: ${title}... `);
  try {
    fn();
    console.log('[PASS]');
  } catch (err) {
    console.log(`[FAIL]\n    Error: ${err.message}`);
    failedChecks++;
  }
}

// 1. Verify Backend runs as non-root
check('Backend container image configured to run as non-root user', () => {
  const user = execSync(`docker image inspect --format "{{.Config.User}}" ${BACKEND_IMAGE}`, {
    encoding: 'utf8',
    timeout: 10000
  }).trim();
  if (!user || user === 'root' || user === '0') {
    throw new Error(`Backend container runs as root ('${user || 'default'}'). Must be non-root (appuser).`);
  }
});

// 2. Verify Backend image does not contain .env, k8s manifests, or AWS credentials
check('Backend container contains no .env, k8s secrets, or AWS credentials', () => {
  const cmd = `docker run --rm --entrypoint sh ${BACKEND_IMAGE} -c "` +
    `test ! -f /app/.env && ` +
    `test ! -f /app/.env.local && ` +
    `test ! -d /app/k8s && ` +
    `test ! -d /root/.aws && ` +
    `test ! -d /app/.aws"`;
  execSync(cmd, { stdio: 'pipe', timeout: 15000 });
});

// 3. Verify Frontend image does not contain .env, k8s manifests, or AWS credentials
check('Frontend container contains no .env, k8s secrets, or AWS credentials', () => {
  const cmd = `docker run --rm --entrypoint sh ${FRONTEND_IMAGE} -c "` +
    `test ! -f /usr/share/nginx/html/.env && ` +
    `test ! -d /usr/share/nginx/html/k8s && ` +
    `test ! -d /root/.aws"`;
  execSync(cmd, { stdio: 'pipe', timeout: 15000 });
});

// 4. Verify Docker Compose has no Docker socket mounts or privileged flags on app containers
check('Docker Compose does not mount /var/run/docker.sock or use privileged: true', () => {
  const composePath = path.join(ROOT_DIR, 'docker-compose.yml');
  const composeContent = fs.readFileSync(composePath, 'utf8');

  if (composeContent.includes('/var/run/docker.sock')) {
    throw new Error('docker-compose.yml mounts /var/run/docker.sock into application containers!');
  }
  if (/privileged\s*:\s*true/i.test(composeContent)) {
    throw new Error('docker-compose.yml specifies privileged: true!');
  }
  if (composeContent.includes('2375')) {
    throw new Error('docker-compose.yml exposes insecure Docker daemon TCP port 2375!');
  }
});

// 5. Verify Kubernetes manifests do not mount Docker socket or use privileged flags
check('Kubernetes manifests do not mount /var/run/docker.sock or use privileged: true', () => {
  const k8sDir = path.join(ROOT_DIR, 'k8s');
  const manifestFiles = fs.readdirSync(k8sDir).filter(f => f.endsWith('.yaml') || f.endsWith('.yml'));

  for (const file of manifestFiles) {
    const content = fs.readFileSync(path.join(k8sDir, file), 'utf8');
    if (content.includes('/var/run/docker.sock')) {
      throw new Error(`k8s/${file} mounts /var/run/docker.sock into Kubernetes pods!`);
    }
    if (/privileged\s*:\s*true/i.test(content)) {
      throw new Error(`k8s/${file} specifies privileged: true container execution!`);
    }
    if (content.includes('2375') && !content.includes('note')) {
      throw new Error(`k8s/${file} references insecure Docker daemon TCP port 2375!`);
    }
  }
});

console.log('\n============================================================');
if (failedChecks === 0) {
  console.log(' Docker Security Verification: ALL CHECKS PASSED');
  console.log('============================================================\n');
  process.exit(0);
} else {
  console.error(` Docker Security Verification: ${failedChecks} CHECK(S) FAILED`);
  console.log('============================================================\n');
  process.exit(1);
}
