"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createAdminAppReleaseRequest,
  fetchAdminAppReleasesRequest,
  publishAdminAppReleaseRequest,
  retireAdminAppReleaseRequest,
  updateAdminAppReleaseRequest
} from "../../lib/app-releases/client";
import { AppReleaseFormValue, AppReleaseItem } from "../../lib/app-releases/types";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import styles from "../../styles/sysAdminAppReleases.module.css";

const RELEASES_PER_PAGE = 4;

type CollectionResponse = {
  success?: boolean;
  items?: AppReleaseItem[];
  code?: string;
  message?: string;
};

type ItemResponse = {
  success?: boolean;
  item?: AppReleaseItem;
  code?: string;
  message?: string;
};

function createEmptyForm(): AppReleaseFormValue {
  return {
    versionName: "",
    buildNumber: 1,
    releaseNotes: "",
    forceUpdate: false,
    androidUrl: "",
    harmonyUrl: ""
  };
}

export function SysAdminAppReleases() {
  const t = useT("adminAppReleases");
  const tPortal = useT("portal");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate(["ADMIN", "SUPER_ADMIN"]);
  const [items, setItems] = useState<AppReleaseItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionReleaseId, setActionReleaseId] = useState<number | null>(null);
  const [selectedId, setSelectedId] = useState<number | "new" | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [form, setForm] = useState<AppReleaseFormValue>(createEmptyForm());

  const selectedItem = useMemo(
    () => (typeof selectedId === "number" ? items.find((item) => item.id === selectedId) || null : null),
    [items, selectedId]
  );
  const totalPages = useMemo(() => Math.max(1, Math.ceil(items.length / RELEASES_PER_PAGE)), [items.length]);
  const pagedItems = useMemo(() => {
    const start = currentPage * RELEASES_PER_PAGE;
    return items.slice(start, start + RELEASES_PER_PAGE);
  }, [currentPage, items]);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadItems();
  }, [authorized]);

  useEffect(() => {
    setCurrentPage((previousPage) => Math.min(previousPage, totalPages - 1));
  }, [totalPages]);

  if (checking || loading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : t("states.loading")}</p>
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
              <i className="fas fa-mobile-screen-button" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => {
                void loadItems();
              }}
              disabled={refreshing}
            >
              <i className="fas fa-rotate" />
              {refreshing ? t("actions.refreshing") : t("actions.refresh")}
            </button>
            <button type="button" className={styles.primaryButton} onClick={handleCreateDraft}>
              <i className="fas fa-plus" />
              {t("actions.create")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <aside className={styles.listCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{t("list.title")}</h2>
              <p>{t("list.description")}</p>
            </div>
            <span className={styles.countBadge}>{items.length}</span>
          </div>

          {!items.length ? (
            <div className={styles.emptyState}>
              <i className="fas fa-box-open" />
              <h3>{t("list.emptyTitle")}</h3>
              <p>{t("list.emptyDescription")}</p>
            </div>
          ) : (
            <>
              <div className={styles.releaseList}>
                {pagedItems.map((item) => {
                  const active = item.id === selectedId;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      className={`${styles.releaseCard} ${active ? styles.releaseCardActive : ""}`}
                      onClick={() => handleSelectItem(item)}
                    >
                      <div className={styles.releaseTop}>
                        <strong>{item.versionName}</strong>
                        <span className={`${styles.statusTag} ${styles[`status${item.status}`]}`}>
                          {t(`status.${item.status.toLowerCase()}`)}
                        </span>
                      </div>
                      <p>{t("list.buildLabel", { buildNumber: item.buildNumber })}</p>
                      <small>
                        {item.publishedAt
                          ? t("list.publishedAt", { publishedAt: formatDate(item.publishedAt) })
                          : t("list.createdAt", { createdAt: formatDate(item.createdAt) })}
                      </small>
                    </button>
                  );
                })}
              </div>
              {totalPages > 1 ? (
                <nav className={styles.pagination} aria-label={t("list.paginationLabel")}>
                  <button
                    type="button"
                    className={styles.paginationButton}
                    onClick={() => setCurrentPage((page) => Math.max(0, page - 1))}
                    disabled={currentPage === 0}
                  >
                    {t("list.previousPage")}
                  </button>
                  <div className={styles.paginationPages}>
                    {Array.from({ length: totalPages }, (_, index) => {
                      const active = index === currentPage;
                      return (
                        <button
                          key={index}
                          type="button"
                          className={`${styles.pageButton} ${active ? styles.pageButtonActive : ""}`}
                          onClick={() => setCurrentPage(index)}
                          aria-current={active ? "page" : undefined}
                        >
                          {index + 1}
                        </button>
                      );
                    })}
                  </div>
                  <button
                    type="button"
                    className={styles.paginationButton}
                    onClick={() => setCurrentPage((page) => Math.min(totalPages - 1, page + 1))}
                    disabled={currentPage >= totalPages - 1}
                  >
                    {t("list.nextPage")}
                  </button>
                </nav>
              ) : null}
            </>
          )}
        </aside>

        <section className={styles.editorCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{selectedId === "new" ? t("editor.newTitle") : t("editor.title")}</h2>
              <p>{selectedId === "new" ? t("editor.newDescription") : t("editor.description")}</p>
            </div>
            {selectedItem?.status === "PUBLISHED" ? <span className={styles.liveBadge}>{t("editor.live")}</span> : null}
          </div>

          {!selectedItem && selectedId !== "new" ? (
            <div className={styles.emptyState}>
              <i className="fas fa-mobile-alt" />
              <h3>{t("editor.unselectedTitle")}</h3>
              <p>{t("editor.unselectedDescription")}</p>
            </div>
          ) : (
            <form className={styles.form} onSubmit={handleSave}>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>{t("fields.versionName")}</span>
                  <input
                    value={form.versionName}
                    onChange={(event) => setForm((current) => ({ ...current, versionName: event.target.value }))}
                    placeholder={t("placeholders.versionName")}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.buildNumber")}</span>
                  <input
                    type="number"
                    min={1}
                    value={form.buildNumber}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        buildNumber: Number(event.target.value || 1)
                      }))
                    }
                    placeholder={t("placeholders.buildNumber")}
                  />
                </label>
              </div>

              <label className={styles.field}>
                <span>{t("fields.releaseNotes")}</span>
                <textarea
                  value={form.releaseNotes}
                  onChange={(event) => setForm((current) => ({ ...current, releaseNotes: event.target.value }))}
                  placeholder={t("placeholders.releaseNotes")}
                  rows={8}
                />
              </label>

              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>{t("fields.androidUrl")}</span>
                  <input
                    value={form.androidUrl}
                    onChange={(event) => setForm((current) => ({ ...current, androidUrl: event.target.value }))}
                    placeholder={t("placeholders.androidUrl")}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.harmonyUrl")}</span>
                  <input
                    value={form.harmonyUrl}
                    onChange={(event) => setForm((current) => ({ ...current, harmonyUrl: event.target.value }))}
                    placeholder={t("placeholders.harmonyUrl")}
                  />
                </label>
              </div>

              <label className={styles.switchField}>
                <input
                  type="checkbox"
                  checked={form.forceUpdate}
                  onChange={(event) => setForm((current) => ({ ...current, forceUpdate: event.target.checked }))}
                />
                <div>
                  <strong>{t("fields.forceUpdate")}</strong>
                  <p>{t("fields.forceUpdateHint")}</p>
                </div>
              </label>

              <div className={styles.footerActions}>
                <button type="submit" className={styles.primaryButton} disabled={saving}>
                  <i className="fas fa-floppy-disk" />
                  {saving ? t("actions.saving") : t("actions.save")}
                </button>
                {selectedItem ? (
                  <>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => {
                        void handlePublish(selectedItem);
                      }}
                      disabled={actionReleaseId === selectedItem.id}
                    >
                      <i className="fas fa-cloud-arrow-up" />
                      {t("actions.publish")}
                    </button>
                    <button
                      type="button"
                      className={styles.dangerButton}
                      onClick={() => {
                        void handleRetire(selectedItem);
                      }}
                      disabled={actionReleaseId === selectedItem.id || selectedItem.status !== "PUBLISHED"}
                    >
                      <i className="fas fa-box-archive" />
                      {t("actions.retire")}
                    </button>
                  </>
                ) : null}
              </div>
            </form>
          )}
        </section>
      </section>
    </main>
  );

  async function loadItems(preferredSelection?: number | "new" | null) {
    if (refreshing) {
      return;
    }

    if (loading) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }

    try {
      const response = await fetchAdminAppReleasesRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as CollectionResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.items)) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }

      setItems(result.items);
      const targetSelection = preferredSelection ?? selectedId;
      if (targetSelection === "new") {
        setSelectedId("new");
        setForm(createEmptyForm());
        return;
      }

      const matched = typeof targetSelection === "number" ? result.items.find((item) => item.id === targetSelection) : null;
      const nextSelected = matched || result.items[0] || null;
      setCurrentPage(resolvePageIndex(result.items, nextSelected?.id ?? null));
      setSelectedId(nextSelected ? nextSelected.id : null);
      setForm(nextSelected ? toFormValue(nextSelected) : createEmptyForm());
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  function handleCreateDraft() {
    setSelectedId("new");
    setForm(createEmptyForm());
  }

  function handleSelectItem(item: AppReleaseItem) {
    setCurrentPage(resolvePageIndex(items, item.id));
    setSelectedId(item.id);
    setForm(toFormValue(item));
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) {
      return;
    }

    setSaving(true);
    try {
      const response =
        typeof selectedId === "number"
          ? await updateAdminAppReleaseRequest(selectedId, form)
          : await createAdminAppReleaseRequest(form);

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as ItemResponse | null;
      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(result?.message || t("messages.saveFailed"));
        return;
      }

      notify.success(typeof selectedId === "number" ? t("messages.updated") : t("messages.created"));
      await loadItems(result.item.id);
    } finally {
      setSaving(false);
    }
  }

  async function handlePublish(item: AppReleaseItem) {
    const accepted = await confirm.confirm({
      title: t("confirm.publishTitle"),
      message: t("confirm.publishMessage", { versionName: item.versionName }),
      confirmText: t("confirm.publishAction"),
      cancelText: t("confirm.cancelAction"),
      tone: "primary"
    });
    if (!accepted) {
      return;
    }

    setActionReleaseId(item.id);
    try {
      const response = await publishAdminAppReleaseRequest(item.id);
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as ItemResponse | null;
      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(result?.message || t("messages.publishFailed"));
        return;
      }

      notify.success(t("messages.published"));
      await loadItems(result.item.id);
    } finally {
      setActionReleaseId(null);
    }
  }

  async function handleRetire(item: AppReleaseItem) {
    const accepted = await confirm.confirm({
      title: t("confirm.retireTitle"),
      message: t("confirm.retireMessage", { versionName: item.versionName }),
      confirmText: t("confirm.retireAction"),
      cancelText: t("confirm.cancelAction"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setActionReleaseId(item.id);
    try {
      const response = await retireAdminAppReleaseRequest(item.id);
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as ItemResponse | null;
      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(result?.message || t("messages.retireFailed"));
        return;
      }

      notify.success(t("messages.retired"));
      await loadItems(result.item.id);
    } finally {
      setActionReleaseId(null);
    }
  }
}

function toFormValue(item: AppReleaseItem): AppReleaseFormValue {
  return {
    versionName: item.versionName,
    buildNumber: item.buildNumber,
    releaseNotes: item.releaseNotes,
    forceUpdate: item.forceUpdate,
    androidUrl: item.androidUrl,
    harmonyUrl: item.harmonyUrl
  };
}

function formatDate(value?: string | null) {
  if (!value) {
    return "--";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(
    date.getHours()
  ).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function resolvePageIndex(items: AppReleaseItem[], itemId: number | null) {
  if (!itemId) {
    return 0;
  }

  const itemIndex = items.findIndex((item) => item.id === itemId);
  if (itemIndex < 0) {
    return 0;
  }

  return Math.floor(itemIndex / RELEASES_PER_PAGE);
}
