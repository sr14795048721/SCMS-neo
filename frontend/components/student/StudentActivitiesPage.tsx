"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { fetchActivityRegistrationsRequest } from "../../lib/activity/registrationClient";
import { ActivityRegistrationItem } from "../../lib/activity/registrationTypes";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import {
  cancelActivityRegistrationRequest,
  fetchMyRegistrationIdsRequest,
  fetchStudentActivitiesRequest,
  fetchStudentDiscoverClubsRequest,
  registerActivityRequest,
  updateStudentActivityRequest,
  sortActivitiesByStartTime
} from "../../lib/student/client";
import { formatStudentClassName } from "../../lib/student/fieldConstraints";
import { useStudentPortalState } from "../../lib/student/useStudentPortalState";
import { StudentActivityItem, StudentActivityMutationPayload, StudentDiscoverClubsState } from "../../lib/student/types";
import { paginateItems } from "../../lib/pagination";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/studentFeaturePages.module.css";

type ActivitiesResponse = {
  success?: boolean;
  activities?: StudentActivityItem[];
  message?: string;
};

type RegistrationIdsResponse = {
  success?: boolean;
  activityIds?: number[];
  message?: string;
};

type DiscoverResponse = {
  success?: boolean;
  data?: StudentDiscoverClubsState;
  message?: string;
};

type ActivityRegistrationsResponse = {
  success?: boolean;
  data?: ActivityRegistrationItem[];
  message?: string;
};

type ActivityMutationResponse = {
  success?: boolean;
  data?: StudentActivityItem;
  message?: string;
};

type ActivityFormState = {
  clubId: string;
  title: string;
  description: string;
  location: string;
  startTime: string;
  endTime: string;
  capacity: string;
};

const EMPTY_DISCOVER_STATE: StudentDiscoverClubsState = {
  joinedClub: null,
  pendingRequest: null,
  clubs: []
};

const EMPTY_ACTIVITY_FORM: ActivityFormState = {
  clubId: "",
  title: "",
  description: "",
  location: "",
  startTime: "",
  endTime: "",
  capacity: "30"
};

