"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { NotificationSummaryCard } from "../notifications/NotificationSummaryCard";
import { logoutAdminSession } from "../../lib/admin-system/session";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { fetchNotificationSummaryRequest, markNotificationReadRequest } from "../../lib/notifications/client";
import { NotificationItem, NotificationSummary } from "../../lib/notifications/types";
import styles from "../../styles/sysAdmin.module.css";

type StatTone = "blue" | "green" | "orange" | "purple";

type DashboardStats = {
  totalUsers: number;
  uniqueVisitors: number;
  todayVisitCount: number;
  onlineSessions: number;
};

type StatCardConfig = {
  icon: string;
  tone: StatTone;
  valueKey: keyof DashboardStats;
  titleKey: string;
  descriptionKey: string;
};

type MenuItemConfig = {
  icon: string;
  titleKey?: string;
  descriptionKey?: string;
  title?: string;
  description?: string;
  path: string;
  enabled: boolean;
};

type NotificationSummaryResponse = {
  success?: boolean;
  data?: NotificationSummary;
};

const statCards: StatCardConfig[] = [
  {
    icon: "fas fa-users",
    tone: "blue",
    valueKey: "totalUsers",
    titleKey: "stats.users.title",
    descriptionKey: "stats.users.description"
  },
  {
    icon: "fas fa-eye",
    tone: "green",
    valueKey: "uniqueVisitors",
    titleKey: "stats.visitors.title",
    descriptionKey: "stats.visitors.description"
  },
  {
    icon: "fas fa-calendar-day",
    tone: "orange",
    valueKey: "todayVisitCount",
    titleKey: "stats.today.title",
    descriptionKey: "stats.today.description"
  },
  {
    icon: "fas fa-user-clock",
    tone: "purple",
    valueKey: "onlineSessions",
    titleKey: "stats.sessions.title",
    descriptionKey: "stats.sessions.description"
  }
];

const baseMenuItems: MenuItemConfig[] = [
  {
    icon: "fas fa-home",
    titleKey: "menu.homeConfig.title",
    descriptionKey: "menu.homeConfig.description",
    path: "/sys-admin/home-config",
    enabled: true
  },
  {
    icon: "fas fa-newspaper",
    titleKey: "menu.newsManage.title",
    descriptionKey: "menu.newsManage.description",
    path: "/sys-admin/news-manage",
    enabled: true
  },
  {
    icon: "fas fa-users-cog",
    titleKey: "menu.userManage.title",
    descriptionKey: "menu.userManage.description",
    path: "/sys-admin/user-manage",
    enabled: true
  },
  {
    icon: "fas fa-sitemap",
    titleKey: "menu.clubManage.title",
    descriptionKey: "menu.clubManage.description",
    path: "/sys-admin/club-manage",
    enabled: true
  },
  {
    icon: "fas fa-coins",
    titleKey: "menu.scoreReward.title",
    descriptionKey: "menu.scoreReward.description",
    path: "/sys-admin/score-reward",
    enabled: true
  },
  {
    icon: "fas fa-star",
    titleKey: "menu.clubRecommend.title",
    descriptionKey: "menu.clubRecommend.description",
    path: "/sys-admin/club-recommend",
    enabled: true
  },
  {
    icon: "fas fa-calendar-alt",
    titleKey: "menu.activityManage.title",
    descriptionKey: "menu.activityManage.description",
    path: "/sys-admin/activity-manage",
    enabled: true
  },
  {
    icon: "fas fa-chart-bar",
    titleKey: "menu.dataStatistics.title",
    descriptionKey: "menu.dataStatistics.description",
    path: "/sys-admin/data-statistics",
    enabled: true
  },
  {
    icon: "fas fa-images",
    titleKey: "menu.loginCarousel.title",
    descriptionKey: "menu.loginCarousel.description",
    path: "/sys-admin/login-carousel",
    enabled: true
  },
  {
    icon: "fas fa-cog",
    titleKey: "menu.systemSettings.title",
    descriptionKey: "menu.systemSettings.description",
    path: "/sys-admin/system-settings",
    enabled: true
  },
  {
    icon: "fas fa-clock-rotate-left",
    titleKey: "menu.versionChangelog.title",
    descriptionKey: "menu.versionChangelog.description",
    path: "/sys-admin/version-changelog",
    enabled: true
  },
  {
    icon: "fas fa-trophy",
    titleKey: "menu.competitionTracking.title",
    descriptionKey: "menu.competitionTracking.description",
    path: "/sys-admin/competitions",
    enabled: true
  }
];

const STATS_REFRESH_MS = 60000;
const defaultStats: DashboardStats = {
  totalUsers: 0,
  uniqueVisitors: 0,
  todayVisitCount: 0,
  onlineSessions: 0
};
const defaultSummary: NotificationSummary = {
  unreadCount: 0,
  latestUnread: []
};

