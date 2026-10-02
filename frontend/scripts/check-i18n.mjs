import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const scanDirs = ["app", "components", "lib"];
const codeExtensions = new Set([".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs"]);
const chinesePattern = /[\u3400-\u9fff]/;
const violations = [];

for (const directory of scanDirs) {
  const target = path.join(projectRoot, directory);
  if (fs.existsSync(target)) {
    walk(target);
  }
}

if (violations.length > 0) {
  console.error("i18n check failed. Hardcoded Chinese text found:");
  for (const item of violations) {
    console.error(`- ${item}`);
  }
  process.exit(1);
}

console.log("i18n check passed.");

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath);
      continue;
    }
    if (!entry.isFile()) {
      continue;
    }
    if (!codeExtensions.has(path.extname(entry.name).toLowerCase())) {
      continue;
    }
    checkFile(fullPath);
  }
}

function checkFile(filePath) {
  const content = fs.readFileSync(filePath, "utf8");
  if (!chinesePattern.test(content)) {
    return;
  }

  const lines = content.split(/\r?\n/);
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    if (!chinesePattern.test(line)) {
      continue;
    }
    const trimmed = line.trim();
    if (trimmed.startsWith("// i18n-allow")) {
      continue;
    }
    violations.push(`${relativePath(filePath)}:${i + 1} ${trimmed}`);
  }
}

function relativePath(filePath) {
  return path.relative(projectRoot, filePath).replace(/\\/g, "/");
}