export function StudentActivitiesPage() {
  const t = useT("studentActivities");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useStudentPortalState(t("common.defaultName"));
  const [activities, setActivities] = useState<StudentActivityItem[]>([]);
  const [registeredIds, setRegisteredIds] = useState<number[]>([]);
  const [discoverState, setDiscoverState] = useState<StudentDiscoverClubsState>(EMPTY_DISCOVER_STATE);
  const [activityPage, setActivityPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [pendingActivityId, setPendingActivityId] = useState<number | null>(null);
  const [editingActivityId, setEditingActivityId] = useState<number | null>(null);
  const [activityForm, setActivityForm] = useState<ActivityFormState>(EMPTY_ACTIVITY_FORM);
  const [savingActivity, setSavingActivity] = useState(false);
  const [openRosterActivityId, setOpenRosterActivityId] = useState<number | null>(null);
  const [rosterLoadingActivityId, setRosterLoadingActivityId] = useState<number | null>(null);
  const [activityRegistrations, setActivityRegistrations] = useState<Record<number, ActivityRegistrationItem[]>>({});

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [portal.authorized]);

  const registeredCount = useMemo(
    () => activities.filter((item) => registeredIds.includes(item.id)).length,
    [activities, registeredIds]
  );
  const pagedActivities = useMemo(() => paginateItems(activities, activityPage, 6), [activities, activityPage]);
  const canManageActivities = Boolean(discoverState.joinedClub?.dutyPermissions.includes("ACTIVITY_MANAGEMENT"));

  if (portal.checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{t("common.loading")}</p>
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
      subtitle={t("meta.description")}
      profileName={portal.profileName}
      profileHint={t("common.profileHint")}
      avatarUrl={portal.avatarUrl}
      profileLoading={!portal.studentInfo}
      onProfileClick={() => router.push("/student/profile")}
      logoutLabel={t("common.logout")}
      logoutPendingLabel={t("common.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button type="button" className={styles.backButton} onClick={() => router.push("/student", { scroll: true })}>
            <i className="fas fa-arrow-left" />
            {t("common.back")}
          </button>
        </div>

        <section className={styles.overviewGrid}>
          <article className={styles.overviewCard}>
            <span>{t("summary.totalLabel")}</span>
            <strong>{activities.length}</strong>
            <p>{t("summary.totalHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("summary.registeredLabel")}</span>
            <strong>{registeredCount}</strong>
            <p>{t("summary.registeredHint")}</p>
          </article>
        </section>

        {canManageActivities ? (
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h2 className={styles.panelTitle}>
                  <i className="fas fa-pen-ruler" />
                  {editingActivityId ? t("manage.titleEditing") : t("manage.titleIdle")}
                </h2>
                <p className={styles.panelDescription}>
                  {editingActivityId ? t("manage.descriptionEditing") : t("manage.descriptionIdle")}
                </p>
              </div>
            </div>

            {editingActivityId ? (
              <>
                <div className={styles.manageGrid}>
                  <label className={styles.field}>
                    <span>{t("manage.fields.title")}</span>
                    <input
                      value={activityForm.title}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, title: event.target.value }))}
                    />
                  </label>
                  <label className={styles.field}>
                    <span>{t("manage.fields.location")}</span>
                    <input
                      value={activityForm.location}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, location: event.target.value }))}
                    />
                  </label>
                  <label className={styles.field}>
                    <span>{t("manage.fields.startTime")}</span>
                    <input
                      type="datetime-local"
                      value={activityForm.startTime}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, startTime: event.target.value }))}
                    />
                  </label>
                  <label className={styles.field}>
                    <span>{t("manage.fields.endTime")}</span>
                    <input
                      type="datetime-local"
                      value={activityForm.endTime}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, endTime: event.target.value }))}
                    />
                  </label>
                  <label className={styles.field}>
                    <span>{t("manage.fields.capacity")}</span>
                    <input
                      type="number"
                      min={1}
                      value={activityForm.capacity}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, capacity: event.target.value }))}
                    />
                  </label>
                  <label className={`${styles.field} ${styles.fieldWide}`}>
                    <span>{t("manage.fields.description")}</span>
                    <textarea
                      value={activityForm.description}
                      onChange={(event) => setActivityForm((prev) => ({ ...prev, description: event.target.value }))}
                    />
                  </label>
                </div>
                <div className={styles.actionRow}>
                  <button
                    type="button"
                    className={styles.primaryButton}
                    disabled={savingActivity}
                    onClick={() => {
                      void handleActivityUpdate();
                    }}
                  >
                    <i className="fas fa-floppy-disk" />
                    {savingActivity ? t("manage.actions.saving") : t("manage.actions.save")}
                  </button>
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={resetActivityEditor}
                    disabled={savingActivity}
                  >
                    <i className="fas fa-xmark" />
                    {t("manage.actions.cancel")}
                  </button>
                </div>
              </>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("manage.empty")}</p>
              </div>
            )}
          </article>
        ) : null}

        <article className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h2 className={styles.panelTitle}>
                <i className="fas fa-calendar-check" />
                {t("list.title")}
              </h2>
              <p className={styles.panelDescription}>{t("list.description")}</p>
            </div>
            <div className={styles.panelActions}>
              <button type="button" className={styles.secondaryButton} onClick={() => void loadData()} disabled={loading}>
                <i className="fas fa-rotate-right" />
                {loading ? t("common.loading") : t("list.refresh")}
              </button>
            </div>
          </div>

          {loading ? (
            <div className={styles.emptyPanel}>
              <p>{t("common.loading")}</p>
            </div>
          ) : activities.length ? (
            <>
            <div className={styles.cards}>
              {pagedActivities.items.map((activity) => {
                const registered = registeredIds.includes(activity.id);
                const actionDisabled = pendingActivityId === activity.id;
                const rosterOpen = openRosterActivityId === activity.id;
                const registrations = activityRegistrations[activity.id] || [];

                return (
                  <article key={activity.id} className={styles.activityCard}>
                    <div className={styles.head}>
                      <div>
                        <h3 className={styles.title}>{activity.title || t("list.untitled")}</h3>
                        <p className={styles.clubName}>{activity.location || t("list.locationFallback")}</p>
                      </div>
                      <span className={`${styles.statusBadge} ${registered ? styles.statusActive : styles.statusPending}`}>
                        {registered ? t("list.status.registered") : t("list.status.open")}
                      </span>
                    </div>
                    <p className={styles.description}>{activity.description || t("list.descriptionFallback")}</p>
                    <dl className={styles.metaGrid}>
                      <div className={styles.metaBox}>
                        <dt>{t("list.meta.time")}</dt>
                        <dd>{formatDateTimeRange(activity.startTime, activity.endTime, t("list.timeFallback"))}</dd>
                      </div>
                      <div className={styles.metaBox}>
                        <dt>{t("list.meta.capacity")}</dt>
                        <dd>{activity.capacity > 0 ? activity.capacity : t("list.capacityFallback")}</dd>
                      </div>
                    </dl>
                    <div className={styles.actionRow}>
                      {canManageActivities ? (
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          onClick={() => {
                            fillActivityEditor(activity);
                          }}
                        >
                          <i className="fas fa-pen" />
                          {t("list.editAction")}
                        </button>
                      ) : null}
                      <button
                        type="button"
                        className={registered ? styles.secondaryButton : styles.primaryButton}
                        onClick={() => {
                          void handleActivityAction(activity.id, registered);
                        }}
                        disabled={actionDisabled}
                      >
                        <i className={registered ? "fas fa-ban" : "fas fa-pen-to-square"} />
                        {pendingActivityId === activity.id
                          ? t("list.submitting")
                          : registered
                            ? t("list.cancelAction")
                            : t("list.registerAction")}
                      </button>
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        onClick={() => {
                          void handleRosterToggle(activity.id);
                        }}
                        disabled={rosterLoadingActivityId === activity.id}
                      >
                        <i className="fas fa-users" />
                        {rosterLoadingActivityId === activity.id
                          ? t("list.registrationsLoading")
                          : rosterOpen
                            ? t("list.hideRosterAction")
                            : t("list.viewRosterAction")}
                      </button>
                    </div>
                    {rosterOpen ? (
                      <section className={styles.registrationPanel}>
                        <div className={styles.registrationPanelHead}>
                          <strong>{t("list.registrationsTitle")}</strong>
                          <span>{t("list.registrationsCount", { count: registrations.length })}</span>
                        </div>
                        {rosterLoadingActivityId === activity.id ? (
                          <div className={styles.registrationEmpty}>
                            <p>{t("list.registrationsLoading")}</p>
                          </div>
                        ) : registrations.length ? (
                          <div className={styles.tableWrap}>
                            <table className={styles.dataTable}>
                              <thead>
                                <tr>
                                  <th>{t("table.registration.displayName")}</th>
                                  <th>{t("table.registration.grade")}</th>
                                  <th>{t("table.registration.className")}</th>
                                  <th>{t("table.registration.registeredAt")}</th>
                                </tr>
                              </thead>
                              <tbody>
                                {registrations.map((registration) => (
                                  <tr key={registration.registrationId}>
                                    <td>{registration.displayName || t("common.emptyValue")}</td>
                                    <td>{formatGradeLabel(registration.grade, t("common.emptyValue"), t)}</td>
                                    <td>
                                      {registration.className
                                        ? formatStudentClassName(registration.className)
                                        : t("common.emptyValue")}
                                    </td>
                                    <td>{formatDateTime(registration.registeredAt, t("common.emptyValue"))}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        ) : (
                          <div className={styles.registrationEmpty}>
                            <p>{t("list.registrationsEmpty")}</p>
                          </div>
                        )}
                      </section>
                    ) : null}
                  </article>
                );
              })}
            </div>
            <PaginationBar
              currentPage={pagedActivities.page}
              totalPages={pagedActivities.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedActivities.page, total: pagedActivities.totalPages })}
              onPageChange={setActivityPage}
            />
            </>
          ) : (
            <div className={styles.emptyPanel}>
              <p>{discoverState.joinedClub ? t("list.empty") : t("list.emptyNoClub")}</p>
            </div>
          )}
        </article>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const [activitiesResponse, registrationsResponse, discoverResponse] = await Promise.all([
        fetchStudentActivitiesRequest(),
        fetchMyRegistrationIdsRequest(),
        fetchStudentDiscoverClubsRequest()
      ]);

      if (!activitiesResponse || !registrationsResponse || !discoverResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(activitiesResponse.status))) {
        return;
      }
      if (!(await portal.handleAuthStatus(registrationsResponse.status))) {
        return;
      }
      if (!(await portal.handleAuthStatus(discoverResponse.status))) {
        return;
      }

      const activitiesResult = (await activitiesResponse.json().catch(() => null)) as ActivitiesResponse | null;
      const registrationsResult = (await registrationsResponse.json().catch(() => null)) as RegistrationIdsResponse | null;
      const discoverResult = (await discoverResponse.json().catch(() => null)) as DiscoverResponse | null;

      setActivities(
        activitiesResponse.ok && activitiesResult?.success
          ? sortActivitiesByStartTime(activitiesResult.activities || [])
          : []
      );
      setRegisteredIds(
        registrationsResponse.ok && registrationsResult?.success && Array.isArray(registrationsResult.activityIds)
          ? registrationsResult.activityIds
          : []
      );
      setDiscoverState(
        discoverResponse.ok && discoverResult?.success && discoverResult.data
          ? discoverResult.data
          : EMPTY_DISCOVER_STATE
      );
      setEditingActivityId(null);
      setActivityForm(EMPTY_ACTIVITY_FORM);
      setOpenRosterActivityId(null);
      setActivityRegistrations({});
    } finally {
      setLoading(false);
    }
  }

  async function handleActivityAction(activityId: number, registered: boolean) {
    if (pendingActivityId) {
      return;
    }

    setPendingActivityId(activityId);
    try {
      const response = registered
        ? await cancelActivityRegistrationRequest(activityId)
        : await registerActivityRequest(activityId);

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as { success?: boolean; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.activityActionFailed"));
        return;
      }

      notify.success(registered ? t("messages.activityCanceled") : t("messages.activityRegistered"));
      await loadData();
    } finally {
      setPendingActivityId(null);
    }
  }

  function fillActivityEditor(activity: StudentActivityItem) {
    setEditingActivityId(activity.id);
    setActivityForm({
      clubId: String(activity.clubId),
      title: activity.title,
      description: activity.description || "",
      location: activity.location || "",
      startTime: toLocalDateTimeInput(activity.startTime),
      endTime: toLocalDateTimeInput(activity.endTime),
      capacity: String(activity.capacity || 1)
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetActivityEditor() {
    setEditingActivityId(null);
    setActivityForm(EMPTY_ACTIVITY_FORM);
  }

  async function handleActivityUpdate() {
    if (!editingActivityId || savingActivity) {
      return;
    }

    const payload = buildActivityPayload(activityForm);
    if (!payload) {
      notify.warning(t("manage.validation.invalid"));
      return;
    }

    setSavingActivity(true);
    try {
      const response = await updateStudentActivityRequest(editingActivityId, payload);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ActivityMutationResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.activityUpdateFailed"));
        return;
      }

      notify.success(t("messages.activityUpdated"));
      await loadData();
    } finally {
      setSavingActivity(false);
    }
  }

  async function handleRosterToggle(activityId: number) {
    if (openRosterActivityId === activityId) {
      setOpenRosterActivityId(null);
      return;
    }

    setOpenRosterActivityId(activityId);
    if (Object.prototype.hasOwnProperty.call(activityRegistrations, activityId) || rosterLoadingActivityId === activityId) {
      return;
    }

    setRosterLoadingActivityId(activityId);
    try {
      const response = await fetchActivityRegistrationsRequest(activityId);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        setOpenRosterActivityId(null);
        return;
      }

      const result = (await response.json().catch(() => null)) as ActivityRegistrationsResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.activityRosterFailed"));
        setOpenRosterActivityId(null);
        return;
      }

      setActivityRegistrations((prev) => ({
        ...prev,
        [activityId]: result.data || []
      }));
    } finally {
      setRosterLoadingActivityId(null);
    }
  }
}

