"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { clearPersistedAuthTokens } from "../../lib/auth/session";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { NotificationSummaryCard } from "../notifications/NotificationSummaryCard";
import { PortalShell } from "../portal/PortalShell";
import {
  fetchNotificationSummaryRequest,
  markNotificationReadRequest
} from "../../lib/notifications/client";
import { NotificationItem, NotificationSummary } from "../../lib/notifications/types";
import {
  cancelActivityRegistrationRequest,
  createStudentClubJoinRequestRequest,
  fetchStudentRewardOrdersRequest,
  fetchStudentScoreSummaryRequest,
  fetchStudentActivitiesRequest,
  fetchStudentDiscoverClubsRequest,
  fetchMyRegistrationIdsRequest,
  fetchStudentAvatarRequest,
  fetchStudentInfoRequest,
  registerActivityRequest,
  sortActivitiesByStartTime
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
  StudentActivityItem,
  StudentDiscoverClubsState,
  StudentInfo,
  StudentRewardOrder,
  StudentScoreSummary
} from "../../lib/student/types";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/studentDashboard.module.css";

type StudentInfoResponse = {
  success?: boolean;
  data?: StudentInfo;
  message?: string;
};

type ActivitiesResponse = {
  success?: boolean;
  activities?: StudentActivityItem[];
};

type RegistrationIdsResponse = {
  success?: boolean;
  activityIds?: number[];
};

type StudentDiscoverResponse = {
  success?: boolean;
  data?: StudentDiscoverClubsState;
  message?: string;
};

type StudentScoreSummaryResponse = {
  success?: boolean;
  data?: StudentScoreSummary;
  message?: string;
};

type StudentRewardOrdersResponse = {
  success?: boolean;
  data?: StudentRewardOrder[];
  message?: string;
};

type NotificationSummaryResponse = {
  success?: boolean;
  data?: NotificationSummary;
  message?: string;
};

const EMPTY_DISCOVER_STATE: StudentDiscoverClubsState = {
  joinedClub: null,
  pendingRequest: null,
  clubs: []
};

const EMPTY_SCORE_SUMMARY: StudentScoreSummary = {
  totalScore: 0,
  redeemedScore: 0,
  balanceScore: 0,
  clubs: []
};

const EMPTY_NOTIFICATION_SUMMARY: NotificationSummary = {
  unreadCount: 0,
  latestUnread: []
};

