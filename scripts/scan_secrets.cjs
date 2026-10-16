/**
 * Cloud IDE - Repository Secret Scanner
 * Phase 11.3: Automated Security Gate
 *
 * Scans tracked repository files for accidental secrets, credentials, and API keys.
 * Rules:
 *  - Masks all findings so secret values are never printed to logs.
 *  - Ignores .git, node_modules, dist, target, documentation examples, and .example files.
 *  - Fails (exit code 1) if any real secret pattern is matched.
 */

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');

// Regex rules for sensitive patterns
const RULES = [
  {
    name: 'AWS Access Key ID',
    regex: /(?:A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}/g,
  },
  {
    name: 'AWS Secret Access Key',
    regex: /(?:aws_secret_access_key|aws_secret_key)\s*[:=]\s*["']?([A-Za-z0-9/+=]{40})["']?/gi,
  },
  {
    name: 'Private Key Block',
    regex: /-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----/g,
  },
  {
    name: 'Hardcoded JWT Secret',
    regex: /JWT_SECRET\s*=\s*["']?([a-zA-Z0-9+/=_-]{32,})["']?/gi,
    excludePlaceholders: ['your-jwt-secret-key-at-least-256-bits-long', 'your-jwt-secret', 'replace-with-a-long-random-secret'],
  },
  {
    name: 'Hardcoded Execution Worker Secret',
    regex: /EXECUTION_WORKER_SECRET\s*=\s*["']?([a-fA-F0-9]{32,})["']?/gi,
    excludePlaceholders: ['replace-with-a-long-random-secret', 'test_worker_secret_12345'],
  },
  {
    name: 'Hardcoded Database Password',
    regex: /(?:MYSQL_PASSWORD|MYSQL_ROOT_PASSWORD)\s*=\s*["']?([a-zA-Z0-9_!@#$%^&*]{6,})["']?/gi,
    excludePlaceholders: ['cloudide_secret', 'root_secret', 'your-db-password', 'change-me'],
  },
  {
    name: 'Generic Private Token / API Key',
    regex: /(?:api_key|apikey|secret_key|auth_token)\s*[:=]\s*["']([A-Za-z0-9_-]{32,})["']/gi,
    excludePlaceholders: ['placeholder', 'example', 'dummy', 'change-me'],
  }
];

// Directories & files to ignore during secret scan
const IGNORE_DIRS = new Set([
  '.git',
  'node_modules',
  'dist',
  'target',
  '.vscode',
  '.gemini',
]);

const IGNORE_FILES = new Set([
  '.env',
  '.env.local',
  '.env.production',
  'package-lock.json',
  'yarn.lock',
]);

const IGNORE_EXTENSIONS = new Set([
  '.png', '.jpg', '.jpeg', '.gif', '.ico', '.svg', '.woff', '.woff2', '.ttf', '.eot',
  '.jar', '.war', '.class', '.zip', '.tar', '.gz', '.log'
]);

// Exclude template/example files explicitly allowed to have placeholders
function isExampleFile(filePath) {
  const base = path.basename(filePath);
  return base.includes('.example') || 
         base.endsWith('-example.yaml') || 
         base.endsWith('.example.yaml') ||
         filePath.includes('k8s' + path.sep + 'secret.example.yaml') ||
         filePath.includes('docs' + path.sep) ||
         filePath.includes('src' + path.sep + 'test' + path.sep);
}

function maskSecret(val) {
  if (!val || val.length <= 4) return '****';
  return val.substring(0, 4) + '*'.repeat(Math.min(val.length - 4, 16));
}

function scanFile(filePath, relativePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const lines = content.split(/\r?\n/);
  const findings = [];

  for (let lineNum = 1; lineNum <= lines.length; lineNum++) {
    const line = lines[lineNum - 1];

    for (const rule of RULES) {
      rule.regex.lastIndex = 0;
      let match;
      while ((match = rule.regex.exec(line)) !== null) {
        const fullMatch = match[0];
        const secretVal = match[1] || match[0];

        // Check if placeholder is exempted
        if (rule.excludePlaceholders) {
          const isPlaceholder = rule.excludePlaceholders.some(p => 
            fullMatch.toLowerCase().includes(p.toLowerCase()) || 
            secretVal.toLowerCase().includes(p.toLowerCase())
          );
          if (isPlaceholder) continue;
        }

        // Check if file is an example / documentation file
        if (isExampleFile(filePath)) {
          continue;
        }

        findings.push({
          rule: rule.name,
          line: lineNum,
          file: relativePath,
          preview: maskSecret(secretVal)
        });
      }
    }
  }

  return findings;
}

function walkDir(dir, fileList = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    const relPath = path.relative(ROOT_DIR, fullPath);

    if (entry.isDirectory()) {
      if (!IGNORE_DIRS.has(entry.name)) {
        walkDir(fullPath, fileList);
      }
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name).toLowerCase();
      if (!IGNORE_FILES.has(entry.name) && !IGNORE_EXTENSIONS.has(ext)) {
        fileList.push({ fullPath, relPath });
      }
    }
  }
  return fileList;
}

console.log('============================================================');
console.log(' Cloud IDE Phase 11.3: Repository Secret Scanning Gate');
console.log('============================================================');

const filesToScan = walkDir(ROOT_DIR);
console.log(`Scanning ${filesToScan.length} tracked/workspace files across repository...`);

let totalFindings = 0;
const allFindings = [];

for (const { fullPath, relPath } of filesToScan) {
  try {
    const findings = scanFile(fullPath, relPath);
    if (findings.length > 0) {
      allFindings.push(...findings);
      totalFindings += findings.length;
    }
  } catch (err) {
    console.error(`[WARN] Could not scan ${relPath}: ${err.message}`);
  }
}

if (totalFindings > 0) {
  console.error('\n[SECURITY ALERT] Potential credentials/secrets detected:');
  for (const f of allFindings) {
    console.error(`  - [${f.rule}] in ${f.file}:${f.line} -> Pattern: ${f.preview}`);
  }
  console.error(`\nFailed: ${totalFindings} secret finding(s) detected. Pipeline halted.`);
  process.exit(1);
} else {
  console.log('[OK] Secret scan completed: 0 credentials or secrets found in tracked sources.');
  console.log('============================================================\n');
  process.exit(0);
}
