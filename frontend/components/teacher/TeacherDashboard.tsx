"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { NotificationSummaryCard } from "../notifications/NotificationSummaryCard";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { paginateItems } from "../../lib/pagination";
import { fetchManagerClubsRequest } from "../../lib/manager/clubClient";
import { ManagerClubSummary } from "../../lib/manager/clubTypes";
import { fetchNotificationSummaryRequest, markNotificationReadRequest } from "../../lib/notifications/client";
import { NotificationItem, NotificationSummary } from "../../lib/notifications/types";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import { PRIMARY_APP_CLUB_NAME } from "../../lib/club/appIdentity";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherClubs.module.css";

type ClubsResponse = {
  success?: boolean;
  data?: ManagerClubSummary[];
  message?: string;
};

type NotificationSummaryResponse = {
  success?: boolean;
  data?: NotificationSummary;
  message?: string;
};

type ShortcutItem = {
  icon: string;
  titleKey: string;
  descriptionKey: string;
  path: string;
  enabled: boolean;
};

const shortcutItems: ShortcutItem[] = [
  {
    icon: "fas fa-users-gear",
    titleKey: "shortcuts.guidedClubs.title",
    descriptionKey: "shortcuts.guidedClubs.description",
    path: "/club-admin",
    enabled: true
  },
  {
    icon: "fas fa-calendar-days",
    titleKey: "shortcuts.activities.title",
    descriptionKey: "shortcuts.activities.description",
    path: "/club-admin/activities",
    enabled: true
  },
  {
    icon: "fas fa-newspaper",
    titleKey: "shortcuts.news.title",
    descriptionKey: "shortcuts.news.description",
    path: "/club-admin/news-publish",
    enabled: true
  },
  {
    icon: "fas fa-star",
    titleKey: "shortcuts.recommend.title",
    descriptionKey: "shortcuts.recommend.description",
    path: "/club-admin/club-recommend",
    enabled: true
  },
  {
    icon: "fas fa-plus-circle",
    titleKey: "shortcuts.createClub.title",
    descriptionKey: "shortcuts.createClub.description",
    path: "/club-admin/create-club-apply",
    enabled: true
  }
];

