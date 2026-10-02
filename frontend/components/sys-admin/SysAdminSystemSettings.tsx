"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AdminDirtyDataCleanupResponse,
  changeAdminPasswordRequest,
  cleanAdminDirtyDataRequest
} from "../../lib/admin-system/client";
import { logoutAdminSession } from "../../lib/admin-system/session";
import { AdminDirtyDataCleanupResult, ChangeAdminPasswordPayload } from "../../lib/admin-system/types";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import styles from "../../styles/sysAdminSystemSettings.module.css";

type BasicResponse = {
  success?: boolean;
  code?: string;
  message?: string;
};

const EMPTY_PASSWORD_FORM: ChangeAdminPasswordPayload = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
};

export function SysAdminSystemSettings() {
  const t = useT("adminSystemSettings");
  const tPortal = useT("portal");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate(["ADMIN", "SUPER_ADMIN"]);
  const [passwordForm, setPasswordForm] = useState<ChangeAdminPasswordPayload>(EMPTY_PASSWORD_FORM);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);
  const [cleanupSubmitting, setCleanupSubmitting] = useState(false);
  const [cleanupResult, setCleanupResult] = useState<AdminDirtyDataCleanupResult | null>(null);

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

  const finishLogoutRedirect = async () => {
    const result = await logoutAdminSession();
    if (!result.ok && result.status !== 401 && result.status !== 403) {
      notify.warning(t("messages.logoutFallback"));
    }
    router.replace("/login");
  };

  const handlePasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (passwordSubmitting) {
      return;
    }

    setPasswordSubmitting(true);
    try {
      const response = await changeAdminPasswordRequest(passwordForm);
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as BasicResponse | null;

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        await finishLogoutRedirect();
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.passwordFailed"));
        return;
      }

      notify.success(t("messages.passwordSuccess"));
      setPasswordForm(EMPTY_PASSWORD_FORM);
      await finishLogoutRedirect();
    } finally {
      setPasswordSubmitting(false);
    }
  };

  const handleLogout = async () => {
    if (logoutSubmitting) {
      return;
    }

    const accepted = await confirm.confirm({
      title: t("logout.confirmTitle"),
      message: t("logout.confirmMessage"),
      confirmText: t("logout.confirmAction"),
      cancelText: t("logout.cancelAction"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setLogoutSubmitting(true);
    try {
      await finishLogoutRedirect();
    } finally {
      setLogoutSubmitting(false);
    }
  };

  const handleDirtyDataCleanup = async () => {
    if (cleanupSubmitting) {
      return;
    }

    const accepted = await confirm.confirm({
      title: t("maintenance.confirmTitle"),
      message: t("maintenance.confirmMessage"),
      confirmText: t("maintenance.confirmAction"),
      cancelText: t("maintenance.cancelAction"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setCleanupSubmitting(true);
    try {
      const response = await cleanAdminDirtyDataRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        await finishLogoutRedirect();
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as AdminDirtyDataCleanupResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.cleanupFailed"));
        return;
      }

      setCleanupResult(result.data);
      notify.success(
        result.data.totalRemoved > 0
          ? t("messages.cleanupSuccess", { count: result.data.totalRemoved })
          : t("messages.cleanupNoop")
      );
    } finally {
      setCleanupSubmitting(false);
    }
  };

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerCopy}>
            <h1>
              <i className="fas fa-cog" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
            <i className="fas fa-arrow-left" />
            {t("actions.back")}
          </button>
        </div>
      </header>

      <section className={styles.container}>
        <article className={styles.card}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("password.title")}</h2>
              <p>{t("password.description")}</p>
            </div>
            <span className={styles.badge}>{t("password.badge")}</span>
          </div>

          <form className={styles.form} onSubmit={handlePasswordSubmit}>
            <label className={styles.field}>
              <span>{t("password.fields.currentPassword")}</span>
              <input
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) =>
                  setPasswordForm((prev) => ({ ...prev, currentPassword: event.target.value }))
                }
                placeholder={t("password.placeholders.currentPassword")}
                className={styles.input}
                autoComplete="current-password"
              />
            </label>
            <label className={styles.field}>
              <span>{t("password.fields.newPassword")}</span>
              <input
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) => setPasswordForm((prev) => ({ ...prev, newPassword: event.target.value }))}
                placeholder={t("password.placeholders.newPassword")}
                className={styles.input}
                autoComplete="new-password"
              />
            </label>
            <label className={styles.field}>
              <span>{t("password.fields.confirmPassword")}</span>
              <input
                type="password"
                value={passwordForm.confirmPassword}
                onChange={(event) =>
                  setPasswordForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
                }
                placeholder={t("password.placeholders.confirmPassword")}
                className={styles.input}
                autoComplete="new-password"
              />
            </label>
            <div className={styles.formFooter}>
              <p>{t("password.hint")}</p>
              <button type="submit" className={styles.primaryButton} disabled={passwordSubmitting}>
                <i className="fas fa-key" />
                {passwordSubmitting ? t("password.submitting") : t("password.submit")}
              </button>
            </div>
          </form>
        </article>

        <article className={styles.card}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("maintenance.title")}</h2>
              <p>{t("maintenance.description")}</p>
            </div>
            <span className={styles.badge}>{t("maintenance.badge")}</span>
          </div>

          <div className={styles.maintenancePanel}>
            <div className={styles.maintenanceCopy}>
              <strong>{t("maintenance.panelTitle")}</strong>
              <p>{t("maintenance.panelHint")}</p>
            </div>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={handleDirtyDataCleanup}
              disabled={cleanupSubmitting}
            >
              <i className="fas fa-broom" />
              {cleanupSubmitting ? t("maintenance.submitting") : t("maintenance.submit")}
            </button>
          </div>

          {cleanupResult ? (
            <div className={styles.cleanupSummary}>
              <strong>{t("maintenance.summaryTitle", { count: cleanupResult.totalRemoved })}</strong>
              <div className={styles.cleanupGrid}>
                <span>{t("maintenance.items.scoreRecords", { count: cleanupResult.scoreRecordsRemoved })}</span>
                <span>{t("maintenance.items.rewardOrders", { count: cleanupResult.rewardOrdersRemoved })}</span>
                <span>{t("maintenance.items.clubJoinRequests", { count: cleanupResult.clubJoinRequestsRemoved })}</span>
                <span>{t("maintenance.items.registrations", { count: cleanupResult.registrationsRemoved })}</span>
                <span>{t("maintenance.items.clubMembers", { count: cleanupResult.clubMembersRemoved })}</span>
                <span>{t("maintenance.items.notifications", { count: cleanupResult.notificationsRemoved })}</span>
                <span>{t("maintenance.items.studentProfiles", { count: cleanupResult.studentProfilesRemoved })}</span>
                <span>{t("maintenance.items.managerProfiles", { count: cleanupResult.managerProfilesRemoved })}</span>
                <span>{t("maintenance.items.clubManagerBindings", { count: cleanupResult.clubManagerBindingsRemoved })}</span>
                <span>{t("maintenance.items.clubDuties", { count: cleanupResult.clubDutiesRemoved })}</span>
                <span>{t("maintenance.items.rewardTargetClubs", { count: cleanupResult.rewardTargetClubsRemoved })}</span>
              </div>
            </div>
          ) : null}
        </article>

        <article className={`${styles.card} ${styles.dangerCard}`}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("logout.title")}</h2>
              <p>{t("logout.description")}</p>
            </div>
            <span className={`${styles.badge} ${styles.dangerBadge}`}>{t("logout.badge")}</span>
          </div>

          <div className={styles.logoutPanel}>
            <div className={styles.logoutCopy}>
              <strong>{t("logout.panelTitle")}</strong>
              <p>{t("logout.panelHint")}</p>
            </div>
            <button type="button" className={styles.dangerButton} onClick={handleLogout} disabled={logoutSubmitting}>
              <i className="fas fa-right-from-bracket" />
              {logoutSubmitting ? t("logout.submitting") : t("logout.submit")}
            </button>
          </div>
        </article>
      </section>
    </main>
  );
}
