"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { useNotify } from "../../lib/notify/useNotify";
import { useT } from "../../lib/i18n/useT";
import {
  fetchManagerClubAppWorkspaceProjectDemoRequest,
  saveManagerClubAppWorkspaceProjectDemoRequest
} from "../../lib/manager/clubClient";
import {
  ManagerClubAppWorkspaceProjectDemo,
  ManagerClubAppWorkspaceProjectDemoStep
} from "../../lib/manager/appWorkspaceDemoTypes";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherAppWorkspaceDemo.module.css";

type DemoResponse = {
  success?: boolean;
  data?: ManagerClubAppWorkspaceProjectDemo;
  message?: string;
};

type EditorStep = ManagerClubAppWorkspaceProjectDemoStep & {
  key: string;
};

const STEP_TRIGGER_OPTIONS = ["SCENE", "AUTO", "MANUAL", "DEVICE"] as const;

export function TeacherClubAppWorkspaceDemoPage({
  clubId,
  projectKey
}: {
  clubId: number;
  projectKey: string;
}) {
  const t = useT("teacherAppWorkspace");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("header.defaultName"));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [demo, setDemo] = useState<ManagerClubAppWorkspaceProjectDemo | null>(null);
  const [overviewTitle, setOverviewTitle] = useState("");
  const [overviewBody, setOverviewBody] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [steps, setSteps] = useState<EditorStep[]>([]);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [portal.authorized, clubId, projectKey]);

  const subtitle = useMemo(() => demo?.subtitle || projectKey, [demo?.subtitle, projectKey]);

  if (portal.checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{t("states.checking")}</p>
        </section>
      </main>
    );
  }

  if (!portal.authorized || portal.redirecting) {
    return null;
  }

  return (
    <PortalShell
      title={t("demo.header.title")}
      subtitle={subtitle}
      profileName={portal.profileName}
      profileHint={t("header.profileHint")}
      avatarUrl={portal.avatarUrl}
      profileLoading={!portal.managerInfo}
      onProfileClick={() => router.push("/club-admin/profile")}
      logoutLabel={t("header.logout")}
      logoutPendingLabel={t("header.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button
            type="button"
            className={styles.backButton}
            onClick={() => router.push(`/club-admin/clubs/${clubId}/app-workspace`, { scroll: true })}
          >
            <i className="fas fa-arrow-left" />
            {t("demo.actions.back")}
          </button>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => {
              void handleSave();
            }}
            disabled={saving || loading}
          >
            <i className="fas fa-floppy-disk" />
            {saving ? t("demo.actions.saving") : t("demo.actions.save")}
          </button>
        </div>

        {loading ? (
          <article className={styles.emptyCard}>
            <p>{t("states.loading")}</p>
          </article>
        ) : (
          <>
            <article className={styles.hero}>
              <div className={styles.heroMedia}>
                {demo?.coverUrl ? (
                  <img src={demo.coverUrl} alt={demo.title || t("preview.alt")} />
                ) : (
                  <div className={styles.coverPlaceholder}>{t("preview.empty")}</div>
                )}
              </div>
              <div className={styles.heroBody}>
                <h2 className={styles.heroTitle}>{demo?.title || projectKey}</h2>
                {demo?.subtitle ? <p className={styles.heroSubtitle}>{demo.subtitle}</p> : null}
                <div className={styles.heroMeta}>
                  <span className={styles.heroPill}>{projectKey}</span>
                  <label className={styles.toggle}>
                    <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
                    <span>{enabled ? t("actions.visible") : t("actions.hidden")}</span>
                  </label>
                </div>
              </div>
            </article>

            <article className={styles.sectionCard}>
              <div className={styles.fieldGrid}>
                <label className={styles.field}>
                  <span>{t("demo.fields.overviewTitle")}</span>
                  <input value={overviewTitle} onChange={(event) => setOverviewTitle(event.target.value)} />
                </label>
                <label className={`${styles.field} ${styles.fieldWide}`}>
                  <span>{t("demo.fields.overviewBody")}</span>
                  <textarea value={overviewBody} onChange={(event) => setOverviewBody(event.target.value)} />
                </label>
              </div>
            </article>

            <article className={styles.sectionCard}>
              <div className={styles.sectionHead}>
                <h3>{t("demo.sections.steps")}</h3>
                <button type="button" className={styles.secondaryButton} onClick={handleAddStep}>
                  <i className="fas fa-plus" />
                  {t("demo.actions.addStep")}
                </button>
              </div>
              {steps.length ? (
                <div className={styles.cardStack}>
                  {steps.map((step, index) => (
                    <article key={step.key} className={styles.itemCard}>
                      <div className={styles.itemHead}>
                        <strong>{step.title || t("demo.fields.stepTitle")}</strong>
                        <div className={styles.itemActions}>
                          <label className={styles.toggle}>
                            <input
                              type="checkbox"
                              checked={step.enabled}
                              onChange={(event) => updateStep(step.key, { enabled: event.target.checked })}
                            />
                            <span>{step.enabled ? t("actions.visible") : t("actions.hidden")}</span>
                          </label>
                          <button
                            type="button"
                            className={styles.iconButton}
                            onClick={() => moveStep(index, -1)}
                            disabled={index === 0}
                            aria-label={t("actions.moveUp")}
                          >
                            <i className="fas fa-arrow-up" />
                          </button>
                          <button
                            type="button"
                            className={styles.iconButton}
                            onClick={() => moveStep(index, 1)}
                            disabled={index === steps.length - 1}
                            aria-label={t("actions.moveDown")}
                          >
                            <i className="fas fa-arrow-down" />
                          </button>
                          <button type="button" className={styles.inlineDangerButton} onClick={() => removeStep(step.key)}>
                            <i className="fas fa-xmark" />
                            {t("demo.actions.deleteStep")}
                          </button>
                        </div>
                      </div>
                      <div className={styles.fieldGrid}>
                        <label className={styles.field}>
                          <span>{t("demo.fields.stepTitle")}</span>
                          <input value={step.title} onChange={(event) => updateStep(step.key, { title: event.target.value })} />
                        </label>
                        <label className={styles.field}>
                          <span>{t("demo.fields.triggerType")}</span>
                          <select
                            value={step.triggerType}
                            onChange={(event) => updateStep(step.key, { triggerType: event.target.value })}
                          >
                            {STEP_TRIGGER_OPTIONS.map((value) => (
                              <option key={value} value={value}>
                                {t(`demo.stepTriggers.${value}`)}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label className={styles.field}>
                          <span>{t("demo.fields.stepTag")}</span>
                          <input
                            value={step.targetSubsystem}
                            onChange={(event) => updateStep(step.key, { targetSubsystem: event.target.value })}
                            placeholder={t("demo.fields.stepTagPlaceholder")}
                          />
                        </label>
                        {isAppStepTag(step.targetSubsystem) ? (
                          <label className={styles.field}>
                            <span>{t("demo.fields.actionKey")}</span>
                            <input
                              value={step.actionKey || ""}
                              onChange={(event) => updateStep(step.key, { actionKey: event.target.value })}
                              placeholder={t("demo.fields.actionKeyPlaceholder")}
                            />
                          </label>
                        ) : null}
                        <label className={`${styles.field} ${styles.fieldWide}`}>
                          <span>{t("demo.fields.stepDescription")}</span>
                          <textarea
                            value={step.description}
                            onChange={(event) => updateStep(step.key, { description: event.target.value })}
                          />
                        </label>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className={styles.emptyInline}>{t("demo.states.emptySteps")}</div>
              )}
            </article>
          </>
        )}
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const response = await fetchManagerClubAppWorkspaceProjectDemoRequest(clubId, projectKey);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as DemoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("demo.messages.loadFailed"));
        router.push(`/club-admin/clubs/${clubId}/app-workspace`, { scroll: true });
        return;
      }

      applyDemo(result.data);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    if (saving) {
      return;
    }

    setSaving(true);
    try {
      const response = await saveManagerClubAppWorkspaceProjectDemoRequest(clubId, projectKey, {
        overviewTitle: overviewTitle.trim(),
        overviewBody: overviewBody.trim(),
        enabled,
        steps: steps.map((step) => ({
          ...(step.id ? { id: step.id } : {}),
          title: step.title.trim(),
          description: step.description.trim(),
          triggerType: step.triggerType,
          targetSubsystem: step.targetSubsystem.trim(),
          actionKey: isAppStepTag(step.targetSubsystem) ? step.actionKey?.trim() || "" : "",
          enabled: step.enabled
        }))
      });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as DemoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("demo.messages.saveFailed"));
        return;
      }

      applyDemo(result.data);
      notify.success(t("demo.messages.saved"));
    } finally {
      setSaving(false);
    }
  }

  function applyDemo(nextDemo: ManagerClubAppWorkspaceProjectDemo) {
    setDemo(nextDemo);
    setOverviewTitle(nextDemo.overviewTitle);
    setOverviewBody(nextDemo.overviewBody);
    setEnabled(nextDemo.enabled);
    setSteps(
      nextDemo.steps.map((item) => ({
        ...item,
        targetSubsystem: normalizeLegacyStepTag(item.targetSubsystem),
        actionKey: item.actionKey || "",
        key: createEditorKey("step", item.id)
      }))
    );
  }

  function handleAddStep() {
    setSteps((current) => [
      ...current,
      {
        key: createEditorKey("step"),
        id: null,
        title: "",
        description: "",
        triggerType: "MANUAL",
        targetSubsystem: "",
        actionKey: "",
        sortOrder: current.length + 1,
        enabled: true
      }
    ]);
  }

  function updateStep(stepKey: string, patch: Partial<EditorStep>) {
    setSteps((current) => current.map((item) => (item.key === stepKey ? { ...item, ...patch } : item)));
  }

  function moveStep(index: number, delta: -1 | 1) {
    setSteps((current) => reorder(current, index, delta));
  }

  function removeStep(stepKey: string) {
    setSteps((current) => current.filter((item) => item.key !== stepKey));
  }

  function normalizeLegacyStepTag(value: string) {
    switch (value.trim().toUpperCase()) {
      case "HELMET":
        return t("demo.legacyStepTags.HELMET");
      case "WATCH":
        return t("demo.legacyStepTags.WATCH");
      case "APP":
        return "App";
      case "CLOUD":
        return t("demo.legacyStepTags.CLOUD");
      default:
        return value;
    }
  }
}

function reorder<T>(items: T[], index: number, delta: -1 | 1) {
  const targetIndex = index + delta;
  if (targetIndex < 0 || targetIndex >= items.length) {
    return items;
  }
  const next = [...items];
  const [moved] = next.splice(index, 1);
  next.splice(targetIndex, 0, moved);
  return next;
}

function createEditorKey(prefix: string, id?: number | null) {
  if (id != null) {
    return `${prefix}-${id}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function isAppStepTag(value: string) {
  return value.trim().toLowerCase().includes("app");
}