function formatDateTimeRange(start: string | null, end: string | null, fallback: string) {
  const startText = formatDateTime(start, fallback);
  const endText = formatDateTime(end, fallback);
  return `${startText} - ${endText}`;
}

function formatDateTime(value: string | null, fallback: string) {
  if (!value) {
    return fallback;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function formatGradeLabel(grade: string, fallback: string, t: (key: string) => string) {
  if (grade === "HIGH_1") {
    return t("common.gradeLabels.HIGH_1");
  }
  if (grade === "HIGH_2") {
    return t("common.gradeLabels.HIGH_2");
  }
  if (grade === "HIGH_3") {
    return t("common.gradeLabels.HIGH_3");
  }
  return fallback;
}

function buildActivityPayload(form: ActivityFormState): StudentActivityMutationPayload | null {
  const clubId = Number(form.clubId);
  const capacity = Number(form.capacity);
  const startTime = toIsoString(form.startTime);
  const endTime = toIsoString(form.endTime);

  if (!clubId || !form.title.trim() || !startTime || !endTime || !Number.isFinite(capacity) || capacity < 1) {
    return null;
  }

  return {
    clubId,
    title: form.title.trim(),
    description: form.description.trim(),
    location: form.location.trim(),
    startTime,
    endTime,
    capacity
  };
}

function toIsoString(value: string) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toISOString();
}

function toLocalDateTimeInput(value: string | null) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
}
