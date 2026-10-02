"use client";

import { ManagerInfo } from "./types";

const STORAGE_PREFIX = "scms.manager.profile";

export function readCachedManagerProfile(): ManagerInfo | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_PREFIX);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as Partial<ManagerInfo>;
    if (!parsed?.userId || !parsed.username) {
      return null;
    }

    return normalizeManagerInfo(parsed);
  } catch {
    return null;
  }
}

export function writeCachedManagerProfile(profile: ManagerInfo): void {
  if (typeof window === "undefined" || !profile.userId) {
    return;
  }

  try {
    window.localStorage.setItem(STORAGE_PREFIX, JSON.stringify(normalizeManagerInfo(profile)));
  } catch {
    // Ignore storage errors.
  }
}

export function clearCachedManagerProfile(): void {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.removeItem(STORAGE_PREFIX);
  } catch {
    // Ignore storage errors.
  }
}

function normalizeManagerInfo(profile: Partial<ManagerInfo>): ManagerInfo {
  return {
    userId: Number(profile.userId || 0),
    username: String(profile.username || ""),
    role: String(profile.role || ""),
    email: String(profile.email || ""),
    displayName: String(profile.displayName || ""),
    managerNo: String(profile.managerNo || ""),
    phone: String(profile.phone || ""),
    bio: String(profile.bio || ""),
    hasAvatar: Boolean(profile.hasAvatar),
    avatarUpdatedAt: profile.avatarUpdatedAt ? String(profile.avatarUpdatedAt) : null
  };
}
