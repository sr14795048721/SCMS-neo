"use client";

import { normalizeStudentClassName, normalizeStudentGrade } from "./fieldConstraints";
import { StudentInfo } from "./types";

export const STUDENT_PROFILE_REQUIRED_FIELDS = ["displayName", "studentNo", "grade", "className"] as const;

export type StudentProfileRequiredField = (typeof STUDENT_PROFILE_REQUIRED_FIELDS)[number];

export function resolveMissingStudentProfileFields(
  profile: Partial<StudentInfo> | null | undefined
): StudentProfileRequiredField[] {
  const missingFields: StudentProfileRequiredField[] = [];

  if (!String(profile?.displayName || "").trim()) {
    missingFields.push("displayName");
  }
  if (!String(profile?.studentNo || "").trim()) {
    missingFields.push("studentNo");
  }
  if (!normalizeStudentGrade(profile?.grade)) {
    missingFields.push("grade");
  }
  if (!normalizeStudentClassName(profile?.className)) {
    missingFields.push("className");
  }

  return missingFields;
}

export function isStudentProfileCompleted(profile: Partial<StudentInfo> | null | undefined): boolean {
  return resolveMissingStudentProfileFields(profile).length === 0;
}
