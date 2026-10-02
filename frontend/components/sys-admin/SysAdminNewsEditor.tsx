"use client";

import dynamic from "next/dynamic";
import { useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useConfirm } from "../../lib/notify/useConfirm";
import { fetchAdminNewsCoverObjectUrl, revokeObjectUrl } from "../../lib/news/adminMedia";
import {
  createAdminNewsPreviewId,
  cleanupExpiredAdminNewsPreviews,
  removeAdminNewsPreview,
  saveAdminNewsPreview
} from "../../lib/news/previewStorage";
import { AdminNewsDetail } from "../../lib/news/types";
import styles from "../../styles/sysAdminNews.module.css";

const MDEditor = dynamic(() => import("@uiw/react-md-editor"), { ssr: false });

type EditorState = AdminNewsDetail;

const EMPTY_EDITOR: EditorState = {
  id: 0,
  title: "",
  status: "DRAFT",
  markdownContent: "",
  cover: undefined,
  authorName: "",
  viewCount: 0,
  publishedAt: null,
  coverUpdatedAt: null,
  createdAt: null,
  updatedAt: null
};

export function SysAdminNewsEditor() {
  const t = useT("adminNews");
  const tPortal = useT("portal");
  const tCommon = useT("common");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const params = useParams<{ newsId: string }>();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [uploadingAsset, setUploadingAsset] = useState(false);
  const [editor, setEditor] = useState<EditorState>(EMPTY_EDITOR);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null);
  const coverPreviewUrlRef = useRef<string | null>(null);
  const markdownShellRef = useRef<HTMLDivElement | null>(null);
  const bodyImageInputRef = useRef<HTMLInputElement | null>(null);
  const selectionRef = useRef({ start: 0, end: 0 });

  useEffect(() => {
    if (!authorized || !params.newsId) {
      return;
    }
    void loadDetail();
  }, [authorized, params.newsId]);

  useEffect(() => {
    cleanupExpiredAdminNewsPreviews();
  }, []);

  useEffect(() => {
    void loadCoverPreview();

    return () => {
      revokeCoverPreview();
    };
  }, [authorized, editor.id, editor.cover, editor.coverUpdatedAt, editor.updatedAt]);

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

  if (checking || loading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : t("editor.loading")}</p>
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
              <i className="fas fa-pen-nib" />
              {t("editor.header.title")}
            </h1>
            <p>{t("editor.header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin/news-manage")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => void saveNews(editor.status === "PUBLISHED" ? "PUBLISHED" : "DRAFT")}
              disabled={saving}
            >
              <i className="fas fa-floppy-disk" />
              {saving ? t("actions.saving") : editor.status === "PUBLISHED" ? t("actions.saveChanges") : t("actions.saveDraft")}
            </button>
            <button type="button" className={styles.secondaryButton} onClick={() => void openPreviewWindow()}>
              <i className="fas fa-up-right-from-square" />
              {t("actions.previewPage")}
            </button>
            <button type="button" className={styles.primaryButton} onClick={() => void saveNews("PUBLISHED")} disabled={saving}>
              <i className="fas fa-paper-plane" />
              {saving ? t("actions.publishing") : t("actions.publish")}
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
                  maxLength={14}
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
                <div className={styles.statusRow}>
                  <span className={styles.sideSectionLabel}>
                    <i className="fas fa-bullhorn" />
                    {t("editor.sideTitle")}
                  </span>
                </div>
                <span className={`${styles.badge} ${editor.status === "PUBLISHED" ? styles.published : styles.draft}`}>
                  {editor.status === "PUBLISHED" ? t("status.published") : t("status.draft")}
                </span>
                <div className={styles.metaList}>
                  <div className={styles.metaItem}>
                    <i className="fas fa-user" />
                    <span>{t("editor.meta.author", { name: editor.authorName || t("list.authorFallback") })}</span>
                  </div>
                  <div className={styles.metaItem}>
                    <i className="fas fa-eye" />
                    <span>{t("editor.meta.views", { count: editor.viewCount })}</span>
                  </div>
                  <div className={styles.metaItem}>
                    <i className="fas fa-calendar-day" />
                    <span>{editor.publishedAt ? new Date(editor.publishedAt).toLocaleString("zh-CN") : t("editor.meta.unpublished")}</span>
                  </div>
                </div>
              </section>

              <section className={styles.sideSection}>
                <div className={styles.coverPreviewBox}>
                  {editor.cover ? (
                    <img
                      src={coverPreviewUrl || undefined}
                      alt={editor.title || t("editor.fields.title")}
                      className={styles.previewCover}
                    />
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
                    <button
                      type="button"
                      className={`${styles.dangerButton} ${styles.sideButton}`}
                      onClick={() => void removeCover()}
                      disabled={uploadingCover}
                    >
                      <i className="fas fa-trash" />
                      {t("actions.removeCover")}
                    </button>
                  ) : null}
                </div>
              </section>

              {editor.id ? (
                <section className={styles.sideSection}>
                  <div className={styles.sideActions}>
                    <button
                      type="button"
                      className={`${styles.secondaryButton} ${styles.sideButton}`}
                      onClick={() => window.open(`/news/${editor.id}.html`, "_blank", "noopener,noreferrer")}
                    >
                      <i className="fas fa-arrow-up-right-from-square" />
                      {t("actions.openPublic")}
                    </button>
                  </div>
                </section>
              ) : null}
            </div>
          </aside>
        </div>
      </section>
    </main>
  );

  async function loadDetail() {
    if (!params.newsId) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return;
    }

    setLoading(true);
    try {
      const response = await authorizedFetch(`/api/admin/news/${params.newsId}`, {
        cache: "no-store"
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: AdminNewsDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.detailFailed"));
        router.replace("/sys-admin/news-manage");
        return;
      }

      setEditor(result.data);
    } catch {
      notify.error(t("messages.detailFailed"));
      router.replace("/sys-admin/news-manage");
    } finally {
      setLoading(false);
    }
  }

  async function saveNews(status: "DRAFT" | "PUBLISHED") {
    if (!params.newsId) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return;
    }

    setSaving(true);
    try {
      const response = await authorizedFetch(`/api/admin/news/${params.newsId}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          title: editor.title,
          markdownContent: editor.markdownContent,
          status
        }),
        cache: "no-store"
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: AdminNewsDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(status === "PUBLISHED" ? t("messages.publishFailed") : t("messages.saveFailed"));
        return;
      }

      setEditor(result.data);
      notify.success(status === "PUBLISHED" ? t("messages.publishSuccess") : t("messages.saveSuccess"));
    } catch {
      notify.error(status === "PUBLISHED" ? t("messages.publishFailed") : t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  async function uploadCover(file: File) {
    if (!params.newsId) {
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    setUploadingCover(true);
    try {
      const response = await authorizedFetch(`/api/admin/news/${params.newsId}/cover`, {
        method: "POST",
        body: formData,
        cache: "no-store"
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: AdminNewsDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.coverUploadFailed"));
        return;
      }

      const nextDetail = result.data;
      revokeCoverPreview();
      setEditor((current) => mergeEditorWithCoverState(current, nextDetail));
      notify.success(t("messages.coverUploadSuccess"));
    } catch {
      notify.error(t("messages.coverUploadFailed"));
    } finally {
      setUploadingCover(false);
    }
  }

  async function removeCover() {
    if (!params.newsId) {
      return;
    }
    const accepted = await confirm.confirm({
      title: t("confirm.removeCoverTitle"),
      message: t("confirm.removeCoverMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setUploadingCover(true);
    try {
      const response = await authorizedFetch(`/api/admin/news/${params.newsId}/cover`, {
        method: "DELETE",
        cache: "no-store"
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: AdminNewsDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(t("messages.coverRemoveFailed"));
        return;
      }

      const nextDetail = result.data;
      revokeCoverPreview();
      setEditor((current) => mergeEditorWithCoverState(current, nextDetail));
      notify.success(t("messages.coverRemoveSuccess"));
    } catch {
      notify.error(t("messages.coverRemoveFailed"));
    } finally {
      setUploadingCover(false);
    }
  }

  async function uploadBodyImage(file: File) {
    if (!params.newsId) {
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    setUploadingAsset(true);
    try {
      const response = await authorizedFetch(`/api/admin/news/${params.newsId}/assets/image`, {
        method: "POST",
        body: formData,
        cache: "no-store"
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: { markdown?: string; url?: string } }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data?.url) {
        notify.error(t("messages.assetUploadFailed"));
        return;
      }

      insertMarkdownAtCursor(`![${t("editor.defaultBodyImageAlt")}](${result.data.url})`);
      notify.success(t("messages.assetUploadSuccess"));
    } catch {
      notify.error(t("messages.assetUploadFailed"));
    } finally {
      setUploadingAsset(false);
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

  async function loadCoverPreview() {
    revokeCoverPreview();

    if (!authorized || !editor.id || !editor.cover) {
      setCoverPreviewUrl(null);
      return;
    }

    try {
      const objectUrl = await fetchAdminNewsCoverObjectUrl(editor.id, editor.coverUpdatedAt || editor.updatedAt);
      if (!objectUrl) {
        setCoverPreviewUrl(null);
        return;
      }
      coverPreviewUrlRef.current = objectUrl;
      setCoverPreviewUrl(objectUrl);
    } catch {
      setCoverPreviewUrl(null);
    }
  }

  function revokeCoverPreview() {
    if (coverPreviewUrlRef.current) {
      revokeObjectUrl(coverPreviewUrlRef.current);
      coverPreviewUrlRef.current = null;
    }
    setCoverPreviewUrl(null);
  }

  function mergeEditorWithCoverState(current: EditorState, incoming: AdminNewsDetail): EditorState {
    return {
      ...incoming,
      title: current.title,
      markdownContent: current.markdownContent,
      status: current.status
    };
  }

  function openPreviewWindow() {
    if (!editor.id) {
      notify.warning(t("messages.previewUnavailable"));
      return;
    }

    const previewId = createAdminNewsPreviewId();
    const previewPayload = {
      previewId,
      newsId: editor.id,
      title: editor.title,
      markdownContent: editor.markdownContent,
      cover: editor.cover,
      coverUpdatedAt: editor.coverUpdatedAt,
      updatedAt: editor.updatedAt,
      authorName: editor.authorName,
      createdAt: new Date().toISOString()
    };

    const previewWindow = window.open("", "_blank", "width=1280,height=860");
    if (!previewWindow) {
      removeAdminNewsPreview(previewId);
      notify.warning(t("messages.previewBlocked"));
      return;
    }

    saveAdminNewsPreview(previewPayload);
    previewWindow.location.href = `/sys-admin/news-preview/${previewId}`;
  }

  function insertMarkdownAtCursor(markdownSnippet: string) {
    const textarea = findMarkdownTextarea();
    const currentContent = editor.markdownContent || "";
    const start = textarea?.selectionStart ?? selectionRef.current.start ?? currentContent.length;
    const end = textarea?.selectionEnd ?? selectionRef.current.end ?? start;
    const nextContent = `${currentContent.slice(0, start)}${markdownSnippet}${currentContent.slice(end)}`;
    const nextCursor = start + markdownSnippet.length;

    setEditor((current) => ({
      ...current,
      markdownContent: nextContent
    }));
    selectionRef.current = { start: nextCursor, end: nextCursor };

    window.requestAnimationFrame(() => {
      const nextTextarea = findMarkdownTextarea();
      if (!nextTextarea) {
        return;
      }
      nextTextarea.focus();
      nextTextarea.setSelectionRange(nextCursor, nextCursor);
    });
  }

  function findMarkdownTextarea() {
    return markdownShellRef.current?.querySelector("textarea") ?? null;
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
}
