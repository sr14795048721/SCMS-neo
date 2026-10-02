"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import {
  bulkMarkManagerAttendanceRequest,
  createManagerAttendanceSessionRequest,
  fetchManagerAttendanceSessionDetailRequest,
  fetchManagerAttendanceSessionsRequest,
  markManagerAttendanceRequest,
  settleManagerAttendanceSessionRequest
} from "../../lib/manager/attendanceClient";
import { ManagerAttendanceSessionDetail, ManagerAttendanceSessionMember, ManagerAttendanceSessionSummary } from "../../lib/manager/attendanceTypes";
import { fetchManagerClubDetailRequest } from "../../lib/manager/clubClient";
import { fetchManagerScoreRulesRequest } from "../../lib/manager/scoreClient";
import { ManagerClubDetail } from "../../lib/manager/clubTypes";
import { ManagerScoreRule } from "../../lib/manager/scoreTypes";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { paginateItems } from "../../lib/pagination";
import { formatStudentClassName, normalizeStudentGrade } from "../../lib/student/fieldConstraints";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherAttendance.module.css";

type ClubDetailResponse = {
  success?: boolean;
  data?: ManagerClubDetail;
  message?: string;
};

type RulesResponse = {
  success?: boolean;
  data?: ManagerScoreRule[];
  message?: string;
};

type SessionsResponse = {
  success?: boolean;
  data?: ManagerAttendanceSessionSummary[];
  message?: string;
};

type SessionDetailResponse = {
  success?: boolean;
  data?: ManagerAttendanceSessionDetail;
  message?: string;
};

type SessionFormState = {
  title: string;
  scoreRuleId: string;
};

const EMPTY_FORM: SessionFormState = {
  title: "",
  scoreRuleId: ""
};

const MEMBER_PAGE_SIZE = 15;

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

function resolveMemberActionLabel(member: ManagerAttendanceSessionMember, t: ReturnType<typeof useT>) {
  if (member.status === "CHECKED_IN") {
    return t("actions.checkOut");
  }
  if (member.status === "CHECKED_OUT") {
    return t("actions.recheckIn");
  }
  return t("actions.checkIn");
}

function resolveMemberStatusClass(status: string, stylesMap: Record<string, string>) {
  if (status === "CHECKED_OUT") {
    return stylesMap.statusCheckedOut;
  }
  if (status === "CHECKED_IN") {
    return stylesMap.statusCheckedIn;
  }
  return stylesMap.statusPending;
}

function gradeLabel(grade: ReturnType<typeof normalizeStudentGrade>, t: ReturnType<typeof useT>) {
  if (grade === "HIGH_1") {
    return t("common.gradeLabels.HIGH_1");
  }
  if (grade === "HIGH_2") {
    return t("common.gradeLabels.HIGH_2");
  }
  if (grade === "HIGH_3") {
    return t("common.gradeLabels.HIGH_3");
  }
  return "";
}

function resolveClassName(member: ManagerAttendanceSessionMember, fallback: string, t: ReturnType<typeof useT>) {
  const normalizedGrade = normalizeStudentGrade(member.grade);
  const normalizedClassName = formatStudentClassName(member.className);
  const parts = [normalizedGrade ? gradeLabel(normalizedGrade, t) : "", normalizedClassName].filter(Boolean);
  return parts.length ? parts.join("") : fallback;
}

