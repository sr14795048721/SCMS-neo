"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { fetchActivityRegistrationsRequest } from "../../lib/activity/registrationClient";
import { ActivityRegistrationItem } from "../../lib/activity/registrationTypes";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { paginateItems } from "../../lib/pagination";
import { fetchManagerClubsRequest } from "../../lib/manager/clubClient";
import {
  closeManagerActivityRequest,
  createManagerActivityRequest,
  fetchManagerActivitiesRequest,
  publishManagerActivityRequest,
  updateManagerActivityRequest
} from "../../lib/manager/activityClient";
import { ManagerClubSummary } from "../../lib/manager/clubTypes";
import { ManagerActivityItem, ManagerActivityMutationPayload } from "../../lib/manager/activityTypes";
import { formatStudentClassName } from "../../lib/student/fieldConstraints";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherActivities.module.css";

type ClubsResponse = {
  success?: boolean;
  data?: ManagerClubSummary[];
  message?: string;
};

type ActivitiesResponse = {
  success?: boolean;
  data?: ManagerActivityItem[];
  message?: string;
};

type ActivityMutationResponse = {
  success?: boolean;
  data?: ManagerActivityItem;
  message?: string;
};

type ActivityRegistrationsResponse = {
  success?: boolean;
  data?: ActivityRegistrationItem[];
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

const EMPTY_FORM: ActivityFormState = {
  clubId: "",
  title: "",
  description: "",
  location: "",
  startTime: "",
  endTime: "",
  capacity: "30"
};

export function TeacherActivitiesPage() {
  const t = useT("teacherActivities");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [clubs, setClubs] = useState<ManagerClubSummary[]>([]);
  const [activities, setActivities] = useState<ManagerActivityItem[]>([]);
  const [activityPage, setActivityPage] = useState(1);
  const [editingActivityId, setEditingActivityId] = useState<number | null>(null);
  const [form, setForm] = useState<ActivityFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionActivityId, setActionActivityId] = useState<number | null>(null);
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

  const draftCount = useMemo(
    () => activities.filter((item) => item.status === "DRAFT").length,
    [activities]
  );
  const pagedActivities = useMemo(() => paginateItems(activities, activityPage, 10), [activities, activityPage]);

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
      profileLoading={!portal.managerInfo}
      onProfileClick={() => router.push("/club-admin/profile")}
      logoutLabel={t("common.logout")}
      logoutPendingLabel={t("common.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button type="button" className={styles.backButton} onClick={() => router.push("/club-admin", { scroll: true })}>
            <i className="fas fa-arrow-left" />
            {t("common.back")}
          </button>
        </div>

        <article className={styles.panel}>
          <h2 className={styles.panelTitle}>
            <i className="fas fa-chart-line" />
            {t("summary.title")}
          </h2>
          <p className={styles.panelHint}>{t("summary.description")}</p>
          <div className={styles.metaGrid}>
            <div className={styles.metaBox}>
              <dt>{t("summary.items.total")}</dt>
              <dd>{activities.length}</dd>
            </div>
            <div className={styles.metaBox}>
              <dt>{t("summary.items.draft")}</dt>
              <dd>{draftCount}</dd>
            </div>
            <div className={styles.metaBox}>
              <dt>{t("summary.items.clubs")}</dt>
              <dd>{clubs.length}</dd>
            </div>
          </div>
        </article>

        <section className={styles.layout}>
          <article className={styles.panel}>
            <h3 className={styles.panelTitle}>
              <i className="fas fa-pen-ruler" />
              {editingActivityId ? t("form.editTitle") : t("form.createTitle")}
            </h3>
            <p className={styles.panelHint}>{t("form.description")}</p>

            <div className={styles.formGrid}>
              <div className={styles.field}>
                <label htmlFor="activity-club">{t("form.fields.club")}</label>
                <select
                  id="activity-club"
                  value={form.clubId}
                  onChange={(event) => setForm((prev) => ({ ...prev, clubId: event.target.value }))}
                >
                  <option value="">{t("form.fields.clubPlaceholder")}</option>
                  {clubs.map((club) => (
                    <option key={club.clubId} value={String(club.clubId)}>
                      {club.clubName}
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-title">{t("form.fields.title")}</label>
                <input
                  id="activity-title"
                  value={form.title}
                  onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-location">{t("form.fields.location")}</label>
                <input
                  id="activity-location"
                  value={form.location}
                  onChange={(event) => setForm((prev) => ({ ...prev, location: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-start">{t("form.fields.startTime")}</label>
                <input
                  id="activity-start"
                  type="datetime-local"
                  value={form.startTime}
                  onChange={(event) => setForm((prev) => ({ ...prev, startTime: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-end">{t("form.fields.endTime")}</label>
                <input
                  id="activity-end"
                  type="datetime-local"
                  value={form.endTime}
                  onChange={(event) => setForm((prev) => ({ ...prev, endTime: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-capacity">{t("form.fields.capacity")}</label>
                <input
                  id="activity-capacity"
                  type="number"
                  min={1}
                  value={form.capacity}
                  onChange={(event) => setForm((prev) => ({ ...prev, capacity: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="activity-description">{t("form.fields.description")}</label>
                <textarea
                  id="activity-description"
                  value={form.description}
                  onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
                />
              </div>
            </div>

            <div className={styles.formActions}>
              <button
                type="button"
                className={styles.primaryButton}
                disabled={submitting}
                onClick={() => {
                  void handleSubmit();
                }}
              >
                {submitting ? t("form.submitting") : editingActivityId ? t("form.save") : t("form.create")}
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => {
                  setEditingActivityId(null);
                  setForm(EMPTY_FORM);
                }}
              >
                {t("form.reset")}
              </button>
            </div>
          </article>

          <article className={styles.panel}>
            <h3 className={styles.panelTitle}>
              <i className="fas fa-calendar-check" />
              {t("list.title")}
            </h3>
            <p className={styles.panelHint}>{t("list.description")}</p>

            {loading ? (
              <div className={styles.emptyPanel}>
                <p>{t("common.loading")}</p>
              </div>
            ) : activities.length ? (
              <>
              <div className={styles.cards}>
                {pagedActivities.items.map((activity) => {
                  const registrations = activityRegistrations[activity.id] || [];
                  const rosterOpen = openRosterActivityId === activity.id;

                  return (
                    <article key={activity.id} className={styles.activityCard}>
                      <div className={styles.head}>
                        <div>
                          <h4 className={styles.title}>{activity.title}</h4>
                          <p className={styles.clubName}>{activity.clubName}</p>
                        </div>
                        <span
                          className={
                            activity.status === "PUBLISHED"
                              ? styles.statusPublished
                              : activity.status === "CLOSED"
                                ? styles.statusClosed
                                : styles.statusDraft
                          }
                        >
                          {t(`status.${activity.status || "DRAFT"}`)}
                        </span>
                      </div>
                      <p className={styles.description}>
                        {activity.description || t("list.descriptionFallback")}
                      </p>
                      <dl className={styles.metaGrid}>
                        <div className={styles.metaBox}>
                          <dt>{t("list.meta.time")}</dt>
                          <dd>{formatDateTimeRange(activity.startTime, activity.endTime, t("common.timeFallback"))}</dd>
                        </div>
                        <div className={styles.metaBox}>
                          <dt>{t("list.meta.location")}</dt>
                          <dd>{activity.location || t("list.locationFallback")}</dd>
                        </div>
                        <div className={styles.metaBox}>
                          <dt>{t("list.meta.capacity")}</dt>
                          <dd>{activity.capacity}</dd>
                        </div>
                        <div className={styles.metaBox}>
                          <dt>{t("list.meta.registrationCount")}</dt>
                          <dd>{activity.registrationCount}</dd>
                        </div>
                      </dl>
                      <div className={styles.actions}>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          onClick={() => {
                            void handleRosterToggle(activity.id);
                          }}
                        >
                          {rosterOpen ? t("list.actions.hideRoster") : t("list.actions.viewRoster")}
                        </button>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          onClick={() => fillFormForEdit(activity)}
                        >
                          {t("list.actions.edit")}
                        </button>
                        {activity.status === "DRAFT" ? (
                          <button
                            type="button"
                            className={styles.primaryButton}
                            disabled={actionActivityId === activity.id}
                            onClick={() => {
                              void handleStatusAction(activity.id, "publish");
                            }}
                          >
                            {actionActivityId === activity.id ? t("list.actions.submitting") : t("list.actions.publish")}
                          </button>
                        ) : null}
                        {activity.status === "PUBLISHED" ? (
                          <button
                            type="button"
                            className={styles.dangerButton}
                            disabled={actionActivityId === activity.id}
                            onClick={() => {
                              void handleStatusAction(activity.id, "close");
                            }}
                          >
                            {actionActivityId === activity.id ? t("list.actions.submitting") : t("list.actions.close")}
                          </button>
                        ) : null}
                      </div>
                      {rosterOpen ? (
                        <section className={styles.registrationPanel}>
                          <div className={styles.registrationPanelHead}>
                            <strong>{t("list.registrationsTitle")}</strong>
                            <span>{t("list.registrationsCount", { count: activity.registrationCount })}</span>
                          </div>
                          {rosterLoadingActivityId === activity.id ? (
                            <div className={styles.registrationEmpty}>
                              <p>{t("list.registrationsLoading")}</p>
                            </div>
                          ) : registrations.length ? (
                            <div className={styles.tableWrap}>
                              <table className={styles.table}>
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
                <p>{t("list.empty")}</p>
              </div>
            )}
          </article>
        </section>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const [clubsResponse, activitiesResponse] = await Promise.all([
        fetchManagerClubsRequest(),
        fetchManagerActivitiesRequest()
      ]);

      if (!clubsResponse || !activitiesResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(clubsResponse.status))) {
        return;
      }
      if (!(await portal.handleAuthStatus(activitiesResponse.status))) {
        return;
      }

      const clubsResult = (await clubsResponse.json().catch(() => null)) as ClubsResponse | null;
      const activitiesResult = (await activitiesResponse.json().catch(() => null)) as ActivitiesResponse | null;

      setClubs(clubsResponse.ok && clubsResult?.success ? clubsResult.data || [] : []);
      setActivities(activitiesResponse.ok && activitiesResult?.success ? activitiesResult.data || [] : []);
      setOpenRosterActivityId(null);
      setActivityRegistrations({});
    } finally {
      setLoading(false);
    }
  }

  function fillFormForEdit(activity: ManagerActivityItem) {
    setEditingActivityId(activity.id);
    setForm({
      clubId: String(activity.clubId),
      title: activity.title,
      description: activity.description || "",
      location: activity.location || "",
      startTime: toLocalDateTimeInput(activity.startTime),
      endTime: toLocalDateTimeInput(activity.endTime),
      capacity: String(activity.capacity || 1)
    });
  }

  async function handleSubmit() {
    if (submitting) {
      return;
    }

    const payload = buildPayload(form);
    if (!payload) {
      notify.warning(t("form.invalid"));
      return;
    }

    setSubmitting(true);
    try {
      const response = editingActivityId
        ? await updateManagerActivityRequest(editingActivityId, payload)
        : await createManagerActivityRequest(payload);

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
        notify.error(result?.message || t("form.submitFailed"));
        return;
      }

      notify.success(editingActivityId ? t("form.saved") : t("form.created"));
      setEditingActivityId(null);
      setForm(EMPTY_FORM);
      void loadData();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusAction(activityId: number, action: "publish" | "close") {
    if (actionActivityId) {
      return;
    }

    setActionActivityId(activityId);
    try {
      const response =
        action === "publish"
          ? await publishManagerActivityRequest(activityId)
          : await closeManagerActivityRequest(activityId);

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ActivityMutationResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("list.actions.failed"));
        return;
      }

      notify.success(action === "publish" ? t("list.actions.published") : t("list.actions.closed"));
      void loadData();
    } finally {
      setActionActivityId(null);
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
        notify.error(result?.message || t("list.actions.loadRosterFailed"));
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

function buildPayload(form: ActivityFormState): ManagerActivityMutationPayload | null {
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
