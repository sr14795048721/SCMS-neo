import fs from "node:fs";
import path from "node:path";
import { TextDecoder } from "node:util";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ignoredDirs = new Set([".next", "node_modules", ".git", "dist", "build", "coverage"]);
const textExtensions = new Set([
  ".ts",
  ".tsx",
  ".js",
  ".jsx",
  ".mjs",
  ".cjs",
  ".json",
  ".css",
  ".scss",
  ".md",
  ".txt",
  ".yml",
  ".yaml"
]);
const includeByName = new Set([".editorconfig", "AGENTS.md"]);
const decoder = new TextDecoder("utf-8", { fatal: true });
const errors = [];

walk(projectRoot);

if (errors.length > 0) {
  console.error("UTF-8 check failed:");
  for (const line of errors) {
    console.error(`- ${line}`);
  }
  process.exit(1);
}

console.log("UTF-8 check passed.");

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.name.startsWith(".DS_Store")) {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (ignoredDirs.has(entry.name)) {
        continue;
      }
      walk(fullPath);
      continue;
    }
    if (!entry.isFile()) {
      continue;
    }
    if (!shouldCheck(fullPath)) {
      continue;
    }
    verifyUtf8(fullPath);
  }
}

function shouldCheck(filePath) {
  const baseName = path.basename(filePath);
  if (includeByName.has(baseName)) {
    return true;
  }
  return textExtensions.has(path.extname(filePath).toLowerCase());
}

function verifyUtf8(filePath) {
  const buffer = fs.readFileSync(filePath);
  if (buffer.length === 0) {
    return;
  }
  if (hasUtf8Bom(buffer)) {
    errors.push(`${relativePath(filePath)} contains UTF-8 BOM`);
    return;
  }
  if (looksBinary(buffer)) {
    return;
  }
  let content = "";
  try {
    content = decoder.decode(buffer);
  } catch {
    errors.push(`${relativePath(filePath)} is not valid UTF-8`);
    return;
  }

  if (content.includes("\uFFFD")) {
    errors.push(`${relativePath(filePath)} contains replacement characters`);
  }

  if (hasCommonMojibake(content)) {
    errors.push(`${relativePath(filePath)} contains mojibake-like text`);
  }
}

function looksBinary(buffer) {
  const sample = buffer.subarray(0, Math.min(buffer.length, 1024));
  return sample.includes(0);
}

function hasUtf8Bom(buffer) {
  return buffer.length >= 3 && buffer[0] === 0xef && buffer[1] === 0xbb && buffer[2] === 0xbf;
}

function hasCommonMojibake(content) {
  const patterns = [/\u951f/];
  return patterns.some((pattern) => pattern.test(content));
}

function relativePath(filePath) {
  return path.relative(projectRoot, filePath).replace(/\\/g, "/");
}
