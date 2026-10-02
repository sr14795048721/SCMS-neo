"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import {
  AdminBulkResetPasswordResult,
  AdminManagerCreateResult,
  AdminManagerDetail,
  AdminManagerListItem,
  AdminManagerPage,
  AdminSortDirection,
  AdminStudentDetail,
  AdminStudentImportFailure,
  AdminStudentImportResult,
  AdminStudentListItem,
  AdminStudentPage,
  AdminUserSortBy,
  AdminUserTab,
  CreateAdminManagerPayload,
  UpdateAdminStudentPayload
} from "../../lib/admin-user/types";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { formatStudentClassName, resolveStudentClassNameInput } from "../../lib/student/fieldConstraints";
import styles from "../../styles/sysAdminUserManage.module.css";

const PAGE_SIZE = 10;

const EMPTY_STUDENT_PAGE: AdminStudentPage = {
  items: [],
  page: 1,
  pageSize: PAGE_SIZE,
  total: 0,
  totalPages: 0
};

const EMPTY_MANAGER_PAGE: AdminManagerPage = {
  items: [],
  page: 1,
  pageSize: PAGE_SIZE,
  total: 0,
  totalPages: 0
};

const EMPTY_STUDENT_DETAIL: AdminStudentDetail = {
  userId: 0,
  username: "",
  email: "",
  enabled: true,
  displayName: "",
  studentNo: "",
  grade: "",
  className: "",
  phone: "",
  bio: "",
  role: "",
  hasAvatar: false,
  avatarUpdatedAt: null,
  createdAt: null,
  updatedAt: null
};

const EMPTY_MANAGER_DETAIL: AdminManagerDetail = {
  userId: 0,
  username: "",
  email: "",
  enabled: true,
  displayName: "",
  managerNo: "",
  phone: "",
  bio: "",
  role: "",
  hasAvatar: false,
  avatarUpdatedAt: null,
  createdAt: null,
  updatedAt: null
};

const USER_SORT_COLUMNS: Array<{
  key: "username" | "displayName" | "identity" | "contact" | "status" | "actions";
  labelKey: string;
  sortKey?: AdminUserSortBy;
}> = [
  { key: "username", labelKey: "list.columns.username", sortKey: "username" },
  { key: "displayName", labelKey: "list.columns.name", sortKey: "displayName" },
  { key: "identity", labelKey: "list.columns.identity", sortKey: "identity" },
  { key: "contact", labelKey: "list.columns.contact", sortKey: "contact" },
  { key: "status", labelKey: "list.columns.status", sortKey: "status" },
  { key: "actions", labelKey: "list.columns.actions" }
];

