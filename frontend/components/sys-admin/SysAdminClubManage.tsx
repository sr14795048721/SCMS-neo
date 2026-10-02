"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import {
  AdminClubDetail,
  AdminClubMutationPayload,
  AdminClubPage,
  AdminClubStatus,
  AdminManagerOption
} from "../../lib/admin-club/types";
import { useT } from "../../lib/i18n/useT";
import { paginateItems } from "../../lib/pagination";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { formatStudentClassName } from "../../lib/student/fieldConstraints";
import { ClubCreationRequestItem } from "../../lib/manager/teacherWorkflowTypes";
import styles from "../../styles/sysAdminClubManage.module.css";

const PAGE_SIZE = 10;

const EMPTY_PAGE: AdminClubPage = { items: [], page: 1, pageSize: PAGE_SIZE, total: 0, totalPages: 0 };
const EMPTY_DETAIL: AdminClubDetail = {
  id: 0,
  name: "",
  type: "",
  status: "ACTIVE",
  description: "",
  memberCount: 0,
  managerCount: 0,
  createdAt: null,
  updatedAt: null,
  managers: [],
  students: []
};
const EMPTY_FORM: AdminClubMutationPayload = { name: "", type: "", status: "ACTIVE", description: "", managerUserIds: [] };

type ClubDetailResponse = { success?: boolean; code?: string; message?: string; data?: AdminClubDetail };
type ClubPageResponse = { success?: boolean; code?: string; message?: string; data?: AdminClubPage };
type ManagerOptionsResponse = { success?: boolean; code?: string; message?: string; data?: AdminManagerOption[] };
type CreationRequestResponse = { success?: boolean; code?: string; message?: string; data?: ClubCreationRequestItem[] };
type PanelMode = "create" | "view" | "edit";
type StatusFilter = "ALL" | AdminClubStatus;

