"use client";

import { ChangeEvent, FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { clearPersistedAuthTokens } from "../../lib/auth/session";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import {
  clearCachedManagerAvatar,
  readCachedManagerAvatar,
  writeCachedManagerAvatar
} from "../../lib/manager/avatarCache";
import {
  clearCachedManagerProfile,
  readCachedManagerProfile,
  writeCachedManagerProfile
} from "../../lib/manager/profileCache";
import { useNotify } from "../../lib/notify/useNotify";
import {
  changeManagerPasswordRequest,
  fetchManagerAvatarRequest,
  fetchManagerInfoRequest,
  updateManagerInfoRequest,
  uploadManagerAvatarRequest
} from "../../lib/manager/client";
import { ChangeManagerPasswordPayload, ManagerInfo, UpdateManagerInfoPayload } from "../../lib/manager/types";
import { PortalShell } from "../portal/PortalShell";
import { ProfileAvatarEditor } from "../profile/ProfileAvatarEditor";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherProfile.module.css";

type ManagerInfoResponse = {
  success?: boolean;
  data?: ManagerInfo;
  message?: string;
};

type PasswordResponse = {
  success?: boolean;
  message?: string;
};

type EditorState = {
  file: File;
  previewUrl: string;
};

const ACCEPTED_AVATAR_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/jpg"]);
const MAX_AVATAR_SIZE_BYTES = 10 * 1024 * 1024;

export function TeacherProfilePage() {
  const t = useT("teacherProfile");
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("CLUB_MANAGER");
  const editorObjectUrlRef = useRef<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [managerInfo, setManagerInfo] = useState<ManagerInfo | null>(() => readCachedManagerProfile());
  const [avatarUrl, setAvatarUrl] = useState<string | null>(() => {
    const cachedProfile = readCachedManagerProfile();
    return cachedProfile?.hasAvatar ? readCachedManagerAvatar(cachedProfile) : null;
  });
  const [profileForm, setProfileForm] = useState<UpdateManagerInfoPayload>(() => {
    const cachedProfile = readCachedManagerProfile();
    return {
      displayName: cachedProfile?.displayName || "",
      managerNo: cachedProfile?.managerNo || "",
      phone: cachedProfile?.phone || "",
      bio: cachedProfile?.bio || ""
    };
  });

  const [logoutSubmitting, setLogoutSubmitting] = useState(false);
  const [profileSubmitting, setProfileSubmitting] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [avatarSubmitting, setAvatarSubmitting] = useState(false);
  const [editorState, setEditorState] = useState<EditorState | null>(null);
  const [passwordForm, setPasswordForm] = useState<ChangeManagerPasswordPayload>({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });

  useEffect(() => {
    return () => {
      revokeEditorUrl();
    };
  }, []);

  useEffect(() => {
    let active = true;

    const loadProfile = async () => {
      if (!authorized) {
        return;
      }

      const response = await fetchManagerInfoRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ManagerInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }

      if (!active) {
        return;
      }

      syncManagerInfo(result.data);
      writeCachedManagerProfile(result.data);
      await loadAvatar(result.data);
    };

    void loadProfile();
    return () => {
      active = false;
    };
  }, [authorized, notify, router, t, tPortal]);

  if (checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{tPortal("auth.checking")}</p>
        </section>
      </main>
    );
  }

  if (!authorized || redirecting) {
    return null;
  }

  const profileName = profileForm.displayName.trim() || t("header.defaultName");

  return (
    <>
      <PortalShell
        leading={
          <button type="button" className={styles.backButton} onClick={() => router.push("/club-admin")}>
            <i className="fas fa-arrow-left" />
            {t("header.back")}
          </button>
        }
        profileName={profileName}
        profileHint={t("header.profileHint")}
        avatarUrl={avatarUrl}
        profileLoading={!managerInfo}
        onProfileClick={() => router.push("/club-admin/profile")}
        logoutLabel={t("header.logout")}
        logoutPendingLabel={t("header.loggingOut")}
        logoutSubmitting={logoutSubmitting}
        onLogout={handleLogout}
      >
        <section className={styles.grid}>
          <article className={`${styles.card} ${styles.profileCard}`}>
            <div className={styles.cardHead}>
              <div>
                <h2 className={styles.cardTitle}>{t("profile.title")}</h2>
                <p className={styles.cardDescription}>{t("profile.description")}</p>
              </div>
            </div>

            <div className={styles.heroPanel}>
              <div className={styles.avatarSection}>
                <div className={`${styles.avatarWrap} ${avatarUrl ? styles.avatarWrapImage : styles.avatarWrapFallback}`}>
                  {avatarUrl ? (
                    <img src={avatarUrl} alt="" className={styles.avatarImage} />
                  ) : (
                    <span className={styles.avatarFallback}>
                      {(profileName.trim().charAt(0) || "T").toUpperCase()}
                    </span>
                  )}
                </div>
                <div className={styles.avatarCopy}>
                  <h3 className={styles.heroTitle}>{t("profile.avatarTitle")}</h3>
                  <p className={styles.heroText}>{t("profile.avatarHint")}</p>
                  <button
                    type="button"
                    className={styles.avatarButton}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={avatarSubmitting}
                  >
                    <i className="fas fa-camera" />
                    {t("profile.avatarEdit")}
                  </button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/jpg"
                    hidden
                    onChange={handleAvatarFileChange}
                  />
                </div>
              </div>

              <div className={styles.summaryCard}>
                <span className={styles.summaryLabel}>{t("labels.email")}</span>
                <strong className={styles.summaryValue}>{managerInfo?.email || "--"}</strong>
                <p className={styles.summaryHint}>{t("helper.emailReadonly")}</p>
              </div>
            </div>

            <form className={styles.profileForm} onSubmit={handleProfileSubmit}>
              <label className={styles.field}>
                <span className={styles.label}>{t("labels.displayName")}</span>
                <input
                  type="text"
                  value={profileForm.displayName}
                  placeholder={t("placeholders.displayName")}
                  className={styles.input}
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, displayName: event.target.value }))}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.managerNo")}</span>
                <input
                  type="text"
                  value={profileForm.managerNo}
                  placeholder={t("placeholders.managerNo")}
                  className={styles.input}
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, managerNo: event.target.value }))}
                />
              </label>

              <label className={`${styles.field} ${styles.fieldFull}`}>
                <span className={styles.label}>{t("labels.email")}</span>
                <input type="email" value={managerInfo?.email || ""} className={styles.input} readOnly />
              </label>

              <label className={`${styles.field} ${styles.fieldFull}`}>
                <span className={styles.label}>{t("labels.phone")}</span>
                <input
                  type="tel"
                  value={profileForm.phone}
                  placeholder={t("placeholders.phone")}
                  className={styles.input}
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, phone: event.target.value }))}
                />
              </label>

              <label className={`${styles.field} ${styles.fieldFull}`}>
                <span className={styles.label}>{t("labels.bio")}</span>
                <textarea
                  value={profileForm.bio}
                  placeholder={t("placeholders.bio")}
                  className={styles.textarea}
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, bio: event.target.value }))}
                />
              </label>

              <div className={styles.actions}>
                <button type="submit" className={styles.primaryButton} disabled={profileSubmitting}>
                  <i className="fas fa-floppy-disk" />
                  {profileSubmitting ? t("profile.saving") : t("profile.save")}
                </button>
              </div>
            </form>
          </article>

          <aside className={`${styles.card} ${styles.securityCard}`}>
            <div className={styles.cardHead}>
              <div>
                <h2 className={styles.cardTitle}>{t("security.title")}</h2>
                <p className={styles.cardDescription}>{t("security.description")}</p>
              </div>
            </div>

            <form className={styles.securityForm} onSubmit={handlePasswordSubmit}>
              <label className={styles.field}>
                <span className={styles.label}>{t("labels.currentPassword")}</span>
                <input
                  type="password"
                  value={passwordForm.currentPassword}
                  placeholder={t("placeholders.currentPassword")}
                  className={styles.input}
                  onChange={(event) =>
                    setPasswordForm((prev) => ({ ...prev, currentPassword: event.target.value }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.newPassword")}</span>
                <input
                  type="password"
                  value={passwordForm.newPassword}
                  placeholder={t("placeholders.newPassword")}
                  className={styles.input}
                  onChange={(event) => setPasswordForm((prev) => ({ ...prev, newPassword: event.target.value }))}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.confirmPassword")}</span>
                <input
                  type="password"
                  value={passwordForm.confirmPassword}
                  placeholder={t("placeholders.confirmPassword")}
                  className={styles.input}
                  onChange={(event) =>
                    setPasswordForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
                  }
                />
              </label>

              <div className={styles.actions}>
                <button type="submit" className={styles.primaryButton} disabled={passwordSubmitting}>
                  <i className="fas fa-shield-halved" />
                  {passwordSubmitting ? t("security.saving") : t("security.save")}
                </button>
              </div>
            </form>
          </aside>
        </section>
      </PortalShell>

      {editorState ? (
        <ProfileAvatarEditor
          namespace="teacherProfile"
          file={editorState.file}
          previewUrl={editorState.previewUrl}
          submitting={avatarSubmitting}
          onCancel={() => {
            if (avatarSubmitting) {
              return;
            }
            closeEditor();
          }}
          onChangeFile={(file) => {
            prepareEditor(file);
          }}
          onSubmit={handleAvatarSubmit}
        />
      ) : null}
    </>
  );

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (profileSubmitting) {
      return;
    }

    setProfileSubmitting(true);
    try {
      const response = await updateManagerInfoRequest(profileForm);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ManagerInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.requestFailed"));
        return;
      }

      syncManagerInfo(result.data);
      notify.success(t("messages.profileSaved"));
    } finally {
      setProfileSubmitting(false);
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (passwordSubmitting) {
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      notify.warning(t("messages.passwordMismatch"));
      return;
    }

    setPasswordSubmitting(true);
    try {
      const response = await changeManagerPasswordRequest(passwordForm);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as PasswordResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.requestFailed"));
        return;
      }

      notify.success(t("messages.passwordUpdated"));
      clearPersistedAuthTokens();
      router.replace("/login");
    } finally {
      setPasswordSubmitting(false);
    }
  }

  async function handleAvatarSubmit(blob: Blob) {
    if (avatarSubmitting) {
      return;
    }

    setAvatarSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("file", new File([blob], "manager-avatar.png", { type: "image/png" }));

      const response = await uploadManagerAvatarRequest(formData);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ManagerInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.requestFailed"));
        return;
      }

      syncManagerInfo(result.data);
      await loadAvatar(result.data);
      closeEditor();
      notify.success(t("messages.avatarSaved"));
    } finally {
      setAvatarSubmitting(false);
    }
  }

  async function loadAvatar(profile: ManagerInfo) {
    if (!profile.hasAvatar) {
      clearCachedManagerAvatar(profile.userId);
      setAvatarUrl(null);
      return;
    }

    const cachedAvatar = readCachedManagerAvatar(profile);
    if (cachedAvatar) {
      setAvatarUrl(cachedAvatar);
      return;
    }

    const response = await fetchManagerAvatarRequest(profile.avatarUpdatedAt);
    if (!response) {
      return;
    }

    if (!(await handleAuthStatus(response.status))) {
      return;
    }

    if (!response.ok) {
      setAvatarUrl(null);
      return;
    }

    const blob = await response.blob();
    setAvatarUrl(await writeCachedManagerAvatar(profile, blob));
  }

  function handleAvatarFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }

    prepareEditor(file);
  }

  function prepareEditor(file: File) {
    if (!ACCEPTED_AVATAR_TYPES.has(file.type)) {
      notify.warning(t("messages.invalidImage"));
      return;
    }
    if (file.size > MAX_AVATAR_SIZE_BYTES) {
      notify.warning(t("messages.avatarTooLarge"));
      return;
    }

    revokeEditorUrl();
    const previewUrl = URL.createObjectURL(file);
    editorObjectUrlRef.current = previewUrl;
    setEditorState({ file, previewUrl });
  }

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      clearCachedManagerProfile();
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return false;
    }
    if (status === 403) {
      clearCachedManagerProfile();
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return false;
    }
    return true;
  }

  function syncManagerInfo(next: ManagerInfo) {
    writeCachedManagerProfile(next);
    setManagerInfo(next);
    setProfileForm({
      displayName: next.displayName || "",
      managerNo: next.managerNo || "",
      phone: next.phone || "",
      bio: next.bio || ""
    });
  }

  function handleLogout() {
    if (logoutSubmitting) {
      return;
    }
    setLogoutSubmitting(true);
    if (managerInfo?.userId) {
      clearCachedManagerAvatar(managerInfo.userId);
    }
    clearCachedManagerProfile();
    clearPersistedAuthTokens();
    router.replace("/login");
  }

  function closeEditor() {
    revokeEditorUrl();
    setEditorState(null);
  }
  function revokeEditorUrl() {
    if (editorObjectUrlRef.current) {
      URL.revokeObjectURL(editorObjectUrlRef.current);
      editorObjectUrlRef.current = null;
    }
  }
}
