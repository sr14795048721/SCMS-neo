import { StudentGrade, STUDENT_GRADE_VALUES } from "./types";

export const MAX_STUDENT_CLASS_DIGITS = 2;
const MAX_STUDENT_CLASS_VALUE = 30;

const LEGACY_GRADE_HIGH_1 = "\u9ad8\u4e00";
const LEGACY_GRADE_HIGH_2 = "\u9ad8\u4e8c";
const LEGACY_GRADE_HIGH_3 = "\u9ad8\u4e09";
const CLASS_SUFFIX = "\u73ed";

const LEGACY_GRADE_MAP: Record<string, StudentGrade> = {
  [LEGACY_GRADE_HIGH_1]: "HIGH_1",
  [LEGACY_GRADE_HIGH_2]: "HIGH_2",
  [LEGACY_GRADE_HIGH_3]: "HIGH_3"
};

const STUDENT_GRADE_SET = new Set<string>(STUDENT_GRADE_VALUES);

export function normalizeStudentGrade(value: unknown): StudentGrade | "" {
  const normalized = String(value ?? "").trim();
  if (!normalized) {
    return "";
  }

  if (STUDENT_GRADE_SET.has(normalized)) {
    return normalized as StudentGrade;
  }

  return LEGACY_GRADE_MAP[normalized] || "";
}

export function sanitizeStudentClassNameInput(value: unknown): string {
  const digitsOnly = String(value ?? "")
    .replace(/\D+/g, "")
    .slice(0, MAX_STUDENT_CLASS_DIGITS);
  const normalized = digitsOnly.replace(/^0+/, "");

  if (!normalized) {
    return "";
  }

  const numericValue = Number(normalized);
  if (!Number.isInteger(numericValue) || numericValue < 1 || numericValue > MAX_STUDENT_CLASS_VALUE) {
    return "";
  }

  return String(numericValue);
}

export function resolveStudentClassNameInput(value: unknown, previousValue: string): string {
  const digitsOnly = String(value ?? "")
    .replace(/\D+/g, "")
    .slice(0, MAX_STUDENT_CLASS_DIGITS);

  if (!digitsOnly) {
    return "";
  }

  const nextValue = sanitizeStudentClassNameInput(digitsOnly);
  return nextValue || previousValue;
}

export function normalizeStudentClassName(value: unknown): string {
  const normalized = String(value ?? "").trim();
  if (!normalized) {
    return "";
  }

  const directMatch = normalized.match(/^(\d{1,3})$/);
  if (directMatch) {
    return sanitizeStudentClassNameInput(directMatch[1]);
  }

  const legacyMatch = normalized.match(new RegExp(`^(\\d{1,3})${CLASS_SUFFIX}$`));
  return sanitizeStudentClassNameInput(legacyMatch ? legacyMatch[1] : "");
}

export function formatStudentClassName(value: unknown): string {
  const normalized = normalizeStudentClassName(value);
  return normalized ? `${normalized}${CLASS_SUFFIX}` : "";
}
