import { authorizedFetch } from "../auth/client";

export async function fetchManagerNewsCoverObjectUrl(newsId: number, version?: string | null) {
  if (!newsId) {
    return null;
  }

  const suffix = version ? `?v=${encodeURIComponent(version)}` : "";

  try {
    const response = await authorizedFetch(`/api/manager/news/${newsId}/cover/content${suffix}`, {
      method: "GET",
      cache: "no-store"
    });

    if (!response?.ok) {
      return null;
    }

    const blob = await response.blob();
    return URL.createObjectURL(blob);
  } catch {
    return null;
  }
}
