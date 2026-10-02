"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { useT } from "../../lib/i18n/useT";
import { paginateItems } from "../../lib/pagination";
import {
  CLUB_DUTY_PERMISSION_VALUES,
  ClubDutyMutationPayload,
  ClubDutyPermission
} from "../../lib/manager/clubTypes";
import { AdminClubDetail, AdminClubDuty, AdminClubStudent } from "../../lib/admin-club/types";
import teacherStyles from "../../styles/teacherClubs.module.css";
import styles from "../../styles/sysAdminClubManage.module.css";

type DetailResponse = {
  success?: boolean;
  data?: AdminClubDetail;
  message?: string;
};

type DutiesResponse = {
  success?: boolean;
  data?: AdminClubDuty[];
  message?: string;
};

type MutationResponse<T> = {
  success?: boolean;
  data?: T;
  message?: string;
};

const EMPTY_FORM: ClubDutyMutationPayload = {
  name: "",
  permissions: []
};

export function SysAdminClubDutiesPage({ clubId }: { clubId: number }) {
  const t = useT("teacherClubs");
  const tAdmin = useT("adminClubManage");
  const tPortal = useT("portal");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate(["ADMIN", "SUPER_ADMIN"]);
  const [detail, setDetail] = useState<AdminClubDetail | null>(null);
  const [duties, setDuties] = useState<AdminClubDuty[]>([]);
  const [draftDutyIds, setDraftDutyIds] = useState<Record<number, string>>({});
  const [memberPage, setMemberPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [savingDuty, setSavingDuty] = useState(false);
  const [deletingDutyId, setDeletingDutyId] = useState<number | null>(null);
  const [editingDutyId, setEditingDutyId] = useState<number | null>(null);
  const [dutyForm, setDutyForm] = useState<ClubDutyMutationPayload>(EMPTY_FORM);
  const pagedStudents = useMemo(
    () => paginateItems(detail?.students || [], memberPage, 8),
    [detail?.students, memberPage]
  );
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
    if (!authorized) {
      return;
    }
    void loadData();
  }, [authorized, clubId]);

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
          <div className={styles.headerTitle}>
            <h1>
              <i className="fas fa-id-badge" />
              {t("positions.metaTitle")}
            </h1>
            <p>{detail?.name || t("positions.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => router.push("/sys-admin/club-manage")}
            >
              <i className="fas fa-arrow-left" />
              {tAdmin("actions.back")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <section className={teacherStyles.stack}>
          <article className={teacherStyles.hero}>
            <h2 className={teacherStyles.heroTitle}>{detail?.name || t("positions.metaTitle")}</h2>
            <p className={teacherStyles.heroDescription}>{t("positions.description")}</p>
            <div className={teacherStyles.heroMeta}>
              <span className={teacherStyles.heroPill}>
                <i className="fas fa-user-group" />
                {t("positions.heroMembers", { count: detail?.students.length || 0 })}
              </span>
              <span className={teacherStyles.heroPill}>
                <i className="fas fa-id-card" />
                {t("positions.heroDuties", { count: duties.length })}
              </span>
            </div>
          </article>

          <section className={teacherStyles.twoColumnGrid}>
            <article className={teacherStyles.panel}>
              <div className={teacherStyles.panelHead}>
                <div>
                  <h3 className={teacherStyles.panelTitle}>
                    <i className="fas fa-list-check" />
                    {t("positions.dutyListTitle")}
                  </h3>
                  <p className={teacherStyles.panelHint}>{t("positions.dutyListDescription")}</p>
                </div>
              </div>

              {loading ? (
                <div className={teacherStyles.emptyPanel}>
                  <p>{t("common.loading")}</p>
                </div>
              ) : duties.length ? (
                <div className={teacherStyles.dutyCardList}>
                  {duties.map((duty) => (
                    <article key={duty.id} className={`${teacherStyles.dutyCard} ${teacherStyles.dutyRoleCard}`}>
                      <div className={teacherStyles.dutyCardBody}>
                        <div className={teacherStyles.dutyCardHead}>
                          <div className={teacherStyles.dutyCardSummary}>
                          <h4 className={teacherStyles.clubTitle}>{duty.name}</h4>
                          <p className={teacherStyles.clubType}>
                            {duty.permissions.length
                              ? t("positions.permissionCount", { count: duty.permissions.length })
                              : t("positions.permissionsEmpty")}
                          </p>
                        </div>
                        </div>
                        <div className={`${teacherStyles.permissionList} ${teacherStyles.dutyPermissionList}`}>
                          {duty.permissions.length ? (
                            duty.permissions.map((permission) => (
                              <span key={permission} className={teacherStyles.permissionChip}>
                                {t(`positions.permissions.${permission}.label`)}
                              </span>
                            ))
                          ) : (
                            <span className={`${teacherStyles.permissionChip} ${teacherStyles.permissionChipMuted}`}>
                              {t("positions.permissionsEmpty")}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className={teacherStyles.dutyCardFooter}>
                        <div className={`${teacherStyles.inlineActions} ${teacherStyles.dutyCardActions}`}>
                          <button
                            type="button"
                            className={teacherStyles.secondaryButton}
                            onClick={() => startEditDuty(duty)}
                          >
                            <i className="fas fa-pen" />
                            {editingDutyId === duty.id ? t("positions.editing") : t("positions.edit")}
                          </button>
                          <button
                            type="button"
                            className={teacherStyles.dangerButton}
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
                  ))}
                </div>
              ) : (
                <div className={teacherStyles.emptyPanel}>
                  <p>{t("positions.noDutyYet")}</p>
                </div>
              )}
            </article>

            <article className={teacherStyles.panel}>
              <div className={teacherStyles.panelHead}>
                <div>
                  <h3 className={teacherStyles.panelTitle}>
                    <i className="fas fa-square-plus" />
                    {editingDutyId ? t("positions.editDutyTitle") : t("positions.createDutyTitle")}
                  </h3>
                  <p className={teacherStyles.panelHint}>{t("positions.formDescription")}</p>
                </div>
              </div>

              <div className={teacherStyles.formGrid}>
                <div className={teacherStyles.field}>
                  <label htmlFor="admin-duty-name">{t("positions.fields.name")}</label>
                  <input
                    id="admin-duty-name"
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

                <div className={teacherStyles.field}>
                  <label>{t("positions.fields.permissions")}</label>
                  <div className={teacherStyles.dutyCardList}>
                    {permissionItems.map((item) => (
                      <label key={item.permission} className={teacherStyles.dutyCard}>
                        <div className={teacherStyles.dutyCardHead}>
                          <div>
                            <h4 className={teacherStyles.clubTitle}>{item.label}</h4>
                            <p className={teacherStyles.clubDescription}>{item.description}</p>
                          </div>
                          <input
                            type="checkbox"
                            checked={dutyForm.permissions.includes(item.permission)}
                            onChange={() => togglePermission(item.permission)}
                          />
                        </div>
                      </label>
                    ))}
                  </div>
                </div>

                <div className={teacherStyles.formActions}>
                  <button
                    type="button"
                    className={teacherStyles.primaryButton}
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
                    className={teacherStyles.secondaryButton}
                    disabled={savingDuty}
                    onClick={() => resetForm()}
                  >
                    <i className="fas fa-rotate-left" />
                    {editingDutyId ? t("positions.cancelEdit") : t("positions.clearForm")}
                  </button>
                </div>
              </div>
            </article>
          </section>

          <section className={teacherStyles.panel}>
            <div className={teacherStyles.panelHead}>
              <div>
                <h3 className={teacherStyles.panelTitle}>
                  <i className="fas fa-users-gear" />
                  {t("positions.tableTitle")}
                </h3>
                <p className={teacherStyles.panelHint}>{t("positions.tableDescription")}</p>
              </div>
            </div>

            {loading ? (
              <div className={teacherStyles.emptyPanel}>
                <p>{t("common.loading")}</p>
              </div>
            ) : detail?.students.length ? (
              <>
                <div className={teacherStyles.tableWrap}>
                  <table className={`${teacherStyles.table} ${teacherStyles.memberTable}`}>
                    <colgroup>
                      <col className={teacherStyles.memberTableStudentCol} />
                      <col className={teacherStyles.memberTableDutyCol} />
                      <col className={teacherStyles.memberTableClassCol} />
                      <col className={teacherStyles.memberTableActionCol} />
                    </colgroup>
                    <thead>
                      <tr>
                        <th className={teacherStyles.memberTableHeadCell}>{t("positions.columns.student")}</th>
                        <th className={teacherStyles.memberTableHeadCell}>{t("positions.columns.currentRole")}</th>
                        <th className={teacherStyles.memberTableHeadCell}>{t("positions.columns.targetRole")}</th>
                        <th className={`${teacherStyles.memberTableHeadCell} ${teacherStyles.memberTableActionHead}`}>
                          {t("positions.columns.action")}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagedStudents.items.map((student) => {
                        const currentDraft =
                          draftDutyIds[student.userId] ?? stringifyDutyId(student.dutyId);
                        return (
                          <tr key={student.userId}>
                            <td className={teacherStyles.memberTableCell}>
                              <strong>{student.displayName || tAdmin("detail.emptyValue")}</strong>
                            </td>
                            <td className={teacherStyles.memberTableCell}>
                              <span className={teacherStyles.statusNeutral}>
                                {student.dutyName || t("positions.noAssignedDuty")}
                              </span>
                            </td>
                            <td className={teacherStyles.memberTableCell}>
                              <select
                                value={currentDraft}
                                onChange={(event) =>
                                  setDraftDutyIds((prev) => ({
                                    ...prev,
                                    [student.userId]: event.target.value
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
                            <td className={`${teacherStyles.actionCell} ${teacherStyles.memberTableActionCell}`}>
                              <div className={`${teacherStyles.inlineActions} ${teacherStyles.memberTableActionStack}`}>
                                <button
                                  type="button"
                                  className={`${teacherStyles.primaryButton} ${teacherStyles.compactActionButton}`}
                                  disabled={
                                    savingUserId === student.userId ||
                                    currentDraft === stringifyDutyId(student.dutyId)
                                  }
                                  onClick={() => {
                                    void handleSaveStudentDuty(student);
                                  }}
                                >
                                  {savingUserId === student.userId ? t("positions.saving") : t("positions.save")}
                                </button>
                                <button
                                  type="button"
                                  className={`${teacherStyles.dangerButton} ${teacherStyles.compactActionButton}`}
                                  disabled={savingUserId === student.userId}
                                  onClick={() => {
                                    void handleRemoveStudent(student);
                                  }}
                                >
                                  {t("positions.removeMember")}
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                <PaginationBar
                  currentPage={pagedStudents.page}
                  totalPages={pagedStudents.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedStudents.page, total: pagedStudents.totalPages })}
                  onPageChange={setMemberPage}
                />
              </>
            ) : (
              <div className={teacherStyles.emptyPanel}>
                <p>{t("positions.empty")}</p>
              </div>
            )}
          </section>
        </section>
      </section>
    </main>
  );

  async function loadData() {
    setLoading(true);
    try {
      const [detailResponse, dutiesResponse] = await Promise.all([
        fetchWithAuthorization(`/api/admin/clubs/${clubId}`),
        fetchWithAuthorization(`/api/admin/clubs/${clubId}/duties`)
      ]);

      if (!detailResponse || !dutiesResponse) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(detailResponse.status))) {
        return;
      }
      if (!(await handleAuthStatus(dutiesResponse.status))) {
        return;
      }

      const detailResult = (await detailResponse.json().catch(() => null)) as DetailResponse | null;
      const dutiesResult = (await dutiesResponse.json().catch(() => null)) as DutiesResponse | null;

      if (!detailResponse.ok || detailResult?.success !== true || !detailResult.data) {
        notify.error(detailResult?.message || t("positions.loadFailed"));
        router.push("/sys-admin/club-manage");
        return;
      }

      const nextDetail = detailResult.data;
      const nextDuties = dutiesResponse.ok && dutiesResult?.success ? dutiesResult.data || [] : [];
      setDetail(nextDetail);
      setDuties(nextDuties);
      setDraftDutyIds(
        Object.fromEntries(nextDetail.students.map((student) => [student.userId, stringifyDutyId(student.dutyId)]))
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveDuty() {
    const payload = buildPayload();
    if (!payload) {
      return;
    }

    setSavingDuty(true);
    try {
      const response = await fetchWithAuthorization(
        editingDutyId ? `/api/admin/clubs/${clubId}/duties/${editingDutyId}` : `/api/admin/clubs/${clubId}/duties`,
        {
          method: editingDutyId ? "PATCH" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        }
      );

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<AdminClubDuty> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("positions.saveFailed"));
        return;
      }

      notify.success(editingDutyId ? t("positions.updated") : t("positions.created"));
      resetForm();
      await loadData();
    } finally {
      setSavingDuty(false);
    }
  }

  async function handleDeleteDuty(duty: AdminClubDuty) {
    setDeletingDutyId(duty.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/clubs/${clubId}/duties/${duty.id}`, {
        method: "DELETE"
      });

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<null> | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("positions.deleteFailed"));
        return;
      }

      notify.success(t("positions.deleted"));
      if (editingDutyId === duty.id) {
        resetForm();
      }
      await loadData();
    } finally {
      setDeletingDutyId(null);
    }
  }

  async function handleSaveStudentDuty(student: AdminClubStudent) {
    setSavingUserId(student.userId);
    try {
      const response = await fetchWithAuthorization(
        `/api/admin/clubs/${clubId}/members/${student.userId}/duty`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            dutyId: parseDutyId(draftDutyIds[student.userId] ?? "")
          })
        }
      );

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
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

  async function handleRemoveStudent(student: AdminClubStudent) {
    const accepted = await confirm.confirm({
      title: t("positions.removeConfirmTitle"),
      message: t("positions.removeConfirmMessage", {
        name: student.displayName || tAdmin("detail.emptyValue")
      }),
      confirmText: t("positions.removeMember"),
      cancelText: tAdmin("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setSavingUserId(student.userId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/clubs/${clubId}/members/${student.userId}`, {
        method: "DELETE"
      });

      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as MutationResponse<null> | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("positions.removeFailed"));
        return;
      }

      notify.success(t("positions.memberRemoved"));
      await loadData();
    } finally {
      setSavingUserId(null);
    }
  }

  function startEditDuty(duty: AdminClubDuty) {
    setEditingDutyId(duty.id);
    setDutyForm({
      name: duty.name,
      permissions: duty.permissions.filter((item): item is ClubDutyPermission =>
        CLUB_DUTY_PERMISSION_VALUES.includes(item as ClubDutyPermission)
      )
    });
  }

  function resetForm() {
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

  function buildPayload(): ClubDutyMutationPayload | null {
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

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return false;
    }

    if (status === 403) {
      notify.error(tPortal("auth.noPermission"));
      router.replace("/sys-admin");
      return false;
    }

    return true;
  }

  function fetchWithAuthorization(url: string, init?: RequestInit) {
    return authorizedFetch(url, {
      ...init,
      cache: "no-store"
    });
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
