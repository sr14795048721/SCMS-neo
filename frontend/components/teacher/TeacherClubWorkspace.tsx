"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { paginateItems } from "../../lib/pagination";
import { formatStudentClassName, normalizeStudentGrade } from "../../lib/student/fieldConstraints";
import {
  approveManagerClubJoinRequest,
  fetchManagerClubDetailRequest,
  fetchManagerClubJoinRequestsRequest,
  fetchManagerClubMembersRequest,
  removeManagerClubMemberRequest,
  rejectManagerClubJoinRequest
} from "../../lib/manager/clubClient";
import {
  createManagerScoreRecordRequest,
  fetchManagerScoreRankingsRequest,
  fetchManagerScoreRecordsRequest,
  fetchManagerScoreRulesRequest
} from "../../lib/manager/scoreClient";
import { ManagerClubDetail, ManagerClubJoinRequest, ManagerClubMember } from "../../lib/manager/clubTypes";
import { ManagerScoreRanking, ManagerScoreRecord, ManagerScoreRule } from "../../lib/manager/scoreTypes";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import { PRIMARY_APP_CLUB_NAME } from "../../lib/club/appIdentity";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherClubs.module.css";

type DetailResponse = {
  success?: boolean;
  data?: ManagerClubDetail;
  message?: string;
};

type MembersResponse = {
  success?: boolean;
  data?: ManagerClubMember[];
  message?: string;
};

type JoinRequestsResponse = {
  success?: boolean;
  data?: ManagerClubJoinRequest[];
  message?: string;
};

type RulesResponse = {
  success?: boolean;
  data?: ManagerScoreRule[];
  message?: string;
};

type RecordsResponse = {
  success?: boolean;
  data?: ManagerScoreRecord[];
  message?: string;
};

type RankingsResponse = {
  success?: boolean;
  data?: ManagerScoreRanking[];
  message?: string;
};

type MutationResponse = {
  success?: boolean;
  message?: string;
};

const RANKING_RANGES = ["all", "week", "month", "term"] as const;

