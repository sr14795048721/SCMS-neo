"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import {
  createStudentRewardOrderRequest,
  fetchStudentClubRankingsRequest,
  fetchStudentRewardImageRequest,
  fetchStudentRewardOrdersRequest,
  fetchStudentRewardsRequest,
  fetchStudentSchoolRankingsRequest,
  fetchStudentScoreSummaryRequest,
  StudentClubRankingsResponse,
  StudentRewardOrdersResponse,
  StudentRewardsResponse,
  StudentSchoolRankingsResponse,
  StudentScoreSummaryResponse
} from "../../lib/student/client";
import { useStudentPortalState } from "../../lib/student/useStudentPortalState";
import {
  StudentClubRankingItem,
  StudentRewardItem,
  StudentRewardOrder,
  StudentSchoolRankingItem,
  StudentScoreSummary
} from "../../lib/student/types";
import { paginateItems } from "../../lib/pagination";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/studentFeaturePages.module.css";

const EMPTY_SUMMARY: StudentScoreSummary = {
  totalScore: 0,
  redeemedScore: 0,
  balanceScore: 0,
  clubs: []
};

const REWARDS_PAGE_SIZE = 4;

export function StudentScoreRewardsPage() {
  const t = useT("studentScoreRewards");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useStudentPortalState(t("common.defaultName"));
  const [summary, setSummary] = useState<StudentScoreSummary>(EMPTY_SUMMARY);
  const [clubRankings, setClubRankings] = useState<StudentClubRankingItem[]>([]);
  const [schoolRankings, setSchoolRankings] = useState<StudentSchoolRankingItem[]>([]);
  const [rewards, setRewards] = useState<StudentRewardItem[]>([]);
  const [orders, setOrders] = useState<StudentRewardOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [redeemingRewardId, setRedeemingRewardId] = useState<number | null>(null);
  const [rewardAssetUrls, setRewardAssetUrls] = useState<Record<number, string>>({});
  const [rewardPage, setRewardPage] = useState(1);
  const [orderPage, setOrderPage] = useState(1);
  const [clubRankingPage, setClubRankingPage] = useState(1);
  const [schoolRankingPage, setSchoolRankingPage] = useState(1);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    return () => {
      Object.values(rewardAssetUrls).forEach((url) => {
        if (url.startsWith("blob:")) {
          URL.revokeObjectURL(url);
        }
      });
    };
  }, [rewardAssetUrls]);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [portal.authorized]);

  const primaryClub = summary.clubs[0] || null;
  const latestOrder = orders[0] || null;
  const pendingOrderCount = useMemo(() => orders.filter((item) => item.status === "PENDING").length, [orders]);
  const pagedRewards = useMemo(() => paginateItems(rewards, rewardPage, REWARDS_PAGE_SIZE), [rewardPage, rewards]);
  const pagedOrders = useMemo(() => paginateItems(orders, orderPage, 6), [orderPage, orders]);
  const pagedClubRankings = useMemo(() => paginateItems(clubRankings, clubRankingPage, 6), [clubRankingPage, clubRankings]);
  const pagedSchoolRankings = useMemo(() => paginateItems(schoolRankings, schoolRankingPage, 6), [schoolRankingPage, schoolRankings]);

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
            <strong>{summary.totalScore}</strong>
            <p>{t("summary.totalHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("summary.balanceLabel")}</span>
            <strong>{summary.balanceScore}</strong>
            <p>{t("summary.balanceHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("summary.redeemedLabel")}</span>
            <strong>{summary.redeemedScore}</strong>
            <p>{t("summary.redeemedHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("summary.pendingLabel")}</span>
            <strong>{pendingOrderCount}</strong>
            <p>{t("summary.pendingHint")}</p>
          </article>
        </section>

        <section className={styles.layout}>
          <div className={styles.column}>
            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h2 className={styles.panelTitle}>
                    <i className="fas fa-gift" />
                    {t("rewards.title")}
                  </h2>
                  <p className={styles.panelDescription}>{t("rewards.description")}</p>
                </div>
                <div className={styles.panelActions}>
                  <button type="button" className={styles.secondaryButton} onClick={() => void loadData()} disabled={loading}>
                    <i className="fas fa-rotate-right" />
                    {loading ? t("common.loading") : t("common.refresh")}
                  </button>
                </div>
              </div>

              {loading ? (
                <div className={styles.emptyPanel}>
                  <p>{t("common.loading")}</p>
                </div>
              ) : rewards.length ? (
                <>
                <div className={styles.rewardGrid}>
                  {pagedRewards.items.map((reward) => {
                    const imageUrl = rewardAssetUrls[reward.id];
                    const canRedeem = summary.balanceScore >= reward.scoreCost && reward.stock > 0;

                    return (
                      <article key={reward.id} className={styles.rewardCard}>
                        <div className={styles.rewardImageFrame}>
                          {imageUrl ? (
                            <img src={imageUrl} alt={reward.name} />
                          ) : (
                            <span className={styles.rewardImagePlaceholder}>
                              <i className="fas fa-gift" />
                            </span>
                          )}
                        </div>
                        <div className={styles.head}>
                          <div>
                            <h3 className={styles.title}>{reward.name}</h3>
                            <p className={styles.clubName}>{t("rewards.scoreCostValue", { value: reward.scoreCost })}</p>
                          </div>
                          <span className={`${styles.statusBadge} ${reward.stock > 0 ? styles.statusActive : styles.statusInactive}`}>
                            {reward.stock > 0 ? t("rewards.inStock") : t("rewards.outOfStock")}
                          </span>
                        </div>
                        <div className={styles.rewardFoot}>
                          <div className={styles.listRow}>
                            <span className={styles.subtleText}>{t("rewards.stockLabel")}</span>
                            <strong className={styles.tablePrimary}>{reward.stock}</strong>
                          </div>
                          <button
                            type="button"
                            className={canRedeem ? styles.primaryButton : styles.secondaryButton}
                            disabled={redeemingRewardId === reward.id || !canRedeem}
                            onClick={() => {
                              void handleRedeem(reward);
                            }}
                          >
                            <i className="fas fa-cart-plus" />
                            {redeemingRewardId === reward.id
                              ? t("rewards.submitting")
                              : !canRedeem
                                ? t("rewards.balanceBlocked")
                                : t("rewards.redeem")}
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
                <PaginationBar
                  currentPage={pagedRewards.page}
                  totalPages={pagedRewards.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedRewards.page, total: pagedRewards.totalPages })}
                  onPageChange={setRewardPage}
                />
                </>
              ) : (
                <div className={styles.emptyPanel}>
                  <p>{t("rewards.empty")}</p>
                </div>
              )}
            </article>

            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h2 className={styles.panelTitle}>
                    <i className="fas fa-receipt" />
                    {t("orders.title")}
                  </h2>
                  <p className={styles.panelDescription}>{t("orders.description")}</p>
                </div>
              </div>

              {loading ? (
                <div className={styles.emptyPanel}>
                  <p>{t("common.loading")}</p>
                </div>
              ) : orders.length ? (
                <>
                <div className={styles.ordersList}>
                  {pagedOrders.items.map((order) => (
                    <article key={order.id} className={styles.orderCard}>
                      <div className={styles.head}>
                        <div>
                          <h3 className={styles.title}>{order.rewardName || t("orders.unknownReward")}</h3>
                          <p className={styles.clubName}>#{order.id}</p>
                        </div>
                        <span
                          className={`${styles.statusBadge} ${
                            order.status === "COMPLETED"
                              ? styles.statusActive
                              : order.status === "REJECTED"
                                ? styles.statusInactive
                                : styles.statusPending
                          }`}
                        >
                          {t(`orders.status.${order.status.toLowerCase()}`)}
                        </span>
                      </div>
                      <div className={styles.orderMeta}>
                        <div className={styles.listRow}>
                          <span className={styles.subtleText}>{t("orders.scoreCostLabel")}</span>
                          <strong className={styles.tablePrimary}>{order.scoreCost}</strong>
                        </div>
                        <div className={styles.listRow}>
                          <span className={styles.subtleText}>{t("orders.createdAtLabel")}</span>
                          <strong className={styles.tablePrimary}>{formatDateTime(order.createdAt, t("common.timeFallback"))}</strong>
                        </div>
                        <div className={styles.listRow}>
                          <span className={styles.subtleText}>{t("orders.processedAtLabel")}</span>
                          <strong className={styles.tablePrimary}>
                            {formatDateTime(
                              order.status === "REJECTED" ? order.rejectedAt : order.completedAt,
                              t("common.timeFallback")
                            )}
                          </strong>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <PaginationBar
                  currentPage={pagedOrders.page}
                  totalPages={pagedOrders.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedOrders.page, total: pagedOrders.totalPages })}
                  onPageChange={setOrderPage}
                />
                </>
              ) : (
                <div className={styles.emptyPanel}>
                  <p>{t("orders.empty")}</p>
                </div>
              )}
            </article>
          </div>

          <div className={styles.column}>
            <article className={styles.summaryCard}>
              <div className={styles.heroCopy}>
                <span className={styles.summaryLabel}>{t("side.latestOrderLabel")}</span>
                <strong className={styles.heroValue}>{summary.balanceScore}</strong>
                <p className={styles.summaryHint}>{t("side.balanceHint")}</p>
              </div>
              <div className={styles.actionRow}>
                <span className={`${styles.statusBadge} ${styles.statusNeutral}`}>
                  {primaryClub ? t("side.clubTag", { name: primaryClub.clubName }) : t("side.noClubTag")}
                </span>
                <span className={`${styles.statusBadge} ${styles.statusNeutral}`}>
                  {latestOrder ? t(`orders.status.${latestOrder.status.toLowerCase()}`) : t("side.noOrderTag")}
                </span>
              </div>
            </article>

            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h2 className={styles.panelTitle}>
                    <i className="fas fa-trophy" />
                    {t("rankings.clubTitle")}
                  </h2>
                  <p className={styles.panelDescription}>
                    {primaryClub
                      ? t("rankings.clubDescription", { name: primaryClub.clubName })
                      : t("rankings.clubEmptyHint")}
                  </p>
                </div>
              </div>
              {loading ? (
                <div className={styles.emptyPanel}>
                  <p>{t("common.loading")}</p>
                </div>
              ) : clubRankings.length ? (
                <>
                <div className={`${styles.rankList} ${styles.panelContentList}`}>
                  {pagedClubRankings.items.map((item) => (
                    <article key={item.userId} className={styles.rankingCard}>
                      <div className={styles.listRow}>
                        <strong className={styles.tablePrimary}>{t("rankings.rankValue", { value: item.rank })}</strong>
                        <span className={`${styles.statusBadge} ${styles.statusNeutral}`}>
                          {t("rankings.scoreValue", { value: item.score })}
                        </span>
                      </div>
                      <div className={styles.rankingMeta}>
                        <div className={styles.tablePrimary}>{item.displayName || t("common.nameFallback")}</div>
                      </div>
                    </article>
                  ))}
                </div>
                <PaginationBar
                  currentPage={pagedClubRankings.page}
                  totalPages={pagedClubRankings.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedClubRankings.page, total: pagedClubRankings.totalPages })}
                  onPageChange={setClubRankingPage}
                />
                </>
              ) : (
                <div className={styles.emptyPanel}>
                  <p>{t("rankings.clubEmpty")}</p>
                </div>
              )}
            </article>

            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h2 className={styles.panelTitle}>
                    <i className="fas fa-school" />
                    {t("rankings.schoolTitle")}
                  </h2>
                  <p className={styles.panelDescription}>{t("rankings.schoolDescription")}</p>
                </div>
              </div>
              {loading ? (
                <div className={styles.emptyPanel}>
                  <p>{t("common.loading")}</p>
                </div>
              ) : schoolRankings.length ? (
                <>
                <div className={`${styles.rankList} ${styles.panelContentList}`}>
                  {pagedSchoolRankings.items.map((item) => (
                    <article key={item.clubId} className={styles.rankingCard}>
                      <div className={styles.listRow}>
                        <strong className={styles.tablePrimary}>{t("rankings.rankValue", { value: item.rank })}</strong>
                        <span className={`${styles.statusBadge} ${styles.statusNeutral}`}>
                          {t("rankings.totalScoreValue", { value: item.totalScore })}
                        </span>
                      </div>
                      <div className={styles.rankingMeta}>
                        <div className={styles.tablePrimary}>{item.clubName || t("common.notSet")}</div>
                        <div className={styles.tableSecondary}>
                          {t("rankings.memberCountValue", { value: item.memberCount })}
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <PaginationBar
                  currentPage={pagedSchoolRankings.page}
                  totalPages={pagedSchoolRankings.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedSchoolRankings.page, total: pagedSchoolRankings.totalPages })}
                  onPageChange={setSchoolRankingPage}
                />
                </>
              ) : (
                <div className={styles.emptyPanel}>
                  <p>{t("rankings.schoolEmpty")}</p>
                </div>
              )}
            </article>
          </div>
        </section>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const summaryResponse = await fetchStudentScoreSummaryRequest();
      if (!summaryResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(summaryResponse.status))) {
        return;
      }

      const summaryResult = (await summaryResponse.json().catch(() => null)) as StudentScoreSummaryResponse | null;
      const nextSummary = summaryResponse.ok && summaryResult?.success && summaryResult.data ? summaryResult.data : EMPTY_SUMMARY;
      setSummary(nextSummary);

      const primaryClubId = nextSummary.clubs[0]?.clubId || 0;
      const [clubRankingsResponse, schoolRankingsResponse, rewardsResponse, ordersResponse] = await Promise.all([
        primaryClubId ? fetchStudentClubRankingsRequest(primaryClubId, "all") : Promise.resolve(null),
        fetchStudentSchoolRankingsRequest(),
        fetchStudentRewardsRequest(),
        fetchStudentRewardOrdersRequest()
      ]);

      const followUpResponses = [clubRankingsResponse, schoolRankingsResponse, rewardsResponse, ordersResponse].filter(Boolean) as Response[];
      for (const response of followUpResponses) {
        if (!(await portal.handleAuthStatus(response.status))) {
          return;
        }
      }

      const clubRankingsResult = clubRankingsResponse
        ? ((await clubRankingsResponse.json().catch(() => null)) as StudentClubRankingsResponse | null)
        : null;
      const schoolRankingsResult = schoolRankingsResponse
        ? ((await schoolRankingsResponse.json().catch(() => null)) as StudentSchoolRankingsResponse | null)
        : null;
      const rewardsResult = rewardsResponse
        ? ((await rewardsResponse.json().catch(() => null)) as StudentRewardsResponse | null)
        : null;
      const ordersResult = ordersResponse
        ? ((await ordersResponse.json().catch(() => null)) as StudentRewardOrdersResponse | null)
        : null;

      setClubRankings(
        clubRankingsResponse?.ok && clubRankingsResult?.success && Array.isArray(clubRankingsResult.data)
          ? clubRankingsResult.data
          : []
      );
      setSchoolRankings(
        schoolRankingsResponse?.ok && schoolRankingsResult?.success && Array.isArray(schoolRankingsResult.data)
          ? schoolRankingsResult.data
          : []
      );

      const nextRewards =
        rewardsResponse?.ok && rewardsResult?.success && Array.isArray(rewardsResult.data) ? rewardsResult.data : [];
      setRewards(nextRewards);
      setRewardPage(1);
      setOrders(
        ordersResponse?.ok && ordersResult?.success && Array.isArray(ordersResult.data) ? ordersResult.data : []
      );
      setOrderPage(1);
      setClubRankingPage(1);
      setSchoolRankingPage(1);
      await loadRewardAssets(nextRewards);
    } finally {
      setLoading(false);
    }
  }

  async function handleRedeem(reward: StudentRewardItem) {
    if (redeemingRewardId) {
      return;
    }

    setRedeemingRewardId(reward.id);
    try {
      const response = await createStudentRewardOrderRequest(reward.id);
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
        const message = String(result?.message || "");
        if (message.includes("insufficient score balance")) {
          notify.warning(t("messages.balanceInsufficient"));
        } else if (message.includes("out of stock")) {
          notify.warning(t("messages.outOfStock"));
        } else {
          notify.error(result?.message || t("messages.redeemFailed"));
        }
        return;
      }

      notify.success(t("messages.redeemSuccess", { name: reward.name }));
      await loadData();
    } finally {
      setRedeemingRewardId(null);
    }
  }

  async function loadRewardAssets(items: StudentRewardItem[]) {
    const nextEntries = await Promise.all(
      items
        .filter((item) => item.hasImage)
        .map(async (item) => {
          try {
            const response = await fetchStudentRewardImageRequest(item.id, item.updatedAt);
            if (!response || !response.ok) {
              return null;
            }
            const blob = await response.blob();
            return [item.id, URL.createObjectURL(blob)] as const;
          } catch {
            return null;
          }
        })
    );

    setRewardAssetUrls((current) => {
      Object.values(current).forEach((url) => {
        if (url.startsWith("blob:")) {
          URL.revokeObjectURL(url);
        }
      });
      const nextUrls: Record<number, string> = {};
      nextEntries.forEach((entry) => {
        if (entry) {
          nextUrls[entry[0]] = entry[1];
        }
      });
      return nextUrls;
    });
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
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