export function TeacherDashboard() {
  const t = useT("teacherDashboard");
  const tClubs = useT("teacherClubs");
  const tAppWorkspace = useT("teacherAppWorkspace");
  const tNotifications = useT("notifications");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("header.defaultName"));
  const shortcutSectionHint = t("shortcuts.sectionHint").trim();
  const managedClubsSectionRef = useRef<HTMLElement | null>(null);
  const [clubs, setClubs] = useState<ManagerClubSummary[]>([]);
  const [managedClubPage, setManagedClubPage] = useState(1);
  const [clubsLoading, setClubsLoading] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summary, setSummary] = useState<NotificationSummary>({ unreadCount: 0, latestUnread: [] });

  const loadClubs = useCallback(
    async (showLoading = true, retryOnEmpty = true) => {
      if (!portal.authorized) {
        return;
      }

      if (showLoading) {
        setClubsLoading(true);
      }

      try {
        for (let attempt = 0; attempt < 2; attempt += 1) {
          const response = await fetchManagerClubsRequest();
          if (!response) {
            notify.warning(t("messages.loginRequired"));
            router.replace("/login");
            return;
          }

          if (!(await portal.handleAuthStatus(response.status))) {
            return;
          }

          const result = (await response.json().catch(() => null)) as ClubsResponse | null;
          if (response.ok && result?.success === true) {
            const nextClubs = Array.isArray(result.data) ? result.data : [];
            if (retryOnEmpty && !nextClubs.length && attempt === 0) {
              await new Promise((resolve) => window.setTimeout(resolve, 450));
              continue;
            }
            setClubs(nextClubs);
            return;
          }

          if (attempt === 0) {
            await new Promise((resolve) => window.setTimeout(resolve, 450));
            continue;
          }

          setClubs([]);
        }
      } finally {
        if (showLoading) {
          setClubsLoading(false);
        }
      }
    },
    [notify, portal.authorized, router, t]
  );

  useEffect(() => {
    void loadClubs(true, true);
  }, [loadClubs]);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }

    const handleWindowFocus = () => {
      void loadClubs(false, false);
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadClubs(false, false);
      }
    };

    window.addEventListener("focus", handleWindowFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      window.removeEventListener("focus", handleWindowFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [loadClubs, portal.authorized]);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadSummary();
  }, [portal.authorized]);

  const pendingRequestCount = useMemo(
    () => clubs.reduce((total, club) => total + club.pendingJoinRequestCount, 0),
    [clubs]
  );
  const draftActivityCount = useMemo(
    () => clubs.reduce((total, club) => total + club.draftActivityCount, 0),
    [clubs]
  );
  const totalMemberCount = useMemo(() => clubs.reduce((total, club) => total + club.memberCount, 0), [clubs]);
  const unreadCount = summary.unreadCount;
  const pagedClubs = useMemo(() => paginateItems(clubs, managedClubPage, 6), [clubs, managedClubPage]);

  if (portal.checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{t("loading")}</p>
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
        <article className={styles.hero}>
          <h2 className={styles.heroTitle}>{t("hero.title")}</h2>
          <p className={styles.heroDescription}>{t("hero.description")}</p>
          <div className={styles.heroMeta}>
            <span className={styles.heroPill}>
              <i className="fas fa-building-columns" />
              {t("hero.managedClubs", { count: clubs.length })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-user-clock" />
              {t("hero.pendingRequests", { count: pendingRequestCount })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-calendar-plus" />
              {t("hero.draftActivities", { count: draftActivityCount })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-bell" />
              {t("hero.unreadNotifications", { count: unreadCount })}
            </span>
          </div>
        </article>

        <section className={styles.statsGrid}>
          <article className={styles.statCard}>
            <p className={styles.statLabel}>{t("stats.clubCount.label")}</p>
            <p className={styles.statValue}>{clubs.length}</p>
            <p className={styles.statHint}>{t("stats.clubCount.hint")}</p>
          </article>
          <article className={styles.statCard}>
            <p className={styles.statLabel}>{t("stats.pendingRequests.label")}</p>
            <p className={styles.statValue}>{pendingRequestCount}</p>
            <p className={styles.statHint}>{t("stats.pendingRequests.hint")}</p>
          </article>
          <article className={styles.statCard}>
            <p className={styles.statLabel}>{t("stats.members.label")}</p>
            <p className={styles.statValue}>{totalMemberCount}</p>
            <p className={styles.statHint}>{t("stats.members.hint")}</p>
          </article>
          <article className={styles.statCard}>
            <p className={styles.statLabel}>{t("stats.drafts.label")}</p>
            <p className={styles.statValue}>{draftActivityCount}</p>
            <p className={styles.statHint}>{t("stats.drafts.hint")}</p>
          </article>
          <article className={styles.statCard}>
            <p className={styles.statLabel}>{t("stats.notifications.label")}</p>
            <p className={styles.statValue}>{unreadCount}</p>
            <p className={styles.statHint}>{t("stats.notifications.hint")}</p>
          </article>
        </section>

        <NotificationSummaryCard
          title={tNotifications("common.summaryTitle")}
          description={tNotifications("teacher.summaryDescription")}
          unreadTag={tNotifications("teacher.unreadTag", { count: unreadCount })}
          loadingLabel={tNotifications("common.loadingSummary")}
          emptyLabel={tNotifications("common.emptyUnread")}
          timeFallbackLabel={tNotifications("common.timeFallback")}
          unreadBadgeLabel={tNotifications("common.badges.unread")}
          viewAllLabel={tNotifications("common.viewAll")}
          summary={summary}
          loading={summaryLoading}
          onOpenAll={() => router.push("/club-admin/notifications", { scroll: true })}
          onItemClick={(notificationId) => void handleSummaryItemClick(notificationId)}
        />

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-bolt" />
                {t("shortcuts.sectionTitle")}
              </h3>
              {shortcutSectionHint ? <p className={styles.panelHint}>{shortcutSectionHint}</p> : null}
            </div>
          </div>
          <div className={styles.shortcutGrid}>
            {shortcutItems.map((item) => (
              <button
                key={item.titleKey}
                type="button"
                className={styles.shortcutButton}
                disabled={!item.enabled}
                onClick={() => {
                  if (!item.enabled) {
                    notify.info(t("messages.comingSoon", { name: t(item.titleKey) }));
                    return;
                  }
                  if (item.path === "/club-admin") {
                    managedClubsSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                    return;
                  }
                  router.push(item.path, { scroll: true });
                }}
              >
                <span className={styles.shortcutIcon}>
                  <i className={item.icon} />
                </span>
                <p className={styles.shortcutTitle}>{t(item.titleKey)}</p>
                <p className={styles.shortcutDescription}>{t(item.descriptionKey)}</p>
              </button>
            ))}
          </div>
        </section>

        <section ref={managedClubsSectionRef} className={styles.panel}>
          <div className={styles.panelHead}>
            <div className={styles.panelCopy}>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-people-group" />
                {tClubs("dashboard.managedClubs.title")}
              </h3>
              <p className={`${styles.panelHint} ${styles.panelHintCompact}`}>
                {tClubs("dashboard.managedClubs.description")}
              </p>
            </div>
            <span className={styles.tag}>{tClubs("dashboard.managedClubs.tag")}</span>
          </div>

          {clubsLoading ? (
            <div className={styles.emptyPanel}>
              <p>{t("messages.loadingClubs")}</p>
            </div>
          ) : clubs.length ? (
            <>
            <div className={styles.clubGrid}>
              {pagedClubs.items.map((club) => (
                <article key={club.clubId} className={styles.clubCard}>
                  <div className={styles.clubHead}>
                    <div>
                      <h4 className={styles.clubTitle}>{club.clubName}</h4>
                      <p className={styles.clubType}>{club.clubType || tClubs("common.typeFallback")}</p>
                    </div>
                    <span className={club.pendingJoinRequestCount > 0 ? styles.statusPending : styles.statusNeutral}>
                      {club.pendingJoinRequestCount > 0
                        ? tClubs("dashboard.managedClubs.pendingTag", { count: club.pendingJoinRequestCount })
                        : tClubs("dashboard.managedClubs.stableTag")}
                    </span>
                  </div>

                  <p className={styles.clubDescription}>{club.description || tClubs("common.descriptionFallback")}</p>

                  <dl className={styles.metaGrid}>
                    <div className={styles.metaBox}>
                      <dt>{tClubs("dashboard.managedClubs.memberCount")}</dt>
                      <dd>{club.memberCount}</dd>
                    </div>
                    <div className={styles.metaBox}>
                      <dt>{tClubs("dashboard.managedClubs.pendingCount")}</dt>
                      <dd>{club.pendingJoinRequestCount}</dd>
                    </div>
                    <div className={styles.metaBox}>
                      <dt>{tClubs("dashboard.managedClubs.draftCount")}</dt>
                      <dd>{club.draftActivityCount}</dd>
                    </div>
                  </dl>

                  <div className={styles.actions}>
                    <button type="button" className={styles.primaryButton} onClick={() => router.push(`/club-admin/clubs/${club.clubId}`, { scroll: true })}>
                      <i className="fas fa-arrow-right" />
                      {tClubs("dashboard.managedClubs.openWorkspace")}
                    </button>
                    {club.clubName === PRIMARY_APP_CLUB_NAME ? (
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        onClick={() => router.push(`/club-admin/clubs/${club.clubId}/app-workspace`, { scroll: true })}
                      >
                        <i className="fas fa-mobile-screen-button" />
                        {tAppWorkspace("actions.open")}
                      </button>
                    ) : null}
                    <button type="button" className={styles.secondaryButton} onClick={() => router.push(`/club-admin/clubs/${club.clubId}/positions`, { scroll: true })}>
                      <i className="fas fa-user-tag" />
                      {tClubs("dashboard.managedClubs.managePositions")}
                    </button>
                  </div>
                </article>
              ))}
            </div>
            <PaginationBar
              currentPage={pagedClubs.page}
              totalPages={pagedClubs.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedClubs.page, total: pagedClubs.totalPages })}
              onPageChange={setManagedClubPage}
            />
            </>
          ) : (
            <div className={styles.emptyPanel}>
              <p>{tClubs("dashboard.managedClubs.empty")}</p>
            </div>
          )}
        </section>
      </section>
    </PortalShell>
  );

  async function loadSummary() {
    setSummaryLoading(true);
    try {
      const response = await fetchNotificationSummaryRequest();
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as NotificationSummaryResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        return;
      }
      setSummary(result.data);
    } finally {
      setSummaryLoading(false);
    }
  }

  async function handleSummaryItemClick(notificationId: number) {
    const item = summary.latestUnread.find((entry) => entry.id === notificationId);
    if (!item) {
      router.push("/club-admin/notifications", { scroll: true });
      return;
    }

    const updated = await markSummaryItemAsRead(item);
    if (updated.targetPath) {
      router.push(updated.targetPath, { scroll: true });
      return;
    }
    router.push("/club-admin/notifications", { scroll: true });
  }

  async function markSummaryItemAsRead(item: NotificationItem) {
    const response = await markNotificationReadRequest(item.id);
    if (!response) {
      return item;
    }
    if (!(await portal.handleAuthStatus(response.status))) {
      return item;
    }
    if (response.ok) {
      setSummary((current) => ({
        unreadCount: Math.max(0, current.unreadCount - 1),
        latestUnread: current.latestUnread.filter((entry) => entry.id !== item.id)
      }));
      return { ...item, status: "READ" as const };
    }
    return item;
  }
}
