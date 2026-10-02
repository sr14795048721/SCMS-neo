"use client";

import { CSSProperties, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminStatisticsResponse, fetchAdminStatisticsRequest } from "../../lib/admin-system/client";
import { AdminStatistics } from "../../lib/admin-system/types";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import styles from "../../styles/sysAdminDataStatistics.module.css";

type StatTone = "blue" | "green" | "orange" | "purple";

type OverviewCard = {
  icon: string;
  tone: StatTone;
  value: number;
  titleKey: string;
  descriptionKey: string;
};

const EMPTY_STATISTICS: AdminStatistics = {
  overview: {
    totalUsers: 0,
    activeUsers: 0,
    totalClubs: 0,
    totalActivities: 0,
    activeRegistrations: 0,
    uniqueVisitors: 0,
    todayVisitCount: 0,
    onlineSessions: 0
  },
  roleDistribution: {
    studentCount: 0,
    managerCount: 0,
    adminCount: 0
  },
  clubRankings: []
};

export function SysAdminDataStatistics() {
  const t = useT("adminDataStatistics");
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [statistics, setStatistics] = useState<AdminStatistics>(EMPTY_STATISTICS);
  const [pageLoading, setPageLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const overviewCards: OverviewCard[] = useMemo(
    () => [
      {
        icon: "fas fa-users",
        tone: "blue",
        value: statistics.overview.totalUsers,
        titleKey: "overview.cards.totalUsers.title",
        descriptionKey: "overview.cards.totalUsers.description"
      },
      {
        icon: "fas fa-user-check",
        tone: "green",
        value: statistics.overview.activeUsers,
        titleKey: "overview.cards.activeUsers.title",
        descriptionKey: "overview.cards.activeUsers.description"
      },
      {
        icon: "fas fa-sitemap",
        tone: "orange",
        value: statistics.overview.totalClubs,
        titleKey: "overview.cards.totalClubs.title",
        descriptionKey: "overview.cards.totalClubs.description"
      },
      {
        icon: "fas fa-calendar-check",
        tone: "purple",
        value: statistics.overview.totalActivities,
        titleKey: "overview.cards.totalActivities.title",
        descriptionKey: "overview.cards.totalActivities.description"
      }
    ],
    [statistics]
  );

  const roleLegend = useMemo(() => {
    const total =
      statistics.roleDistribution.studentCount +
      statistics.roleDistribution.managerCount +
      statistics.roleDistribution.adminCount;

    return [
      {
        label: t("charts.roleDistribution.student"),
        value: statistics.roleDistribution.studentCount,
        percent: total > 0 ? Math.round((statistics.roleDistribution.studentCount / total) * 100) : 0,
        color: "#1976d2"
      },
      {
        label: t("charts.roleDistribution.manager"),
        value: statistics.roleDistribution.managerCount,
        percent: total > 0 ? Math.round((statistics.roleDistribution.managerCount / total) * 100) : 0,
        color: "#388e3c"
      },
      {
        label: t("charts.roleDistribution.admin"),
        value: statistics.roleDistribution.adminCount,
        percent: total > 0 ? Math.round((statistics.roleDistribution.adminCount / total) * 100) : 0,
        color: "#f57c00"
      }
    ];
  }, [statistics, t]);

  const roleChartStyle = useMemo<CSSProperties>(() => {
    const student = statistics.roleDistribution.studentCount;
    const manager = statistics.roleDistribution.managerCount;
    const admin = statistics.roleDistribution.adminCount;
    const total = student + manager + admin;

    if (total <= 0) {
      return {
        background: "conic-gradient(#d8dee8 0deg 360deg)"
      };
    }

    const studentDeg = (student / total) * 360;
    const managerDeg = (manager / total) * 360;

    return {
      background: `conic-gradient(#1976d2 0deg ${studentDeg}deg, #388e3c ${studentDeg}deg ${
        studentDeg + managerDeg
      }deg, #f57c00 ${studentDeg + managerDeg}deg 360deg)`
    };
  }, [statistics]);

  const comparisonRows = useMemo(() => {
    const rows = [
      {
        label: t("charts.coreMetrics.totalUsers"),
        value: statistics.overview.totalUsers,
        color: "#1976d2"
      },
      {
        label: t("charts.coreMetrics.totalClubs"),
        value: statistics.overview.totalClubs,
        color: "#388e3c"
      },
      {
        label: t("charts.coreMetrics.totalActivities"),
        value: statistics.overview.totalActivities,
        color: "#f57c00"
      },
      {
        label: t("charts.coreMetrics.activeRegistrations"),
        value: statistics.overview.activeRegistrations,
        color: "#7b1fa2"
      }
    ];

    const maxValue = Math.max(...rows.map((item) => item.value), 1);
    return rows.map((item) => ({
      ...item,
      widthPercent: Math.max(Math.round((item.value / maxValue) * 100), item.value > 0 ? 8 : 0)
    }));
  }, [statistics, t]);

  useEffect(() => {
    if (authorized) {
      void loadStatistics(true);
    }
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

  if (pageLoading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{t("states.loadingPage")}</p>
        </section>
      </main>
    );
  }

  async function loadStatistics(initial = false) {
    if (initial) {
      setPageLoading(true);
    } else {
      setRefreshing(true);
    }

    try {
      const response = await fetchAdminStatisticsRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as AdminStatisticsResponse | null;

      if (response.status === 401) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 403) {
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }

      setStatistics(result.data);
    } catch {
      notify.error(t("messages.loadFailed"));
    } finally {
      if (initial) {
        setPageLoading(false);
      } else {
        setRefreshing(false);
      }
    }
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerCopy}>
            <h1>
              <i className="fas fa-chart-bar" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => {
                void loadStatistics(false);
              }}
              disabled={refreshing}
            >
              <i className="fas fa-rotate-right" />
              {refreshing ? t("actions.refreshing") : t("actions.refresh")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.statsGrid}>
          {overviewCards.map((card) => (
            <article key={card.titleKey} className={styles.statCard}>
              <div className={`${styles.statIcon} ${styles[card.tone]}`}>
                <i className={card.icon} />
              </div>
              <div className={styles.statInfo}>
                <strong>{card.value}</strong>
                <h2>{t(card.titleKey)}</h2>
                <p>{t(card.descriptionKey)}</p>
              </div>
            </article>
          ))}
        </div>

        <div className={styles.summaryGrid}>
          <article className={styles.summaryCard}>
            <span>{t("overview.panels.uniqueVisitors.label")}</span>
            <strong>{statistics.overview.uniqueVisitors}</strong>
            <p>{t("overview.panels.uniqueVisitors.hint")}</p>
          </article>
          <article className={styles.summaryCard}>
            <span>{t("overview.panels.todayVisitCount.label")}</span>
            <strong>{statistics.overview.todayVisitCount}</strong>
            <p>{t("overview.panels.todayVisitCount.hint")}</p>
          </article>
          <article className={styles.summaryCard}>
            <span>{t("overview.panels.onlineSessions.label")}</span>
            <strong>{statistics.overview.onlineSessions}</strong>
            <p>{t("overview.panels.onlineSessions.hint")}</p>
          </article>
          <article className={styles.summaryCard}>
            <span>{t("overview.panels.activeRegistrations.label")}</span>
            <strong>{statistics.overview.activeRegistrations}</strong>
            <p>{t("overview.panels.activeRegistrations.hint")}</p>
          </article>
        </div>

        <div className={styles.chartGrid}>
          <article className={styles.chartCard}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("charts.roleDistribution.title")}</h2>
                <p>{t("charts.roleDistribution.description")}</p>
              </div>
            </div>
            <div className={styles.roleChartLayout}>
              <div className={styles.roleDonut} style={roleChartStyle}>
                <div className={styles.roleDonutInner}>
                  <span>{t("charts.roleDistribution.totalLabel")}</span>
                  <strong>
                    {statistics.roleDistribution.studentCount +
                      statistics.roleDistribution.managerCount +
                      statistics.roleDistribution.adminCount}
                  </strong>
                </div>
              </div>
              <div className={styles.legendList}>
                {roleLegend.map((item) => (
                  <div key={item.label} className={styles.legendItem}>
                    <span className={styles.legendDot} style={{ backgroundColor: item.color }} />
                    <div className={styles.legendCopy}>
                      <strong>{item.label}</strong>
                      <p>
                        {t("charts.roleDistribution.legendValue", {
                          count: item.value,
                          percent: item.percent
                        })}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </article>

          <article className={styles.chartCard}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("charts.coreMetrics.title")}</h2>
                <p>{t("charts.coreMetrics.description")}</p>
              </div>
            </div>
            <div className={styles.barList}>
              {comparisonRows.map((item) => (
                <div key={item.label} className={styles.barRow}>
                  <div className={styles.barMeta}>
                    <strong>{item.label}</strong>
                    <span>{item.value}</span>
                  </div>
                  <div className={styles.barTrack}>
                    <div
                      className={styles.barFill}
                      style={{
                        width: `${item.widthPercent}%`,
                        backgroundColor: item.color
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </article>
        </div>

        <article className={styles.tableCard}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("ranking.title")}</h2>
              <p>{t("ranking.description")}</p>
            </div>
          </div>

          {statistics.clubRankings.length > 0 ? (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>{t("ranking.columns.rank")}</th>
                    <th>{t("ranking.columns.clubName")}</th>
                    <th>{t("ranking.columns.memberCount")}</th>
                    <th>{t("ranking.columns.activityCount")}</th>
                  </tr>
                </thead>
                <tbody>
                  {statistics.clubRankings.map((item) => (
                    <tr key={item.clubId || item.rank}>
                      <td>{item.rank}</td>
                      <td>{item.clubName || t("ranking.emptyClubName")}</td>
                      <td>{item.memberCount}</td>
                      <td>{item.activityCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className={styles.emptyPanel}>
              <p>{t("ranking.empty")}</p>
            </div>
          )}
        </article>
      </section>
    </main>
  );
}
