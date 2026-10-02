import { AdminNewsPreviewPayload } from "./types";

const PREVIEW_KEY_PREFIX = "scms.news.preview.";
const PREVIEW_TTL_MS = 2 * 60 * 60 * 1000;
const PREVIEW_REGISTRY_KEY = "__SCMS_ADMIN_NEWS_PREVIEWS__";

type StoredPreviewPayload = AdminNewsPreviewPayload & {
  expiresAt: string;
};

type PreviewWindowWithRegistry = Window &
  typeof globalThis & {
    [PREVIEW_REGISTRY_KEY]?: Record<string, AdminNewsPreviewPayload>;
  };

export function createAdminNewsPreviewId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `preview-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function saveAdminNewsPreview(payload: AdminNewsPreviewPayload) {
  if (typeof window === "undefined") {
    return;
  }

  const storedPayload: StoredPreviewPayload = {
    ...payload,
    expiresAt: new Date(Date.now() + PREVIEW_TTL_MS).toISOString()
  };
  window.localStorage.setItem(resolvePreviewStorageKey(payload.previewId), JSON.stringify(storedPayload));
  writePreviewToRegistry(window as PreviewWindowWithRegistry, payload);
}

export function readAdminNewsPreview(previewId: string): AdminNewsPreviewPayload | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(resolvePreviewStorageKey(previewId));
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as StoredPreviewPayload;
    const expiresAt = Date.parse(String(parsed?.expiresAt || ""));
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      removeAdminNewsPreview(previewId);
      return null;
    }

    return {
      previewId: String(parsed.previewId || ""),
      newsId: Number(parsed.newsId || 0),
      title: String(parsed.title || ""),
      markdownContent: String(parsed.markdownContent || ""),
      cover: parsed.cover ? String(parsed.cover) : undefined,
      coverUpdatedAt: parsed.coverUpdatedAt ? String(parsed.coverUpdatedAt) : null,
      updatedAt: parsed.updatedAt ? String(parsed.updatedAt) : null,
      authorName: String(parsed.authorName || ""),
      createdAt: String(parsed.createdAt || "")
    };
  } catch {
    removeAdminNewsPreview(previewId);
    return null;
  }
}

export function removeAdminNewsPreview(previewId: string) {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(resolvePreviewStorageKey(previewId));
  removePreviewFromRegistry(window as PreviewWindowWithRegistry, previewId);
}

export function readAdminNewsPreviewFromOpener(previewId: string): AdminNewsPreviewPayload | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const opener = window.opener as PreviewWindowWithRegistry | null;
    if (!opener) {
      return null;
    }

    return opener[PREVIEW_REGISTRY_KEY]?.[previewId] ?? null;
  } catch {
    return null;
  }
}

export function removeAdminNewsPreviewFromOpener(previewId: string) {
  if (typeof window === "undefined") {
    return;
  }

  try {
    const opener = window.opener as PreviewWindowWithRegistry | null;
    if (!opener) {
      return;
    }

    removePreviewFromRegistry(opener, previewId);
  } catch {
    // Ignore cross-window cleanup failures.
  }
}

export function cleanupExpiredAdminNewsPreviews() {
  if (typeof window === "undefined") {
    return;
  }

  const keysToRemove: string[] = [];
  for (let index = 0; index < window.localStorage.length; index += 1) {
    const key = window.localStorage.key(index);
    if (!key || !key.startsWith(PREVIEW_KEY_PREFIX)) {
      continue;
    }

    const previewId = key.slice(PREVIEW_KEY_PREFIX.length);
    const payload = readAdminNewsPreview(previewId);
    if (!payload) {
      keysToRemove.push(key);
    }
  }

  for (const key of keysToRemove) {
    window.localStorage.removeItem(key);
  }
}

function resolvePreviewStorageKey(previewId: string) {
  return `${PREVIEW_KEY_PREFIX}${previewId}`;
}

function writePreviewToRegistry(targetWindow: PreviewWindowWithRegistry, payload: AdminNewsPreviewPayload) {
  const registry = targetWindow[PREVIEW_REGISTRY_KEY] ?? {};
  registry[payload.previewId] = payload;
  targetWindow[PREVIEW_REGISTRY_KEY] = registry;
}

function removePreviewFromRegistry(targetWindow: PreviewWindowWithRegistry, previewId: string) {
  const registry = targetWindow[PREVIEW_REGISTRY_KEY];
  if (!registry) {
    return;
  }

  delete registry[previewId];
  if (Object.keys(registry).length === 0) {
    delete targetWindow[PREVIEW_REGISTRY_KEY];
  }
}
