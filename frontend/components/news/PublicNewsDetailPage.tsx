"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useT } from "../../lib/i18n/useT";
import { formatNewsDateTime } from "../../lib/news/date";
import { PublicNewsDetail } from "../../lib/news/types";
import { MarkdownArticle } from "./MarkdownArticle";
import styles from "../../styles/newsDetailPage.module.css";

type LoadState = "loading" | "ready" | "error" | "notFound";

export function PublicNewsDetailPage() {
  const t = useT("news");
  const router = useRouter();
  const params = useParams<{ slug: string }>();
  const [state, setState] = useState<LoadState>("loading");
  const [news, setNews] = useState<PublicNewsDetail | null>(null);

  const newsId = useMemo(() => {
    const slug = params.slug || "";
    const normalized = slug.replace(".html", "");
    const value = Number(normalized);
    return Number.isNaN(value) ? 0 : value;
  }, [params.slug]);

  useEffect(() => {
    if (!newsId) {
      setState("notFound");
      return;
    }
    void loadDetail(newsId);
  }, [newsId]);

  return (
    <main className={styles.page}>
      <div className={styles.topBar}>
        <div className={styles.topInner}>
          <button type="button" className={styles.backButton} onClick={() => router.back()}>
            <i className="fas fa-arrow-left" /> {t("action.back")}
          </button>
        </div>
      </div>

      <div className={styles.container}>
        {state === "loading" ? (
          <div className={styles.loading}>
            <i className="fas fa-spinner fa-spin" />
            <p>{t("state.loading")}</p>
          </div>
        ) : null}

        {state === "error" || state === "notFound" ? (
          <div className={styles.empty}>
            <i className="fas fa-circle-exclamation" />
            <p>{state === "error" ? t("state.loadFailed") : t("state.notFound")}</p>
          </div>
        ) : null}

        {state === "ready" && news ? (
          <article className={styles.article}>
            <h1>{news.title}</h1>
            <div className={styles.meta}>
              <span>
                <i className="fas fa-user" /> {news.authorName || t("meta.authorFallback")}
              </span>
              <span>
                <i className="fas fa-calendar-day" />{" "}
                {formatNewsDateTime(news.publishedAt)}
              </span>
              <span>
                <i className="fas fa-eye" /> {t("meta.views", { count: news.viewCount })}
              </span>
            </div>
            {news.cover ? <img src={news.cover} alt={news.title} className={styles.cover} /> : null}
            <div className={styles.content}>
              <MarkdownArticle content={news.markdownContent || t("content.empty")} />
            </div>
          </article>
        ) : null}
      </div>
    </main>
  );

  async function loadDetail(id: number) {
    setState("loading");
    try {
      const response = await fetch(`/api/news/${id}`, {
        cache: "no-store"
      });
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: PublicNewsDetail }
        | null;

      if (response.status === 404) {
        setState("notFound");
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        setState("error");
        return;
      }

      setNews(result.data);
      setState("ready");
    } catch {
      setState("error");
    }
  }
}
