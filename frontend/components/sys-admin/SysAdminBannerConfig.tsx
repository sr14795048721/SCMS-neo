"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { I18nNamespace } from "../../lib/i18n/types";
import { useNotify } from "../../lib/notify/useNotify";
import { useConfirm } from "../../lib/notify/useConfirm";
import { AdminBannerItem, AdminBannerRecompressResult, BannerMediaType } from "../../lib/banner/types";
import styles from "../../styles/sysAdminHomeConfig.module.css";

const IMAGE_MAX_SIZE_BYTES = 10 * 1024 * 1024;
const VIDEO_MAX_SIZE_BYTES = 200 * 1024 * 1024;

type SysAdminBannerConfigProps = {
  namespace: Extract<I18nNamespace, "adminHomeConfig" | "adminLoginConfig">;
  adminApiBase: string;
  previewTarget: string;
  headerIconClass: string;
};

type RecompressResponse = {
  success?: boolean;
  data?: AdminBannerRecompressResult | null;
  code?: string;
  message?: string;
};

export function SysAdminBannerConfig({
  namespace,
  adminApiBase,
  previewTarget,
  headerIconClass
}: SysAdminBannerConfigProps) {
  const tCommon = useT("common");
  const t = useT(namespace);
  const tPortal = useT("portal");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [items, setItems] = useState<AdminBannerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [recompressing, setRecompressing] = useState(false);
  const [ordering, setOrdering] = useState(false);
  const [busyIds, setBusyIds] = useState<number[]>([]);
  const [localPreviewMap, setLocalPreviewMap] = useState<
    Record<number, { mediaUrl: string; posterUrl: string | null; type: BannerMediaType }>
  >({});
  const [previewVersionMap, setPreviewVersionMap] = useState<Record<number, number>>({});

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadItems();
  }, [adminApiBase, authorized]);

  useEffect(
    () => () => {
      Object.values(localPreviewMap).forEach((item) => {
        URL.revokeObjectURL(item.mediaUrl);
        if (item.posterUrl) {
          URL.revokeObjectURL(item.posterUrl);
        }
      });
    },
    [localPreviewMap]
  );

  if (checking || loading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : t("page.loading")}</p>
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
              <i className={headerIconClass} />
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
              onClick={() => window.open(previewTarget, "_blank", "noopener,noreferrer")}
            >
              <i className="fas fa-eye" />
              {t("actions.preview")}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => {
                void handleRecompress();
              }}
              disabled={recompressing}
            >
              <i className="fas fa-wand-magic-sparkles" />
              {recompressing ? t("actions.recompressing") : t("actions.recompress")}
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => {
                void createBanner();
              }}
              disabled={creating}
            >
              <i className="fas fa-plus" />
              {creating ? t("actions.creating") : t("actions.add")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.card}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("section.title")}</h2>
              <p>{t("section.description")}</p>
            </div>
          </div>

          {!items.length ? (
            <div className={styles.emptyState}>
              <i className="fas fa-images" />
              <h3>{t("empty.title")}</h3>
              <p>{t("empty.description")}</p>
            </div>
          ) : (
            <div className={styles.list}>
              {items.map((item, index) => {
                const busy = ordering || busyIds.includes(item.id);
                const preview = localPreviewMap[item.id];
                const mediaSrc = preview ? preview.mediaUrl : withPreviewVersion(item.src, previewVersionMap[item.id]);
                const posterSrc = preview
                  ? preview.posterUrl
                  : withPreviewVersion(item.poster, previewVersionMap[item.id]);
                return (
                  <article key={item.id} className={styles.bannerCard}>
                    <div className={styles.bannerTop}>
                      <div className={styles.orderBadge}>
                        <span>{t("fields.order")}</span>
                        <strong>{index + 1}</strong>
                      </div>
                      <div className={styles.inlineActions}>
                        <button
                          type="button"
                          className={styles.iconButton}
                          onClick={() => {
                            void reorderBanner(index, -1);
                          }}
                          disabled={busy || index === 0}
                          aria-label={t("actions.moveUp")}
                        >
                          <i className="fas fa-arrow-up" />
                        </button>
                        <button
                          type="button"
                          className={styles.iconButton}
                          onClick={() => {
                            void reorderBanner(index, 1);
                          }}
                          disabled={busy || index === items.length - 1}
                          aria-label={t("actions.moveDown")}
                        >
                          <i className="fas fa-arrow-down" />
                        </button>
                        <button
                          type="button"
                          className={`${styles.iconButton} ${styles.dangerButton}`}
                          onClick={() => {
                            void deleteBanner(item.id);
                          }}
                          disabled={busy}
                          aria-label={t("actions.delete")}
                        >
                          <i className="fas fa-trash" />
                        </button>
                      </div>
                    </div>

                    <div className={styles.bannerGrid}>
                      <div className={styles.previewPanel}>
                        {item.hasMedia || preview ? (
                          item.type === "video" ? (
                            posterSrc ? (
                              <img src={posterSrc} alt={item.title || t("preview.videoAlt")} className={styles.previewImage} />
                            ) : (
                              <video src={mediaSrc || ""} controls muted preload="metadata" className={styles.previewVideo} />
                            )
                          ) : (
                            <img src={mediaSrc || ""} alt={item.title || t("preview.imageAlt")} className={styles.previewImage} />
                          )
                        ) : (
                          <div className={styles.previewEmpty}>
                            <i className={`fas ${item.type === "video" ? "fa-film" : "fa-image"}`} />
                            <p>{t("preview.empty")}</p>
                          </div>
                        )}
                        {item.type === "video" && posterSrc && (
                          <span className={styles.previewHint}>{t("preview.posterReady")}</span>
                        )}
                      </div>

                      <div className={styles.formPanel}>
                        <label className={styles.field}>
                          <span>{t("fields.title")}</span>
                          <input
                            value={item.title}
                            onChange={(event) => updateLocalItem(item.id, { title: event.target.value })}
                            onBlur={(event) => {
                              void patchBanner(item.id, { title: event.target.value });
                            }}
                            disabled={busy}
                            maxLength={180}
                          />
                        </label>

                        <label className={styles.field}>
                          <span>{t("fields.desc")}</span>
                          <textarea
                            value={item.desc}
                            onChange={(event) => updateLocalItem(item.id, { desc: event.target.value })}
                            onBlur={(event) => {
                              void patchBanner(item.id, { desc: event.target.value });
                            }}
                            disabled={busy}
                            rows={4}
                            maxLength={1000}
                          />
                        </label>

                        <label className={styles.field}>
                          <span>{t("fields.type")}</span>
                          <select
                            value={item.type}
                            onChange={(event) => {
                              void changeType(item, event.target.value === "video" ? "video" : "image");
                            }}
                            disabled={busy}
                          >
                            <option value="image">{t("types.image")}</option>
                            <option value="video">{t("types.video")}</option>
                          </select>
                        </label>

                        <div className={styles.field}>
                          <span>{t("fields.media")}</span>
                          <div className={styles.uploadRow}>
                            <label className={styles.uploadButton}>
                              <i className="fas fa-upload" />
                              {item.hasMedia ? t("actions.replaceFile") : t("actions.uploadFile")}
                              <input
                                type="file"
                                hidden
                                accept={item.type === "video" ? "video/mp4,video/webm,video/ogg" : "image/jpeg,image/png,image/webp"}
                                disabled={busy}
                                onChange={(event) => {
                                  const file = event.target.files?.[0];
                                  event.currentTarget.value = "";
                                  if (file) {
                                    void uploadFile(item, file);
                                  }
                                }}
                              />
                            </label>
                            <span className={styles.uploadHint}>
                              {item.type === "video" ? t("upload.videoLimit") : t("upload.imageLimit")}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      </section>
    </main>
  );

  async function loadItems() {
    setLoading(true);
    const response = await fetchWithAuth(adminApiBase, { method: "GET" });
    if (!response) {
      setLoading(false);
      return;
    }

    const result = (await response.json().catch(() => null)) as
      | { success?: boolean; items?: AdminBannerItem[] }
      | null;

    if (!(await handleAuthStatus(response.status))) {
      setLoading(false);
      return;
    }

    if (!response.ok || result?.success !== true || !Array.isArray(result.items)) {
      notify.error(t("messages.loadFailed"));
      setLoading(false);
      return;
    }

    setItems(result.items);
    setLoading(false);
  }

  async function createBanner() {
    setCreating(true);
    try {
      const response = await fetchWithAuth(adminApiBase, { method: "POST" });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; item?: AdminBannerItem }
        | null;

      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(t("messages.createFailed"));
        return;
      }

      setItems((current) => [...current, result.item as AdminBannerItem]);
      notify.success(t("messages.createSuccess"));
    } finally {
      setCreating(false);
    }
  }

  async function handleRecompress() {
    if (recompressing) {
      return;
    }

    const accepted = await confirm.confirm({
      title: t("confirm.recompressTitle"),
      message: t("confirm.recompress"),
      confirmText: t("confirm.recompressAction"),
      cancelText: tCommon("action.cancel"),
      tone: "primary"
    });
    if (!accepted) {
      return;
    }

    setRecompressing(true);
    try {
      const response = await fetchWithAuth(`${adminApiBase}/recompress`, {
        method: "POST"
      });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as RecompressResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.recompressFailed"));
        return;
      }

      const summary = result.data;
      if (summary.failedCount > 0) {
        notify.warning(
          t("messages.recompressPartial", {
            processedCount: summary.processedCount,
            skippedCount: summary.skippedCount,
            failedCount: summary.failedCount
          })
        );
        return;
      }

      if (summary.processedCount > 0) {
        notify.success(
          t("messages.recompressSuccess", {
            processedCount: summary.processedCount,
            skippedCount: summary.skippedCount
          })
        );
        return;
      }

      notify.info(
        t("messages.recompressNoop", {
          skippedCount: summary.skippedCount
        })
      );
    } finally {
      setRecompressing(false);
    }
  }

  async function patchBanner(
    id: number,
    payload: Partial<Pick<AdminBannerItem, "title" | "desc">> & { type?: BannerMediaType }
  ) {
    startBusy(id);
    try {
      const response = await fetchWithAuth(`${adminApiBase}/${id}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; item?: AdminBannerItem }
        | null;

      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(t("messages.updateFailed"));
        void loadItems();
        return;
      }

      replaceItem(result.item as AdminBannerItem);
    } finally {
      endBusy(id);
    }
  }

  async function changeType(item: AdminBannerItem, nextType: BannerMediaType) {
    if (item.type === nextType) {
      return;
    }

    if (item.hasMedia) {
      const accepted = await confirm.confirm({
        title: t("confirm.changeTypeTitle"),
        message: t("confirm.changeType"),
        confirmText: tCommon("action.confirm"),
        cancelText: tCommon("action.cancel"),
        tone: "danger"
      });
      if (!accepted) {
        return;
      }
    }

    startBusy(item.id);
    try {
      const response = await fetchWithAuth(`${adminApiBase}/${item.id}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ type: nextType })
      });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; item?: AdminBannerItem }
        | null;

      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(t("messages.typeChangeFailed"));
        clearLocalPreview(item.id);
        void loadItems();
        return;
      }

      replaceItem(result.item as AdminBannerItem);
      clearLocalPreview(item.id);
      notify.success(t("messages.typeChangeSuccess"));
    } finally {
      endBusy(item.id);
    }
  }

  async function reorderBanner(index: number, delta: -1 | 1) {
    const targetIndex = index + delta;
    if (targetIndex < 0 || targetIndex >= items.length) {
      return;
    }

    const nextItems = [...items];
    const [moved] = nextItems.splice(index, 1);
    nextItems.splice(targetIndex, 0, moved);
    setItems(nextItems);
    setOrdering(true);

    try {
      const response = await fetchWithAuth(`${adminApiBase}/order`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          ids: nextItems.map((current) => current.id)
        })
      });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; items?: AdminBannerItem[] }
        | null;

      if (!response.ok || result?.success !== true || !Array.isArray(result.items)) {
        notify.error(t("messages.reorderFailed"));
        void loadItems();
        return;
      }

      setItems(result.items);
    } finally {
      setOrdering(false);
    }
  }

  async function deleteBanner(id: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteTitle"),
      message: t("confirm.delete"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    startBusy(id);
    try {
      const response = await fetchWithAuth(`${adminApiBase}/${id}`, {
        method: "DELETE"
      });
      if (!response || !(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as { success?: boolean } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(t("messages.deleteFailed"));
        return;
      }

      clearLocalPreview(id);
      setItems((current) =>
        current.filter((item) => item.id !== id).map((item, index) => ({ ...item, sortOrder: index + 1 }))
      );
      notify.success(t("messages.deleteSuccess"));
    } finally {
      endBusy(id);
    }
  }

  async function uploadFile(item: AdminBannerItem, file: File) {
    if (!validateFile(item.type, file)) {
      return;
    }

    setLocalPreview(item.id, item.type, file);

    const formData = new FormData();
    formData.append("file", file);

    startBusy(item.id);
    try {
      const response = await authorizedFetch(`${adminApiBase}/${item.id}/file`, {
        method: "POST",
        body: formData
      });
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; item?: AdminBannerItem }
        | null;

      if (!response.ok || result?.success !== true || !result.item) {
        notify.error(t("messages.uploadFailed"));
        clearLocalPreview(item.id);
        return;
      }

      bumpPreviewVersion(item.id);
      replaceItem(result.item as AdminBannerItem);
      clearLocalPreview(item.id);
      notify.success(t("messages.uploadSuccess"));
    } finally {
      endBusy(item.id);
    }
  }

  function updateLocalItem(id: number, patch: Partial<AdminBannerItem>) {
    setItems((current) => current.map((item) => (item.id === id ? { ...item, ...patch } : item)));
  }

  function replaceItem(nextItem: AdminBannerItem) {
    setItems((current) =>
      current
        .map((item) => (item.id === nextItem.id ? nextItem : item))
        .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
    );
  }

  function setLocalPreview(id: number, type: BannerMediaType, file: File) {
    setLocalPreviewMap((current) => {
      const existing = current[id];
      if (existing) {
        URL.revokeObjectURL(existing.mediaUrl);
        if (existing.posterUrl) {
          URL.revokeObjectURL(existing.posterUrl);
        }
      }

      return {
        ...current,
        [id]: {
          mediaUrl: URL.createObjectURL(file),
          posterUrl: null,
          type
        }
      };
    });
  }

  function clearLocalPreview(id: number) {
    setLocalPreviewMap((current) => {
      const existing = current[id];
      if (!existing) {
        return current;
      }

      URL.revokeObjectURL(existing.mediaUrl);
      if (existing.posterUrl) {
        URL.revokeObjectURL(existing.posterUrl);
      }

      const next = { ...current };
      delete next[id];
      return next;
    });
  }

  function bumpPreviewVersion(id: number) {
    setPreviewVersionMap((current) => ({ ...current, [id]: Date.now() }));
  }

  function startBusy(id: number) {
    setBusyIds((current) => (current.includes(id) ? current : [...current, id]));
  }

  function endBusy(id: number) {
    setBusyIds((current) => current.filter((item) => item !== id));
  }

  function validateFile(type: BannerMediaType, file: File) {
    if (type === "image") {
      if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
        notify.error(t("validation.imageType"));
        return false;
      }
      if (file.size > IMAGE_MAX_SIZE_BYTES) {
        notify.error(t("validation.imageSize"));
        return false;
      }
      return true;
    }

    if (!["video/mp4", "video/webm", "video/ogg"].includes(file.type)) {
      notify.error(t("validation.videoType"));
      return false;
    }
    if (file.size > VIDEO_MAX_SIZE_BYTES) {
      notify.error(t("validation.videoSize"));
      return false;
    }
    return true;
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

  async function fetchWithAuth(input: string, init?: RequestInit) {
    const response = await authorizedFetch(input, {
      ...init,
      cache: "no-store"
    });
    if (!response) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return null;
    }
    return response;
  }
}

function withPreviewVersion(value: string | undefined, version: number | undefined) {
  if (!value) {
    return "";
  }
  if (!version) {
    return value;
  }
  const separator = value.includes("?") ? "&" : "?";
  return `${value}${separator}v=${version}`;
}
