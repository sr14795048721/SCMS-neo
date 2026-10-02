"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { fetchAdminNewsCoverObjectUrl, revokeObjectUrl } from "../../lib/news/adminMedia";
import { formatNewsDateTime } from "../../lib/news/date";
import {
  readAdminNewsPreview,
  readAdminNewsPreviewFromOpener,
  removeAdminNewsPreview,
  removeAdminNewsPreviewFromOpener,
  saveAdminNewsPreview
} from "../../lib/news/previewStorage";
import { AdminNewsPreviewPayload } from "../../lib/news/types";
import { MarkdownArticle } from "../news/MarkdownArticle";
import styles from "../../styles/newsDetailPage.module.css";

type PreviewState = "loading" | "ready" | "expired";
const PREVIEW_READ_RETRY_MS = 180;
const PREVIEW_READ_MAX_ATTEMPTS = 12;

export function SysAdminNewsPreviewPage() {
  const t = useT("adminNews");
  const tPortal = useT("portal");
  const router = useRouter();
  const params = useParams<{ previewId: string }>();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [state, setState] = useState<PreviewState>("loading");
  const [payload, setPayload] = useState<AdminNewsPreviewPayload | null>(null);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null);
  const coverPreviewUrlRef = useRef<string | null>(null);

  const previewId = useMemo(() => String(params.previewId || ""), [params.previewId]);

  useEffect(() => {
    if (!authorized || !previewId) {
      return;
    }

    let cancelled = false;
    let attempts = 0;

    const resolvePreview = () => {
      const preview = readAdminNewsPreview(previewId) ?? readAdminNewsPreviewFromOpener(previewId);
      if (preview) {
        saveAdminNewsPreview(preview);
        if (!cancelled) {
          setPayload(preview);
          setState("ready");
        }
        return;
      }

      attempts += 1;
      if (attempts >= PREVIEW_READ_MAX_ATTEMPTS) {
        if (!cancelled) {
          setState("expired");
        }
        return;
      }

      window.setTimeout(resolvePreview, PREVIEW_READ_RETRY_MS);
    };

    resolvePreview();

    return () => {
      cancelled = true;
    };
  }, [authorized, previewId]);

  useEffect(() => {
    if (!payload?.newsId || !payload.cover) {
      clearCoverPreview();
      return;
    }

    const currentPayload = payload;
    let disposed = false;

    async function loadCoverPreview() {
      clearCoverPreview();
      const objectUrl = await fetchAdminNewsCoverObjectUrl(
        currentPayload.newsId,
        currentPayload.coverUpdatedAt || currentPayload.updatedAt
      );
      if (!objectUrl || disposed) {
        if (objectUrl) {
          revokeObjectUrl(objectUrl);
        }
        return;
      }

      coverPreviewUrlRef.current = objectUrl;
      setCoverPreviewUrl(objectUrl);
    }

    void loadCoverPreview();

    return () => {
      disposed = true;
      clearCoverPreview();
    };
  }, [payload]);

  useEffect(() => {
    if (!previewId) {
      return;
    }

    const cleanup = () => {
      removeAdminNewsPreview(previewId);
      removeAdminNewsPreviewFromOpener(previewId);
    };

    window.addEventListener("beforeunload", cleanup);
    window.addEventListener("pagehide", cleanup);

    return () => {
      window.removeEventListener("beforeunload", cleanup);
      window.removeEventListener("pagehide", cleanup);
    };
  }, [previewId]);
  if (checking || state === "loading") {
    return (
      <main className={styles.page}>
        <div className={styles.container}>
          <div className={styles.loading}>
            <i className="fas fa-spinner fa-spin" />
            <p>{checking ? tPortal("auth.checking") : t("preview.loading")}</p>
          </div>
        </div>
      </main>
    );
  }

  if (!authorized || redirecting) {
    return null;
  }

  return (
    <main className={styles.page}>
      <div className={styles.topBar}>
        <div className={styles.topInner}>
          <button type="button" className={styles.backButton} onClick={() => window.close()}>
            <i className="fas fa-xmark" /> {t("preview.close")}
          </button>
          <button type="button" className={styles.backButton} onClick={() => router.push("/sys-admin/news-manage")}>
            <i className="fas fa-arrow-left" /> {t("actions.back")}
          </button>
        </div>
      </div>

      <div className={styles.container}>
        {state === "expired" || !payload ? (
          <div className={styles.empty}>
            <i className="fas fa-link-slash" />
            <p>{t("preview.expired")}</p>
          </div>
        ) : (
          <article className={styles.article}>
            <h1>{payload.title || t("list.untitled")}</h1>
            <div className={styles.meta}>
              <span>
                <i className="fas fa-user" /> {payload.authorName || t("list.authorFallback")}
              </span>
              <span>
                <i className="fas fa-calendar-day" /> {formatNewsDateTime(payload.createdAt)}
              </span>
              <span>
                <i className="fas fa-eye" /> {t("preview.previewBadge")}
              </span>
            </div>
            {coverPreviewUrl ? <img src={coverPreviewUrl} alt={payload.title} className={styles.cover} /> : null}
            <div className={styles.content}>
              <MarkdownArticle content={payload.markdownContent || t("editor.previewEmpty")} mode="admin" />
            </div>
          </article>
        )}
      </div>
    </main>
  );

  function clearCoverPreview() {
    if (coverPreviewUrlRef.current) {
      revokeObjectUrl(coverPreviewUrlRef.current);
      coverPreviewUrlRef.current = null;
    }
    setCoverPreviewUrl(null);
  }
}
