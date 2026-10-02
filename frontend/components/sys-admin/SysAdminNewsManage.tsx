"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useConfirm } from "../../lib/notify/useConfirm";
import { fetchAdminNewsCoverObjectUrl, revokeObjectUrl } from "../../lib/news/adminMedia";
import { AdminNewsListItem } from "../../lib/news/types";
import styles from "../../styles/sysAdminNews.module.css";

type PagePayload = {
  items: AdminNewsListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

const DEFAULT_PAGE: PagePayload = {
  items: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
};

function NewsListCover({ item, alt }: { item: AdminNewsListItem; alt: string }) {
  const [coverUrl, setCoverUrl] = useState<string | null>(null);
  const coverUrlRef = useRef<string | null>(null);

  useEffect(() => {
    let disposed = false;

    async function loadCover() {
      if (!item.hasCover) {
        clearCover();
        return;
      }

      clearCover();
      const objectUrl = await fetchAdminNewsCoverObjectUrl(item.id, item.updatedAt);
      if (!objectUrl || disposed) {
        if (objectUrl) {
          revokeObjectUrl(objectUrl);
        }
        return;
      }

      coverUrlRef.current = objectUrl;
      setCoverUrl(objectUrl);
    }

    void loadCover();

    return () => {
      disposed = true;
      clearCover();
    };
  }, [item.hasCover, item.id, item.updatedAt]);

  if (coverUrl) {
    return <img src={coverUrl} alt={alt} className={styles.cover} />;
  }

  return (
    <div className={styles.coverPlaceholder}>
      <i className="fas fa-image" />
    </div>
  );

  function clearCover() {
    if (coverUrlRef.current) {
      revokeObjectUrl(coverUrlRef.current);
      coverUrlRef.current = null;
    }
    setCoverUrl(null);
  }
}

export function SysAdminNewsManage() {
  const t = useT("adminNews");
  const tPortal = useT("portal");
  const tCommon = useT("common");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [pageData, setPageData] = useState<PagePayload>(DEFAULT_PAGE);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [sourceFilter, setSourceFilter] = useState<"ALL" | "ADMIN" | "CLUB_MANAGER">("ALL");

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadPage(1);
  }, [authorized, sourceFilter]);

  if (checking || loading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : t("list.loading")}</p>
        </section>
      </main>
    );
  }

  if (!authorized || redirecting) {
    return null;
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerTitle}>
            <h1>
              <i className="fas fa-newspaper" />
              {t("list.header.title")}
            </h1>
            <p>{t("list.header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button type="button" className={styles.primaryButton} onClick={() => void createDraft()} disabled={creating}>
              <i className="fas fa-plus" />
              {creating ? t("actions.creating") : t("actions.create")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.card}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("list.sectionTitle")}</h2>
              <p>{t("list.sectionDesc")}</p>
            </div>
            <div className={styles.headerActions}>
              <select
                className={styles.filterSelect}
                value={sourceFilter}
                onChange={(event) => setSourceFilter(event.target.value as "ALL" | "ADMIN" | "CLUB_MANAGER")}
              >
                <option value="ALL">{t("filters.all")}</option>
                <option value="ADMIN">{t("filters.admin")}</option>
                <option value="CLUB_MANAGER">{t("filters.teacher")}</option>
              </select>
              <p className={styles.metaText}>{t("list.count", { count: pageData.total })}</p>
            </div>
          </div>

          {!pageData.items.length ? (
            <div className={styles.emptyState}>
              <div>
                <i className="fas fa-newspaper" />
                <h3>{t("list.empty.title")}</h3>
                <p>{t("list.empty.description")}</p>
              </div>
            </div>
          ) : (
            <>
              <div className={styles.list}>
                {pageData.items.map((item) => (
                  <article key={item.id} className={styles.item}>
                    <NewsListCover item={item} alt={item.title} />

                    <div className={styles.itemMain}>
                      <div className={styles.itemTitleRow}>
                        <h3 className={styles.itemTitle}>{item.title || t("list.untitled")}</h3>
                        <span className={`${styles.badge} ${item.status === "PUBLISHED" ? styles.published : styles.draft}`}>
                          {item.status === "PUBLISHED" ? t("status.published") : t("status.draft")}
                        </span>
                        <span className={`${styles.badge} ${styles.draft}`}>
                          {item.authorRole === "CLUB_MANAGER" ? t("list.sourceTeacher") : t("list.sourceAdmin")}
                        </span>
                      </div>
                      <div className={styles.itemMeta}>
                        <span>
                          <i className="fas fa-user" /> {item.authorName || t("list.authorFallback")}
                        </span>
                        <span>
                          <i className="fas fa-eye" /> {t("list.views", { count: item.viewCount })}
                        </span>
                        <span>
                          <i className="fas fa-calendar-day" />{" "}
                          {item.publishedAt ? new Date(item.publishedAt).toLocaleString("zh-CN") : t("list.unpublished")}
                        </span>
                        <span>
                          <i className="fas fa-pen-to-square" />{" "}
                          {item.updatedAt ? new Date(item.updatedAt).toLocaleString("zh-CN") : "-"}
                        </span>
                      </div>
                    </div>

                    <div className={styles.itemActions}>
                      <button
                        type="button"
                        className={styles.rowAction}
                        onClick={() => router.push(`/sys-admin/news-manage/${item.id}`)}
                        aria-label={t("actions.edit")}
                        disabled={busyId === item.id}
                      >
                        <i className="fas fa-pen" />
                      </button>
                      <button
                        type="button"
                        className={`${styles.rowAction} ${styles.dangerGhost}`}
                        onClick={() => void deleteNews(item.id)}
                        aria-label={t("actions.delete")}
                        disabled={busyId === item.id}
                      >
                        <i className="fas fa-trash" />
                      </button>
                    </div>
                  </article>
                ))}
              </div>

              <div className={styles.footerBar}>
                <p className={styles.subtleText}>
                  {t("list.pageInfo", { page: pageData.page, totalPages: pageData.totalPages || 1 })}
                </p>
                <div className={styles.pager}>
                  <button
                    type="button"
                    className={styles.pagerButton}
                    onClick={() => void loadPage(pageData.page - 1)}
                    disabled={pageData.page <= 1}
                  >
                    {tCommon("action.previous")}
                  </button>
                  <button
                    type="button"
                    className={styles.pagerButton}
                    onClick={() => void loadPage(pageData.page + 1)}
                    disabled={pageData.totalPages <= 0 || pageData.page >= pageData.totalPages}
                  >
                    {tCommon("action.next")}
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  );

  async function loadPage(page: number) {
    const query = new URLSearchParams({
      page: String(Math.max(page, 1)),
      pageSize: "10"
    });
    if (sourceFilter !== "ALL") {
      query.set("source", sourceFilter);
    }

    const response = await authorizedFetch(`/api/admin/news?${query.toString()}`, {
      cache: "no-store"
    });
    if (!response) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return;
    }

    setLoading(true);
    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: PagePayload }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.loadFailed"));
        return;
      }

      setPageData(result.data);
    } catch {
      notify.error(t("messages.loadFailed"));
    } finally {
      setLoading(false);
    }
  }

  async function createDraft() {
    const response = await authorizedFetch("/api/admin/news", {
      method: "POST",
      cache: "no-store"
    });
    if (!response) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return;
    }

    setCreating(true);
    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: { id?: number } }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data?.id) {
        notify.error(t("messages.createFailed"));
        return;
      }

      notify.success(t("messages.createSuccess"));
      router.push(`/sys-admin/news-manage/${result.data.id}`);
    } catch {
      notify.error(t("messages.createFailed"));
    } finally {
      setCreating(false);
    }
  }

  async function deleteNews(newsId: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteTitle"),
      message: t("confirm.deleteMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    const response = await authorizedFetch(`/api/admin/news/${newsId}`, {
      method: "DELETE",
      cache: "no-store"
    });
    if (!response) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return;
    }

    setBusyId(newsId);
    try {
      const result = (await response.json().catch(() => null)) as { success?: boolean } | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true) {
        notify.error(t("messages.deleteFailed"));
        return;
      }

      notify.success(t("messages.deleteSuccess"));
      void loadPage(pageData.page);
    } catch {
      notify.error(t("messages.deleteFailed"));
    } finally {
      setBusyId(null);
    }
  }

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return false;
    }
    if (status === 403) {
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return false;
    }
    return true;
  }
}
