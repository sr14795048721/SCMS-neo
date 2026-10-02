"use client";

import { ManagerInfo } from "./types";

type AvatarCacheRecord = {
  avatarUpdatedAt: string | null;
  dataUrl: string;
};

const STORAGE_PREFIX = "scms.manager.avatar";
const THUMBNAIL_SIZE = 160;

export function readCachedManagerAvatar(profile: Pick<ManagerInfo, "userId" | "avatarUpdatedAt">): string | null {
  if (typeof window === "undefined" || !profile.userId) {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(resolveStorageKey(profile.userId));
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as AvatarCacheRecord;
    if (!parsed?.dataUrl || parsed.avatarUpdatedAt !== (profile.avatarUpdatedAt ?? null)) {
      return null;
    }

    return parsed.dataUrl;
  } catch {
    return null;
  }
}

export async function writeCachedManagerAvatar(
  profile: Pick<ManagerInfo, "userId" | "avatarUpdatedAt">,
  blob: Blob
): Promise<string> {
  const dataUrl = await createAvatarThumbnailDataUrl(blob);

  if (typeof window !== "undefined" && profile.userId) {
    try {
      const payload: AvatarCacheRecord = {
        avatarUpdatedAt: profile.avatarUpdatedAt ?? null,
        dataUrl
      };
      window.localStorage.setItem(resolveStorageKey(profile.userId), JSON.stringify(payload));
    } catch {
      // Ignore quota/storage errors and still return the generated thumbnail.
    }
  }

  return dataUrl;
}

export function clearCachedManagerAvatar(userId: number): void {
  if (typeof window === "undefined" || !userId) {
    return;
  }

  try {
    window.localStorage.removeItem(resolveStorageKey(userId));
  } catch {
    // Ignore storage errors.
  }
}

async function createAvatarThumbnailDataUrl(blob: Blob): Promise<string> {
  const image = await loadImageFromBlob(blob);
  const canvas = document.createElement("canvas");
  canvas.width = THUMBNAIL_SIZE;
  canvas.height = THUMBNAIL_SIZE;
  const context = canvas.getContext("2d");

  if (!context) {
    throw new Error("avatar thumbnail context unavailable");
  }

  context.clearRect(0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
  context.drawImage(image, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
  return canvas.toDataURL("image/png");
}

function loadImageFromBlob(blob: Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(blob);
    const image = new Image();

    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("avatar image load failed"));
    };
    image.src = objectUrl;
  });
}

function resolveStorageKey(userId: number): string {
  return `${STORAGE_PREFIX}.${userId}`;
}
