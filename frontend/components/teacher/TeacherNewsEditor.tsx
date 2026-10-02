"use client";

import dynamic from "next/dynamic";
import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useConfirm } from "../../lib/notify/useConfirm";
import { revokeObjectUrl } from "../../lib/news/adminMedia";
import { fetchManagerNewsCoverObjectUrl } from "../../lib/news/managerMedia";
import { TeacherNewsDetail } from "../../lib/manager/teacherWorkflowTypes";
import {
  deleteManagerNewsCoverRequest,
  fetchManagerNewsDetailRequest,
  updateManagerNewsRequest,
  uploadManagerNewsBodyImageRequest,
  uploadManagerNewsCoverRequest
} from "../../lib/manager/teacherWorkflowClient";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/sysAdminNews.module.css";

const MDEditor = dynamic(() => import("@uiw/react-md-editor"), { ssr: false });

const EMPTY_EDITOR: TeacherNewsDetail = {
  id: 0,
  title: "",
  status: "DRAFT",
  markdownContent: "",
  cover: undefined,
  authorName: "",
  authorRole: "CLUB_MANAGER",
  viewCount: 0,
  publishedAt: null,
  coverUpdatedAt: null,
  createdAt: null,
  updatedAt: null
};

export function TeacherNewsEditor() {
  const t = useT("teacherNews");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const params = useParams<{ newsId: string }>();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [uploadingAsset, setUploadingAsset] = useState(false);
  const [editor, setEditor] = useState<TeacherNewsDetail>(EMPTY_EDITOR);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null);
  const coverPreviewUrlRef = useRef<string | null>(null);
  const markdownShellRef = useRef<HTMLDivElement | null>(null);
  const bodyImageInputRef = useRef<HTMLInputElement | null>(null);
  const selectionRef = useRef({ start: 0, end: 0 });

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized || !params.newsId) {
      return;
    }
    void loadDetail();
  }, [portal.authorized, params.newsId]);

  useEffect(() => {
    void loadCoverPreview();
    return () => {
      revokeCoverPreview();
    };
  }, [editor.id, editor.cover, editor.coverUpdatedAt, editor.updatedAt]);

  useEffect(() => {
    const textarea = findMarkdownTextarea();
    if (!textarea) {
      return;
    }

    const updateSelection = () => {
      selectionRef.current = {
        start: textarea.selectionStart ?? 0,
        end: textarea.selectionEnd ?? 0
      };
    };

    updateSelection();
    textarea.addEventListener("select", updateSelection);
    textarea.addEventListener("click", updateSelection);
    textarea.addEventListener("keyup", updateSelection);

    return () => {
      textarea.removeEventListener("select", updateSelection);
      textarea.removeEventListener("click", updateSelection);
      textarea.removeEventListener("keyup", updateSelection);
    };
  }, [editor.id, editor.markdownContent]);

  if (portal.checking || loading) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{portal.checking ? t("states.checking") : t("editor.loading")}</p>
        </section>
      </main>
    );
  }

  if (!portal.authorized || portal.redirecting) {
    return null;
  }

  return (
    <PortalShell
      title={t("editor.header.title")}
      subtitle={t("editor.header.subtitle")}
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
                <i className="fas fa-pen-nib" />
                {t("editor.header.title")}
              </h1>
              <p>{t("editor.header.subtitle")}</p>
            </div>
            <div className={styles.headerActions}>
              <button type="button" className={styles.secondaryButton} onClick={() => router.push("/club-admin/news-publish")}>
                <i className="fas fa-arrow-left" />
                {t("actions.back")}
              </button>
              <button type="button" className={styles.primaryButton} onClick={() => void saveNews()} disabled={saving}>
                <i className="fas fa-floppy-disk" />
                {saving ? t("actions.saving") : t("actions.saveDraft")}
              </button>
            </div>
          </div>
        </header>

        <section className={styles.container}>
          <div className={styles.editorGrid}>
            <div className={styles.editorCard}>
              <div className={styles.editorForm}>
                <label className={styles.field}>
                  <div className={styles.fieldLabel}>
                    <span>{t("editor.fields.title")}</span>
                  </div>
                  <input
                    className={`${styles.input} ${styles.titleInput}`}
                    value={editor.title}
                    onChange={(event) => setEditor((current) => ({ ...current, title: event.target.value }))}
                    placeholder={t("editor.fields.titlePlaceholder")}
                    maxLength={80}
                  />
                </label>

                <div className={styles.field}>
                  <div className={styles.fieldLabel}>
                    <span>{t("editor.fields.content")}</span>
                    <div className={styles.toolbarRow}>
                      <button
                        type="button"
                        className={styles.uploadButton}
                        disabled={uploadingAsset}
                        onClick={() => {
                          captureSelection();
                          bodyImageInputRef.current?.click();
                        }}
                      >
                        <i className="fas fa-image" />
                        {uploadingAsset ? t("actions.uploadingImage") : t("actions.uploadBodyImage")}
                      </button>
                      <input
                        ref={bodyImageInputRef}
                        type="file"
                        hidden
                        accept="image/jpeg,image/png,image/webp"
                        disabled={uploadingAsset}
                        onChange={(event) => {
                          const file = event.target.files?.[0];
                          event.currentTarget.value = "";
                          if (file) {
                            void uploadBodyImage(file);
                          }
                        }}
                      />
                    </div>
                  </div>
                  <div ref={markdownShellRef} className={styles.markdownShell} data-color-mode="light">
                    <MDEditor
                      value={editor.markdownContent}
                      onChange={(value) => {
                        setEditor((current) => ({ ...current, markdownContent: value || "" }));
                      }}
                      preview="edit"
                      height={520}
                      visibleDragbar={false}
                    />
                  </div>
                  <p className={styles.editorHint}>{t("editor.markdownHint")}</p>
                </div>
              </div>
            </div>

            <aside className={styles.sideCard}>
              <div className={styles.cardHead}>
                <div>
                  <h2>{t("editor.sideTitle")}</h2>
                </div>
              </div>

              <div className={styles.sidePanel}>
                <section className={styles.sideSection}>
                  <span className={`${styles.badge} ${styles.draft}`}>{t("status.draft")}</span>
                  <div className={styles.metaList}>
                    <div className={styles.metaItem}>
                      <i className="fas fa-user" />
                      <span>{t("editor.meta.author", { name: editor.authorName || t("list.authorFallback") })}</span>
                    </div>
                    <div className={styles.metaItem}>
                      <i className="fas fa-pen-to-square" />
                      <span>{editor.updatedAt ? new Date(editor.updatedAt).toLocaleString("zh-CN") : t("editor.meta.unpublished")}</span>
                    </div>
                  </div>
                </section>

                <section className={styles.sideSection}>
                  <div className={styles.coverPreviewBox}>
                    {editor.cover ? (
                      <img src={coverPreviewUrl || undefined} alt={editor.title || t("editor.fields.title")} className={styles.previewCover} />
                    ) : (
                      <div className={styles.coverPlaceholder}>
                        <i className="fas fa-image" />
                      </div>
                    )}
                  </div>

                  <div className={styles.coverActions}>
                    <label className={`${styles.uploadButton} ${styles.sideButton}`}>
                      <i className="fas fa-upload" />
                      {uploadingCover ? t("actions.uploadingCover") : editor.cover ? t("actions.replaceCover") : t("actions.uploadCover")}
                      <input
                        type="file"
                        hidden
                        accept="image/jpeg,image/png,image/webp"
                        disabled={uploadingCover}
                        onChange={(event) => {
                          const file = event.target.files?.[0];
                          event.currentTarget.value = "";
                          if (file) {
                            void uploadCover(file);
                          }
                        }}
                      />
                    </label>
                    {editor.cover ? (
                      <button type="button" className={`${styles.dangerButton} ${styles.sideButton}`} onClick={() => void removeCover()} disabled={uploadingCover}>
                        <i className="fas fa-trash" />
                        {t("actions.removeCover")}
                      </button>
                    ) : null}
                  </div>
                </section>
              </div>
            </aside>
          </div>
        </section>
      </main>
    </PortalShell>
  );

  async function loadDetail() {
    const newsId = Number(params.newsId);
    const response = await fetchManagerNewsDetailRequest(newsId);
    if (!response) {
      notify.warning(t("messages.loginRequired"));
      router.replace("/login");
      return;
    }
    if (!(await portal.handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as { success?: boolean; data?: TeacherNewsDetail } | null;
    if (!response.ok || result?.success !== true || !result.data) {
      notify.error(t("messages.detailFailed"));
      router.replace("/club-admin/news-publish");
      return;
    }
    setEditor(result.data);
    setLoading(false);
  }

  async function saveNews() {
    setSaving(true);
    try {
      const response = await updateManagerNewsRequest(Number(params.newsId), {
        title: editor.title,
        markdownContent: editor.markdownContent,
        status: "DRAFT"
      });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; data?: TeacherNewsDetail } | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.saveFailed"));
        return;
      }
      setEditor(result.data);
      notify.success(t("messages.saveSuccess"));
    } catch {
      notify.error(t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  async function uploadCover(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    setUploadingCover(true);
    try {
      const response = await uploadManagerNewsCoverRequest(Number(params.newsId), formData);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; data?: TeacherNewsDetail } | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.coverUploadFailed"));
        return;
      }
      setEditor(result.data);
      notify.success(t("messages.coverUploadSuccess"));
    } catch {
      notify.error(t("messages.coverUploadFailed"));
    } finally {
      setUploadingCover(false);
    }
  }

  async function removeCover() {
    const accepted = await confirm.confirm({
      title: t("confirm.removeCoverTitle"),
      message: t("confirm.removeCoverMessage"),
      confirmText: t("actions.confirm"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }
    setUploadingCover(true);
    try {
      const response = await deleteManagerNewsCoverRequest(Number(params.newsId));
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; data?: TeacherNewsDetail } | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.coverRemoveFailed"));
        return;
      }
      setEditor(result.data);
      notify.success(t("messages.coverRemoveSuccess"));
    } catch {
      notify.error(t("messages.coverRemoveFailed"));
    } finally {
      setUploadingCover(false);
    }
  }

  async function uploadBodyImage(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    setUploadingAsset(true);
    try {
      const response = await uploadManagerNewsBodyImageRequest(Number(params.newsId), formData);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: { markdown?: string } }
        | null;
      if (!response.ok || result?.success !== true || !result.data?.markdown) {
        notify.error(t("messages.assetUploadFailed"));
        return;
      }

      insertMarkdown(result.data.markdown);
      notify.success(t("messages.assetUploadSuccess"));
    } catch {
      notify.error(t("messages.assetUploadFailed"));
    } finally {
      setUploadingAsset(false);
    }
  }

  async function loadCoverPreview() {
    if (!editor.cover || !editor.id) {
      revokeCoverPreview();
      return;
    }
    revokeCoverPreview();
    const objectUrl = await fetchManagerNewsCoverObjectUrl(editor.id, editor.coverUpdatedAt || editor.updatedAt);
    if (!objectUrl) {
      return;
    }
    coverPreviewUrlRef.current = objectUrl;
    setCoverPreviewUrl(objectUrl);
  }

  function revokeCoverPreview() {
    if (coverPreviewUrlRef.current) {
      revokeObjectUrl(coverPreviewUrlRef.current);
      coverPreviewUrlRef.current = null;
    }
    setCoverPreviewUrl(null);
  }

  function captureSelection() {
    const textarea = findMarkdownTextarea();
    if (!textarea) {
      return;
    }
    selectionRef.current = {
      start: textarea.selectionStart ?? 0,
      end: textarea.selectionEnd ?? 0
    };
  }

  function insertMarkdown(markdown: string) {
    setEditor((current) => {
      const content = current.markdownContent || "";
      const { start, end } = selectionRef.current;
      return {
        ...current,
        markdownContent: `${content.slice(0, start)}${markdown}${content.slice(end)}`
      };
    });
  }

  function findMarkdownTextarea() {
    return markdownShellRef.current?.querySelector("textarea") || null;
  }
}
