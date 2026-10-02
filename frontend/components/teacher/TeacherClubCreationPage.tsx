"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { ClubCreationRequestItem } from "../../lib/manager/teacherWorkflowTypes";
import {
  createManagerClubCreationRequest,
  fetchManagerClubCreationRequests
} from "../../lib/manager/teacherWorkflowClient";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherWorkflows.module.css";

type RequestsResponse = {
  success?: boolean;
  data?: ClubCreationRequestItem[];
};

type MutationResponse = {
  success?: boolean;
  code?: string;
  message?: string;
};

const EMPTY_FORM = {
  name: "",
  type: "",
  description: "",
  applyReason: ""
};

export function TeacherClubCreationPage() {
  const t = useT("teacherClubCreation");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [requests, setRequests] = useState<ClubCreationRequestItem[]>([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const hasPendingRequest = requests.some((item) => item.status === "PENDING");

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadRequests();
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
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button type="button" className={styles.secondaryButton} onClick={() => router.push("/club-admin")}>
            <i className="fas fa-arrow-left" /> {t("actions.back")}
          </button>
        </div>

        <section className={styles.twoColumn}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h2 className={styles.panelTitle}>
                  <i className="fas fa-plus-circle" />
                  {t("form.title")}
                </h2>
                <p className={styles.panelHint}>{t("form.subtitle")}</p>
              </div>
            </div>

            <div className={styles.fieldGrid}>
              <div className={styles.field}>
                <label htmlFor="club-create-name">{t("form.fields.name")}</label>
                <input id="club-create-name" className={styles.input} value={form.name} maxLength={12} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value.slice(0, 12) }))} />
              </div>
              <div className={styles.field}>
                <label htmlFor="club-create-type">{t("form.fields.type")}</label>
                <input id="club-create-type" className={styles.input} value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))} />
              </div>
              <div className={styles.fieldWide}>
                <label htmlFor="club-create-description">{t("form.fields.description")}</label>
                <textarea id="club-create-description" className={styles.textarea} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} />
              </div>
              <div className={styles.fieldWide}>
                <label htmlFor="club-create-reason">{t("form.fields.applyReason")}</label>
                <textarea id="club-create-reason" className={styles.textarea} value={form.applyReason} onChange={(event) => setForm((current) => ({ ...current, applyReason: event.target.value }))} />
              </div>
            </div>

            {hasPendingRequest ? <p className={styles.helperText}>{t("form.pendingHint")}</p> : null}

            <div className={`${styles.actions} ${styles.formActions}`}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSubmit()} disabled={submitting || hasPendingRequest}>
                <i className="fas fa-paper-plane" /> {submitting ? t("actions.submitting") : t("actions.submit")}
              </button>
            </div>
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h2 className={styles.panelTitle}>
                  <i className="fas fa-circle-info" />
                  {t("guide.title")}
                </h2>
                <p className={styles.panelHint}>{t("guide.subtitle")}</p>
              </div>
            </div>
            <div className={styles.list}>
              <div className={styles.card}><p className={styles.cardDescription}>{t("guide.step1")}</p></div>
              <div className={styles.card}><p className={styles.cardDescription}>{t("guide.step2")}</p></div>
              <div className={styles.card}><p className={styles.cardDescription}>{t("guide.step3")}</p></div>
            </div>
          </article>
        </section>

        <article className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h2 className={styles.panelTitle}>
                <i className="fas fa-list-check" />
                {t("history.title")}
              </h2>
              <p className={styles.panelHint}>{t("history.subtitle")}</p>
            </div>
          </div>
          <div className={styles.list}>
            {requests.length ? (
              requests.map((item) => (
                <article key={item.id} className={styles.card}>
                  <div className={styles.cardHead}>
                    <div>
                      <h3 className={styles.cardTitle}>{item.name}</h3>
                      <div className={styles.cardMeta}>
                        <span>{item.type || t("common.empty")}</span>
                        <span>{item.createdAt ? new Date(item.createdAt).toLocaleString("zh-CN") : "-"}</span>
                      </div>
                    </div>
                    <span className={`${styles.tag} ${item.status === "REJECTED" ? styles.tagDanger : item.status === "APPROVED" ? styles.tagMuted : ""}`}>
                      {t(`status.${item.status.toLowerCase()}`)}
                    </span>
                  </div>
                  <p className={styles.cardDescription}>{item.description || t("common.emptyDescription")}</p>
                  <p className={styles.helperText}>{t("history.reason", { reason: item.applyReason })}</p>
                </article>
              ))
            ) : (
              <div className={styles.emptyState}>{t("history.empty")}</div>
            )}
          </div>
        </article>
      </section>
    </PortalShell>
  );

  async function loadRequests() {
    setLoading(true);
    try {
      const response = await fetchManagerClubCreationRequests();
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as RequestsResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(t("messages.loadFailed"));
        return;
      }
      setRequests(Array.isArray(result?.data) ? result.data : []);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit() {
    if (hasPendingRequest) {
      notify.warning(t("messages.pendingBlocked"));
      return;
    }

    if (!form.name.trim() || !form.type.trim() || !form.applyReason.trim()) {
      notify.warning(t("messages.validation"));
      return;
    }
    if (form.name.trim().length > 12) {
      notify.warning(t("messages.nameTooLong"));
      return;
    }

    setSubmitting(true);
    try {
      const response = await createManagerClubCreationRequest(form);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as MutationResponse | null;
      if (!response.ok || result?.success !== true) {
        if (result?.message?.includes("already pending")) {
          notify.warning(t("messages.pendingBlocked"));
          void loadRequests();
          return;
        }
        if (result?.message?.includes("club name already exists")) {
          notify.warning(t("messages.nameConflict"));
          return;
        }
        if (result?.message?.includes("club name too long")) {
          notify.warning(t("messages.nameTooLong"));
          return;
        }
        notify.error(result?.message || t("messages.submitFailed"));
        return;
      }
      notify.success(t("messages.submitSuccess"));
      setForm(EMPTY_FORM);
      void loadRequests();
    } catch {
      notify.error(t("messages.submitFailed"));
    } finally {
      setSubmitting(false);
    }
  }
}
