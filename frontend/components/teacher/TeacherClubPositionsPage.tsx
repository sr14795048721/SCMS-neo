"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { paginateItems } from "../../lib/pagination";
import {
  CLUB_DUTY_PERMISSION_VALUES,
  ClubDuty,
  ClubDutyMutationPayload,
  ClubDutyPermission,
  ManagerClubDetail,
  ManagerClubMember
} from "../../lib/manager/clubTypes";
import {
  createManagerClubDutyRequest,
  deleteManagerClubDutyRequest,
  fetchManagerClubDetailRequest,
  fetchManagerClubDutiesRequest,
  fetchManagerClubMembersRequest,
  updateManagerClubDutyRequest,
  updateManagerClubMemberDutyRequest
} from "../../lib/manager/clubClient";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
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

type DutiesResponse = {
  success?: boolean;
  data?: ClubDuty[];
  message?: string;
};

type MutationResponse<T> = {
  success?: boolean;
  data?: T;
  message?: string;
};

type DutyFormState = ClubDutyMutationPayload;

const EMPTY_FORM: DutyFormState = {
  name: "",
  permissions: []
};

export function TeacherClubPositionsPage({ clubId }: { clubId: number }) {
  const t = useT("teacherClubs");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [detail, setDetail] = useState<ManagerClubDetail | null>(null);
  const [members, setMembers] = useState<ManagerClubMember[]>([]);
  const [duties, setDuties] = useState<ClubDuty[]>([]);
  const [memberPage, setMemberPage] = useState(1);
  const [draftDutyIds, setDraftDutyIds] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [savingDuty, setSavingDuty] = useState(false);
  const [deletingDutyId, setDeletingDutyId] = useState<number | null>(null);
  const [editingDutyId, setEditingDutyId] = useState<number | null>(null);
  const [dutyForm, setDutyForm] = useState<DutyFormState>(EMPTY_FORM);
  const pagedMembers = useMemo(() => paginateItems(members, memberPage, 8), [memberPage, members]);
  const permissionItems = useMemo(
    () =>
      CLUB_DUTY_PERMISSION_VALUES.map((permission) => ({
        permission,
        label: t(`positions.permissions.${permission}.label`),
        description: t(`positions.permissions.${permission}.description`)
      })),
    [t]
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
      title={t("positions.metaTitle")}
      subtitle={detail?.clubName || t("positions.subtitle")}
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
          <button
            type="button"
            className={styles.backButton}
            onClick={() => router.push(`/club-admin/clubs/${clubId}`)}
          >
            <i className="fas fa-arrow-left" />
            {t("positions.back")}
          </button>
        </div>

        <article className={styles.hero}>
          <h2 className={styles.heroTitle}>{detail?.clubName || t("positions.metaTitle")}</h2>
          <p className={styles.heroDescription}>{t("positions.description")}</p>
          <div className={styles.heroMeta}>
            <span className={styles.heroPill}>
              <i className="fas fa-user-group" />
              {t("positions.heroMembers", { count: members.length })}
            </span>
            <span className={styles.heroPill}>
              <i className="fas fa-id-badge" />
              {t("positions.heroDuties", { count: duties.length })}
            </span>
          </div>
        </article>

        <section className={styles.twoColumnGrid}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-list-check" />
                  {t("positions.dutyListTitle")}
                </h3>
                <p className={styles.panelHint}>{t("positions.dutyListDescription")}</p>
              </div>
              <span className={styles.tag}>{t("positions.heroDuties", { count: duties.length })}</span>
            </div>

            {loading ? (
              <div className={styles.emptyPanel}>
                <p>{t("common.loading")}</p>
              </div>
            ) : duties.length ? (
              <div className={styles.dutyCardList}>
                {duties.map((duty) => {
                  const active = editingDutyId === duty.id;
                  return (
                    <article key={duty.id} className={`${styles.dutyCard} ${styles.dutyRoleCard}`}>
                      <div className={styles.dutyCardBody}>
                        <div className={styles.dutyCardHead}>
                          <div className={styles.dutyCardSummary}>
                          <h4 className={styles.clubTitle}>{duty.name}</h4>
                          <p className={styles.clubType}>
                            {duty.permissions.length
                              ? t("positions.permissionCount", { count: duty.permissions.length })
                              : t("positions.permissionsEmpty")}
                          </p>
                        </div>
                        </div>
                        <div className={`${styles.permissionList} ${styles.dutyPermissionList}`}>
                          {duty.permissions.length ? (
                            duty.permissions.map((permission) => (
                              <span key={permission} className={styles.permissionChip}>
                                {t(`positions.permissions.${permission}.label`)}
                              </span>
                            ))
                          ) : (
                            <span className={`${styles.permissionChip} ${styles.permissionChipMuted}`}>
                              {t("positions.permissionsEmpty")}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className={styles.dutyCardFooter}>
                        <div className={`${styles.inlineActions} ${styles.dutyCardActions}`}>
                          <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => startEditDuty(duty)}
                          >
                            <i className="fas fa-pen" />
                            {active ? t("positions.editing") : t("positions.edit")}
                          </button>
                          <button
                            type="button"
                            className={styles.dangerButton}
                            disabled={deletingDutyId === duty.id}
                            onClick={() => {
                              void handleDeleteDuty(duty);
                            }}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingDutyId === duty.id ? t("positions.deleting") : t("positions.delete")}
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <div className={styles.emptyPanel}>
                <p>{t("positions.noDutyYet")}</p>
              </div>
            )}
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h3 className={styles.panelTitle}>
                  <i className="fas fa-square-plus" />
                  {editingDutyId ? t("positions.editDutyTitle") : t("positions.createDutyTitle")}
                </h3>
                <p className={styles.panelHint}>{t("positions.formDescription")}</p>
              </div>
            </div>

            <div className={styles.formGrid}>
              <div className={styles.field}>
                <label htmlFor="duty-name">{t("positions.fields.name")}</label>
                <input
                  id="duty-name"
                  value={dutyForm.name}
                  maxLength={40}
                  onChange={(event) =>
                    setDutyForm((prev) => ({
                      ...prev,
                      name: event.target.value
                    }))
                  }
                  placeholder={t("positions.fields.namePlaceholder")}
                />
              </div>

              <div className={styles.field}>
                <label>{t("positions.fields.permissions")}</label>
                <div className={styles.dutyCardList}>
                  {permissionItems.map((item) => {
                    const checked = dutyForm.permissions.includes(item.permission);
                    return (
                      <label key={item.permission} className={styles.dutyCard}>
                        <div className={styles.dutyCardHead}>
                          <div>
                            <h4 className={styles.clubTitle}>{item.label}</h4>
                            <p className={styles.clubDescription}>{item.description}</p>
                          </div>
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => togglePermission(item.permission)}
                          />
                        </div>
                      </label>
                    );
                  })}
                </div>
              </div>

              <div className={styles.formActions}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  disabled={savingDuty}
                  onClick={() => {
                    void handleSaveDuty();
                  }}
                >
                  <i className="fas fa-floppy-disk" />
                  {savingDuty
                    ? t("positions.saving")
                    : editingDutyId
                      ? t("positions.saveDuty")
                      : t("positions.createDuty")}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={savingDuty}
                  onClick={() => resetDutyForm()}
                >
                  <i className="fas fa-rotate-left" />
                  {editingDutyId ? t("positions.cancelEdit") : t("positions.clearForm")}
                </button>
              </div>
            </div>
          </article>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-users-gear" />
                {t("positions.tableTitle")}
              </h3>
              <p className={styles.panelHint}>{t("positions.tableDescription")}</p>
            </div>
          </div>

          {loading ? (
            <div className={styles.emptyPanel}>
              <p>{t("common.loading")}</p>
            </div>
          ) : members.length ? (
            <>
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>{t("positions.columns.student")}</th>
                      <th>{t("positions.columns.currentRole")}</th>
                      <th>{t("positions.columns.targetRole")}</th>
                      <th>{t("positions.columns.action")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedMembers.items.map((member) => {
                      const currentDraft = draftDutyIds[member.userId] ?? stringifyDutyId(member.dutyId);
                      return (
                        <tr key={member.userId}>
                          <td>
                            <strong>{member.displayName || t("common.nameFallback")}</strong>
                          </td>
                          <td>
                            <span className={styles.statusNeutral}>
                              {member.dutyName || t("positions.noAssignedDuty")}
                            </span>
                          </td>
                          <td>
                            <select
                              value={currentDraft}
                              onChange={(event) =>
                                setDraftDutyIds((prev) => ({
                                  ...prev,
                                  [member.userId]: event.target.value
                                }))
                              }
                            >
                              <option value="">{t("positions.noAssignedDuty")}</option>
                              {duties.map((duty) => (
                                <option key={duty.id} value={String(duty.id)}>
                                  {duty.name}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td className={styles.actionCell}>
                            <button
                              type="button"
                              className={`${styles.primaryButton} ${styles.compactActionButton}`}
                              disabled={
                                savingUserId === member.userId ||
                                currentDraft === stringifyDutyId(member.dutyId)
                              }
                              onClick={() => {
                                void handleSaveMemberDuty(member.userId);
                              }}
                            >
                              {savingUserId === member.userId ? t("positions.saving") : t("positions.save")}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
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
              <p>{t("positions.empty")}</p>
            </div>
          )}
        </section>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const [detailResponse, membersResponse, dutiesResponse] = await Promise.all([
        fetchManagerClubDetailRequest(clubId),
        fetchManagerClubMembersRequest(clubId),
        fetchManagerClubDutiesRequest(clubId)
      ]);

      if (!detailResponse || !membersResponse || !dutiesResponse) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(detailResponse.status))) {
        return;
      }
      if (!(await portal.handleAuthStatus(membersResponse.status))) {
        return;
      }
      if (!(await portal.handleAuthStatus(dutiesResponse.status))) {
        return;
      }

      const detailResult = (await detailResponse.json().catch(() => null)) as DetailResponse | null;
      const membersResult = (await membersResponse.json().catch(() => null)) as MembersResponse | null;
      const dutiesResult = (await dutiesResponse.json().catch(() => null)) as DutiesResponse | null;

      if (!detailResponse.ok || detailResult?.success !== true || !detailResult.data) {
        notify.error(detailResult?.message || t("positions.loadFailed"));
        router.push("/club-admin");
        return;
      }

      const nextMembers = membersResponse.ok && membersResult?.success ? membersResult.data || [] : [];
      const nextDuties = dutiesResponse.ok && dutiesResult?.success ? dutiesResult.data || [] : [];
      setDetail(detailResult.data);
      setMembers(nextMembers);
      setDuties(nextDuties);
      setDraftDutyIds(
        Object.fromEntries(nextMembers.map((member) => [member.userId, stringifyDutyId(member.dutyId)]))
      );
    } finally {
      setLoading(false);
    }
  }

  function startEditDuty(duty: ClubDuty) {
    setEditingDutyId(duty.id);
    setDutyForm({
      name: duty.name,
      permissions: duty.permissions
    });
  }

  function resetDutyForm() {
    setEditingDutyId(null);
    setDutyForm(EMPTY_FORM);
  }

  function togglePermission(permission: ClubDutyPermission) {
    setDutyForm((prev) => ({
      ...prev,
      permissions: prev.permissions.includes(permission)
        ? prev.permissions.filter((item) => item !== permission)
        : [...prev.permissions, permission]
    }));
  }

  async function handleSaveDuty() {
    const payload = buildDutyPayload();
    if (!payload) {
      return;
    }

    setSavingDuty(true);
    try {
      const response = editingDutyId
        ? await updateManagerClubDutyRequest(clubId, editingDutyId, payload)
        : await createManagerClubDutyRequest(clubId, payload);

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<ClubDuty> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("positions.saveFailed"));
        return;
      }

      notify.success(editingDutyId ? t("positions.updated") : t("positions.created"));
      resetDutyForm();
      await loadData();
    } finally {
      setSavingDuty(false);
    }
  }

  async function handleDeleteDuty(duty: ClubDuty) {
    setDeletingDutyId(duty.id);
    try {
      const response = await deleteManagerClubDutyRequest(clubId, duty.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<null> | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("positions.deleteFailed"));
        return;
      }

      notify.success(t("positions.deleted"));
      if (editingDutyId === duty.id) {
        resetDutyForm();
      }
      await loadData();
    } finally {
      setDeletingDutyId(null);
    }
  }

  async function handleSaveMemberDuty(userId: number) {
    setSavingUserId(userId);
    try {
      const draftValue = draftDutyIds[userId] ?? "";
      const response = await updateManagerClubMemberDutyRequest(clubId, userId, {
        dutyId: parseDutyId(draftValue)
      });

      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<null> | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("positions.saveFailed"));
        return;
      }

      notify.success(t("positions.memberDutySaved"));
      await loadData();
    } finally {
      setSavingUserId(null);
    }
  }

  function buildDutyPayload(): ClubDutyMutationPayload | null {
    const name = dutyForm.name.trim();
    if (!name) {
      notify.warning(t("positions.validation.nameRequired"));
      return null;
    }

    return {
      name,
      permissions: dutyForm.permissions
    };
  }
}

function stringifyDutyId(dutyId: number | null | undefined) {
  return dutyId == null ? "" : String(dutyId);
}

function parseDutyId(value: string) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}
