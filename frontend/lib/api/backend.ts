export const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";

export function resolveBackendBaseUrl(): string {
  return process.env.BACKEND_BASE_URL || DEFAULT_BACKEND_BASE_URL;
}

export function resolveCoreServiceBaseUrl(): string {
  return resolveBackendBaseUrl();
}

export async function parseJsonSafely<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export function createForwardHeaders(
  contentType: string,
  authorization?: string | null
): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": contentType
  };

  const bearer = String(authorization || "").trim();
  if (bearer) {
    headers.Authorization = bearer;
  }

  return headers;
}

export function createProxyHeaders(
  authorization?: string | null,
  contentType?: string | null
): Record<string, string> {
  const headers: Record<string, string> = {};
  const bearer = String(authorization || "").trim();
  const normalizedContentType = String(contentType || "").trim();

  if (normalizedContentType) {
    headers["Content-Type"] = normalizedContentType;
  }
  if (bearer) {
    headers.Authorization = bearer;
  }

  return headers;
}

export function resolveGatewayAssetUrl(path?: string | null): string | undefined {
  const value = String(path || "").trim();
  if (!value) {
    return undefined;
  }
  if (/^https?:\/\//i.test(value)) {
    return value;
  }

  const backendBase = resolveBackendBaseUrl().replace(/\/$/, "");
  return `${backendBase}${value.startsWith("/") ? value : `/${value}`}`;
}

export async function fetchWithCoreFallback(
  path: string,
  init: RequestInit
): Promise<Response> {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const candidates = Array.from(
    new Set([resolveBackendBaseUrl(), resolveCoreServiceBaseUrl()].map((base) => base.replace(/\/$/, "")))
  );

  let lastError: unknown;

  for (const baseUrl of candidates) {
    try {
      return await fetch(`${baseUrl}${normalizedPath}`, init);
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError instanceof Error ? lastError : new Error("backend unavailable");
}