export function SysAdminClubManage() {
  const t = useT("adminClubManage");
  const tDuties = useT("teacherClubs");
  const tPortal = useT("portal");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [pageData, setPageData] = useState<AdminClubPage>(EMPTY_PAGE);
  const [listLoading, setListLoading] = useState(true);
  const [pageNumber, setPageNumber] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelMode, setPanelMode] = useState<PanelMode>("view");
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [currentDetail, setCurrentDetail] = useState<AdminClubDetail>(EMPTY_DETAIL);
  const [formState, setFormState] = useState<AdminClubMutationPayload>(EMPTY_FORM);
  const [managerSearch, setManagerSearch] = useState("");
  const [managerOptionsLoading, setManagerOptionsLoading] = useState(false);
  const [managerOptions, setManagerOptions] = useState<AdminManagerOption[]>([]);
  const [selectedManagers, setSelectedManagers] = useState<AdminManagerOption[]>([]);
  const [viewMode, setViewMode] = useState<"clubs" | "requests">("clubs");
  const [requestsLoading, setRequestsLoading] = useState(false);
  const [creationRequests, setCreationRequests] = useState<ClubCreationRequestItem[]>([]);
  const [requestPage, setRequestPage] = useState(1);
  const [managerPage, setManagerPage] = useState(1);
  const [studentPage, setStudentPage] = useState(1);

  useEffect(() => {
    if (authorized) {
      void loadClubs(pageNumber, searchQuery, statusFilter);
    }
  }, [authorized, pageNumber, searchQuery, statusFilter]);

  useEffect(() => {
    const tab = searchParams.get("tab");
    if (tab === "requests") {
      setViewMode("requests");
    }
  }, [searchParams]);

  useEffect(() => {
    if (authorized && viewMode === "requests") {
      void loadCreationRequests();
    }
  }, [authorized, viewMode]);

  useEffect(() => {
    if (!panelOpen || panelMode === "view") {
      return;
    }
    const timer = window.setTimeout(() => void loadManagerOptions(managerSearch), 180);
    return () => window.clearTimeout(timer);
  }, [managerSearch, panelMode, panelOpen]);

  const mergedManagerOptions = useMemo(() => {
    const map = new Map<number, AdminManagerOption>();
    selectedManagers.forEach((item) => map.set(item.userId, item));
    managerOptions.forEach((item) => map.set(item.userId, item));
    return Array.from(map.values());
  }, [managerOptions, selectedManagers]);

  const visiblePages = useMemo(() => buildVisiblePages(pageData.totalPages, pageData.page), [pageData.page, pageData.totalPages]);
  const pagedRequests = useMemo(() => paginateItems(creationRequests, requestPage, 10), [creationRequests, requestPage]);
  const pagedManagers = useMemo(() => paginateItems(currentDetail.managers, managerPage, 8), [currentDetail.managers, managerPage]);
  const pagedStudents = useMemo(() => paginateItems(currentDetail.students, studentPage, 8), [currentDetail.students, studentPage]);
  const isCreateMode = panelMode === "create";
  const isEditingMode = panelMode !== "view";

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
              <i className="fas fa-sitemap" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button type="button" className={styles.primaryButton} onClick={openCreatePanel}>
              <i className="fas fa-plus" />
              {t("actions.create")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.viewTabs}>
          <button type="button" className={`${styles.viewTab} ${viewMode === "clubs" ? styles.viewTabActive : ""}`} onClick={() => setViewMode("clubs")}>
            {t("tabs.clubs")}
          </button>
          <button type="button" className={`${styles.viewTab} ${viewMode === "requests" ? styles.viewTabActive : ""}`} onClick={() => {
            setViewMode("requests");
            setRequestPage(1);
          }}>
            {t("tabs.requests")}
          </button>
        </div>
        {viewMode === "clubs" ? (
        <div className={styles.card}>
          <div className={styles.toolbar}>
            <form className={styles.searchForm} onSubmit={(event) => handleSearchSubmit(event)}>
              <i className={`fas fa-search ${styles.searchIcon}`} />
              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                className={styles.searchInput}
                placeholder={t("filters.searchPlaceholder")}
              />
              <select
                value={statusFilter}
                onChange={(event) => {
                  setPageNumber(1);
                  setStatusFilter(event.target.value as StatusFilter);
                }}
                className={styles.filterSelect}
              >
                <option value="ALL">{t("filters.allStatuses")}</option>
                <option value="ACTIVE">{t("status.active")}</option>
                <option value="INACTIVE">{t("status.inactive")}</option>
              </select>
              <button type="submit" className={styles.primaryButton} disabled={listLoading}>
                {t("actions.search")}
              </button>
            </form>
          </div>

          {listLoading ? (
            <div className={styles.loadingInline}>{t("states.loadingList")}</div>
          ) : !pageData.items.length ? (
            <div className={styles.emptyState}>
              <i className="fas fa-inbox" />
              <h3>{t("empty.title")}</h3>
              <p>{t("empty.description")}</p>
            </div>
          ) : (
            <div className={styles.list}>
              {pageData.items.map((club) => (
                <article key={club.id} className={styles.itemCard}>
                  <div className={styles.itemHead}>
                    <div className={styles.itemTitleBlock}>
                      <div className={styles.itemTitleRow}>
                        <h2>{club.name}</h2>
                        <span className={`${styles.statusBadge} ${club.status === "ACTIVE" ? styles.statusActive : styles.statusInactive}`}>
                          {t(club.status === "ACTIVE" ? "status.active" : "status.inactive")}
                        </span>
                      </div>
                      <div className={styles.metaRow}>
                        <span>{club.type || t("detail.emptyValue")}</span>
                        <span>{t("list.memberCount", { count: club.memberCount })}</span>
                        <span>{t("list.managerCount", { count: club.managerCount })}</span>
                        <span>{formatDateTime(club.createdAt, t("detail.emptyValue"))}</span>
                      </div>
                    </div>
                    <div className={styles.itemActions}>
                      <button type="button" className={styles.rowAction} onClick={() => void openDetailPanel(club.id, "view")}>
                        <i className="fas fa-eye" />
                        {t("actions.view")}
                      </button>
                      <button type="button" className={styles.rowAction} onClick={() => void openDetailPanel(club.id, "edit")}>
                        <i className="fas fa-pen" />
                        {t("actions.edit")}
                      </button>
                      <button
                        type="button"
                        className={styles.rowAction}
                        onClick={() => router.push(`/sys-admin/club-manage/${club.id}/duties`)}
                      >
                        <i className="fas fa-id-badge" />
                        {tDuties("positions.metaTitle")}
                      </button>
                      <button
                        type="button"
                        className={`${styles.rowAction} ${styles.rowDangerAction}`}
                        onClick={() => void handleDelete(club.id, club.name)}
                        disabled={deletingId === club.id}
                      >
                        <i className="fas fa-trash-can" />
                        {deletingId === club.id ? t("actions.deleting") : t("actions.delete")}
                      </button>
                    </div>
                  </div>
                  <p className={styles.itemDescription}>{club.description || t("list.noDescription")}</p>
                </article>
              ))}
            </div>
          )}

          {pageData.totalPages > 1 ? (
            <div className={styles.pagination}>
              <button
                type="button"
                className={styles.pagerButton}
                disabled={pageData.page <= 1}
                onClick={() => setPageNumber((prev) => Math.max(prev - 1, 1))}
              >
                <i className="fas fa-angle-left" />
                {t("actions.prevPage")}
              </button>
              <div className={styles.pageList}>
                {visiblePages.map((page) => (
                  <button key={page} type="button" className={`${styles.pageChip} ${page === pageData.page ? styles.pageChipActive : ""}`} onClick={() => setPageNumber(page)}>
                    {page}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className={styles.pagerButton}
                disabled={pageData.page >= pageData.totalPages}
                onClick={() => setPageNumber((prev) => Math.min(prev + 1, pageData.totalPages))}
              >
                {t("actions.nextPage")}
                <i className="fas fa-angle-right" />
              </button>
            </div>
          ) : null}
        </div>
        ) : (
          <div className={styles.card}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("requests.title")}</h2>
                <p>{t("requests.subtitle")}</p>
              </div>
            </div>

            {requestsLoading ? (
              <div className={styles.loadingInline}>{t("requests.loading")}</div>
            ) : creationRequests.length ? (
              <>
              <div className={styles.list}>
                {pagedRequests.items.map((item) => (
                  <article key={item.id} className={styles.itemCard}>
                    <div className={styles.itemHead}>
                      <div className={styles.itemTitleBlock}>
                        <div className={styles.itemTitleRow}>
                          <h2>{item.name}</h2>
                          <span
                            className={`${styles.statusBadge} ${
                              item.status === "APPROVED"
                                ? styles.statusActive
                                : item.status === "REJECTED"
                                  ? styles.statusInactive
                                  : styles.statusPending
                            }`}
                          >
                            {t(`requests.status.${item.status.toLowerCase()}`)}
                          </span>
                        </div>
                        <div className={styles.metaRow}>
                          <span>{item.type || t("detail.emptyValue")}</span>
                          <span>{t("requests.applicant", { name: item.applicantName || t("detail.emptyValue") })}</span>
                          <span>{item.createdAt ? formatDateTime(item.createdAt, t("detail.emptyValue")) : t("detail.emptyValue")}</span>
                        </div>
                      </div>
                      {item.status === "PENDING" ? (
                        <div className={styles.itemActions}>
                          <button
                            type="button"
                            className={styles.rowAction}
                            onClick={() => void approveCreationRequest(item.id)}
                          >
                            <i className="fas fa-check" />
                            {t("requests.approve")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void rejectCreationRequest(item.id)}
                          >
                            <i className="fas fa-xmark" />
                            {t("requests.reject")}
                          </button>
                        </div>
                      ) : null}
                    </div>
                    <p className={styles.itemDescription}>{item.description || t("detail.emptyDescription")}</p>
                    <p className={styles.requestReason}>{t("requests.reason", { reason: item.applyReason })}</p>
                  </article>
                ))}
              </div>
              <PaginationBar
                currentPage={pagedRequests.page}
                totalPages={pagedRequests.totalPages}
                prevLabel={tPagination("prev")}
                nextLabel={tPagination("next")}
                pageLabel={tPagination("status", { page: pagedRequests.page, total: pagedRequests.totalPages })}
                onPageChange={setRequestPage}
              />
              </>
            ) : (
              <div className={styles.emptyState}>
                <i className="fas fa-inbox" />
                <h3>{t("requests.emptyTitle")}</h3>
                <p>{t("requests.emptyDescription")}</p>
              </div>
            )}
          </div>
        )}
      </section>

      {panelOpen ? (
        <div className={styles.panelLayer} onClick={() => closePanel()}>
          <aside className={styles.panel} onClick={(event) => event.stopPropagation()}>
            <div className={styles.panelHead}>
              <div className={styles.panelHeadCopy}>
                <span className={styles.panelBadge}>{t(isCreateMode ? "panel.createBadge" : panelMode === "edit" ? "panel.editBadge" : "panel.viewBadge")}</span>
                <h2>{t(panelMode === "create" ? "panel.createTitle" : panelMode === "edit" ? "panel.editTitle" : "panel.viewTitle")}</h2>
                <p>{t(panelMode === "view" ? "panel.viewSubtitle" : "panel.editSubtitle")}</p>
              </div>
              <div className={styles.panelHeadActions}>
                {panelMode === "view" && currentDetail.id ? (
                  <>
                    <button type="button" className={styles.secondaryButton} onClick={() => setPanelMode("edit")}>
                      <i className="fas fa-pen" />
                      {t("actions.edit")}
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => router.push(`/sys-admin/club-manage/${currentDetail.id}/duties`)}
                    >
                      <i className="fas fa-id-badge" />
                      {tDuties("positions.metaTitle")}
                    </button>
                    <button type="button" className={`${styles.rowAction} ${styles.rowDangerAction}`} onClick={() => void handleDelete(currentDetail.id, currentDetail.name)} disabled={deletingId === currentDetail.id}>
                      <i className="fas fa-trash-can" />
                      {t("actions.delete")}
                    </button>
                  </>
                ) : isEditingMode ? (
                  <>
                    <button type="button" className={styles.secondaryButton} onClick={() => closePanel()} disabled={saving}>
                      <i className="fas fa-arrow-left" />
                      {t("actions.cancel")}
                    </button>
                    <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={saving}>
                      <i className="fas fa-floppy-disk" />
                      {saving ? t("actions.saving") : t("actions.save")}
                    </button>
                  </>
                ) : null}
                <button type="button" className={styles.closeButton} onClick={() => closePanel()}>
                  <i className="fas fa-xmark" />
                </button>
              </div>
            </div>

            {detailLoading ? (
              <div className={styles.panelLoading}>{t("states.loadingDetail")}</div>
            ) : (
              <div className={styles.panelBody}>
                {isEditingMode ? (
                  <section className={styles.panelSummary}>
                    <span className={styles.summaryPill}>{t("fields.name")}</span>
                    <span className={styles.summaryPill}>{t("fields.type")}</span>
                    <span className={styles.summaryPill}>{t("fields.status")}</span>
                    <span className={styles.summaryPill}>{t("detail.managerCount", { count: selectedManagers.length })}</span>
                  </section>
                ) : null}

                <div className={isEditingMode ? styles.editorGrid : styles.viewStack}>
                  <section className={styles.sectionCard}>
                    <div className={styles.sectionHead}>
                      <div>
                        <h3>{t("detail.basicTitle")}</h3>
                        {isEditingMode ? <p className={styles.sectionDescription}>{t("panel.basicSectionHint")}</p> : null}
                      </div>
                    </div>

                    {panelMode === "view" ? (
                      <div className={styles.viewGrid}>
                        <div className={styles.infoTile}><span>{t("fields.name")}</span><strong>{currentDetail.name || t("detail.emptyValue")}</strong></div>
                        <div className={styles.infoTile}><span>{t("fields.type")}</span><strong>{currentDetail.type || t("detail.emptyValue")}</strong></div>
                        <div className={styles.infoTile}><span>{t("fields.status")}</span><strong>{t(currentDetail.status === "ACTIVE" ? "status.active" : "status.inactive")}</strong></div>
                        <div className={styles.infoTile}><span>{t("fields.createdAt")}</span><strong>{formatDateTime(currentDetail.createdAt, t("detail.emptyValue"))}</strong></div>
                        <div className={styles.infoTile}><span>{t("fields.memberCount")}</span><strong>{currentDetail.memberCount}</strong></div>
                        <div className={styles.infoTile}><span>{t("fields.managerCount")}</span><strong>{currentDetail.managerCount}</strong></div>
                        <div className={`${styles.infoTile} ${styles.infoTileWide}`}><span>{t("fields.description")}</span><strong>{currentDetail.description || t("detail.emptyDescription")}</strong></div>
                      </div>
                    ) : (
                      <div className={styles.formGrid}>
                        <label className={styles.field}>
                          <span>{t("fields.name")}</span>
                          <input value={formState.name} onChange={(event) => setFormState((prev) => ({ ...prev, name: event.target.value.slice(0, 12) }))} className={styles.input} maxLength={12} />
                        </label>
                        <label className={styles.field}>
                          <span>{t("fields.type")}</span>
                          <input value={formState.type} onChange={(event) => setFormState((prev) => ({ ...prev, type: event.target.value.slice(0, 64) }))} className={styles.input} maxLength={64} />
                        </label>
                        <label className={styles.field}>
                          <span>{t("fields.status")}</span>
                          <select value={formState.status} onChange={(event) => setFormState((prev) => ({ ...prev, status: event.target.value as AdminClubStatus }))} className={styles.select}>
                            <option value="ACTIVE">{t("status.active")}</option>
                            <option value="INACTIVE">{t("status.inactive")}</option>
                          </select>
                        </label>
                        <label className={`${styles.field} ${styles.fieldWide}`}>
                          <span>{t("fields.description")}</span>
                          <textarea value={formState.description} onChange={(event) => setFormState((prev) => ({ ...prev, description: event.target.value.slice(0, 1000) }))} className={styles.textarea} maxLength={1000} />
                        </label>
                      </div>
                    )}
                  </section>

                  <section className={styles.sectionCard}>
                    <div className={styles.sectionHead}>
                      <div>
                        <h3>{t("detail.managersTitle")}</h3>
                        <p className={styles.sectionDescription}>
                          {panelMode === "view" ? t("panel.managerSectionViewHint") : t("panel.managerSectionEditHint")}
                        </p>
                      </div>
                      <span className={styles.sectionMeta}>{t("detail.managerCount", { count: selectedManagers.length || currentDetail.managers.length })}</span>
                    </div>
                    {panelMode === "view" ? renderManagerView() : renderManagerEditor()}
                  </section>
                </div>

                {isCreateMode ? (
                  <section className={`${styles.sectionCard} ${styles.helperCard}`}>
                    <div className={styles.sectionHead}>
                      <div>
                        <h3>{t("panel.createGuideTitle")}</h3>
                        <p className={styles.sectionDescription}>{t("panel.createGuideDescription")}</p>
                      </div>
                    </div>
                    <div className={styles.helperList}>
                      <article className={styles.helperItem}>
                        <strong>01</strong>
                        <p>{t("panel.createGuideStep1")}</p>
                      </article>
                      <article className={styles.helperItem}>
                        <strong>02</strong>
                        <p>{t("panel.createGuideStep2")}</p>
                      </article>
                      <article className={styles.helperItem}>
                        <strong>03</strong>
                        <p>{t("panel.createGuideStep3")}</p>
                      </article>
                    </div>
                  </section>
                ) : null}

                {!isCreateMode ? (
                  <section className={styles.sectionCard}>
                    <div className={styles.sectionHead}>
                      <div>
                        <h3>{t("detail.studentsTitle")}</h3>
                        <p className={styles.sectionDescription}>{t("panel.studentSectionHint")}</p>
                      </div>
                      <span className={styles.sectionMeta}>{t("detail.studentCount", { count: currentDetail.students.length })}</span>
                    </div>
                    {renderStudentView()}
                  </section>
                ) : null}
              </div>
            )}
          </aside>
        </div>
      ) : null}
    </main>
  );

  function renderManagerView() {
    return currentDetail.managers.length ? (
      <>
      <div className={styles.tableWrapper}>
        <table className={styles.dataTable}>
          <thead>
            <tr>
              <th>{t("table.manager.displayName")}</th>
              <th>{t("table.manager.username")}</th>
              <th>{t("table.manager.managerNo")}</th>
              <th>{t("table.manager.phone")}</th>
            </tr>
          </thead>
          <tbody>
            {pagedManagers.items.map((manager) => (
              <tr key={manager.userId}>
                <td>{manager.displayName || t("detail.emptyValue")}</td>
                <td>{manager.username || t("detail.emptyValue")}</td>
                <td>{manager.managerNo || t("detail.emptyValue")}</td>
                <td>{manager.phone || t("detail.emptyValue")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <PaginationBar
        currentPage={pagedManagers.page}
        totalPages={pagedManagers.totalPages}
        prevLabel={tPagination("prev")}
        nextLabel={tPagination("next")}
        pageLabel={tPagination("status", { page: pagedManagers.page, total: pagedManagers.totalPages })}
        onPageChange={setManagerPage}
      />
      </>
    ) : (
      <div className={styles.emptyBlock}>{t("empty.managers")}</div>
    );
  }

  function renderStudentView() {
    return currentDetail.students.length ? (
      <>
      <div className={styles.tableWrapper}>
        <table className={styles.dataTable}>
          <thead>
            <tr>
              <th>{t("table.student.displayName")}</th>
              <th>{t("table.student.username")}</th>
              <th>{t("table.student.studentNo")}</th>
              <th>{t("table.student.grade")}</th>
              <th>{t("table.student.className")}</th>
            </tr>
          </thead>
          <tbody>
            {pagedStudents.items.map((student) => (
              <tr key={student.userId}>
                <td>{student.displayName || t("detail.emptyValue")}</td>
                <td>{student.username || t("detail.emptyValue")}</td>
                <td>{student.studentNo || t("detail.emptyValue")}</td>
                <td>{student.grade ? t(`grades.${student.grade}`) : t("detail.emptyValue")}</td>
                <td>{student.className ? formatStudentClassName(student.className) : t("detail.emptyValue")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <PaginationBar
        currentPage={pagedStudents.page}
        totalPages={pagedStudents.totalPages}
        prevLabel={tPagination("prev")}
        nextLabel={tPagination("next")}
        pageLabel={tPagination("status", { page: pagedStudents.page, total: pagedStudents.totalPages })}
        onPageChange={setStudentPage}
      />
      </>
    ) : (
      <div className={styles.emptyBlock}>{t("empty.students")}</div>
    );
  }

  function renderManagerEditor() {
    return (
      <div className={styles.bindingEditor}>
        <label className={styles.field}>
          <span>{t("fields.managerSearch")}</span>
          <input value={managerSearch} onChange={(event) => setManagerSearch(event.target.value)} className={styles.input} placeholder={t("fields.managerSearchPlaceholder")} />
        </label>
        <div className={styles.optionList}>
          {managerOptionsLoading ? (
            <div className={styles.optionHint}>{t("states.loadingManagers")}</div>
          ) : mergedManagerOptions.length ? (
            mergedManagerOptions.filter((option) => !formState.managerUserIds.includes(option.userId)).map((option) => (
              <button key={option.userId} type="button" className={styles.optionButton} onClick={() => addManager(option)}>
                <strong>{option.displayName || option.username}</strong>
                <span>{renderManagerOptionMeta(option)}</span>
              </button>
            ))
          ) : (
            <div className={styles.optionHint}>{t("empty.managerOptions")}</div>
          )}
        </div>
        <div className={styles.selectedList}>
          {selectedManagers.length ? (
            selectedManagers.map((manager) => (
              <span key={manager.userId} className={styles.selectedChip}>
                <strong>{manager.displayName || manager.username}</strong>
                <small>{renderManagerOptionMeta(manager)}</small>
                <button type="button" onClick={() => removeManager(manager.userId)}>
                  <i className="fas fa-xmark" />
                </button>
              </span>
            ))
          ) : (
            <div className={styles.emptyBlock}>{t("empty.managers")}</div>
          )}
        </div>
      </div>
    );
  }

  async function loadClubs(page: number, keyword: string, status: StatusFilter) {
    setListLoading(true);
    try {
      const query = new URLSearchParams({ page: String(page), pageSize: String(PAGE_SIZE), keyword });
      if (status !== "ALL") {
        query.set("status", status);
      }
      const response = await fetchWithAuthorization(`/api/admin/clubs?${query.toString()}`, { method: "GET" });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ClubPageResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        return;
      }
      setPageData(result.data);
    } catch {
      notify.error(t("messages.loadFailed"));
    } finally {
      setListLoading(false);
    }
  }

  async function loadManagerOptions(keyword: string) {
    setManagerOptionsLoading(true);
    try {
      const query = new URLSearchParams();
      if (keyword.trim()) {
        query.set("keyword", keyword.trim());
      }
      const response = await fetchWithAuthorization(`/api/admin/managers/options?${query.toString()}`, { method: "GET" });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ManagerOptionsResponse | null;
      setManagerOptions(response.ok && result?.success === true && Array.isArray(result.data) ? result.data : []);
    } catch {
      setManagerOptions([]);
    } finally {
      setManagerOptionsLoading(false);
    }
  }

  async function openDetailPanel(clubId: number, mode: PanelMode) {
    setPanelOpen(true);
    setPanelMode(mode);
    setDetailLoading(true);
    try {
      const response = await fetchWithAuthorization(`/api/admin/clubs/${clubId}`, { method: "GET" });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ClubDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadDetailFailed"));
        closePanel();
        return;
      }
      applyDetail(result.data);
    } catch {
      notify.error(t("messages.loadDetailFailed"));
      closePanel();
    } finally {
      setDetailLoading(false);
    }
  }

  function openCreatePanel() {
    setPanelOpen(true);
    setPanelMode("create");
    setDetailLoading(false);
    setCurrentDetail(EMPTY_DETAIL);
    setFormState(EMPTY_FORM);
    setSelectedManagers([]);
    setManagerSearch("");
    void loadManagerOptions("");
  }

  function closePanel() {
    if (saving) {
      return;
    }
    setPanelOpen(false);
    setPanelMode("view");
    setDetailLoading(false);
    setCurrentDetail(EMPTY_DETAIL);
    setFormState(EMPTY_FORM);
    setSelectedManagers([]);
    setManagerOptions([]);
    setManagerSearch("");
  }

  async function handleSave() {
    const payload = buildPayload();
    if (!payload) {
      return;
    }
    setSaving(true);
    try {
      const isCreate = panelMode === "create";
      const response = await fetchWithAuthorization(isCreate ? "/api/admin/clubs" : `/api/admin/clubs/${currentDetail.id}`, {
        method: isCreate ? "POST" : "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ClubDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        if (result?.message?.includes("club name too long")) {
          notify.warning(t("validation.nameTooLong"));
          return;
        }
        notify.error(result?.message || t("messages.saveFailed"));
        return;
      }
      applyDetail(result.data);
      setPanelMode("view");
      notify.success(t(isCreate ? "messages.createSuccess" : "messages.updateSuccess"));
      const nextPage = isCreate ? 1 : pageNumber;
      if (isCreate) {
        setPageNumber(1);
      }
      await loadClubs(nextPage, searchQuery, statusFilter);
    } catch {
      notify.error(t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(clubId: number, clubName: string) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteTitle"),
      message: t("confirm.deleteMessage", { name: clubName }),
      confirmText: t("actions.delete"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }
    setDeletingId(clubId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/clubs/${clubId}`, { method: "DELETE" });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean; code?: string; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.code === "DEPENDENCY_EXISTS" ? t("messages.deleteBlocked") : result?.message || t("messages.deleteFailed"));
        return;
      }
      notify.success(t("messages.deleteSuccess"));
      if (panelOpen && currentDetail.id === clubId) {
        closePanel();
      }
      const nextPage = pageData.items.length === 1 && pageNumber > 1 ? pageNumber - 1 : pageNumber;
      if (nextPage !== pageNumber) {
        setPageNumber(nextPage);
      }
      await loadClubs(nextPage, searchQuery, statusFilter);
    } catch {
      notify.error(t("messages.deleteFailed"));
    } finally {
      setDeletingId(null);
    }
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPageNumber(1);
    setSearchQuery(searchInput.trim());
  }

  function addManager(option: AdminManagerOption) {
    if (formState.managerUserIds.includes(option.userId)) {
      return;
    }
    setSelectedManagers((prev) => [...prev, option]);
    setFormState((prev) => ({ ...prev, managerUserIds: [...prev.managerUserIds, option.userId] }));
  }

  function removeManager(userId: number) {
    setSelectedManagers((prev) => prev.filter((item) => item.userId !== userId));
    setFormState((prev) => ({ ...prev, managerUserIds: prev.managerUserIds.filter((item) => item !== userId) }));
  }

  function applyDetail(detail: AdminClubDetail) {
    setCurrentDetail(detail);
    setManagerPage(1);
    setStudentPage(1);
    setFormState({ name: detail.name, type: detail.type, status: detail.status, description: detail.description, managerUserIds: detail.managers.map((item) => item.userId) });
    setSelectedManagers(detail.managers.map((item) => ({ userId: item.userId, username: item.username, displayName: item.displayName, managerNo: item.managerNo })));
    setManagerSearch("");
  }

  function buildPayload(): AdminClubMutationPayload | null {
    const name = formState.name.trim();
    const type = formState.type.trim();
    const description = formState.description.trim();
    if (!name) {
      notify.warning(t("validation.nameRequired"));
      return null;
    }
    if (name.length > 12) {
      notify.warning(t("validation.nameTooLong"));
      return null;
    }
    if (!type) {
      notify.warning(t("validation.typeRequired"));
      return null;
    }
    return {
      name,
      type,
      status: formState.status,
      description,
      managerUserIds: formState.managerUserIds
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
      router.replace("/login");
      return false;
    }
    return true;
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

  async function loadCreationRequests() {
    setRequestsLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/club-creation-requests", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as CreationRequestResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(t("requests.loadFailed"));
        return;
      }
      setRequestPage(1);
      setCreationRequests(result.data);
    } finally {
      setRequestsLoading(false);
    }
  }

  async function approveCreationRequest(requestId: number) {
    await handleCreationRequestAction(requestId, "approve");
  }

  async function rejectCreationRequest(requestId: number) {
    await handleCreationRequestAction(requestId, "reject");
  }

  async function handleCreationRequestAction(requestId: number, action: "approve" | "reject") {
    try {
      const response = await fetchWithAuthorization(`/api/admin/club-creation-requests/${requestId}/${action}`, {
        method: "POST"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as { success?: boolean } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(action === "approve" ? t("requests.approveFailed") : t("requests.rejectFailed"));
        return;
      }
      notify.success(action === "approve" ? t("requests.approveSuccess") : t("requests.rejectSuccess"));
      void loadCreationRequests();
      if (action === "approve") {
        void loadClubs(pageNumber, searchQuery, statusFilter);
      }
    } catch {
      notify.error(action === "approve" ? t("requests.approveFailed") : t("requests.rejectFailed"));
    }
  }
}

function buildVisiblePages(totalPages: number, currentPage: number) {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }
  const start = Math.max(1, Math.min(currentPage - 2, totalPages - 4));
  return Array.from({ length: 5 }, (_, index) => start + index);
}

function formatDateTime(value: string | null, fallback: string) {
  if (!value) return fallback;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;
  return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date);
}

function renderManagerOptionMeta(option: AdminManagerOption) {
  return [option.managerNo, option.username].map((item) => String(item || "").trim()).filter(Boolean).join(" / ");
}
