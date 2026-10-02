"use client";

import { ChangeEvent, FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { clearPersistedAuthTokens } from "../../lib/auth/session";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { PortalShell } from "../portal/PortalShell";
import { ProfileAvatarEditor } from "../profile/ProfileAvatarEditor";
import {
  changeStudentPasswordRequest,
  fetchStudentAvatarRequest,
  fetchStudentInfoRequest,
  updateStudentInfoRequest,
  uploadStudentAvatarRequest
} from "../../lib/student/client";
import {
  clearCachedStudentAvatar,
  readCachedStudentAvatar,
  writeCachedStudentAvatar
} from "../../lib/student/avatarCache";
import {
  clearCachedStudentProfile,
  readCachedStudentProfile,
  writeCachedStudentProfile
} from "../../lib/student/profileCache";
import {
  MAX_STUDENT_CLASS_DIGITS,
  normalizeStudentClassName,
  normalizeStudentGrade,
  resolveStudentClassNameInput
} from "../../lib/student/fieldConstraints";
import { StudentProfileRequiredField } from "../../lib/student/profileCompletion";
import {
  ChangeStudentPasswordPayload,
  StudentInfo,
  UpdateStudentInfoPayload
} from "../../lib/student/types";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/studentProfile.module.css";

type StudentInfoResponse = {
  success?: boolean;
  data?: StudentInfo;
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
const STUDENT_GRADE_OPTIONS = [
  { value: "HIGH_1", labelKey: "gradeOptions.HIGH_1" },
  { value: "HIGH_2", labelKey: "gradeOptions.HIGH_2" },
  { value: "HIGH_3", labelKey: "gradeOptions.HIGH_3" }
] as const;

export function StudentProfilePage() {
  const t = useT("studentProfile");
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("STUDENT");
  const editorObjectUrlRef = useRef<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [studentInfo, setStudentInfo] = useState<StudentInfo | null>(() => readCachedStudentProfile());
  const [avatarUrl, setAvatarUrl] = useState<string | null>(() => {
    const cachedProfile = readCachedStudentProfile();
    return cachedProfile?.hasAvatar ? readCachedStudentAvatar(cachedProfile) : null;
  });
  const [profileForm, setProfileForm] = useState<UpdateStudentInfoPayload>(() => {
    const cachedProfile = readCachedStudentProfile();
    return {
      displayName: cachedProfile?.displayName || "",
      studentNo: cachedProfile?.studentNo || "",
      grade: normalizeStudentGrade(cachedProfile?.grade),
      className: normalizeStudentClassName(cachedProfile?.className),
      phone: cachedProfile?.phone || "",
      bio: cachedProfile?.bio || ""
    };
  });
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);
  const [profileSubmitting, setProfileSubmitting] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [avatarSubmitting, setAvatarSubmitting] = useState(false);
  const [editorState, setEditorState] = useState<EditorState | null>(null);
  const [passwordForm, setPasswordForm] = useState<ChangeStudentPasswordPayload>({
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

      const response = await fetchStudentInfoRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as StudentInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }

      if (!active) {
        return;
      }

      syncStudentInfo(result.data);
      writeCachedStudentProfile(result.data);
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
  const completionMissingFields = (studentInfo?.missingRequiredFields || []) as StudentProfileRequiredField[];
  const profileLocked = Boolean(studentInfo && !studentInfo.profileCompleted);

  return (
    <>
      <PortalShell
        leading={
          <button type="button" className={styles.backButton} onClick={() => router.push("/student")}>
            <i className="fas fa-arrow-left" />
            {t("header.back")}
          </button>
        }
        profileName={profileName}
        profileHint={t("header.profileHint")}
        avatarUrl={avatarUrl}
        profileLoading={!studentInfo}
        onProfileClick={() => router.push("/student/profile")}
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

            {profileLocked ? (
              <section className={styles.noticeCard}>
                <div className={styles.noticeHead}>
                  <div>
                    <h3 className={styles.noticeTitle}>{t("completion.title")}</h3>
                    <p className={styles.noticeDescription}>{t("completion.description")}</p>
                  </div>
                  <span className={styles.noticeBadge}>{t("completion.badge")}</span>
                </div>
                <div className={styles.noticeFields}>
                  <strong>{t("completion.pendingLabel")}</strong>
                  <div className={styles.noticeFieldList}>
                    {completionMissingFields.map((field) => (
                      <span key={field} className={styles.noticeFieldTag}>
                        {t(`completion.requiredFields.${field}`)}
                      </span>
                    ))}
                  </div>
                </div>
              </section>
            ) : null}

            <div className={styles.heroPanel}>
              <div className={styles.avatarSection}>
                <div className={`${styles.avatarWrap} ${avatarUrl ? styles.avatarWrapImage : styles.avatarWrapFallback}`}>
                  {avatarUrl ? (
                    <img src={avatarUrl} alt="" className={styles.avatarImage} />
                  ) : (
                    <span className={styles.avatarFallback}>
                      {(profileName.trim().charAt(0) || "S").toUpperCase()}
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
                <strong className={styles.summaryValue}>{studentInfo?.email || "--"}</strong>
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
                  required
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, displayName: event.target.value }))}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.studentNo")}</span>
                <input
                  type="text"
                  value={profileForm.studentNo}
                  placeholder={t("placeholders.studentNo")}
                  className={styles.input}
                  required
                  onChange={(event) => setProfileForm((prev) => ({ ...prev, studentNo: event.target.value }))}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.grade")}</span>
                <select
                  value={profileForm.grade}
                  className={`${styles.input} ${styles.select}`}
                  required
                  onChange={(event) =>
                    setProfileForm((prev) => ({
                      ...prev,
                      grade: normalizeStudentGrade(event.target.value)
                    }))
                  }
                >
                  <option value="">{t("placeholders.grade")}</option>
                  {STUDENT_GRADE_OPTIONS.map((grade) => (
                    <option key={grade.value} value={grade.value}>
                      {t(grade.labelKey)}
                    </option>
                  ))}
                </select>
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.className")}</span>
                <div className={styles.inputGroup}>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={profileForm.className}
                    placeholder={t("placeholders.className")}
                    className={`${styles.input} ${styles.inputWithSuffix}`}
                    maxLength={MAX_STUDENT_CLASS_DIGITS}
                    required
                    onChange={(event) =>
                      setProfileForm((prev) => ({
                        ...prev,
                        className: resolveStudentClassNameInput(event.target.value, prev.className)
                      }))
                    }
                  />
                  <span className={styles.inputSuffix}>{t("helper.classSuffix")}</span>
                </div>
              </label>

              <label className={styles.field}>
                <span className={styles.label}>{t("labels.email")}</span>
                <input type="email" value={studentInfo?.email || ""} className={styles.input} readOnly />
              </label>

              <label className={styles.field}>
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
          namespace="studentProfile"
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

    const nextGrade = normalizeStudentGrade(profileForm.grade);
    const nextClassName = normalizeStudentClassName(profileForm.className);
    const nextDisplayName = profileForm.displayName.trim();
    const nextStudentNo = profileForm.studentNo.trim();

    if (!nextDisplayName) {
      notify.warning(t("messages.displayNameRequired"));
      return;
    }

    if (!nextStudentNo) {
      notify.warning(t("messages.studentNoRequired"));
      return;
    }

    if (!nextGrade) {
      notify.warning(t("messages.gradeRequired"));
      return;
    }

    if (!profileForm.className.trim()) {
      notify.warning(t("messages.classNameRequired"));
      return;
    }

    if (!nextClassName) {
      notify.warning(t("messages.invalidClassName"));
      return;
    }

    const wasIncomplete = Boolean(studentInfo && !studentInfo.profileCompleted);

    setProfileSubmitting(true);
    try {
      const response = await updateStudentInfoRequest({
        ...profileForm,
        displayName: nextDisplayName,
        studentNo: nextStudentNo,
        grade: nextGrade,
        className: nextClassName
      });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as StudentInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.requestFailed"));
        return;
      }

      syncStudentInfo(result.data);
      notify.success(
        wasIncomplete && result.data.profileCompleted ? t("messages.profileCompletedUnlocked") : t("messages.profileSaved")
      );
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
      const response = await changeStudentPasswordRequest(passwordForm);
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
      formData.append("file", new File([blob], "student-avatar.png", { type: "image/png" }));

      const response = await uploadStudentAvatarRequest(formData);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as StudentInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.requestFailed"));
        return;
      }

      syncStudentInfo(result.data);
      await loadAvatar(result.data);
      closeEditor();
      notify.success(t("messages.avatarSaved"));
    } finally {
      setAvatarSubmitting(false);
    }
  }

  async function loadAvatar(profile: StudentInfo) {
    if (!profile.hasAvatar) {
      clearCachedStudentAvatar(profile.userId);
      setAvatarUrl(null);
      return;
    }

    const cachedAvatar = readCachedStudentAvatar(profile);
    if (cachedAvatar) {
      setAvatarUrl(cachedAvatar);
      return;
    }

    const response = await fetchStudentAvatarRequest(profile.avatarUpdatedAt);
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
    setAvatarUrl(await writeCachedStudentAvatar(profile, blob));
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
      clearCachedStudentProfile();
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return false;
    }

    if (status === 403) {
      clearCachedStudentProfile();
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return false;
    }

    return true;
  }

  function handleLogout() {
    if (logoutSubmitting) {
      return;
    }
    setLogoutSubmitting(true);
    if (studentInfo?.userId) {
      clearCachedStudentAvatar(studentInfo.userId);
    }
    clearCachedStudentProfile();
    clearPersistedAuthTokens();
    router.replace("/login");
  }

  function syncStudentInfo(nextProfile: StudentInfo) {
    const normalizedGrade = normalizeStudentGrade(nextProfile.grade);
    const normalizedClassName = normalizeStudentClassName(nextProfile.className);
    const normalizedProfile: StudentInfo = {
      ...nextProfile,
      grade: normalizedGrade,
      className: normalizedClassName
    };

    setStudentInfo(normalizedProfile);
    setProfileForm({
      displayName: nextProfile.displayName || "",
      studentNo: nextProfile.studentNo || "",
      grade: normalizedGrade,
      className: normalizedClassName,
      phone: nextProfile.phone || "",
      bio: nextProfile.bio || ""
    });
    writeCachedStudentProfile(normalizedProfile);
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
