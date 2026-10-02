"use client";

import { normalizeStudentClassName, normalizeStudentGrade } from "./fieldConstraints";
import { isStudentProfileCompleted, resolveMissingStudentProfileFields } from "./profileCompletion";
import { StudentInfo } from "./types";

const STORAGE_PREFIX = "scms.student.profile";

export function readCachedStudentProfile(): StudentInfo | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_PREFIX);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as Partial<StudentInfo>;
    if (!parsed?.userId || !parsed.username) {
      return null;
    }

    return normalizeStudentInfo(parsed);
  } catch {
    return null;
  }
}

export function writeCachedStudentProfile(profile: StudentInfo): void {
  if (typeof window === "undefined" || !profile.userId) {
    return;
  }

  try {
    window.localStorage.setItem(STORAGE_PREFIX, JSON.stringify(normalizeStudentInfo(profile)));
  } catch {
    // Ignore storage errors.
  }
}

export function clearCachedStudentProfile(): void {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.removeItem(STORAGE_PREFIX);
  } catch {
    // Ignore storage errors.
  }
}

function normalizeStudentInfo(profile: Partial<StudentInfo>): StudentInfo {
  const normalizedProfile = {
    userId: Number(profile.userId || 0),
    username: String(profile.username || ""),
    role: String(profile.role || ""),
    email: String(profile.email || ""),
    displayName: String(profile.displayName || ""),
    studentNo: String(profile.studentNo || ""),
    grade: normalizeStudentGrade(profile.grade),
    className: normalizeStudentClassName(profile.className),
    phone: String(profile.phone || ""),
    bio: String(profile.bio || ""),
    hasAvatar: Boolean(profile.hasAvatar),
    avatarUpdatedAt: profile.avatarUpdatedAt ? String(profile.avatarUpdatedAt) : null
  };
  const missingRequiredFields = Array.isArray(profile.missingRequiredFields)
    ? profile.missingRequiredFields.map((field) => String(field || ""))
    : resolveMissingStudentProfileFields(normalizedProfile);

  return {
    ...normalizedProfile,
    profileCompleted:
      typeof profile.profileCompleted === "boolean"
        ? profile.profileCompleted
        : isStudentProfileCompleted(normalizedProfile),
    missingRequiredFields
  };
}