export function StudentDashboard() {
  const t = useT("studentDashboard");
  const tCadre = useT("studentClubCadre");
  const tPortal = useT("portal");
  const tProfile = useT("studentProfile");
  const tNotifications = useT("notifications");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("STUDENT");
  const [studentInfo, setStudentInfo] = useState<StudentInfo | null>(() => readCachedStudentProfile());
  const [avatarUrl, setAvatarUrl] = useState<string | null>(() => {
    const cachedProfile = readCachedStudentProfile();
    return cachedProfile?.hasAvatar ? readCachedStudentAvatar(cachedProfile) : null;
  });
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);
  const [activitiesLoading, setActivitiesLoading] = useState(true);
  const [clubsLoading, setClubsLoading] = useState(true);
  const [activities, setActivities] = useState<StudentActivityItem[]>([]);
  const [discoverState, setDiscoverState] = useState<StudentDiscoverClubsState>(EMPTY_DISCOVER_STATE);
  const [scoreSummary, setScoreSummary] = useState<StudentScoreSummary>(EMPTY_SCORE_SUMMARY);
  const [rewardOrders, setRewardOrders] = useState<StudentRewardOrder[]>([]);
  const [registeredIds, setRegisteredIds] = useState<number[]>([]);
  const [notificationSummary, setNotificationSummary] = useState<NotificationSummary>(EMPTY_NOTIFICATION_SUMMARY);
  const [notificationLoading, setNotificationLoading] = useState(true);
  const [pendingActivityId, setPendingActivityId] = useState<number | null>(null);
  const [pendingClubId, setPendingClubId] = useState<number | null>(null);

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
      if (!response.ok || result?.success !== true || !result.data || !active) {
        return;
      }

      setStudentInfo(result.data);
      writeCachedStudentProfile(result.data);
      await loadAvatar(result.data);
    };

    void loadProfile();
    return () => {
      active = false;
    };
  }, [authorized, notify, router, tPortal]);

  useEffect(() => {
    let active = true;

    const loadDashboardData = async () => {
      if (!authorized) {
        return;
      }

      setActivitiesLoading(true);
      setClubsLoading(true);

      try {
        const [
          activitiesResponse,
          discoverResponse,
          registrationsResponse,
          scoreSummaryResponse,
          rewardOrdersResponse
        ] = await Promise.all([
          fetchStudentActivitiesRequest(),
          fetchStudentDiscoverClubsRequest(),
          fetchMyRegistrationIdsRequest(),
          fetchStudentScoreSummaryRequest(),
          fetchStudentRewardOrdersRequest()
        ]);

        if (!active) {
          return;
        }

        if (!activitiesResponse) {
          notify.warning(tPortal("auth.loginRequired"));
          router.replace("/login");
          return;
        }
        if (discoverResponse && !(await handleAuthStatus(discoverResponse.status))) {
          return;
        }
        if (registrationsResponse && !(await handleAuthStatus(registrationsResponse.status))) {
          return;
        }
        if (scoreSummaryResponse && !(await handleAuthStatus(scoreSummaryResponse.status))) {
          return;
        }
        if (rewardOrdersResponse && !(await handleAuthStatus(rewardOrdersResponse.status))) {
          return;
        }
        if (!(await handleAuthStatus(activitiesResponse.status))) {
          return;
        }

        const activitiesResult = (await activitiesResponse.json().catch(() => null)) as ActivitiesResponse | null;
        if (activitiesResponse.ok && activitiesResult?.success) {
          setActivities(sortActivitiesByStartTime(activitiesResult.activities || []));
        } else {
          setActivities([]);
        }

        const discoverResult = discoverResponse
          ? ((await discoverResponse.json().catch(() => null)) as StudentDiscoverResponse | null)
          : null;
        if (discoverResponse?.ok && discoverResult?.success && discoverResult.data) {
          setDiscoverState(discoverResult.data);
        } else {
          setDiscoverState(EMPTY_DISCOVER_STATE);
        }

        const registrationsResult = registrationsResponse
          ? ((await registrationsResponse.json().catch(() => null)) as RegistrationIdsResponse | null)
          : null;
        if (registrationsResponse?.ok && registrationsResult?.success) {
          setRegisteredIds(Array.isArray(registrationsResult.activityIds) ? registrationsResult.activityIds : []);
        } else {
          setRegisteredIds([]);
        }

        const summaryResult = scoreSummaryResponse
          ? ((await scoreSummaryResponse.json().catch(() => null)) as StudentScoreSummaryResponse | null)
          : null;
        if (scoreSummaryResponse?.ok && summaryResult?.success && summaryResult.data) {
          setScoreSummary(summaryResult.data);
        } else {
          setScoreSummary(EMPTY_SCORE_SUMMARY);
        }

        const rewardOrdersResult = rewardOrdersResponse
          ? ((await rewardOrdersResponse.json().catch(() => null)) as StudentRewardOrdersResponse | null)
          : null;
        if (rewardOrdersResponse?.ok && rewardOrdersResult?.success && Array.isArray(rewardOrdersResult.data)) {
          setRewardOrders(rewardOrdersResult.data);
        } else {
          setRewardOrders([]);
        }
      } catch {
        if (active) {
          setActivities([]);
          setDiscoverState(EMPTY_DISCOVER_STATE);
          setScoreSummary(EMPTY_SCORE_SUMMARY);
          setRewardOrders([]);
          setRegisteredIds([]);
        }
      } finally {
        if (active) {
          setActivitiesLoading(false);
          setClubsLoading(false);
        }
      }
    };

    void loadDashboardData();
    return () => {
      active = false;
    };
  }, [authorized]);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadNotificationSummary();
  }, [authorized]);

  useEffect(() => {
    if (!authorized) {
      return;
    }

    const handleWindowFocus = () => {
      void reloadDiscoverClubs(false);
      void loadNotificationSummary(false);
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void reloadDiscoverClubs(false);
        void loadNotificationSummary(false);
      }
    };

    window.addEventListener("focus", handleWindowFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      window.removeEventListener("focus", handleWindowFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [authorized]);

  const upcomingActivities = useMemo(() => activities.slice(0, 4), [activities]);
  const discoverClubs = useMemo(() => discoverState.clubs, [discoverState.clubs]);
  const joinedClub = discoverState.joinedClub;
  const pendingJoinRequest = discoverState.pendingRequest;
  const latestRewardOrder = rewardOrders[0] || null;
  const nextRegisteredActivity = useMemo(
    () => activities.find((activity) => registeredIds.includes(activity.id)) || null,
    [activities, registeredIds]
  );
  const nextOpenActivity = useMemo(
    () =>
      activities.find(
        (activity) =>
          String(activity.status || "").toUpperCase() === "PUBLISHED" && !registeredIds.includes(activity.id)
      ) || null,
    [activities, registeredIds]
  );

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

  const profileName = studentInfo?.displayName || t("header.defaultName");

  return (
    <PortalShell
      title={t("header.title").trim()}
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
      <div className={styles.notificationSummary}>
        <NotificationSummaryCard
          title={t("messages.systemTitle")}
          description={tNotifications("student.summaryDescription")}
          unreadTag={tNotifications("student.unreadTag", { count: notificationSummary.unreadCount })}
          loadingLabel={tNotifications("common.loadingSummary")}
          emptyLabel={tNotifications("common.emptyUnread")}
          timeFallbackLabel={tNotifications("common.timeFallback")}
          unreadBadgeLabel={tNotifications("common.badges.unread")}
          viewAllLabel={tNotifications("common.viewAll")}
          summary={notificationSummary}
          loading={notificationLoading}
          onOpenAll={() => router.push("/student/notifications", { scroll: true })}
          onItemClick={(notificationId) => {
            void handleNotificationClick(notificationId);
          }}
        />
      </div>

      <section className={styles.layout}>
        <div className={styles.mainColumn}>
          <article className={styles.card}>
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle}>
                <i className="fas fa-users" />
                {t("sections.myClubs.title")}
              </h2>
              <span className={styles.sectionTag}>{t("sections.myClubs.tag")}</span>
            </div>
            {clubsLoading ? (
              <div className={styles.placeholderCard}>
                <p>{t("sections.myClubs.loading")}</p>
              </div>
            ) : joinedClub ? (
              <div className={styles.itemGrid}>
                <article className={styles.itemCard}>
                  <div className={styles.itemHead}>
                    <h3>{joinedClub.clubName}</h3>
                    <span className={`${styles.statusBadge} ${styles.statusBadgeActive}`}>
                      {t("sections.myClubs.joinedStatus")}
                    </span>
                  </div>
                  <p>{joinedClub.description || t("sections.myClubs.noDescription")}</p>
                  <dl className={styles.metaList}>
                    <div>
                      <dt>{t("sections.myClubs.meta.type")}</dt>
                      <dd>{joinedClub.clubType || t("sections.myClubs.typeEmpty")}</dd>
                    </div>
                    <div>
                      <dt>{t("sections.myClubs.meta.members")}</dt>
                      <dd>{t("sections.myClubs.memberCount", { count: joinedClub.memberCount })}</dd>
                    </div>
                    <div>
                      <dt>{tCadre("workspace.dutyNameLabel")}</dt>
                      <dd>{joinedClub.dutyName || tCadre("workspace.noDuty")}</dd>
                    </div>
                  </dl>
                  {joinedClub.canManage ? (
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => router.push(`/student/clubs/${joinedClub.clubId}/manage`, { scroll: true })}
                    >
                      <i className="fas fa-screwdriver-wrench" />
                      {t("sections.myClubs.manageAction")}
                    </button>
                  ) : null}
                </article>
              </div>
            ) : pendingJoinRequest ? (
              <div className={styles.itemGrid}>
                <article className={styles.itemCard}>
                  <div className={styles.itemHead}>
                    <h3>{pendingJoinRequest.clubName}</h3>
                    <span className={`${styles.statusBadge} ${styles.statusBadgeMuted}`}>
                      {t("sections.myClubs.pendingStatus")}
                    </span>
                  </div>
                  <p>{pendingJoinRequest.description || t("sections.myClubs.noDescription")}</p>
                  <dl className={styles.metaList}>
                    <div>
                      <dt>{t("sections.myClubs.meta.type")}</dt>
                      <dd>{pendingJoinRequest.clubType || t("sections.myClubs.typeEmpty")}</dd>
                    </div>
                    <div>
                      <dt>{t("sections.myClubs.meta.members")}</dt>
                      <dd>{t("sections.myClubs.memberCount", { count: pendingJoinRequest.memberCount })}</dd>
                    </div>
                    <div>
                      <dt>{t("sections.myClubs.meta.reason")}</dt>
                      <dd>{pendingJoinRequest.reason || t("sections.myClubs.reasonEmpty")}</dd>
                    </div>
                  </dl>
                </article>
              </div>
            ) : (
              <div className={styles.placeholderCard}>
                <p>{t("sections.myClubs.empty")}</p>
              </div>
            )}
          </article>

          <article className={styles.card}>
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle}>
                <i className="fas fa-calendar-check" />
                {t("sections.activities.title")}
              </h2>
              <span className={styles.sectionTag}>{t("sections.activities.tag")}</span>
            </div>

            {activitiesLoading ? (
              <div className={styles.placeholderCard}>
                <p>{t("sections.activities.loading")}</p>
              </div>
            ) : upcomingActivities.length ? (
              <>
                <div className={styles.itemGrid}>
                  {upcomingActivities.map((activity) => {
                    const registered = registeredIds.includes(activity.id);
                    const published = String(activity.status || "").toUpperCase() === "PUBLISHED";
                    const actionDisabled = pendingActivityId === activity.id || !published;
                    const actionLabel = registered
                      ? t("sections.activities.cancelAction")
                      : published
                        ? t("sections.activities.registerAction")
                        : t("sections.activities.closedAction");

                    return (
                      <article key={activity.id} className={styles.itemCard}>
                        <div className={styles.itemHead}>
                          <h3>{activity.title || t("sections.activities.untitled")}</h3>
                          <span
                            className={`${styles.statusBadge} ${
                              registered ? styles.statusBadgeActive : styles.statusBadgeMuted
                            }`}
                          >
                            {registered
                              ? t("sections.activities.registered")
                              : published
                                ? t("sections.activities.open")
                                : t("sections.activities.closed")}
                          </span>
                        </div>
                        <p>{activity.description || t("sections.activities.noDescription")}</p>
                        <dl className={styles.metaList}>
                          <div>
                            <dt>{t("sections.activities.meta.time")}</dt>
                            <dd>{formatDateTime(activity.startTime, t("sections.activities.timeEmpty"))}</dd>
                          </div>
                          <div>
                            <dt>{t("sections.activities.meta.location")}</dt>
                            <dd>{activity.location || t("sections.activities.locationEmpty")}</dd>
                          </div>
                          <div>
                            <dt>{t("sections.activities.meta.capacity")}</dt>
                            <dd>{activity.capacity > 0 ? String(activity.capacity) : t("sections.activities.capacityEmpty")}</dd>
                          </div>
                        </dl>
                        <button
                          type="button"
                          className={styles.primaryButton}
                          onClick={() => {
                            void handleActivityAction(activity.id, registered);
                          }}
                          disabled={actionDisabled}
                        >
                          <i className={registered ? "fas fa-ban" : "fas fa-pen-to-square"} />
                          {pendingActivityId === activity.id ? t("sections.activities.submitting") : actionLabel}
                        </button>
                      </article>
                    );
                  })}
                </div>
                <div className={styles.sectionFooter}>
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => router.push("/student/activities", { scroll: true })}
                  >
                    <i className="fas fa-arrow-right" />
                    {t("sections.activities.viewAll")}
                  </button>
                </div>
              </>
            ) : (
              <div className={styles.placeholderCard}>
                <p>{joinedClub ? t("sections.activities.empty") : t("sections.activities.emptyNoClub")}</p>
              </div>
            )}
          </article>

          <article className={styles.card}>
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle}>
                <i className="fas fa-compass" />
                {t("sections.discover.title")}
              </h2>
              <span className={styles.sectionTag}>{t("sections.discover.tag")}</span>
            </div>

            {clubsLoading ? (
              <div className={styles.placeholderCard}>
                <p>{t("sections.discover.loading")}</p>
              </div>
            ) : joinedClub ? (
              <div className={styles.placeholderCard}>
                <p>{t("sections.discover.joinedLocked")}</p>
              </div>
            ) : discoverClubs.length ? (
              <div className={styles.itemGrid}>
                {discoverClubs.map((club) => (
                  <article key={club.clubId} className={styles.itemCard}>
                    <div className={styles.itemHead}>
                      <h3>{club.clubName}</h3>
                      <span className={`${styles.statusBadge} ${styles.statusBadgeNeutral}`}>
                        {t("sections.discover.memberCount", { count: club.memberCount || 0 })}
                      </span>
                    </div>
                    <p>{club.description || t("sections.discover.noDescription")}</p>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={pendingClubId === club.clubId || Boolean(pendingJoinRequest)}
                      onClick={() => {
                        void handleClubJoinRequest(club);
                      }}
                    >
                      <i className="fas fa-plus-circle" />
                      {pendingClubId === club.clubId
                        ? t("sections.discover.submitting")
                        : pendingJoinRequest
                          ? t("sections.discover.pendingLocked")
                          : t("sections.discover.applyAction")}
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              <div className={styles.placeholderCard}>
                <p>{t("sections.discover.empty")}</p>
              </div>
            )}
          </article>

          <article className={styles.card}>
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle}>
                <i className="fas fa-medal" />
                {t("sections.points.title")}
              </h2>
              <span className={styles.sectionTag}>{t("sections.points.tag")}</span>
            </div>
            <div className={styles.scoreGrid}>
              <div className={styles.scoreCard}>
                <strong>{t("sections.points.cards.points.title")}</strong>
                <p>{t("sections.points.cards.points.value", { value: scoreSummary.totalScore })}</p>
              </div>
              <div className={styles.scoreCard}>
                <strong>{t("sections.points.cards.balance.title")}</strong>
                <p>{t("sections.points.cards.balance.value", { value: scoreSummary.balanceScore })}</p>
              </div>
              <div className={styles.scoreCard}>
                <strong>{t("sections.points.cards.orders.title")}</strong>
                <p>
                  {latestRewardOrder
                    ? t(`sections.points.cards.orders.status.${latestRewardOrder.status.toLowerCase()}`)
                    : t("sections.points.cards.orders.empty")}
                </p>
              </div>
            </div>
            <div className={styles.sectionFooter}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => router.push("/student/score-rewards", { scroll: true })}
              >
                <i className="fas fa-arrow-right" />
                {t("sections.points.viewAll")}
              </button>
            </div>
          </article>

        </div>

        <aside className={styles.card}>
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle}>
              <i className="fas fa-bolt" />
              {t("sections.insights.title")}
            </h2>
            <span className={styles.sectionTag}>{t("sections.insights.tag")}</span>
          </div>

          <div className={styles.insightGrid}>
            <article className={styles.insightCard}>
              <div className={styles.insightHead}>
                <strong>{t("sections.insights.cards.notifications.title")}</strong>
                <span className={`${styles.statusBadge} ${notificationSummary.unreadCount ? styles.statusBadgeMuted : styles.statusBadgeNeutral}`}>
                  {t("sections.insights.cards.notifications.badge", { count: notificationSummary.unreadCount })}
                </span>
              </div>
              <p>
                {notificationSummary.latestUnread[0]
                  ? notificationSummary.latestUnread[0].title
                  : t("sections.insights.cards.notifications.empty")}
              </p>
            </article>

            <article className={styles.insightCard}>
              <div className={styles.insightHead}>
                <strong>{t("sections.insights.cards.club.title")}</strong>
                <span
                  className={`${styles.statusBadge} ${
                    joinedClub ? styles.statusBadgeActive : pendingJoinRequest ? styles.statusBadgeMuted : styles.statusBadgeNeutral
                  }`}
                >
                  {joinedClub
                    ? t("sections.myClubs.joinedStatus")
                    : pendingJoinRequest
                      ? t("sections.myClubs.pendingStatus")
                      : t("sections.insights.cards.club.emptyBadge")}
                </span>
              </div>
              <p>
                {joinedClub
                  ? t("sections.insights.cards.club.joinedContent", {
                      clubName: joinedClub.clubName,
                      role: joinedClub.dutyName || tCadre("workspace.noDuty")
                    })
                  : pendingJoinRequest
                    ? t("sections.insights.cards.club.pendingContent", { clubName: pendingJoinRequest.clubName })
                    : t("sections.insights.cards.club.empty")}
              </p>
            </article>

            <article className={styles.insightCard}>
              <div className={styles.insightHead}>
                <strong>{t("sections.insights.cards.activity.title")}</strong>
                <span className={`${styles.statusBadge} ${(nextRegisteredActivity || nextOpenActivity) ? styles.statusBadgeActive : styles.statusBadgeNeutral}`}>
                  {nextRegisteredActivity
                    ? t("sections.activities.registered")
                    : nextOpenActivity
                      ? t("sections.activities.open")
                      : t("sections.insights.cards.activity.emptyBadge")}
                </span>
              </div>
              <p>
                {nextRegisteredActivity
                  ? t("sections.insights.cards.activity.registeredContent", {
                      name: nextRegisteredActivity.title || t("sections.activities.untitled")
                    })
                  : nextOpenActivity
                    ? t("sections.insights.cards.activity.openContent", {
                        name: nextOpenActivity.title || t("sections.activities.untitled")
                      })
                    : t("sections.insights.cards.activity.empty")}
              </p>
            </article>

            <article className={styles.insightCard}>
              <div className={styles.insightHead}>
                <strong>{t("sections.insights.cards.reward.title")}</strong>
                <span
                  className={`${styles.statusBadge} ${
                    latestRewardOrder
                      ? latestRewardOrder.status === "COMPLETED"
                        ? styles.statusBadgeActive
                        : latestRewardOrder.status === "REJECTED"
                          ? styles.statusBadgeNeutral
                          : styles.statusBadgeMuted
                      : styles.statusBadgeNeutral
                  }`}
                >
                  {latestRewardOrder
                    ? t(`sections.points.cards.orders.status.${latestRewardOrder.status.toLowerCase()}`)
                    : t("sections.insights.cards.reward.emptyBadge")}
                </span>
              </div>
              <p>
                {latestRewardOrder
                  ? t("sections.insights.cards.reward.content", { rewardName: latestRewardOrder.rewardName })
                  : t("sections.insights.cards.reward.empty")}
              </p>
            </article>
          </div>

          <div className={styles.insightActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => router.push("/student/notifications", { scroll: true })}
            >
              <i className="fas fa-bell" />
              {t("sections.insights.actions.notifications")}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => router.push("/student/activities", { scroll: true })}
            >
              <i className="fas fa-calendar-check" />
              {t("sections.insights.actions.activities")}
            </button>
          </div>
        </aside>
      </section>
    </PortalShell>
  );

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

    if (status === 428) {
      notify.warning(tProfile("completion.redirectNotice"));
      router.replace("/student/profile");
      return false;
    }

    return true;
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
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as { success?: boolean; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.activityActionFailed"));
        return;
      }

      setRegisteredIds((prev) =>
        registered ? prev.filter((id) => id !== activityId) : Array.from(new Set([...prev, activityId]))
      );
      notify.success(registered ? t("messages.activityCanceled") : t("messages.activityRegistered"));
    } finally {
      setPendingActivityId(null);
    }
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

  async function handleClubJoinRequest(club: StudentDiscoverClubsState["clubs"][number]) {
    if (pendingClubId || pendingJoinRequest || joinedClub) {
      return;
    }

    setPendingClubId(club.clubId);
    try {
      const response = await createStudentClubJoinRequestRequest({
        clubId: club.clubId,
        reason: t("sections.discover.defaultReason", { name: club.clubName })
      });

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as { success?: boolean; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        if (result?.message?.includes("already a club member")) {
          notify.warning(t("messages.clubJoinMemberBlocked"));
          void reloadDiscoverClubs();
          return;
        }
        if (result?.message?.includes("already pending")) {
          notify.warning(t("messages.clubJoinPendingBlocked"));
          void reloadDiscoverClubs();
          return;
        }
        notify.error(result?.message || t("messages.clubJoinFailed"));
        return;
      }

      notify.success(t("messages.clubJoinRequested", { name: club.clubName }));
      await reloadDiscoverClubs(true);
    } finally {
      setPendingClubId(null);
    }
  }

  async function reloadDiscoverClubs(showLoading = true) {
    if (showLoading) {
      setClubsLoading(true);
    }
    try {
      const response = await fetchStudentDiscoverClubsRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as StudentDiscoverResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        setDiscoverState(EMPTY_DISCOVER_STATE);
        return;
      }
      setDiscoverState(result.data);
    } finally {
      if (showLoading) {
        setClubsLoading(false);
      }
    }
  }

  async function loadNotificationSummary(showLoading = true) {
    if (showLoading) {
      setNotificationLoading(true);
    }

    try {
      const response = await fetchNotificationSummaryRequest();
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as NotificationSummaryResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        setNotificationSummary(EMPTY_NOTIFICATION_SUMMARY);
        return;
      }

      setNotificationSummary(result.data);
    } finally {
      if (showLoading) {
        setNotificationLoading(false);
      }
    }
  }

  async function handleNotificationClick(notificationId: number) {
    const item = notificationSummary.latestUnread.find((entry) => entry.id === notificationId);
    if (!item) {
      return;
    }

    const updated = item.status === "UNREAD" ? await markNotificationAsRead(item) : item;
    if (updated.targetPath) {
      router.push(updated.targetPath, { scroll: true });
    }
  }

  async function markNotificationAsRead(item: NotificationItem) {
    const response = await markNotificationReadRequest(item.id);
    if (!response) {
      return item;
    }
    if (!(await handleAuthStatus(response.status))) {
      return item;
    }
    if (!response.ok) {
      return item;
    }

    setNotificationSummary((current) => ({
      unreadCount: Math.max(0, current.unreadCount - 1),
      latestUnread: current.latestUnread.filter((entry) => entry.id !== item.id)
    }));
    return { ...item, status: "READ" as const };
  }
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
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