export function SysAdminUserManage() {
  const t = useT("adminUserManage");
  const tPortal = useT("portal");
  const tCommon = useT("common");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [activeTab, setActiveTab] = useState<AdminUserTab>("students");
  const [initialLoading, setInitialLoading] = useState(true);
  const [listLoading, setListLoading] = useState(false);
  const [studentsLoaded, setStudentsLoaded] = useState(false);
  const [managersLoaded, setManagersLoaded] = useState(false);
  const [studentPage, setStudentPage] = useState<AdminStudentPage>(EMPTY_STUDENT_PAGE);
  const [managerPage, setManagerPage] = useState<AdminManagerPage>(EMPTY_MANAGER_PAGE);
  const [studentSearchInput, setStudentSearchInput] = useState("");
  const [managerSearchInput, setManagerSearchInput] = useState("");
  const [studentQuery, setStudentQuery] = useState("");
  const [managerQuery, setManagerQuery] = useState("");
  const [studentSortBy, setStudentSortBy] = useState<AdminUserSortBy | null>(null);
  const [studentSortDirection, setStudentSortDirection] = useState<AdminSortDirection>("asc");
  const [managerSortBy, setManagerSortBy] = useState<AdminUserSortBy | null>(null);
  const [managerSortDirection, setManagerSortDirection] = useState<AdminSortDirection>("asc");
  const [studentImportResult, setStudentImportResult] = useState<AdminStudentImportResult | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [savingDetail, setSavingDetail] = useState(false);
  const [managerDrawerMode, setManagerDrawerMode] = useState<"create" | "edit">("edit");
  const [deletingUserId, setDeletingUserId] = useState<number | null>(null);
  const [resettingStudentPasswordId, setResettingStudentPasswordId] = useState<number | null>(null);
  const [studentDetail, setStudentDetail] = useState<AdminStudentDetail>(EMPTY_STUDENT_DETAIL);
  const [managerDetail, setManagerDetail] = useState<AdminManagerDetail>(EMPTY_MANAGER_DETAIL);
  const [importingStudents, setImportingStudents] = useState(false);
  const [downloadingTemplate, setDownloadingTemplate] = useState(false);
  const [exportingStudents, setExportingStudents] = useState(false);
  const [resettingPasswords, setResettingPasswords] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const isStudentTab = activeTab === "students";
  const searchValue = isStudentTab ? studentSearchInput : managerSearchInput;
  const pageInfo = isStudentTab ? studentPage : managerPage;
  const currentSortBy = isStudentTab ? studentSortBy : managerSortBy;
  const currentSortDirection = isStudentTab ? studentSortDirection : managerSortDirection;

  useEffect(() => {
    if (!authorized) {
      return;
    }

    if (activeTab === "students" && !studentsLoaded) {
      void loadStudents(1, studentQuery, true, studentSortBy, studentSortDirection);
      return;
    }

    if (activeTab === "managers" && !managersLoaded) {
      void loadManagers(1, managerQuery, true, managerSortBy, managerSortDirection);
      return;
    }

    setInitialLoading(false);
  }, [
    activeTab,
    authorized,
    managerQuery,
    managerSortBy,
    managerSortDirection,
    managersLoaded,
    studentQuery,
    studentSortBy,
    studentSortDirection,
    studentsLoaded
  ]);

  if (checking || initialLoading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : t("states.loading")}</p>
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
              <i className="fas fa-users-cog" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.card}>
          <div className={styles.cardHead}>
            <div>
              <h2>{t("tabs.sectionTitle")}</h2>
              <p>{t("tabs.sectionSubtitle")}</p>
            </div>
          </div>

          <div className={styles.tabRow}>
            <button
              type="button"
              className={`${styles.tabButton} ${isStudentTab ? styles.activeTab : ""}`}
              onClick={() => {
                closeDetail();
                setActiveTab("students");
              }}
            >
              <i className="fas fa-user-graduate" />
              {t("tabs.students")}
            </button>
            <button
              type="button"
              className={`${styles.tabButton} ${!isStudentTab ? styles.activeTab : ""}`}
              onClick={() => {
                closeDetail();
                setActiveTab("managers");
              }}
            >
              <i className="fas fa-chalkboard-teacher" />
              {t("tabs.managers")}
            </button>
          </div>

          <div className={styles.toolbar}>
            <form className={styles.searchForm} onSubmit={(event) => void handleSearchSubmit(event)}>
              <i className={`fas fa-search ${styles.searchIcon}`} />
              <input
                value={searchValue}
                onChange={(event) => {
                  if (isStudentTab) {
                    setStudentSearchInput(event.target.value);
                    return;
                  }
                  setManagerSearchInput(event.target.value);
                }}
                className={styles.searchInput}
                placeholder={t(isStudentTab ? "search.studentsPlaceholder" : "search.managersPlaceholder")}
              />
              <button type="submit" className={styles.primaryButton} disabled={listLoading}>
                {t("actions.search")}
              </button>
            </form>

            <div className={styles.toolbarActions}>
              {isStudentTab ? (
                <>
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => void downloadStudentsTemplate()}
                    disabled={downloadingTemplate}
                  >
                    <i className="fas fa-file-arrow-down" />
                    {downloadingTemplate ? t("actions.downloadingTemplate") : t("actions.downloadTemplate")}
                  </button>
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={importingStudents}
                  >
                    <i className="fas fa-file-import" />
                    {importingStudents ? t("actions.importingStudents") : t("actions.importStudents")}
                  </button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    hidden
                    accept=".xlsx"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      event.currentTarget.value = "";
                      if (file) {
                        void importStudents(file);
                      }
                    }}
                  />
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => void exportStudents()}
                    disabled={exportingStudents}
                  >
                    <i className="fas fa-file-export" />
                    {exportingStudents ? t("actions.exportingStudents") : t("actions.exportStudents")}
                  </button>
                </>
              ) : null}
              {!isStudentTab ? (
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() => openCreateManager()}
                  disabled={savingDetail || detailLoading}
                >
                  <i className="fas fa-user-plus" />
                  {t("actions.createManager")}
                </button>
              ) : null}
              <button
                type="button"
                className={styles.dangerButton}
                onClick={() => void resetPasswords()}
                disabled={resettingPasswords}
              >
                <i className="fas fa-key" />
                {resettingPasswords
                  ? t(isStudentTab ? "actions.resettingStudents" : "actions.resettingManagers")
                  : t(isStudentTab ? "actions.resetStudentsPassword" : "actions.resetManagersPassword")}
              </button>
            </div>
          </div>

          {isStudentTab && studentImportResult ? (
            <section className={styles.importResultCard}>
              <div className={styles.importSummary}>
                <h3>{t("importResult.title")}</h3>
                <div className={styles.summaryGrid}>
                  <div className={styles.summaryItem}>
                    <span>{t("importResult.totalRows")}</span>
                    <strong>{studentImportResult.totalRows}</strong>
                  </div>
                  <div className={styles.summaryItem}>
                    <span>{t("importResult.successCount")}</span>
                    <strong>{studentImportResult.successCount}</strong>
                  </div>
                  <div className={styles.summaryItem}>
                    <span>{t("importResult.failureCount")}</span>
                    <strong>{studentImportResult.failureCount}</strong>
                  </div>
                </div>
              </div>

              {studentImportResult.failures.length ? (
                <div className={styles.failureList}>
                  {studentImportResult.failures.map((failure) => (
                    <article key={`${failure.rowNumber}-${failure.reasonCode}-${failure.email}`} className={styles.failureItem}>
                      <strong>{t("importResult.failureRow", { rowNumber: failure.rowNumber })}</strong>
                      <p>{`${failure.email || t("importResult.emptyEmail")} / ${resolveFailureMessage(failure)}`}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <p className={styles.emptyHint}>{t("importResult.noFailures")}</p>
              )}
            </section>
          ) : null}

          <div className={styles.tableShell}>
            <div className={styles.tableHeader}>
              {USER_SORT_COLUMNS.map((item) => {
                const sortKey = item.sortKey;
                const isActive = sortKey != null && currentSortBy === sortKey;
                const sortIconClassName = sortKey == null
                  ? ""
                  : isActive
                    ? currentSortDirection === "desc"
                      ? "fas fa-sort-down"
                      : "fas fa-sort-up"
                    : "fas fa-sort";
                const wrapperClassName = item.key === "status" ? styles.tableHeaderStatus : undefined;

                if (sortKey == null) {
                  return (
                    <span key={item.key} className={wrapperClassName}>
                      {t(item.labelKey)}
                    </span>
                  );
                }

                return (
                  <button
                    key={item.key}
                    type="button"
                    className={`${styles.tableHeaderButton} ${wrapperClassName ?? ""} ${isActive ? styles.tableHeaderButtonActive : ""}`.trim()}
                    onClick={() => void handleSortToggle(sortKey)}
                    aria-sort={resolveAriaSort(sortKey)}
                  >
                    <span>{t(item.labelKey)}</span>
                    <i className={`${sortIconClassName} ${styles.sortIcon}`} />
                  </button>
                );
              })}
            </div>

            {listLoading ? (
              <div className={styles.loadingInline}>{t("states.loadingList")}</div>
            ) : !pageInfo.items.length ? (
              <div className={styles.emptyState}>
                <i className={`fas ${isStudentTab ? "fa-user-graduate" : "fa-chalkboard-teacher"}`} />
                <h3>{t(isStudentTab ? "empty.studentsTitle" : "empty.managersTitle")}</h3>
                <p>{t(isStudentTab ? "empty.studentsDescription" : "empty.managersDescription")}</p>
              </div>
            ) : (
              <div className={styles.tableBody}>
                {isStudentTab
                  ? studentPage.items.map((item) => (
                      <article key={item.userId} className={styles.tableRow}>
                        <div className={styles.cellPrimary}>
                          <strong>{item.username}</strong>
                        </div>
                        <span className={styles.cellText}>{item.displayName || t("list.emptyValue")}</span>
                        <span className={styles.cellText}>{formatStudentIdentity(item)}</span>
                        <span className={styles.cellText}>{item.phone || t("list.emptyValue")}</span>
                        <span
                          className={`${styles.statusBadge} ${
                            item.enabled ? styles.statusEnabled : styles.statusDisabled
                          }`}
                        >
                          {item.enabled ? t("status.enabled") : t("status.disabled")}
                        </span>
                        <div className={styles.rowActions}>
                          <button
                            type="button"
                            className={styles.rowAction}
                            onClick={() => void openStudentDetail(item.userId)}
                            disabled={deletingUserId === item.userId}
                          >
                            <i className="fas fa-pen" />
                            {t("actions.edit")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void deleteStudent(item.userId)}
                            disabled={deletingUserId === item.userId}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingUserId === item.userId ? t("actions.deleting") : t("actions.delete")}
                          </button>
                        </div>
                      </article>
                    ))
                  : managerPage.items.map((item) => (
                      <article key={item.userId} className={styles.tableRow}>
                        <div className={styles.cellPrimary}>
                          <strong>{item.username}</strong>
                        </div>
                        <span className={styles.cellText}>{item.displayName || t("list.emptyValue")}</span>
                        <span className={styles.cellText}>{item.managerNo || t("list.emptyValue")}</span>
                        <span className={styles.cellText}>{item.phone || t("list.emptyValue")}</span>
                        <span
                          className={`${styles.statusBadge} ${
                            item.enabled ? styles.statusEnabled : styles.statusDisabled
                          }`}
                        >
                          {item.enabled ? t("status.enabled") : t("status.disabled")}
                        </span>
                        <div className={styles.rowActions}>
                          <button
                            type="button"
                            className={styles.rowAction}
                            onClick={() => void openManagerDetail(item.userId)}
                            disabled={deletingUserId === item.userId}
                          >
                            <i className="fas fa-pen" />
                            {t("actions.edit")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void deleteManager(item.userId)}
                            disabled={deletingUserId === item.userId}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingUserId === item.userId ? t("actions.deleting") : t("actions.delete")}
                          </button>
                        </div>
                      </article>
                    ))}
              </div>
            )}
          </div>

          <div className={styles.footerBar}>
            <p className={styles.footerMeta}>
              {t("list.pageInfo", {
                page: pageInfo.page,
                totalPages: pageInfo.totalPages || 1,
                total: pageInfo.total
              })}
            </p>
            <div className={styles.pager}>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={pageInfo.page <= 1 || listLoading}
                onClick={() => void changePage(pageInfo.page - 1)}
              >
                {tCommon("action.previous")}
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={pageInfo.totalPages <= 0 || pageInfo.page >= pageInfo.totalPages || listLoading}
                onClick={() => void changePage(pageInfo.page + 1)}
              >
                {tCommon("action.next")}
              </button>
            </div>
          </div>
        </div>
      </section>

      {detailOpen ? renderDetailDrawer() : null}
    </main>
  );

  function renderDetailDrawer() {
    const studentMode = activeTab === "students";
    const managerCreateMode = !studentMode && managerDrawerMode === "create";

    return (
      <div className={styles.drawerLayer} onClick={() => closeDetail()}>
        <aside className={styles.drawer} onClick={(event) => event.stopPropagation()}>
          <div className={styles.drawerHead}>
            <div>
              <h3>
                {t(
                  studentMode
                    ? "drawer.studentTitle"
                    : managerCreateMode
                      ? "drawer.managerCreateTitle"
                      : "drawer.managerTitle"
                )}
              </h3>
              <p>
                {t(
                  studentMode
                    ? "drawer.studentSubtitle"
                    : managerCreateMode
                      ? "drawer.managerCreateSubtitle"
                      : "drawer.managerSubtitle"
                )}
              </p>
            </div>
            <button type="button" className={styles.closeButton} onClick={() => closeDetail()}>
              <i className="fas fa-xmark" />
            </button>
          </div>

          {detailLoading ? (
            <div className={styles.drawerLoading}>{t("states.loadingDetail")}</div>
          ) : studentMode ? (
            <form className={styles.drawerBody} onSubmit={(event) => void saveStudentDetail(event)}>
              <div className={styles.metaPanel}>
                <div className={styles.metaItem}>
                  <span>{t("drawer.readonly.role")}</span>
                  <strong>{studentDetail.role || t("list.emptyValue")}</strong>
                </div>
                <div className={styles.metaItem}>
                  <span>{t("drawer.readonly.createdAt")}</span>
                  <strong>{formatDateTime(studentDetail.createdAt)}</strong>
                </div>
                <div className={styles.metaItem}>
                  <span>{t("drawer.readonly.updatedAt")}</span>
                  <strong>{formatDateTime(studentDetail.updatedAt)}</strong>
                </div>
                <div className={styles.metaItem}>
                  <span>{t("drawer.readonly.avatar")}</span>
                  <strong>{studentDetail.hasAvatar ? t("drawer.hasAvatar") : t("drawer.noAvatar")}</strong>
                </div>
              </div>

              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>{t("form.username")}</span>
                  <input
                    className={styles.input}
                    value={studentDetail.username}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, username: event.target.value }))}
                    maxLength={64}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.email")}</span>
                  <input
                    className={styles.input}
                    value={studentDetail.email}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, email: event.target.value }))}
                    maxLength={128}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.displayName")}</span>
                  <input
                    className={styles.input}
                    value={studentDetail.displayName}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, displayName: event.target.value }))}
                    maxLength={120}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.studentNo")}</span>
                  <input
                    className={styles.input}
                    value={studentDetail.studentNo}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, studentNo: event.target.value }))}
                    maxLength={64}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.grade")}</span>
                  <select
                    className={styles.select}
                    value={studentDetail.grade}
                    onChange={(event) =>
                      setStudentDetail((current) => ({
                        ...current,
                        grade: (event.target.value as AdminStudentDetail["grade"]) || ""
                      }))
                    }
                  >
                    <option value="">{t("grades.placeholder")}</option>
                    <option value="HIGH_1">{t("grades.HIGH_1")}</option>
                    <option value="HIGH_2">{t("grades.HIGH_2")}</option>
                    <option value="HIGH_3">{t("grades.HIGH_3")}</option>
                  </select>
                </label>
                <label className={styles.field}>
                  <span>{t("form.className")}</span>
                  <div className={styles.classInputGroup}>
                    <input
                      className={styles.input}
                      value={studentDetail.className}
                      onChange={(event) =>
                        setStudentDetail((current) => ({
                          ...current,
                          className: resolveStudentClassNameInput(event.target.value, current.className)
                        }))
                      }
                      inputMode="numeric"
                    />
                    <span className={styles.classSuffix}>{t("form.classSuffix")}</span>
                  </div>
                </label>
                <label className={styles.field}>
                  <span>{t("form.phone")}</span>
                  <input
                    className={styles.input}
                    value={studentDetail.phone}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, phone: event.target.value }))}
                    maxLength={32}
                  />
                </label>
                <label className={styles.toggleField}>
                  <span>{t("form.enabled")}</span>
                  <button
                    type="button"
                    className={`${styles.toggleButton} ${studentDetail.enabled ? styles.enabled : styles.disabled}`}
                    onClick={() => setStudentDetail((current) => ({ ...current, enabled: !current.enabled }))}
                  >
                    {studentDetail.enabled ? t("status.enabled") : t("status.disabled")}
                  </button>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>{t("form.bio")}</span>
                  <textarea
                    className={styles.textarea}
                    value={studentDetail.bio}
                    onChange={(event) => setStudentDetail((current) => ({ ...current, bio: event.target.value }))}
                    rows={5}
                    maxLength={1000}
                  />
                </label>
              </div>

              <div className={styles.drawerActions}>
                <button
                  type="button"
                  className={`${styles.secondaryButton} ${styles.drawerActionButton}`}
                  onClick={() => void resetSingleStudentPassword(studentDetail.userId)}
                  disabled={savingDetail || deletingUserId === studentDetail.userId || resettingStudentPasswordId === studentDetail.userId}
                >
                  <i className="fas fa-key" />
                  {resettingStudentPasswordId === studentDetail.userId
                    ? t("actions.resettingSingleStudentPassword")
                    : t("actions.resetSingleStudentPassword")}
                </button>
                <button
                  type="button"
                  className={`${styles.dangerButton} ${styles.drawerActionButton}`}
                  onClick={() => void deleteStudent(studentDetail.userId)}
                  disabled={savingDetail || deletingUserId === studentDetail.userId}
                >
                  {deletingUserId === studentDetail.userId ? t("actions.deleting") : t("actions.delete")}
                </button>
                <button
                  type="button"
                  className={`${styles.secondaryButton} ${styles.drawerActionButton}`}
                  onClick={() => closeDetail()}
                >
                  {t("actions.cancel")}
                </button>
                <button
                  type="submit"
                  className={`${styles.primaryButton} ${styles.drawerActionButton}`}
                  disabled={savingDetail}
                >
                  {savingDetail ? t("actions.saving") : t("actions.save")}
                </button>
              </div>
            </form>
          ) : (
            <form className={styles.drawerBody} onSubmit={(event) => void saveManagerDetail(event)}>
              {managerCreateMode ? (
                <section className={styles.createIntro}>
                  <div className={styles.createIntroBadge}>
                    <i className="fas fa-id-badge" />
                    {t("drawer.managerCreateBadge")}
                  </div>
                  <h4>{t("drawer.managerCreateLead")}</h4>
                  <p>{t("drawer.managerCreateHint")}</p>
                </section>
              ) : (
                <div className={styles.metaPanel}>
                  <div className={styles.metaItem}>
                    <span>{t("drawer.readonly.role")}</span>
                    <strong>{managerDetail.role || t("list.emptyValue")}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("drawer.readonly.createdAt")}</span>
                    <strong>{formatDateTime(managerDetail.createdAt)}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("drawer.readonly.updatedAt")}</span>
                    <strong>{formatDateTime(managerDetail.updatedAt)}</strong>
                  </div>
                  <div className={styles.metaItem}>
                    <span>{t("drawer.readonly.avatar")}</span>
                    <strong>{managerDetail.hasAvatar ? t("drawer.hasAvatar") : t("drawer.noAvatar")}</strong>
                  </div>
                </div>
              )}

              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>{t("form.username")}</span>
                  <input
                    className={styles.input}
                    value={managerDetail.username}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, username: event.target.value }))}
                    maxLength={64}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.email")}</span>
                  <input
                    className={styles.input}
                    value={managerDetail.email}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, email: event.target.value }))}
                    maxLength={128}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.displayName")}</span>
                  <input
                    className={styles.input}
                    value={managerDetail.displayName}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, displayName: event.target.value }))}
                    maxLength={120}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.managerNo")}</span>
                  <input
                    className={styles.input}
                    value={managerDetail.managerNo}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, managerNo: event.target.value }))}
                    maxLength={64}
                  />
                </label>
                <label className={styles.field}>
                  <span>{t("form.phone")}</span>
                  <input
                    className={styles.input}
                    value={managerDetail.phone}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, phone: event.target.value }))}
                    maxLength={32}
                  />
                </label>
                <label className={styles.toggleField}>
                  <span>{t("form.enabled")}</span>
                  <button
                    type="button"
                    className={`${styles.toggleButton} ${managerDetail.enabled ? styles.enabled : styles.disabled}`}
                    onClick={() => setManagerDetail((current) => ({ ...current, enabled: !current.enabled }))}
                  >
                    {managerDetail.enabled ? t("status.enabled") : t("status.disabled")}
                  </button>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>{t("form.bio")}</span>
                  <textarea
                    className={styles.textarea}
                    value={managerDetail.bio}
                    onChange={(event) => setManagerDetail((current) => ({ ...current, bio: event.target.value }))}
                    rows={5}
                    maxLength={1000}
                  />
                </label>
              </div>

              <div className={styles.drawerActions}>
                {!managerCreateMode ? (
                  <button
                    type="button"
                    className={`${styles.dangerButton} ${styles.drawerActionButton}`}
                    onClick={() => void deleteManager(managerDetail.userId)}
                    disabled={savingDetail || deletingUserId === managerDetail.userId}
                  >
                    {deletingUserId === managerDetail.userId ? t("actions.deleting") : t("actions.delete")}
                  </button>
                ) : null}
                <button
                  type="button"
                  className={`${styles.secondaryButton} ${styles.drawerActionButton}`}
                  onClick={() => closeDetail()}
                >
                  {t("actions.cancel")}
                </button>
                <button
                  type="submit"
                  className={`${styles.primaryButton} ${styles.drawerActionButton}`}
                  disabled={savingDetail}
                >
                  {savingDetail
                    ? t(managerCreateMode ? "actions.creatingManager" : "actions.saving")
                    : t(managerCreateMode ? "actions.confirmCreateManager" : "actions.save")}
                </button>
              </div>
            </form>
          )}
        </aside>
      </div>
    );
  }

  async function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    closeDetail();

    if (isStudentTab) {
      const nextQuery = studentSearchInput.trim();
      setStudentQuery(nextQuery);
      await loadStudents(1, nextQuery, false, studentSortBy, studentSortDirection);
      return;
    }

    const nextQuery = managerSearchInput.trim();
    setManagerQuery(nextQuery);
    await loadManagers(1, nextQuery, false, managerSortBy, managerSortDirection);
  }

  async function changePage(nextPage: number) {
    closeDetail();

    if (isStudentTab) {
      await loadStudents(nextPage, studentQuery, false, studentSortBy, studentSortDirection);
      return;
    }

    await loadManagers(nextPage, managerQuery, false, managerSortBy, managerSortDirection);
  }

  async function handleSortToggle(sortBy: AdminUserSortBy) {
    closeDetail();

    if (isStudentTab) {
      const nextDirection = studentSortBy === sortBy ? toggleSortDirection(studentSortDirection) : "asc";
      setStudentSortBy(sortBy);
      setStudentSortDirection(nextDirection);
      await loadStudents(1, studentQuery, false, sortBy, nextDirection);
      return;
    }

    const nextDirection = managerSortBy === sortBy ? toggleSortDirection(managerSortDirection) : "asc";
    setManagerSortBy(sortBy);
    setManagerSortDirection(nextDirection);
    await loadManagers(1, managerQuery, false, sortBy, nextDirection);
  }

  async function loadStudents(
    page: number,
    keyword: string,
    initial = false,
    sortBy: AdminUserSortBy | null = studentSortBy,
    sortDirection: AdminSortDirection = studentSortDirection
  ) {
    const query = new URLSearchParams({
      page: String(Math.max(page, 1)),
      pageSize: String(PAGE_SIZE),
      keyword
    });
    if (sortBy) {
      query.set("sortBy", sortBy);
      query.set("sortDirection", sortDirection);
    }

    const response = await fetchWithAuthorization(`/api/admin/students?${query.toString()}`);
    if (!response) {
      if (initial) {
        setInitialLoading(false);
      }
      return;
    }

    setListLoading(true);
    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminStudentPage }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadStudentsFailed"));
        return;
      }

      setStudentPage({
        items: Array.isArray(result.data.items) ? result.data.items : [],
        page: Number(result.data.page || 1),
        pageSize: Number(result.data.pageSize || PAGE_SIZE),
        total: Number(result.data.total || 0),
        totalPages: Number(result.data.totalPages || 0)
      });
      setStudentsLoaded(true);
    } catch {
      notify.error(t("messages.loadStudentsFailed"));
    } finally {
      setListLoading(false);
      if (initial) {
        setInitialLoading(false);
      }
    }
  }

  async function loadManagers(
    page: number,
    keyword: string,
    initial = false,
    sortBy: AdminUserSortBy | null = managerSortBy,
    sortDirection: AdminSortDirection = managerSortDirection
  ) {
    const query = new URLSearchParams({
      page: String(Math.max(page, 1)),
      pageSize: String(PAGE_SIZE),
      keyword
    });
    if (sortBy) {
      query.set("sortBy", sortBy);
      query.set("sortDirection", sortDirection);
    }

    const response = await fetchWithAuthorization(`/api/admin/managers?${query.toString()}`);
    if (!response) {
      if (initial) {
        setInitialLoading(false);
      }
      return;
    }

    setListLoading(true);
    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminManagerPage }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadManagersFailed"));
        return;
      }

      setManagerPage({
        items: Array.isArray(result.data.items) ? result.data.items : [],
        page: Number(result.data.page || 1),
        pageSize: Number(result.data.pageSize || PAGE_SIZE),
        total: Number(result.data.total || 0),
        totalPages: Number(result.data.totalPages || 0)
      });
      setManagersLoaded(true);
    } catch {
      notify.error(t("messages.loadManagersFailed"));
    } finally {
      setListLoading(false);
      if (initial) {
        setInitialLoading(false);
      }
    }
  }

  async function openStudentDetail(userId: number) {
    const response = await fetchWithAuthorization(`/api/admin/students/${userId}`);
    if (!response) {
      return;
    }

    setDetailOpen(true);
    setDetailLoading(true);

    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminStudentDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadDetailFailed"));
        closeDetail();
        return;
      }

      setStudentDetail(result.data);
    } catch {
      notify.error(t("messages.loadDetailFailed"));
      closeDetail();
    } finally {
      setDetailLoading(false);
    }
  }

  async function openManagerDetail(userId: number) {
    const response = await fetchWithAuthorization(`/api/admin/managers/${userId}`);
    if (!response) {
      return;
    }

    setManagerDrawerMode("edit");
    setDetailOpen(true);
    setDetailLoading(true);

    try {
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminManagerDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadDetailFailed"));
        closeDetail();
        return;
      }

      setManagerDetail(result.data);
    } catch {
      notify.error(t("messages.loadDetailFailed"));
      closeDetail();
    } finally {
      setDetailLoading(false);
    }
  }

  function openCreateManager() {
    setManagerDrawerMode("create");
    setDetailLoading(false);
    setManagerDetail(EMPTY_MANAGER_DETAIL);
    setDetailOpen(true);
  }

  async function saveStudentDetail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const payload: UpdateAdminStudentPayload = {
      username: studentDetail.username.trim(),
      email: studentDetail.email.trim(),
      enabled: studentDetail.enabled,
      displayName: studentDetail.displayName.trim(),
      studentNo: studentDetail.studentNo.trim(),
      grade: studentDetail.grade,
      className: studentDetail.className.trim(),
      phone: studentDetail.phone.trim(),
      bio: studentDetail.bio.trim()
    };

    setSavingDetail(true);
    try {
      const response = await fetchWithAuthorization(`/api/admin/students/${studentDetail.userId}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminStudentDetail }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveStudentFailed"));
        return;
      }

      setStudentDetail(result.data);
      setStudentPage((current) => ({
        ...current,
        items: current.items.map((item) => (item.userId === result.data?.userId ? toStudentListItem(result.data) : item))
      }));
      notify.success(t("messages.saveStudentSuccess"));
    } catch {
      notify.error(t("messages.saveStudentFailed"));
    } finally {
      setSavingDetail(false);
    }
  }

  async function saveManagerDetail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const creatingManager = managerDrawerMode === "create";

    const payload: CreateAdminManagerPayload = {
      username: managerDetail.username.trim(),
      email: managerDetail.email.trim(),
      enabled: managerDetail.enabled,
      displayName: managerDetail.displayName.trim(),
      managerNo: managerDetail.managerNo.trim(),
      phone: managerDetail.phone.trim(),
      bio: managerDetail.bio.trim()
    };

    setSavingDetail(true);
    try {
      const response = await fetchWithAuthorization(creatingManager ? "/api/admin/managers" : `/api/admin/managers/${managerDetail.userId}`, {
        method: creatingManager ? "POST" : "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminManagerDetail | AdminManagerCreateResult }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t(creatingManager ? "messages.createManagerFailed" : "messages.saveManagerFailed"));
        return;
      }

      if (creatingManager) {
        const createResult = result.data as AdminManagerCreateResult;
        setManagerDrawerMode("edit");
        setManagerDetail(createResult.manager);
        await loadManagers(1, managerQuery);
        notify.success(
          t("messages.createManagerSuccess", {
            password: createResult.defaultPassword
          })
        );
        return;
      }

      const detail = result.data as AdminManagerDetail;
      setManagerDetail(detail);
      setManagerPage((current) => ({
        ...current,
        items: current.items.map((item) => (item.userId === detail.userId ? toManagerListItem(detail) : item))
      }));
      notify.success(t("messages.saveManagerSuccess"));
    } catch {
      notify.error(t(creatingManager ? "messages.createManagerFailed" : "messages.saveManagerFailed"));
    } finally {
      setSavingDetail(false);
    }
  }

  async function importStudents(file: File) {
    setImportingStudents(true);
    try {
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetchWithAuthorization("/api/admin/students/import", {
        method: "POST",
        body: formData
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminStudentImportResult }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.importFailed"));
        return;
      }

      setStudentImportResult(result.data);
      notify.success(
        t("messages.importFinished", {
          successCount: result.data.successCount,
          failureCount: result.data.failureCount
        })
      );
      await loadStudents(1, studentQuery);
    } catch {
      notify.error(t("messages.importFailed"));
    } finally {
      setImportingStudents(false);
    }
  }

  async function downloadStudentsTemplate() {
    setDownloadingTemplate(true);
    try {
      await downloadBinary("/api/admin/students/template", t("downloads.studentTemplate"));
    } finally {
      setDownloadingTemplate(false);
    }
  }

  async function exportStudents() {
    setExportingStudents(true);
    try {
      const query = studentQuery.trim() ? `?keyword=${encodeURIComponent(studentQuery.trim())}` : "";
      await downloadBinary(`/api/admin/students/export${query}`, t("downloads.studentExport"));
    } finally {
      setExportingStudents(false);
    }
  }

  async function downloadBinary(url: string, fallbackName: string) {
    try {
      const response = await fetchWithAuthorization(url);
      if (!response) {
        return;
      }

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok) {
        const result = (await response.json().catch(() => null)) as { message?: string } | null;
        notify.error(result?.message || t("messages.downloadFailed"));
        return;
      }

      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = resolveDownloadName(response.headers.get("content-disposition"), fallbackName);
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch {
      notify.error(t("messages.downloadFailed"));
    }
  }

  async function resetPasswords() {
    const accepted = await confirm.confirm({
      title: t(isStudentTab ? "confirm.resetStudentsTitle" : "confirm.resetManagersTitle"),
      message: t(isStudentTab ? "confirm.resetStudentsMessage" : "confirm.resetManagersMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setResettingPasswords(true);
    try {
      const response = await fetchWithAuthorization(isStudentTab ? "/api/admin/students/reset-passwords" : "/api/admin/managers/reset-passwords", {
        method: "POST"
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminBulkResetPasswordResult }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.resetFailed"));
        return;
      }

      notify.success(
        t(isStudentTab ? "messages.resetStudentsSuccess" : "messages.resetManagersSuccess", {
          count: result.data.resetCount,
          password: result.data.defaultPassword
        })
      );
    } catch {
      notify.error(t("messages.resetFailed"));
    } finally {
      setResettingPasswords(false);
    }
  }

  async function deleteStudent(userId: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteStudentTitle"),
      message: t("confirm.deleteStudentMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingUserId(userId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/students/${userId}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: { deletedUserId?: number } }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.deleteStudentFailed"));
        return;
      }

      closeDetailIfMatches(userId);
      await loadStudents(resolveNextPageAfterDeletion(studentPage.page, studentPage.total), studentQuery);
      notify.success(t("messages.deleteStudentSuccess"));
    } catch {
      notify.error(t("messages.deleteStudentFailed"));
    } finally {
      setDeletingUserId(null);
    }
  }

  async function resetSingleStudentPassword(userId: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.resetSingleStudentTitle"),
      message: t("confirm.resetSingleStudentMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setResettingStudentPasswordId(userId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/students/${userId}/reset-password`, {
        method: "POST"
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; message?: string; data?: AdminBulkResetPasswordResult }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.resetSingleStudentFailed"));
        return;
      }

      notify.success(
        t("messages.resetSingleStudentSuccess", {
          password: result.data.defaultPassword
        })
      );
    } catch {
      notify.error(t("messages.resetSingleStudentFailed"));
    } finally {
      setResettingStudentPasswordId(null);
    }
  }

  async function deleteManager(userId: number) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteManagerTitle"),
      message: t("confirm.deleteManagerMessage"),
      confirmText: tCommon("action.confirm"),
      cancelText: tCommon("action.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingUserId(userId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/managers/${userId}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; code?: string; message?: string; data?: { deletedUserId?: number } }
        | null;

      if (!(await handleAuthStatus(response.status))) {
        return;
      }

      if (!response.ok || result?.success !== true) {
        if (result?.code === "DEPENDENCY_EXISTS") {
          notify.error(t("messages.deleteManagerBlocked"));
          return;
        }
        notify.error(result?.message || t("messages.deleteManagerFailed"));
        return;
      }

      closeDetailIfMatches(userId);
      await loadManagers(resolveNextPageAfterDeletion(managerPage.page, managerPage.total), managerQuery);
      notify.success(t("messages.deleteManagerSuccess"));
    } catch {
      notify.error(t("messages.deleteManagerFailed"));
    } finally {
      setDeletingUserId(null);
    }
  }

  function closeDetail() {
    setDetailOpen(false);
    setDetailLoading(false);
    setSavingDetail(false);
    setManagerDrawerMode("edit");
    setResettingStudentPasswordId(null);
    setStudentDetail(EMPTY_STUDENT_DETAIL);
    setManagerDetail(EMPTY_MANAGER_DETAIL);
  }

  function closeDetailIfMatches(userId: number) {
    if (activeTab === "students" && studentDetail.userId === userId) {
      closeDetail();
      return;
    }
    if (activeTab === "managers" && managerDetail.userId === userId) {
      closeDetail();
    }
  }

  function resolveFailureMessage(failure: AdminStudentImportFailure) {
    const reasonKey = `importResult.reasons.${failure.reasonCode}`;
    const translated = t(reasonKey);
    if (translated !== reasonKey) {
      return translated;
    }
    return failure.reasonMessage || t("importResult.reasons.default");
  }

  async function fetchWithAuthorization(url: string, init?: RequestInit) {
    const response = await authorizedFetch(url, {
      ...init,
      cache: "no-store"
    });
    if (!response) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return null;
    }
    return response;
  }

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      router.replace("/login");
      return false;
    }
    if (status === 403) {
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return false;
    }
    return true;
  }

  function resolveDownloadName(contentDisposition: string | null, fallbackName: string) {
    if (!contentDisposition) {
      return fallbackName;
    }

    const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (encodedMatch?.[1]) {
      return decodeURIComponent(encodedMatch[1]);
    }

    const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
    if (plainMatch?.[1]) {
      return plainMatch[1];
    }

    return fallbackName;
  }

  function formatDateTime(value: string | null) {
    if (!value) {
      return t("list.emptyValue");
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return t("list.emptyValue");
    }

    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    }).format(parsed);
  }

  function formatStudentIdentity(item: AdminStudentListItem) {
    const identity = [
      item.grade ? t(`grades.${item.grade}`) : "",
      item.className ? formatStudentClassName(item.className) : "",
      item.studentNo ? `${item.studentNo}${t("list.studentNoSuffix")}` : ""
    ]
      .filter(Boolean)
      .join("");

    return identity || t("list.emptyValue");
  }

  function resolveNextPageAfterDeletion(currentPage: number, total: number) {
    if (total <= 1) {
      return 1;
    }
    const nextTotal = total - 1;
    const nextTotalPages = Math.max(1, Math.ceil(nextTotal / PAGE_SIZE));
    return Math.min(currentPage, nextTotalPages);
  }

  function toggleSortDirection(direction: AdminSortDirection): AdminSortDirection {
    return direction === "asc" ? "desc" : "asc";
  }

  function resolveAriaSort(sortBy: AdminUserSortBy): "none" | "ascending" | "descending" {
    if (currentSortBy !== sortBy) {
      return "none";
    }
    return currentSortDirection === "desc" ? "descending" : "ascending";
  }

  function toStudentListItem(detail: AdminStudentDetail): AdminStudentListItem {
    return {
      userId: detail.userId,
      username: detail.username,
      email: detail.email,
      enabled: detail.enabled,
      displayName: detail.displayName,
      studentNo: detail.studentNo,
      grade: detail.grade,
      className: detail.className,
      phone: detail.phone,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt
    };
  }

  function toManagerListItem(detail: AdminManagerDetail): AdminManagerListItem {
    return {
      userId: detail.userId,
      username: detail.username,
      email: detail.email,
      enabled: detail.enabled,
      displayName: detail.displayName,
      managerNo: detail.managerNo,
      phone: detail.phone,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt
    };
  }
}