export function TeacherAttendancePage({ clubId }: { clubId: number }) {
  const t = useT("teacherAttendance");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [clubDetail, setClubDetail] = useState<ManagerClubDetail | null>(null);
  const [rules, setRules] = useState<ManagerScoreRule[]>([]);
  const [sessions, setSessions] = useState<ManagerAttendanceSessionSummary[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [selectedSession, setSelectedSession] = useState<ManagerAttendanceSessionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [memberActionStudentId, setMemberActionStudentId] = useState<number | null>(null);
  const [settling, setSettling] = useState(false);
  const [sessionPage, setSessionPage] = useState(1);
  const [memberPage, setMemberPage] = useState(1);
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([]);
  const [bulkAction, setBulkAction] = useState<"bulk-check-in" | "bulk-check-out" | null>(null);
  const [form, setForm] = useState<SessionFormState>(EMPTY_FORM);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [clubId, portal.authorized]);

  useEffect(() => {
    setMemberPage(1);
    setSelectedMemberIds([]);
  }, [selectedSessionId]);

  useEffect(() => {
    setSelectedMemberIds([]);
  }, [memberPage]);

  const pagedSessions = useMemo(() => paginateItems(sessions, sessionPage, 6), [sessionPage, sessions]);
  const pagedMembers = useMemo(
    () => paginateItems(selectedSession?.members ?? [], memberPage, MEMBER_PAGE_SIZE),
    [memberPage, selectedSession?.members]
  );
  const activeRuleOptions = useMemo(() => rules.filter((rule) => rule.status === "ACTIVE"), [rules]);
  const openSessionCount = useMemo(() => sessions.filter((session) => session.status === "OPEN").length, [sessions]);
  const currentPageMemberIds = useMemo(() => pagedMembers.items.map((member) => member.studentUserId), [pagedMembers.items]);
  const allCurrentPageSelected = currentPageMemberIds.length > 0 && currentPageMemberIds.every((id) => selectedMemberIds.includes(id));

  if (portal.checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{t("actions.loading")}</p>
        </section>
      </main>
    );
  }

  if (!portal.authorized || portal.redirecting) {
    return null;
  }

  return (
    <PortalShell
      title={clubDetail?.clubName || t("header.title")}
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
          <button type="button" className={styles.secondaryButton} onClick={() => router.push(`/club-admin/clubs/${clubId}`)}>
            <i className="fas fa-arrow-left" />
            {t("actions.backToWorkspace")}
          </button>
          <button type="button" className={styles.secondaryButton} onClick={() => void loadData(selectedSessionId)}>
            <i className="fas fa-rotate-right" />
            {t("actions.refresh")}
          </button>
        </div>

        <article className={styles.hero}>
          <h2 className={styles.heroTitle}>{clubDetail?.clubName || t("header.title")}</h2>
          <p className={styles.heroDescription}>{clubDetail?.description || t("common.descriptionFallback")}</p>
        </article>

        <section className={styles.summaryGrid}>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("summary.items.sessions")}</p>
            <strong className={styles.summaryValue}>{sessions.length}</strong>
            <span className={styles.summaryHelp}>{t("summary.description")}</span>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("summary.items.rules")}</p>
            <strong className={styles.summaryValue}>{activeRuleOptions.length}</strong>
            <span className={styles.summaryHelp}>{t("createForm.fields.scoreRule")}</span>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("summary.items.open")}</p>
            <strong className={styles.summaryValue}>{openSessionCount}</strong>
            <span className={styles.summaryHelp}>{t("sessionList.description")}</span>
          </article>
        </section>

        <section className={styles.layout}>
          <div className={styles.sideColumn}>
            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h3 className={styles.panelTitle}>{t("createForm.title")}</h3>
                  <p className={styles.panelHint}>{t("createForm.description")}</p>
                </div>
              </div>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>{t("createForm.fields.title")}</span>
                  <input value={form.title} onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))} placeholder={t("createForm.fields.titlePlaceholder")} />
                </label>
                <label className={styles.field}>
                  <span>{t("createForm.fields.scoreRule")}</span>
                  <select value={form.scoreRuleId} onChange={(event) => setForm((prev) => ({ ...prev, scoreRuleId: event.target.value }))}>
                    <option value="">{t("createForm.fields.scoreRulePlaceholder")}</option>
                    {activeRuleOptions.map((rule) => (
                      <option key={rule.id} value={String(rule.id)}>
                        {`${rule.name} (${rule.scoreDelta > 0 ? `+${rule.scoreDelta}` : rule.scoreDelta})`}
                      </option>
                    ))}
                  </select>
                </label>
                <div className={styles.actions}>
                  <button type="button" className={styles.primaryButton} disabled={submitting} onClick={() => void handleCreateSession()}>
                    <i className="fas fa-plus" />
                    {submitting ? t("actions.creating") : t("actions.create")}
                  </button>
                </div>
              </div>
            </article>

            <article className={styles.panel}>
              <div className={styles.panelHead}>
                <div>
                  <h3 className={styles.panelTitle}>{t("sessionList.title")}</h3>
                  <p className={styles.panelHint}>{t("sessionList.description")}</p>
                </div>
              </div>
              {loading ? (
                <div className={styles.emptyPanel}>
                  <p>{t("actions.loading")}</p>
                </div>
              ) : sessions.length ? (
                <>
                  <div className={styles.sessionCardList}>
                    {pagedSessions.items.map((session) => (
                      <article key={session.sessionId} className={`${styles.sessionCard} ${selectedSessionId === session.sessionId ? styles.sessionCardActive : ""}`}>
                        <div className={styles.sessionCardHead}>
                          <div>
                            <h4 className={styles.sessionCardTitle}>{session.title}</h4>
                            <p className={styles.sessionCardMeta}>
                              {session.scoreRuleName}
                              {session.scoreDelta > 0 ? ` (+${session.scoreDelta})` : ` (${session.scoreDelta})`}
                            </p>
                          </div>
                          <span className={session.status === "OPEN" ? styles.statusOpen : styles.statusCompleted}>
                            {t(`sessionList.status.${session.status}`)}
                          </span>
                        </div>
                        <div className={styles.sessionCounters}>
                          <span>{t("sessionList.counts.members", { count: session.totalMembers })}</span>
                          <span>{t("sessionList.counts.checkedIn", { count: session.checkedInCount })}</span>
                          <span>{t("sessionList.counts.checkedOut", { count: session.checkedOutCount })}</span>
                          <span>{t("sessionList.counts.settled", { count: session.settledCount })}</span>
                        </div>
                        <div className={styles.actions}>
                          <button type="button" className={styles.secondaryButton} onClick={() => void loadSessionDetail(session.sessionId)}>
                            <i className="fas fa-clipboard-check" />
                            {t("actions.openSession")}
                          </button>
                        </div>
                      </article>
                    ))}
                  </div>
                  <PaginationBar
                    currentPage={pagedSessions.page}
                    totalPages={pagedSessions.totalPages}
                    prevLabel={tPagination("prev")}
                    nextLabel={tPagination("next")}
                    pageLabel={tPagination("status", { page: pagedSessions.page, total: pagedSessions.totalPages })}
                    onPageChange={setSessionPage}
                  />
                </>
              ) : (
                <div className={styles.emptyPanel}>
                  <p>{t("sessionList.empty")}</p>
                </div>
              )}
            </article>
          </div>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>{t("detail.title")}</h3>
                <p className={styles.panelHint}>{t("detail.description")}</p>
              </div>
              {selectedSession?.session ? (
                <button type="button" className={styles.primaryButton} disabled={settling} onClick={() => void handleSettle()}>
                  <i className="fas fa-medal" />
                  {settling ? t("actions.settling") : selectedSession.session.status === "COMPLETED" ? t("actions.resettle") : t("actions.settle")}
                </button>
              ) : null}
            </div>

            {selectedSession?.session ? (
              <>
                <section className={styles.sessionMetaGrid}>
                  <div className={styles.metaItem}>
                    <span>{t("detail.session.rule")}</span>
                    <strong>{selectedSession.session.scoreRuleName}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("detail.session.startedAt")}</span>
                    <strong>{formatDateTime(selectedSession.session.startedAt, t("common.timeFallback"))}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("detail.session.endedAt")}</span>
                    <strong>{formatDateTime(selectedSession.session.endedAt, t("common.timeFallback"))}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("detail.session.settledAt")}</span>
                    <strong>{formatDateTime(selectedSession.session.settledAt, t("common.timeFallback"))}</strong>
                  </div>
                </section>

                {selectedSession.members.length ? (
                  <>
                    <div className={styles.bulkToolbar}>
                      <p className={styles.selectionHint}>{t("detail.bulk.selected", { count: selectedMemberIds.length })}</p>
                      <div className={styles.bulkActionGroup}>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          disabled={selectedMemberIds.length === 0 || bulkAction !== null}
                          onClick={() => void handleBulkAction("bulk-check-in")}
                        >
                          {bulkAction === "bulk-check-in" ? t("actions.loading") : t("actions.bulkCheckIn")}
                        </button>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          disabled={selectedMemberIds.length === 0 || bulkAction !== null}
                          onClick={() => void handleBulkAction("bulk-check-out")}
                        >
                          {bulkAction === "bulk-check-out" ? t("actions.loading") : t("actions.bulkCheckOut")}
                        </button>
                      </div>
                    </div>
                    <div className={styles.tableWrap}>
                      <table className={styles.table}>
                        <thead>
                          <tr>
                            <th className={styles.selectionCell}>
                              <input
                                type="checkbox"
                                className={styles.tableCheckbox}
                                aria-label={t("detail.bulk.selectCurrentPage")}
                                checked={allCurrentPageSelected}
                                onChange={(event) => handleToggleCurrentPage(event.target.checked)}
                              />
                            </th>
                            <th>{t("detail.table.student")}</th>
                            <th>{t("detail.table.className")}</th>
                            <th>{t("detail.table.checkInAt")}</th>
                            <th>{t("detail.table.checkOutAt")}</th>
                            <th>{t("detail.table.status")}</th>
                            <th>{t("detail.table.action")}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {pagedMembers.items.map((member) => (
                            <tr key={member.studentUserId}>
                              <td className={styles.selectionCell}>
                                <input
                                  type="checkbox"
                                  className={styles.tableCheckbox}
                                  aria-label={t("detail.bulk.selectStudent", { name: member.displayName || t("common.nameFallback") })}
                                  checked={selectedMemberIds.includes(member.studentUserId)}
                                  onChange={() => toggleMemberSelection(member.studentUserId)}
                                />
                              </td>
                              <td>{member.displayName || t("common.nameFallback")}</td>
                              <td>{resolveClassName(member, t("common.classFallback"), t)}</td>
                              <td>{formatDateTime(member.checkInAt, t("common.timeFallback"))}</td>
                              <td>{formatDateTime(member.checkOutAt, t("common.timeFallback"))}</td>
                              <td>
                                <div className={styles.statusStack}>
                                  <span className={resolveMemberStatusClass(member.status, styles)}>{t(`detail.status.${member.status}`)}</span>
                                  <span className={member.settled ? styles.settledBadge : styles.pendingBadge}>
                                    {member.settled ? t("detail.settled.yes") : t("detail.settled.no")}
                                  </span>
                                </div>
                              </td>
                              <td>
                                <div className={styles.actionInline}>
                                  <button
                                    type="button"
                                    className={styles.secondaryButton}
                                    disabled={memberActionStudentId === member.studentUserId || bulkAction !== null}
                                    onClick={() => void handleMemberAction(member)}
                                  >
                                    {memberActionStudentId === member.studentUserId ? t("actions.loading") : resolveMemberActionLabel(member, t)}
                                  </button>
                                </div>
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
                    <p>{t("detail.emptyMembers")}</p>
                  </div>
                )}
              </>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{loading ? t("actions.loading") : t("detail.empty")}</p>
              </div>
            )}
          </article>
        </section>
      </section>
    </PortalShell>
  );

  async function loadData(preferredSessionId?: number | null) {
    setLoading(true);
    try {
      const [clubResponse, rulesResponse, sessionsResponse] = await Promise.all([
        fetchManagerClubDetailRequest(clubId),
        fetchManagerScoreRulesRequest(clubId),
        fetchManagerAttendanceSessionsRequest(clubId)
      ]);

      if (!clubResponse || !rulesResponse || !sessionsResponse) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(clubResponse.status)) || !(await portal.handleAuthStatus(rulesResponse.status)) || !(await portal.handleAuthStatus(sessionsResponse.status))) {
        return;
      }

      const clubResult = (await clubResponse.json().catch(() => null)) as ClubDetailResponse | null;
      const ruleResult = (await rulesResponse.json().catch(() => null)) as RulesResponse | null;
      const sessionResult = (await sessionsResponse.json().catch(() => null)) as SessionsResponse | null;

      if (!clubResponse.ok || clubResult?.success !== true || !clubResult.data) {
        notify.error(clubResult?.message || t("messages.loadFailed"));
        return;
      }
      if (!rulesResponse.ok || ruleResult?.success !== true) {
        notify.error(ruleResult?.message || t("messages.loadFailed"));
        return;
      }
      if (!sessionsResponse.ok || sessionResult?.success !== true) {
        notify.error(sessionResult?.message || t("messages.loadFailed"));
        return;
      }

      const nextSessions = Array.isArray(sessionResult.data) ? sessionResult.data : [];
      setClubDetail(clubResult.data);
      setRules(Array.isArray(ruleResult.data) ? ruleResult.data : []);
      setSessions(nextSessions);

      const nextSelectedSessionId = preferredSessionId && nextSessions.some((item) => item.sessionId === preferredSessionId)
        ? preferredSessionId
        : nextSessions[0]?.sessionId ?? null;
      setSelectedSessionId(nextSelectedSessionId);

      if (nextSelectedSessionId) {
        await loadSessionDetail(nextSelectedSessionId, false);
      } else {
        setSelectedSession(null);
      }
    } finally {
      setLoading(false);
    }
  }

  async function loadSessionDetail(sessionId: number, withLoading = true) {
    if (withLoading) {
      setLoading(true);
    }
    try {
      const response = await fetchManagerAttendanceSessionDetailRequest(clubId, sessionId);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as SessionDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }
      setSelectedSessionId(sessionId);
      setSelectedSession(result.data);
      setMemberPage(1);
      setSelectedMemberIds([]);
    } finally {
      if (withLoading) {
        setLoading(false);
      }
    }
  }

  async function handleCreateSession() {
    const title = form.title.trim();
    const scoreRuleId = Number(form.scoreRuleId);
    if (!title) {
      notify.warning(t("createForm.validation.titleRequired"));
      return;
    }
    if (!scoreRuleId) {
      notify.warning(t("createForm.validation.scoreRuleRequired"));
      return;
    }

    setSubmitting(true);
    try {
      const response = await createManagerAttendanceSessionRequest(clubId, { title, scoreRuleId });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as SessionDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.sessionCreateFailed"));
        return;
      }

      setForm(EMPTY_FORM);
      setSelectedSession(result.data);
      setSelectedSessionId(result.data.session.sessionId);
      setMemberPage(1);
      setSelectedMemberIds([]);
      notify.success(t("messages.sessionCreated"));
      await loadData(result.data.session.sessionId);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleMemberAction(member: ManagerAttendanceSessionMember) {
    if (!selectedSession?.session.sessionId) {
      return;
    }

    const action = member.status === "CHECKED_IN" ? "check-out" : "check-in";
    setMemberActionStudentId(member.studentUserId);
    try {
      const response = await markManagerAttendanceRequest(clubId, selectedSession.session.sessionId, action, {
        studentUserId: member.studentUserId
      });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as SessionDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.memberActionFailed"));
        return;
      }

      setSelectedSession(result.data);
      notify.success(
        member.status === "CHECKED_IN"
          ? t("messages.memberCheckedOut")
          : member.status === "CHECKED_OUT"
            ? t("messages.memberRecheckedIn")
            : t("messages.memberCheckedIn")
      );
      await refreshSessionSummaries(result.data.session.sessionId);
    } finally {
      setMemberActionStudentId(null);
    }
  }

  async function handleBulkAction(action: "bulk-check-in" | "bulk-check-out") {
    if (!selectedSession?.session.sessionId || selectedMemberIds.length === 0) {
      notify.warning(t("messages.selectionRequired"));
      return;
    }

    setBulkAction(action);
    try {
      const response = await bulkMarkManagerAttendanceRequest(clubId, selectedSession.session.sessionId, action, {
        studentUserIds: selectedMemberIds
      });
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as SessionDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.memberActionFailed"));
        return;
      }

      setSelectedSession(result.data);
      setSelectedMemberIds([]);
      notify.success(action === "bulk-check-in" ? t("messages.bulkCheckedIn") : t("messages.bulkCheckedOut"));
      await refreshSessionSummaries(result.data.session.sessionId);
    } finally {
      setBulkAction(null);
    }
  }

  async function handleSettle() {
    if (!selectedSession?.session.sessionId || settling) {
      return;
    }

    const accepted = await confirm.confirm({
      title: t("messages.confirmSettleTitle"),
      message: t("messages.confirmSettleMessage"),
      confirmText: selectedSession.session.status === "COMPLETED" ? t("actions.resettle") : t("actions.settle"),
      cancelText: t("common.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setSettling(true);
    try {
      const response = await settleManagerAttendanceSessionRequest(clubId, selectedSession.session.sessionId);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as SessionDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.settleFailed"));
        return;
      }

      setSelectedSession(result.data);
      notify.success(t("messages.settled"));
      await refreshSessionSummaries(result.data.session.sessionId);
    } finally {
      setSettling(false);
    }
  }

  async function refreshSessionSummaries(preferredSessionId: number) {
    const response = await fetchManagerAttendanceSessionsRequest(clubId);
    if (!response) {
      return;
    }
    if (!(await portal.handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as SessionsResponse | null;
    if (!response.ok || result?.success !== true) {
      return;
    }
    const nextSessions = Array.isArray(result.data) ? result.data : [];
    setSessions(nextSessions);
    if (nextSessions.some((item) => item.sessionId === preferredSessionId)) {
      setSelectedSessionId(preferredSessionId);
    }
  }

  function handleToggleCurrentPage(checked: boolean) {
    if (!currentPageMemberIds.length) {
      return;
    }
    setSelectedMemberIds(checked ? currentPageMemberIds : []);
  }

  function toggleMemberSelection(studentUserId: number) {
    setSelectedMemberIds((prev) => (
      prev.includes(studentUserId)
        ? prev.filter((id) => id !== studentUserId)
        : [...prev, studentUserId]
    ));
  }
}
