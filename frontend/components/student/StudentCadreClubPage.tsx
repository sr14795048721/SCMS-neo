"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import {
  fetchStudentCadreClubDetailRequest,
  fetchStudentCadreClubMembersRequest
} from "../../lib/student/client";
import { useStudentPortalState } from "../../lib/student/useStudentPortalState";
import { paginateItems } from "../../lib/pagination";
import { ManagerClubMember } from "../../lib/manager/clubTypes";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherClubs.module.css";

type WorkspaceDetail = {
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  status: string;
  memberCount: number;
  pendingJoinRequestCount: number;
  totalJoinRequestCount: number;
  activityCount: number;
  dutyId: number | null;
  dutyName: string;
  permissions: string[];
  createdAt: string | null;
  updatedAt: string | null;
};

type DetailResponse = {
  success?: boolean;
  data?: WorkspaceDetail;
  message?: string;
};

type MembersResponse = {
  success?: boolean;
  data?: ManagerClubMember[];
  message?: string;
};

export function StudentCadreClubPage({ clubId }: { clubId: number }) {
  const t = useT("studentClubCadre");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useStudentPortalState(t("common.defaultName"));
  const [detail, setDetail] = useState<WorkspaceDetail | null>(null);
  const [members, setMembers] = useState<ManagerClubMember[]>([]);
  const [membersVisible, setMembersVisible] = useState(false);
  const [loading, setLoading] = useState(true);
  const [memberPage, setMemberPage] = useState(1);
  const pagedMembers = useMemo(() => paginateItems(members, memberPage, 8), [memberPage, members]);
  const dutyPermissions = detail?.permissions || [];
  const featureEntries = useMemo(
    () =>
      dutyPermissions.map((permission) => {
        const available = permission === "SCORE_MANAGEMENT" || permission === "ACTIVITY_MANAGEMENT";
        const iconClass =
          permission === "SCORE_MANAGEMENT"
            ? "fas fa-medal"
            : permission === "ACTIVITY_MANAGEMENT"
              ? "fas fa-calendar-check"
              : permission === "JOIN_APPROVAL"
                ? "fas fa-user-check"
                : "fas fa-newspaper";
        const actionLabel =
          permission === "SCORE_MANAGEMENT"
            ? t("workspace.entries.SCORE_MANAGEMENT.action")
            : permission === "ACTIVITY_MANAGEMENT"
              ? t("workspace.entries.ACTIVITY_MANAGEMENT.action")
              : t("workspace.entries.pendingAction");
        return {
          permission,
          available,
          iconClass,
          actionLabel
        };
      }),
    [dutyPermissions, t]
  );

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [clubId, portal.authorized]);

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
      subtitle={detail?.clubName || t("meta.description")}
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

        <article className={styles.hero}>
          <h2 className={styles.heroTitle}>{detail?.clubName || t("workspace.title")}</h2>
          <p className={styles.heroDescription}>{detail?.description || t("workspace.description")}</p>
          <div className={styles.heroMeta}>
            <span className={styles.heroPill}>
              <i className="fas fa-id-badge" />
              {detail?.dutyName || t("workspace.noDuty")}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-key" />
              {t("workspace.permissionCount", { count: dutyPermissions.length })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-user-group" />
              {t("workspace.memberCount", { count: detail?.memberCount || 0 })}
            </span>
          </div>
        </article>

        <section className={styles.twoColumnGrid}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-user-shield" />
                  {t("workspace.currentDutyTitle")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.currentDutyDescription")}</p>
              </div>
            </div>

            <div className={styles.summaryGrid}>
              <article className={styles.summaryCard}>
                <p className={styles.summaryLabel}>{t("workspace.dutyNameLabel")}</p>
                <strong className={styles.summaryValue}>{detail?.dutyName || t("workspace.noDuty")}</strong>
                <span className={styles.summaryHelp}>{t("workspace.dutyNameHint")}</span>
              </article>
              <article className={styles.summaryCard}>
                <p className={styles.summaryLabel}>{t("workspace.permissionSummaryLabel")}</p>
                <strong className={styles.summaryValue}>{dutyPermissions.length}</strong>
                <span className={styles.summaryHelp}>{t("workspace.permissionSummaryHint")}</span>
              </article>
            </div>
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-list-check" />
                  {t("workspace.permissionsTitle")}
                </h3>
                <p className={styles.panelHint}>{t("workspace.permissionsDescription")}</p>
              </div>
            </div>

            {dutyPermissions.length ? (
              <div className={styles.dutyCardList}>
                {dutyPermissions.map((permission) => (
                  <article key={permission} className={styles.dutyCard}>
                    <h4 className={styles.clubTitle}>{t(`permissions.${permission}.label`)}</h4>
                    <p className={styles.clubDescription}>{t(`permissions.${permission}.description`)}</p>
                  </article>
                ))}
              </div>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("workspace.permissionsEmpty")}</p>
              </div>
            )}
          </article>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-compass-drafting" />
                {t("workspace.entriesTitle")}
              </h3>
              <p className={styles.panelHint}>{t("workspace.entriesDescription")}</p>
            </div>
          </div>

          {featureEntries.length ? (
            <div className={styles.shortcutGrid}>
              {featureEntries.map((entry) => (
                <button
                  key={entry.permission}
                  type="button"
                  className={styles.shortcutButton}
                  disabled={!entry.available}
                  onClick={() => {
                    void handleFeatureEntry(entry.permission);
                  }}
                >
                  <span className={styles.shortcutIcon}>
                    <i className={entry.iconClass} />
                  </span>
                  <h4 className={styles.shortcutTitle}>{t(`permissions.${entry.permission}.label`)}</h4>
                  <p className={styles.shortcutDescription}>
                    {entry.available
                      ? t(`workspace.entries.${entry.permission}.description`)
                      : t("workspace.entries.pendingDescription")}
                  </p>
                  <span className={styles.statusNeutral}>{entry.actionLabel}</span>
                </button>
              ))}
            </div>
          ) : (
            <div className={styles.emptyPanel}>
              <p>{t("workspace.permissionsEmpty")}</p>
            </div>
          )}
        </section>

        <section id="members-section" className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-users" />
                {t("workspace.membersTitle")}
              </h3>
              <p className={styles.panelHint}>{t("workspace.membersDescription")}</p>
            </div>
          </div>

          {loading ? (
            <div className={styles.emptyPanel}>
              <p>{t("common.loading")}</p>
            </div>
          ) : !membersVisible ? (
            <div className={styles.emptyPanel}>
              <p>{t("workspace.membersLocked")}</p>
            </div>
          ) : members.length ? (
            <>
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>{t("table.student")}</th>
                      <th>{t("table.duty")}</th>
                      <th>{t("table.permissions")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedMembers.items.map((member) => (
                      <tr key={member.userId}>
                        <td>
                          <strong>{member.displayName || t("common.nameFallback")}</strong>
                        </td>
                        <td>{member.dutyName || t("workspace.noDuty")}</td>
                        <td>
                          {member.dutyPermissions.length ? (
                            <div className={styles.permissionInlineList}>
                              {member.dutyPermissions.map((permission) => (
                                <span key={permission} className={`${styles.permissionChip} ${styles.permissionCompactChip}`}>
                                  {t(`permissions.${permission}.label`)}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <span className={`${styles.permissionChip} ${styles.permissionChipMuted} ${styles.permissionCompactChip}`}>
                              {t("workspace.permissionsEmpty")}
                            </span>
                          )}
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
              <p>{t("workspace.empty")}</p>
            </div>
          )}
        </section>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const detailResponse = await fetchStudentCadreClubDetailRequest(clubId);
      if (!detailResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(detailResponse.status))) {
        return;
      }

      const detailResult = (await detailResponse.json().catch(() => null)) as DetailResponse | null;
      if (!detailResponse.ok || detailResult?.success !== true || !detailResult.data) {
        notify.error(detailResult?.message || t("messages.loadFailed"));
        router.push("/student");
        return;
      }

      const nextDetail = detailResult.data;
      const canViewMembers = nextDetail.permissions.includes("SCORE_MANAGEMENT");
      setDetail(nextDetail);
      setMembersVisible(canViewMembers);

      if (!canViewMembers) {
        setMembers([]);
        return;
      }

      const membersResponse = await fetchStudentCadreClubMembersRequest(clubId);
      if (!membersResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(membersResponse.status))) {
        return;
      }

      const membersResult = (await membersResponse.json().catch(() => null)) as MembersResponse | null;
      if (!membersResponse.ok || membersResult?.success !== true) {
        notify.error(membersResult?.message || t("messages.loadFailed"));
        setMembers([]);
        return;
      }

      setMembers(membersResult.data || []);
    } finally {
      setLoading(false);
    }
  }

  async function handleFeatureEntry(permission: string) {
    if (permission === "SCORE_MANAGEMENT") {
      document.getElementById("members-section")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (permission === "ACTIVITY_MANAGEMENT") {
      router.push("/student/activities", { scroll: true });
      return;
    }
  }
}