export function TeacherClubWorkspace({ clubId }: { clubId: number }) {
  const t = useT("teacherClubs");
  const tAppWorkspace = useT("teacherAppWorkspace");
  const tAttendance = useT("teacherAttendance");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [detail, setDetail] = useState<ManagerClubDetail | null>(null);
  const [members, setMembers] = useState<ManagerClubMember[]>([]);
  const [joinRequests, setJoinRequests] = useState<ManagerClubJoinRequest[]>([]);
  const [rules, setRules] = useState<ManagerScoreRule[]>([]);
  const [records, setRecords] = useState<ManagerScoreRecord[]>([]);
  const [rankings, setRankings] = useState<ManagerScoreRanking[]>([]);
  const [loading, setLoading] = useState(true);
  const [rankingRange, setRankingRange] = useState<(typeof RANKING_RANGES)[number]>("all");
  const [joinRequestPage, setJoinRequestPage] = useState(1);
  const [memberPage, setMemberPage] = useState(1);
  const [rankingPage, setRankingPage] = useState(1);
  const [recordPage, setRecordPage] = useState(1);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedRuleId, setSelectedRuleId] = useState("");
  const [scoreReason, setScoreReason] = useState("");
  const [scoreSubmitting, setScoreSubmitting] = useState(false);
  const [pendingRequestActionId, setPendingRequestActionId] = useState<number | null>(null);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadAllData("all");
  }, [clubId, portal.authorized]);

  useEffect(() => {
    if (!selectedRuleId) {
      setScoreReason("");
      return;
    }

    const matchedRule = rules.find((item) => String(item.id) === selectedRuleId);
    setScoreReason(matchedRule?.name ?? "");
  }, [rules, selectedRuleId]);

  const pendingRequests = useMemo(
    () => joinRequests.filter((request) => request.status === "PENDING"),
    [joinRequests]
  );
  const pagedPendingRequests = useMemo(() => paginateItems(pendingRequests, joinRequestPage, 8), [joinRequestPage, pendingRequests]);
  const pagedMembers = useMemo(() => paginateItems(members, memberPage, 8), [memberPage, members]);
  const pagedRankings = useMemo(() => paginateItems(rankings, rankingPage, 8), [rankingPage, rankings]);
  const pagedRecords = useMemo(() => paginateItems(records, recordPage, 8), [recordPage, records]);

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
      title={detail?.clubName || t("workspace.metaTitle")}
      subtitle={t("workspace.subtitle")}
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
          <button type="button" className={styles.backButton} onClick={() => router.push("/club-admin")}>
            <i className="fas fa-arrow-left" />
            {t("workspace.back")}
          </button>
          <div className={styles.inlineActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => router.push(`/club-admin/clubs/${clubId}/attendance`)}
            >
              <i className="fas fa-clipboard-check" />
              {tAttendance("workspace.openAttendance")}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => router.push(`/club-admin/clubs/${clubId}/positions`)}
            >
              <i className="fas fa-user-tag" />
              {t("workspace.managePositions")}
            </button>
            {detail?.clubName === PRIMARY_APP_CLUB_NAME ? (
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => router.push(`/club-admin/clubs/${clubId}/app-workspace`)}
              >
                <i className="fas fa-mobile-screen-button" />
                {tAppWorkspace("actions.open")}
              </button>
            ) : null}
          </div>
        </div>

        <article className={styles.hero}>
          <h2 className={styles.heroTitle}>{detail?.clubName || t("workspace.metaTitle")}</h2>
          <p className={styles.heroDescription}>
            {detail?.description || t("common.descriptionFallback")}
          </p>
          <div className={styles.heroMeta}>
            <span className={styles.heroPill}>
              <i className="fas fa-shapes" />
              {detail?.clubType || t("common.typeFallback")}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-user-group" />
              {t("workspace.heroMembers", { count: detail?.memberCount || 0 })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-user-clock" />
              {t("workspace.heroPending", { count: detail?.pendingJoinRequestCount || 0 })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-medal" />
              {t("workspace.heroRecords", { count: records.length })}
            </span>
          </div>
        </article>

        <section className={styles.summaryGrid}>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("workspace.summary.members")}</p>
            <strong className={styles.summaryValue}>{detail?.memberCount || 0}</strong>
            <span className={styles.summaryHelp}>{t("workspace.summary.membersHelp")}</span>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("workspace.summary.pending")}</p>
            <strong className={styles.summaryValue}>{pendingRequests.length}</strong>
            <span className={styles.summaryHelp}>{t("workspace.summary.pendingHelp")}</span>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("workspace.summary.activities")}</p>
            <strong className={styles.summaryValue}>{detail?.activityCount || 0}</strong>
            <span className={styles.summaryHelp}>{t("workspace.summary.activitiesHelp")}</span>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("workspace.summary.rules")}</p>
            <strong className={styles.summaryValue}>{rules.length}</strong>
            <span className={styles.summaryHelp}>{t("workspace.summary.rulesHelp")}</span>
          </article>
        </section>

        <section className={styles.dualGrid}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-user-check" />
                  {t("workspace.joinRequests.title")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.joinRequests.description")}</p>
              </div>
              <span className={styles.tag}>{t("workspace.joinRequests.tag", { count: pendingRequests.length })}</span>
            </div>

            {loading ? (
              <div className={styles.emptyPanel}>
                <p>{t("common.loading")}</p>
              </div>
            ) : pendingRequests.length ? (
              <>
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>{t("workspace.joinRequests.columns.student")}</th>
                      <th>{t("workspace.joinRequests.columns.reason")}</th>
                      <th>{t("workspace.joinRequests.columns.action")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedPendingRequests.items.map((request) => (
                      <tr key={request.requestId}>
                        <td>
                          <strong>{request.displayName || t("common.nameFallback")}</strong>
                        </td>
                        <td>{request.reason || t("workspace.joinRequests.noReason")}</td>
                        <td>
                          <div className={styles.joinRequestActionGroup}>
                            <button
                              type="button"
                              className={styles.primaryButton}
                              disabled={pendingRequestActionId === request.requestId}
                              onClick={() => {
                                void handleJoinRequestAction(request.requestId, "approve");
                              }}
                            >
                              {pendingRequestActionId === request.requestId
                                ? t("workspace.joinRequests.submitting")
                                : t("workspace.joinRequests.approve")}
                            </button>
                            <button
                              type="button"
                              className={styles.dangerButton}
                              disabled={pendingRequestActionId === request.requestId}
                              onClick={() => {
                                void handleJoinRequestAction(request.requestId, "reject");
                              }}
                            >
                              {t("workspace.joinRequests.reject")}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <PaginationBar
                currentPage={pagedPendingRequests.page}
                totalPages={pagedPendingRequests.totalPages}
                prevLabel={tPagination("prev")}
                nextLabel={tPagination("next")}
                pageLabel={tPagination("status", { page: pagedPendingRequests.page, total: pagedPendingRequests.totalPages })}
                onPageChange={setJoinRequestPage}
              />
              </>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("workspace.joinRequests.empty")}</p>
              </div>
            )}
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-medal" />
                  {t("workspace.addScore.title")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.addScore.description")}</p>
              </div>
            </div>

            <div className={styles.formGrid}>
              <div className={styles.field}>
                <label htmlFor="score-member">{t("workspace.addScore.member")}</label>
                <select
                  id="score-member"
                  value={selectedUserId}
                  onChange={(event) => setSelectedUserId(event.target.value)}
                >
                  <option value="">{t("workspace.addScore.memberPlaceholder")}</option>
                      {members.map((member) => (
                        <option key={member.userId} value={String(member.userId)}>
                          {member.displayName || t("common.nameFallback")}
                        </option>
                      ))}
                </select>
              </div>
              <div className={styles.field}>
                <label htmlFor="score-rule">{t("workspace.addScore.rule")}</label>
                <select
                  id="score-rule"
                  value={selectedRuleId}
                  onChange={(event) => {
                    setSelectedRuleId(event.target.value);
                  }}
                >
                  <option value="">{t("workspace.addScore.rulePlaceholder")}</option>
                  {rules
                    .filter((rule) => rule.status === "ACTIVE")
                    .map((rule) => (
                      <option key={rule.id} value={String(rule.id)}>
                        {`${rule.name} (${rule.scoreDelta > 0 ? `+${rule.scoreDelta}` : rule.scoreDelta})`}
                      </option>
                    ))}
                </select>
              </div>
              <div className={styles.field}>
                <label htmlFor="score-reason">{t("workspace.addScore.reason")}</label>
                <textarea
                  id="score-reason"
                  value={scoreReason}
                  onChange={(event) => setScoreReason(event.target.value)}
                  placeholder={t("workspace.addScore.reasonPlaceholder")}
                />
              </div>
              <div className={styles.actions}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  disabled={scoreSubmitting || !selectedUserId || !selectedRuleId}
                  onClick={() => {
                    void handleScoreSubmit();
                  }}
                >
                  <i className="fas fa-plus" />
                  {scoreSubmitting ? t("workspace.addScore.submitting") : t("workspace.addScore.submit")}
                </button>
              </div>
            </div>
          </article>
        </section>

        <section className={styles.dualGrid}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-users" />
                  {t("workspace.members.title")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.members.description")}</p>
              </div>
            </div>

            {members.length ? (
              <>
              <div className={styles.tableWrap}>
                <table className={`${styles.table} ${styles.memberTable}`}>
                  <colgroup>
                    <col className={styles.memberTableStudentCol} />
                    <col className={styles.memberTableDutyCol} />
                    <col className={styles.memberTableClassCol} />
                    <col className={styles.memberTableActionCol} />
                  </colgroup>
                  <thead>
                    <tr>
                      <th className={styles.memberTableHeadCell}>{t("workspace.members.columns.student")}</th>
                      <th className={styles.memberTableHeadCell}>{t("workspace.members.columns.role")}</th>
                      <th className={styles.memberTableHeadCell}>{t("workspace.members.columns.className")}</th>
                      <th className={`${styles.memberTableHeadCell} ${styles.memberTableActionHead}`}>
                        {t("workspace.members.columns.action")}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedMembers.items.map((member) => (
                      <tr key={member.userId}>
                        <td className={styles.memberTableCell}>
                          <strong>{member.displayName || t("common.nameFallback")}</strong>
                        </td>
                        <td className={styles.memberTableCell}>
                          <span className={styles.statusNeutral}>{member.dutyName || t("positions.noAssignedDuty")}</span>
                        </td>
                        <td className={styles.memberTableCell}>
                          {formatMemberClassInfo(member.grade, member.className, member.studentNo, t)}
                        </td>
                        <td className={`${styles.actionCell} ${styles.memberTableActionCell}`}>
                          <button
                            type="button"
                            className={`${styles.dangerButton} ${styles.compactActionButton}`}
                            onClick={() => {
                              void handleRemoveMember(member);
                            }}
                          >
                            {t("workspace.members.remove")}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <PaginationBar
                currentPage={pagedMembers.page}
                totalPages={pagedMembers.totalPages}
                prevLabel={tPagination("prev")}
                nextLabel={tPagination("next")}
                pageLabel={tPagination("status", { page: pagedMembers.page, total: pagedMembers.totalPages })}
                onPageChange={setMemberPage}
              />
              </>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("workspace.members.empty")}</p>
              </div>
            )}
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-ranking-star" />
                  {t("workspace.rankings.title")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.rankings.description")}</p>
              </div>
              <div className={styles.field}>
                <label htmlFor="ranking-range">{t("workspace.rankings.range")}</label>
                <select
                  id="ranking-range"
                  value={rankingRange}
                  onChange={(event) => {
                    const nextRange = event.target.value as (typeof RANKING_RANGES)[number];
                    setRankingRange(nextRange);
                    setRankingPage(1);
                    void loadRankings(nextRange);
                  }}
                >
                  {RANKING_RANGES.map((range) => (
                    <option key={range} value={range}>
                      {t(`workspace.rankings.ranges.${range}`)}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {rankings.length ? (
              <>
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>{t("workspace.rankings.columns.rank")}</th>
                      <th>{t("workspace.rankings.columns.student")}</th>
                      <th>{t("workspace.rankings.columns.score")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedRankings.items.map((item) => (
                      <tr key={item.userId}>
                        <td>{item.rank}</td>
                        <td>
                          <strong>{item.displayName || t("common.nameFallback")}</strong>
                        </td>
                        <td>{item.score}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <PaginationBar
                currentPage={pagedRankings.page}
                totalPages={pagedRankings.totalPages}
                prevLabel={tPagination("prev")}
                nextLabel={tPagination("next")}
                pageLabel={tPagination("status", { page: pagedRankings.page, total: pagedRankings.totalPages })}
                onPageChange={setRankingPage}
              />
              </>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("workspace.rankings.empty")}</p>
              </div>
            )}
          </article>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-clock-rotate-left" />
                {t("workspace.records.title")}
              </h3>
              <p className={styles.panelHint}>{t("workspace.records.description")}</p>
            </div>
            <span className={styles.tag}>{t("workspace.records.tag", { count: records.length })}</span>
          </div>

          {records.length ? (
            <>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>{t("workspace.records.columns.student")}</th>
                    <th>{t("workspace.records.columns.rule")}</th>
                    <th>{t("workspace.records.columns.delta")}</th>
                    <th>{t("workspace.records.columns.reason")}</th>
                    <th>{t("workspace.records.columns.time")}</th>
                  </tr>
                </thead>
                <tbody>
                  {pagedRecords.items.map((record) => (
                    <tr key={record.id}>
                      <td>
                        <strong>{record.displayName || t("common.nameFallback")}</strong>
                      </td>
                      <td>{record.ruleName || t("workspace.records.manualRule")}</td>
                      <td>{record.scoreDelta > 0 ? `+${record.scoreDelta}` : record.scoreDelta}</td>
                      <td>{record.reason || t("workspace.records.reasonFallback")}</td>
                      <td>{formatDateTime(record.createdAt, t("common.timeFallback"))}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationBar
              currentPage={pagedRecords.page}
              totalPages={pagedRecords.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedRecords.page, total: pagedRecords.totalPages })}
              onPageChange={setRecordPage}
            />
            </>
          ) : (
            <div className={styles.emptyPanel}>
              <p>{t("workspace.records.empty")}</p>
            </div>
          )}
        </section>
      </section>
    </PortalShell>
  );

  async function loadAllData(nextRange: (typeof RANKING_RANGES)[number]) {
    setLoading(true);
    try {
      const [detailResponse, membersResponse, joinRequestsResponse, rulesResponse, recordsResponse, rankingsResponse] =
        await Promise.all([
          fetchManagerClubDetailRequest(clubId),
          fetchManagerClubMembersRequest(clubId),
          fetchManagerClubJoinRequestsRequest(clubId),
          fetchManagerScoreRulesRequest(clubId),
          fetchManagerScoreRecordsRequest(clubId),
          fetchManagerScoreRankingsRequest(clubId, nextRange)
        ]);

      const responses = [
        detailResponse,
        membersResponse,
        joinRequestsResponse,
        rulesResponse,
        recordsResponse,
        rankingsResponse
      ];
      const unauthorized = responses.find((item) => !item);
      if (unauthorized) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      for (const response of responses) {
        if (!(await portal.handleAuthStatus(response!.status))) {
          return;
        }
      }

      const detailResult = (await detailResponse!.json().catch(() => null)) as DetailResponse | null;
      const membersResult = (await membersResponse!.json().catch(() => null)) as MembersResponse | null;
      const joinRequestsResult = (await joinRequestsResponse!.json().catch(() => null)) as JoinRequestsResponse | null;
      const rulesResult = (await rulesResponse!.json().catch(() => null)) as RulesResponse | null;
      const recordsResult = (await recordsResponse!.json().catch(() => null)) as RecordsResponse | null;
      const rankingsResult = (await rankingsResponse!.json().catch(() => null)) as RankingsResponse | null;

      if (!detailResponse!.ok || detailResult?.success !== true || !detailResult.data) {
        notify.error(detailResult?.message || t("workspace.loadFailed"));
        router.push("/club-admin");
        return;
      }

      setDetail(detailResult.data);
      setMembers(membersResponse!.ok && membersResult?.success ? membersResult.data || [] : []);
      setJoinRequests(joinRequestsResponse!.ok && joinRequestsResult?.success ? joinRequestsResult.data || [] : []);
      setRules(rulesResponse!.ok && rulesResult?.success ? rulesResult.data || [] : []);
      setRecords(recordsResponse!.ok && recordsResult?.success ? recordsResult.data || [] : []);
      setRankings(rankingsResponse!.ok && rankingsResult?.success ? rankingsResult.data || [] : []);
      setRankingRange(nextRange);
    } finally {
      setLoading(false);
    }
  }

  async function loadRankings(nextRange: (typeof RANKING_RANGES)[number]) {
    const response = await fetchManagerScoreRankingsRequest(clubId, nextRange);
    if (!response) {
      notify.warning(t("common.loginRequired"));
      router.replace("/login");
      return;
    }

    if (!(await portal.handleAuthStatus(response.status))) {
      return;
    }

    const result = (await response.json().catch(() => null)) as RankingsResponse | null;
    if (!response.ok || result?.success !== true) {
      notify.error(result?.message || t("workspace.rankings.loadFailed"));
      return;
    }

    setRankings(result.data || []);
  }

  async function handleJoinRequestAction(requestId: number, action: "approve" | "reject") {
    if (pendingRequestActionId) {
      return;
    }

    setPendingRequestActionId(requestId);
    try {
      const response =
        action === "approve"
          ? await approveManagerClubJoinRequest(clubId, requestId)
          : await rejectManagerClubJoinRequest(clubId, requestId);

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("workspace.joinRequests.actionFailed"));
        return;
      }

      notify.success(
        action === "approve" ? t("workspace.joinRequests.approved") : t("workspace.joinRequests.rejected")
      );
      void loadAllData(rankingRange);
    } finally {
      setPendingRequestActionId(null);
    }
  }

  async function handleScoreSubmit() {
    if (scoreSubmitting || !selectedUserId || !selectedRuleId) {
      return;
    }

    setScoreSubmitting(true);
    try {
      const response = await createManagerScoreRecordRequest(clubId, {
        userId: Number(selectedUserId),
        ruleId: Number(selectedRuleId),
        reason: scoreReason.trim()
      });

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("workspace.addScore.submitFailed"));
        return;
      }

      notify.success(t("workspace.addScore.submitted"));
      setSelectedUserId("");
      setSelectedRuleId("");
      setScoreReason("");
      void loadAllData(rankingRange);
    } finally {
      setScoreSubmitting(false);
    }
  }

  async function handleRemoveMember(member: ManagerClubMember) {
    const accepted = await confirm.confirm({
      title: t("workspace.members.removeConfirmTitle"),
      message: t("workspace.members.removeConfirmMessage", {
        name: member.displayName || t("common.nameFallback")
      }),
      confirmText: t("workspace.members.remove"),
      cancelText: t("common.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    const response = await removeManagerClubMemberRequest(clubId, member.userId);
    if (!response) {
      notify.warning(t("common.loginRequired"));
      router.replace("/login");
      return;
    }

    if (!(await portal.handleAuthStatus(response.status))) {
      return;
    }

    const result = (await response.json().catch(() => null)) as MutationResponse | null;
    if (!response.ok || result?.success !== true) {
      notify.error(result?.message || t("workspace.members.removeFailed"));
      return;
    }

    notify.success(t("workspace.members.removed"));
    void loadAllData(rankingRange);
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

function formatMemberClassInfo(
  grade: string | null | undefined,
  className: string | null | undefined,
  studentNo: string | null | undefined,
  t: ReturnType<typeof useT>
) {
  const gradeLabel = formatGradeLabel(grade, t);
  const classLabel = formatStudentClassName(className);
  const studentNoLabel = formatStudentNoLabel(studentNo, t);
  const combined = `${gradeLabel}${classLabel}${studentNoLabel}`.trim();
  return combined || t("common.classInfoFallback");
}

function formatGradeLabel(grade: string | null | undefined, t: ReturnType<typeof useT>) {
  const normalized = normalizeStudentGrade(grade);
  return normalized ? t(`common.gradeLabels.${normalized}`) : "";
}

function formatStudentNoLabel(studentNo: string | null | undefined, t: ReturnType<typeof useT>) {
  const normalized = String(studentNo ?? "").trim();
  if (!normalized) {
    return "";
  }

  const numericOnly = normalized.replace(/\D+/g, "");
  if (numericOnly && numericOnly.length <= 3) {
    const compact = Number.parseInt(numericOnly, 10);
    if (Number.isFinite(compact)) {
      return t("common.studentNoShort", { number: compact });
    }
  }

  return t("common.studentNoLong", { studentNo: normalized });
}