export function SysAdminDashboard() {
  const t = useT("adminDashboard");
  const tAppReleases = useT("adminAppReleases");
  const tPortal = useT("portal");
  const tNotifications = useT("notifications");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);
  const [stats, setStats] = useState<DashboardStats>(defaultStats);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summary, setSummary] = useState<NotificationSummary>(defaultSummary);
  const statsNotifiedRef = useRef(false);
  const menuItems: MenuItemConfig[] = [
    ...baseMenuItems,
    {
      icon: "fas fa-mobile-screen-button",
      title: tAppReleases("menu.title"),
      description: tAppReleases("menu.description"),
      path: "/sys-admin/app-releases",
      enabled: true
    }
  ];

  const handleMenuClick = (item: MenuItemConfig) => {
    if (!item.enabled) {
      notify.info(t("messages.comingSoon", { name: resolveMenuLabel(item, t) }));
      return;
    }
    router.push(item.path);
  };

  const handleLogout = async () => {
    if (logoutSubmitting) {
      return;
    }
    setLogoutSubmitting(true);
    await logoutAdminSession();
    router.replace("/login");
  };

  useEffect(() => {
    if (!authorized) {
      return;
    }

    let disposed = false;

    const loadStats = async () => {
      const response = await authorizedFetch("/api/admin/stats", {
        method: "GET",
        cache: "no-store"
      });
      if (!response) {
        return;
      }

      try {
        const result = (await response.json().catch(() => null)) as
          | {
              success?: boolean;
              code?: string;
              data?: DashboardStats;
            }
          | null;

        if (response.status === 401) {
          notify.warning(tPortal("auth.loginRequired"));
          await logoutAdminSession();
          router.replace("/login");
          return;
        }

        if (response.status === 403) {
          notify.error(tPortal("auth.noPermission"));
          router.replace("/login");
          return;
        }

        if (!response.ok || result?.success !== true || !result.data) {
          if (!statsNotifiedRef.current) {
            notify.warning(t("stats.loadFailed"));
            statsNotifiedRef.current = true;
          }
          return;
        }

        if (!disposed) {
          setStats({
            totalUsers: Number(result.data.totalUsers || 0),
            uniqueVisitors: Number(result.data.uniqueVisitors || 0),
            todayVisitCount: Number(result.data.todayVisitCount || 0),
            onlineSessions: Number(result.data.onlineSessions || 0)
          });
          statsNotifiedRef.current = false;
        }
      } catch {
        if (!statsNotifiedRef.current) {
          notify.warning(t("stats.loadFailed"));
          statsNotifiedRef.current = true;
        }
      }
    };

    void loadStats();
    const timer = setInterval(() => {
      void loadStats();
    }, STATS_REFRESH_MS);

    return () => {
      disposed = true;
      clearInterval(timer);
    };
  }, [authorized, notify, router, t, tPortal]);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadSummary();
  }, [authorized]);

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

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <h1>
            <i className="fas fa-user-shield" />
            {t("header.title")}
          </h1>
          <button
            type="button"
            className={styles.logoutButton}
            onClick={handleLogout}
            disabled={logoutSubmitting}
          >
            <i className="fas fa-sign-out-alt" />
            {logoutSubmitting ? t("header.loggingOut") : t("header.logout")}
          </button>
        </div>
      </header>

      <div className={styles.container}>
        <div className={styles.stack}>
          <section className={styles.statsGrid} aria-label={t("stats.sectionLabel")}>
            {statCards.map((card) => (
              <article key={card.titleKey} className={styles.statCard}>
                <div className={`${styles.statIcon} ${styles[card.tone]}`}>
                  <i className={card.icon} />
                </div>
                <div className={styles.statInfo}>
                  <h2>{stats[card.valueKey]}</h2>
                  <p>{t(card.titleKey)}</p>
                  <span>{t(card.descriptionKey)}</span>
                </div>
              </article>
            ))}
          </section>

          <NotificationSummaryCard
            title={tNotifications("common.summaryTitle")}
            description={tNotifications("admin.summaryDescription")}
            unreadTag={tNotifications("admin.unreadTag", { count: summary.unreadCount })}
            loadingLabel={tNotifications("common.loadingSummary")}
            emptyLabel={tNotifications("common.emptyUnread")}
            timeFallbackLabel={tNotifications("common.timeFallback")}
            unreadBadgeLabel={tNotifications("common.badges.unread")}
            viewAllLabel={tNotifications("common.viewAll")}
            summary={summary}
            loading={summaryLoading}
            onOpenAll={() => router.push("/sys-admin/notifications")}
            onItemClick={(notificationId) => void handleSummaryItemClick(notificationId)}
          />

          <section className={styles.card}>
            <h2 className={styles.sectionTitle}>
              <i className="fas fa-th-large" />
              {t("menu.sectionTitle")}
            </h2>
            <div className={styles.menuGrid}>
              {menuItems.map((item) => (
                <button
                  key={item.path}
                  type="button"
                  className={styles.menuItem}
                  onClick={() => {
                    handleMenuClick(item);
                  }}
                >
                  <i className={item.icon} />
                  <h3>{resolveMenuLabel(item, t)}</h3>
                  <p>{resolveMenuDescription(item, t)}</p>
                </button>
              ))}
            </div>
          </section>
        </div>
      </div>
    </main>
  );

  async function loadSummary() {
    setSummaryLoading(true);
    try {
      const response = await fetchNotificationSummaryRequest();
      if (!response) {
        return;
      }

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        await logoutAdminSession();
        router.replace("/login");
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
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
      router.push("/sys-admin/notifications");
      return;
    }

    const updated = await markUnreadNotification(item);
    router.push(updated.targetPath || "/sys-admin/notifications");
  }

  async function markUnreadNotification(item: NotificationItem) {
    const response = await markNotificationReadRequest(item.id);
    if (response?.status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      await logoutAdminSession();
      router.replace("/login");
      return item;
    }
    if (response?.status === 403) {
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return item;
    }
    if (response?.ok) {
      setSummary((current) => ({
        unreadCount: Math.max(0, current.unreadCount - 1),
        latestUnread: current.latestUnread.filter((entry) => entry.id !== item.id)
      }));
      return { ...item, status: "READ" as const };
    }
    return item;
  }
}

function resolveMenuLabel(item: MenuItemConfig, t: (key: string) => string) {
  return item.title || (item.titleKey ? t(item.titleKey) : item.path);
}

function resolveMenuDescription(item: MenuItemConfig, t: (key: string) => string) {
  return item.description || (item.descriptionKey ? t(item.descriptionKey) : "");
}
