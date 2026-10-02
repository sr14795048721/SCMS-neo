"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useConfirm } from "../../lib/notify/useConfirm";
import { revokeObjectUrl } from "../../lib/news/adminMedia";
import { fetchManagerNewsCoverObjectUrl } from "../../lib/news/managerMedia";
import { AdminNewsListItem } from "../../lib/news/types";
import {
  createManagerNewsDraftRequest,
  deleteManagerNewsRequest,
  fetchManagerNewsRequest
} from "../../lib/manager/teacherWorkflowClient";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/sysAdminNews.module.css";

type PagePayload = {
  items: AdminNewsListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

type NewsPageResponse = {
  success?: boolean;
  data?: PagePayload;
};

const EMPTY_PAGE: PagePayload = {
  items: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
};

function TeacherNewsCover({ item, alt }: { item: AdminNewsListItem; alt: string }) {
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
      const objectUrl = await fetchManagerNewsCoverObjectUrl(item.id, item.updatedAt);
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

export function TeacherNewsManage() {
  const t = useT("teacherNews");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [pageData, setPageData] = useState<PagePayload>(EMPTY_PAGE);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadPage(1);
  }, [portal.authorized]);

  if (portal.checking || loading) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{portal.checking ? t("states.checking") : t("states.loading")}</p>
        </section>
      </main>
    );
  }

  if (!portal.authorized || portal.redirecting) {
    return null;
  }

  return (
    <PortalShell
      title={t("header.title")}
      subtitle={t("header.subtitle")}
      profileName={portal.profileName}
      profileHint={t("common.profileHint")}
      avatarUrl={portal.avatarUrl}
      profileLoading={!portal.managerInfo}
      onProfileClick={() => router.push("/club-admin/profile")}
      logoutLabel={t("common.logout")}
      logoutPendingLabel={t("common.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <main className={styles.page}>
        <header className={styles.header}>
          <div className={styles.headerInner}>
            <div className={styles.headerTitle}>
              <h1>
                <i className="fas fa-newspaper" />
                {t("header.title")}
              </h1>
              <p>{t("header.subtitle")}</p>
            </div>
            <div className={styles.headerActions}>
              <button type="button" className={styles.secondaryButton} onClick={() => router.push("/club-admin")}>
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
              <p className={styles.metaText}>{t("list.count", { count: pageData.total })}</p>
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
                      <TeacherNewsCover item={item} alt={item.title} />
                      <div className={styles.itemMain}>
                        <div className={styles.itemTitleRow}>
                          <h3 className={styles.itemTitle}>{item.title || t("list.untitled")}</h3>
                          <span className={`${styles.badge} ${styles.draft}`}>{t("status.draft")}</span>
                        </div>
                        <div className={styles.itemMeta}>
                          <span>
                            <i className="fas fa-user" /> {item.authorName || t("list.authorFallback")}
                          </span>
                          <span>
                            <i className="fas fa-clock" /> {item.updatedAt ? new Date(item.updatedAt).toLocaleString("zh-CN") : "-"}
                          </span>
                        </div>
                      </div>
                      <div className={styles.itemActions}>
                        <button
                          type="button"
                          className={styles.rowAction}
                          onClick={() => router.push(`/club-admin/news-publish/${item.id}`)}
                          disabled={busyId === item.id}
                        >
                          <i className="fas fa-pen" />
                        </button>
                        <button
                          type="button"
                          className={`${styles.rowAction} ${styles.dangerGhost}`}
                          onClick={() => void handleDelete(item.id)}
                          disabled={busyId === item.id}
                        >
                          <i className="fas fa-trash" />
                        </button>
                      </div>
                    </article>
                  ))}
                </div>

                <div className={styles.footerBar}>
                  <p className={styles.subtleText}>{t("list.pageInfo", { page: pageData.page, totalPages: pageData.totalPages || 1 })}</p>
                  <div className={styles.pager}>
                    <button type="button" className={styles.pagerButton} onClick={() => void loadPage(pageData.page - 1)} disabled={pageData.page <= 1}>
                      {t("actions.previous")}
                    </button>
                    <button
                      type="button"
                      className={styles.pagerButton}
                      onClick={() => void loadPage(pageData.page + 1)}
                      disabled={pageData.totalPages <= 0 || pageData.page >= pageData.totalPages}
                    >
                      {t("actions.next")}
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </section>
      </main>
    </PortalShell>
  );

  async function loadPage(page: number) {
    setLoading(true);
    try {
      const response = await fetchManagerNewsRequest(Math.max(page, 1), 10);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as NewsPageResponse | null;
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
    setCreating(true);
    try {
      const response = await createManagerNewsDraftRequest();
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; data?: { id?: number } } | null;
      if (!response.ok || result?.success !== true || !result.data?.id) {
        notify.error(t("messages.createFailed"));
        return;
      }
      notify.success(t("messages.createSuccess"));
      router.push(`/club-admin/news-publish/${result.data.id}`);
    } catch {
      notify.error(t("messages.createFailed"));
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete(newsId: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteTitle"),
      message: t("confirm.deleteMessage"),
      confirmText: t("actions.confirm"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setBusyId(newsId);
    try {
      const response = await deleteManagerNewsRequest(newsId);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean } | null;
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
}
