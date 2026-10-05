"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { VersionChangelogTimeline } from "../changelog/VersionChangelogTimeline";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { logoutAdminSession } from "../../lib/admin-system/session";
import {
  createVersionChangelogRequest,
  deleteVersionChangelogRequest,
  fetchVersionChangelogsRequest,
  updateVersionChangelogRequest
} from "../../lib/version-changelog/client";
import { VersionChangelogItem } from "../../lib/version-changelog/types";
import styles from "../../styles/sysAdmin.module.css";
import changeStyles from "../../styles/versionChangelog.module.css";

type ChangelogResponse = {
  success?: boolean;
  data?: VersionChangelogItem[];
  message?: string;
};

export function SysAdminVersionChangelog() {
  const t = useT("adminVersionChangelog");
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [items, setItems] = useState<VersionChangelogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formVersion, setFormVersion] = useState("");
  const [formTitle, setFormTitle] = useState("");
  const [formDate, setFormDate] = useState("");
  const [formContent, setFormContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadChangelogs();
  }, [authorized]);

  if (checking) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{tPortal("auth.checking")}</p>
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
          <h1>
            <i className="fas fa-history" />
            {t("header.title")}
          </h1>
          <button type="button" className={styles.logoutButton} onClick={() => void handleLogout()}>
            <i className="fas fa-sign-out-alt" />
            {t("header.logout")}
          </button>
        </div>
      </header>

      <div className={styles.container}>
        <div className={styles.stack}>
          <section className={styles.card}>
            <div className={changeStyles.headerRow}>
              <h2>
                <i className="fas fa-clock-rotate-left" />
                {t("list.title")}
              </h2>
              <button
                type="button"
                className={changeStyles.versionPrimaryButton}
                onClick={() => openCreateForm()}
                disabled={submitting}
              >
                <i className="fas fa-plus" />
                {t("actions.create")}
              </button>
            </div>

            {formOpen ? (
              <form className={changeStyles.form} onSubmit={(event) => void handleSubmit(event)}>
                <div className={changeStyles.formRow}>
                  <label className={changeStyles.formLabel}>
                    {t("form.version")}
                    <input
                      className={changeStyles.formInput}
                      value={formVersion}
                      onChange={(event) => setFormVersion(event.target.value)}
                      placeholder="v0.2.0"
                      required
                    />
                  </label>
                  <label className={changeStyles.formLabel}>
                    {t("form.releasedAt")}
                    <input
                      className={changeStyles.formInput}
                      type="date"
                      value={formDate}
                      onChange={(event) => setFormDate(event.target.value)}
                      required
                    />
                  </label>
                </div>
                <label className={changeStyles.formLabel}>
                  {t("form.title")}
                  <input
                    className={changeStyles.formInput}
                    value={formTitle}
                    onChange={(event) => setFormTitle(event.target.value)}
                    required
                  />
                </label>
                <label className={changeStyles.formLabel}>
                  {t("form.content")}
                  <textarea
                    className={changeStyles.formTextarea}
                    value={formContent}
                    onChange={(event) => setFormContent(event.target.value)}
                    placeholder={t("form.contentPlaceholder")}
                    rows={6}
                    required
                  />
                </label>
                <div className={changeStyles.formActions}>
                  <button
                    type="button"
                    className={changeStyles.versionSecondaryButton}
                    onClick={() => setFormOpen(false)}
                    disabled={submitting}
                  >
                    {t("actions.cancel")}
                  </button>
                  <button type="submit" className={changeStyles.versionPrimaryButton} disabled={submitting}>
                    {submitting ? t("actions.saving") : t("actions.save")}
                  </button>
                </div>
              </form>
            ) : null}

            <VersionChangelogTimeline
              items={items}
              loading={loading}
              loadingLabel={t("common.loading")}
              emptyLabel={t("list.empty")}
              timeFallbackLabel={t("common.timeFallback")}
              renderActions={(item) => (
                <>
                  <button
                    type="button"
                    className={`${changeStyles.versionActionButton} ${changeStyles.versionActionEdit}`}
                    onClick={() => openEditForm(item)}
                    disabled={deletingId === item.id}
                  >
                    <i className="fas fa-pen" />
                    {t("actions.edit")}
                  </button>
                  <button
                    type="button"
                    className={`${changeStyles.versionActionButton} ${changeStyles.versionActionDanger}`}
                    onClick={() => void handleDelete(item)}
                    disabled={deletingId === item.id}
                  >
                    <i className="fas fa-trash" />
                    {deletingId === item.id ? t("actions.deleting") : t("actions.delete")}
                  </button>
                </>
              )}
            />
          </section>
        </div>
      </div>
    </main>
  );

  function openCreateForm() {
    setEditingId(null);
    setFormVersion("");
    setFormTitle("");
    setFormDate(new Date().toISOString().slice(0, 10));
    setFormContent("");
    setFormOpen(true);
  }

  function openEditForm(item: VersionChangelogItem) {
    setEditingId(item.id);
    setFormVersion(item.version);
    setFormTitle(item.title);
    setFormDate(item.releasedAt ? item.releasedAt.slice(0, 10) : new Date().toISOString().slice(0, 10));
    setFormContent(item.content);
    setFormOpen(true);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) {
      return;
    }

    const releasedAt = formDate ? new Date(`${formDate}T00:00:00`).toISOString() : new Date().toISOString();
    const payload = {
      version: formVersion.trim(),
      title: formTitle.trim(),
      content: formContent.trim(),
      releasedAt
    };
    if (!payload.version || !payload.title || !payload.content) {
      notify.warning(t("messages.fillRequired"));
      return;
    }

    setSubmitting(true);
    try {
      const response = editingId == null
        ? await createVersionChangelogRequest(payload)
        : await updateVersionChangelogRequest(editingId, payload);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(String(result?.message || t("messages.saveFailed")));
        return;
      }
      notify.success(editingId == null ? t("messages.created") : t("messages.updated"));
      setFormOpen(false);
      await loadChangelogs();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(item: VersionChangelogItem) {
    if (deletingId) {
      return;
    }
    if (!window.confirm(t("messages.deleteConfirm", { version: item.version }))) {
      return;
    }
    setDeletingId(item.id);
    try {
      const response = await deleteVersionChangelogRequest(item.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      if (!response.ok) {
        notify.error(t("messages.deleteFailed"));
        return;
      }
      notify.success(t("messages.deleted"));
      if (editingId === item.id) {
        setFormOpen(false);
      }
      await loadChangelogs();
    } finally {
      setDeletingId(null);
    }
  }

  async function loadChangelogs() {
    setLoading(true);
    try {
      const response = await fetchVersionChangelogsRequest();
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ChangelogResponse | null;
      setItems(response.ok && result?.success && Array.isArray(result.data) ? result.data : []);
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    await logoutAdminSession();
    router.replace("/login");
  }

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      await logoutAdminSession();
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
