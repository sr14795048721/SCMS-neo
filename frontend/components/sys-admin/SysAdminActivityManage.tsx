"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import {
  AdminActivityClubOption,
  AdminActivityDetail,
  AdminActivityMutationPayload,
  AdminActivityPage,
  AdminActivityStatus
} from "../../lib/admin-activity/types";
import { useT } from "../../lib/i18n/useT";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import { formatStudentClassName } from "../../lib/student/fieldConstraints";
import styles from "../../styles/sysAdminClubManage.module.css";

const PAGE_SIZE = 10;
const EMPTY_PAGE: AdminActivityPage = { items: [], page: 1, pageSize: PAGE_SIZE, total: 0, totalPages: 0 };
const EMPTY_DETAIL: AdminActivityDetail = {
  id: 0,
  clubId: 0,
  clubName: "",
  title: "",
  description: "",
  location: "",
  startTime: null,
  endTime: null,
  capacity: 1,
  status: "DRAFT",
  registrationCount: 0,
  createdAt: null,
  updatedAt: null,
  registrations: []
};
const EMPTY_FORM = {
  clubId: "",
  title: "",
  description: "",
  location: "",
  startTime: "",
  endTime: "",
  capacity: "1"
};

type ActivityForm = typeof EMPTY_FORM;
type ActivityDetailResponse = { success?: boolean; code?: string; message?: string; data?: AdminActivityDetail };
type ActivityPageResponse = { success?: boolean; code?: string; message?: string; data?: AdminActivityPage };
type ClubOptionsResponse = { success?: boolean; code?: string; message?: string; data?: AdminActivityClubOption[] };
type PanelMode = "create" | "view" | "edit";
type StatusFilter = "ALL" | AdminActivityStatus;
type PendingAction = "publish" | "close" | null;

export function SysAdminActivityManage() {
  const t = useT("adminActivityManage");
  const tPortal = useT("portal");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [pageData, setPageData] = useState<AdminActivityPage>(EMPTY_PAGE);
  const [listLoading, setListLoading] = useState(true);
  const [pageNumber, setPageNumber] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [clubFilter, setClubFilter] = useState("ALL");
  const [clubOptions, setClubOptions] = useState<AdminActivityClubOption[]>([]);
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelMode, setPanelMode] = useState<PanelMode>("view");
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [actionId, setActionId] = useState<number | null>(null);
  const [actionType, setActionType] = useState<PendingAction>(null);
  const [currentDetail, setCurrentDetail] = useState<AdminActivityDetail>(EMPTY_DETAIL);
  const [formState, setFormState] = useState<ActivityForm>(EMPTY_FORM);

  useEffect(() => {
    if (!authorized) return;
    void loadClubOptions();
  }, [authorized]);

  useEffect(() => {
    if (!authorized) return;
    void loadActivities(pageNumber, searchQuery, statusFilter, clubFilter);
  }, [authorized, pageNumber, searchQuery, statusFilter, clubFilter]);

  const visiblePages = useMemo(() => buildVisiblePages(pageData.totalPages, pageData.page), [pageData.totalPages, pageData.page]);
  const editable = panelMode !== "view";
  const clubChoices = useMemo(() => {
    const map = new Map<number, AdminActivityClubOption>();
    clubOptions.forEach((item) => map.set(item.id, item));
    if (currentDetail.clubId && currentDetail.clubName && !map.has(currentDetail.clubId)) {
      map.set(currentDetail.clubId, { id: currentDetail.clubId, name: currentDetail.clubName, type: "", description: "", memberCount: 0 });
    }
    return Array.from(map.values());
  }, [clubOptions, currentDetail.clubId, currentDetail.clubName]);

  if (checking) {
    return <main className={styles.page}><section className={styles.loadingCard}><p>{tPortal("auth.checking")}</p></section></main>;
  }
  if (!authorized || redirecting) {
    return null;
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerTitle}>
            <h1><i className="fas fa-calendar-alt" />{t("header.title")}</h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />{t("actions.back")}
            </button>
            <button type="button" className={styles.primaryButton} onClick={openCreatePanel}>
              <i className="fas fa-plus" />{t("actions.create")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.card}>
          <div className={styles.toolbar}>
            <form className={`${styles.searchForm} ${styles.activitySearchForm}`} onSubmit={(event) => handleSearchSubmit(event)}>
              <div className={styles.searchField}>
                <i className={`fas fa-search ${styles.searchIcon}`} />
                <input
                  className={styles.searchInput}
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder={t("filters.searchPlaceholder")}
                />
              </div>
              <select className={styles.filterSelect} value={clubFilter} onChange={(event) => { setPageNumber(1); setClubFilter(event.target.value); }}>
                <option value="ALL">{t("filters.allClubs")}</option>
                {clubOptions.map((club) => <option key={club.id} value={String(club.id)}>{club.name}</option>)}
              </select>
              <select className={styles.filterSelect} value={statusFilter} onChange={(event) => { setPageNumber(1); setStatusFilter(event.target.value as StatusFilter); }}>
                <option value="ALL">{t("filters.allStatuses")}</option>
                <option value="DRAFT">{t("status.draft")}</option>
                <option value="PUBLISHED">{t("status.published")}</option>
                <option value="CLOSED">{t("status.closed")}</option>
              </select>
              <button type="submit" className={`${styles.primaryButton} ${styles.searchSubmitButton}`}>
                <i className="fas fa-magnifying-glass" />
                {t("actions.search")}
              </button>
            </form>
          </div>

          {listLoading ? <div className={styles.loadingInline}>{t("states.loadingList")}</div> : !pageData.items.length ? (
            <div className={styles.emptyState}><i className="fas fa-calendar-xmark" /><h3>{t("empty.title")}</h3><p>{t("empty.description")}</p></div>
          ) : (
            <div className={styles.list}>
              {pageData.items.map((item) => (
                <article key={item.id} className={styles.itemCard}>
                  <div className={styles.itemHead}>
                    <div className={styles.itemTitleBlock}>
                      <div className={styles.itemTitleRow}>
                        <h2>{item.title}</h2>
                        <span className={`${styles.statusBadge} ${resolveStatusClass(styles, item.status)}`}>{t(resolveStatusKey(item.status))}</span>
                      </div>
                      <div className={styles.metaRow}>
                        <span>{item.clubName || t("detail.emptyValue")}</span>
                        <span>{formatDateTime(item.startTime, t("detail.emptyValue"))}</span>
                        <span>{item.location || t("detail.emptyValue")}</span>
                        <span>{t("list.capacity", { count: item.capacity })}</span>
                        <span>{t("list.registrationCount", { count: item.registrationCount })}</span>
                      </div>
                    </div>
                    <div className={styles.itemActions}>
                      <button type="button" className={styles.rowAction} onClick={() => void openDetailPanel(item.id, "view")}><i className="fas fa-eye" />{t("actions.view")}</button>
                      <button type="button" className={styles.rowAction} onClick={() => void openDetailPanel(item.id, "edit")}><i className="fas fa-pen" />{t("actions.edit")}</button>
                      {item.status === "DRAFT" ? <button type="button" className={styles.rowAction} onClick={() => void handleStatusAction(item.id, "publish")} disabled={actionId === item.id}><i className="fas fa-paper-plane" />{actionId === item.id && actionType === "publish" ? t("actions.publishing") : t("actions.publish")}</button> : null}
                      {item.status === "PUBLISHED" ? <button type="button" className={styles.rowAction} onClick={() => void handleStatusAction(item.id, "close")} disabled={actionId === item.id}><i className="fas fa-ban" />{actionId === item.id && actionType === "close" ? t("actions.closing") : t("actions.close")}</button> : null}
                      <button type="button" className={`${styles.rowAction} ${styles.rowDangerAction}`} onClick={() => void handleDelete(item.id, item.title)} disabled={deletingId === item.id}><i className="fas fa-trash-can" />{deletingId === item.id ? t("actions.deleting") : t("actions.delete")}</button>
                    </div>
                  </div>
                  <p className={styles.itemDescription}>{item.description || t("list.noDescription")}</p>
                </article>
              ))}
            </div>
          )}

          {pageData.totalPages > 1 ? (
            <div className={styles.pagination}>
              <button type="button" className={styles.pagerButton} disabled={pageData.page <= 1} onClick={() => setPageNumber((prev) => Math.max(prev - 1, 1))}><i className="fas fa-angle-left" />{t("actions.prevPage")}</button>
              <div className={styles.pageList}>
                {visiblePages.map((page) => <button key={page} type="button" className={`${styles.pageChip} ${page === pageData.page ? styles.pageChipActive : ""}`} onClick={() => setPageNumber(page)}>{page}</button>)}
              </div>
              <button type="button" className={styles.pagerButton} disabled={pageData.page >= pageData.totalPages} onClick={() => setPageNumber((prev) => Math.min(prev + 1, pageData.totalPages))}>{t("actions.nextPage")}<i className="fas fa-angle-right" /></button>
            </div>
          ) : null}
        </div>
      </section>

      {panelOpen ? <div className={styles.panelLayer} onClick={closePanel}>
        <aside className={styles.panel} onClick={(event) => event.stopPropagation()}>
          <div className={styles.panelHead}>
            <div className={styles.panelHeadCopy}>
              <h2>{t(panelMode === "create" ? "panel.createTitle" : panelMode === "edit" ? "panel.editTitle" : "panel.viewTitle")}</h2>
              <p>{t(panelMode === "create" ? "panel.createSubtitle" : panelMode === "edit" ? "panel.editSubtitle" : "panel.viewSubtitle")}</p>
            </div>
            <div className={styles.panelHeadActions}>
              {panelMode === "view" && currentDetail.id ? <button type="button" className={styles.secondaryButton} onClick={() => setPanelMode("edit")}><i className="fas fa-pen" />{t("actions.edit")}</button> : null}
              {editable ? <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={saving}><i className="fas fa-floppy-disk" />{saving ? t("actions.saving") : t("actions.save")}</button> : null}
              <button type="button" className={styles.closeButton} onClick={closePanel}><i className="fas fa-xmark" /></button>
            </div>
          </div>

          {detailLoading ? <div className={styles.panelLoading}>{t("states.loadingDetail")}</div> : <div className={styles.panelBody}>
            <section className={styles.sectionCard}>
              <div className={styles.sectionHead}>
                <div>
                  <h3>{t("detail.basicTitle")}</h3>
                  <p className={styles.sectionDescription}>{t(editable ? "panel.basicEditHint" : "panel.basicViewHint")}</p>
                </div>
              </div>

              {editable ? <div className={styles.formGrid}>
                <label className={styles.field}><span>{t("fields.club")}</span><select className={styles.select} value={formState.clubId} onChange={(event) => setFormState((prev) => ({ ...prev, clubId: event.target.value }))}><option value="">{t("fields.clubPlaceholder")}</option>{clubChoices.map((club) => <option key={club.id} value={String(club.id)}>{club.name}</option>)}</select></label>
                <label className={styles.field}><span>{t("fields.title")}</span><input className={styles.input} value={formState.title} onChange={(event) => setFormState((prev) => ({ ...prev, title: event.target.value.slice(0, 180) }))} /></label>
                <label className={styles.field}><span>{t("fields.startTime")}</span><input type="datetime-local" className={styles.input} value={formState.startTime} onChange={(event) => setFormState((prev) => ({ ...prev, startTime: event.target.value }))} /></label>
                <label className={styles.field}><span>{t("fields.endTime")}</span><input type="datetime-local" className={styles.input} value={formState.endTime} onChange={(event) => setFormState((prev) => ({ ...prev, endTime: event.target.value }))} /></label>
                <label className={styles.field}><span>{t("fields.location")}</span><input className={styles.input} value={formState.location} onChange={(event) => setFormState((prev) => ({ ...prev, location: event.target.value.slice(0, 160) }))} /></label>
                <label className={styles.field}><span>{t("fields.capacity")}</span><input type="number" min={1} className={styles.input} value={formState.capacity} onChange={(event) => setFormState((prev) => ({ ...prev, capacity: event.target.value }))} /></label>
                <label className={`${styles.field} ${styles.fieldWide}`}><span>{t("fields.description")}</span><textarea className={styles.textarea} value={formState.description} onChange={(event) => setFormState((prev) => ({ ...prev, description: event.target.value.slice(0, 1000) }))} /></label>
              </div> : <div className={styles.viewGrid}>
                <div className={styles.infoTile}><span>{t("fields.club")}</span><strong>{currentDetail.clubName || t("detail.emptyValue")}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.title")}</span><strong>{currentDetail.title || t("detail.emptyValue")}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.status")}</span><strong>{t(resolveStatusKey(currentDetail.status))}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.capacity")}</span><strong>{currentDetail.capacity}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.registrationCount")}</span><strong>{currentDetail.registrationCount}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.updatedAt")}</span><strong>{formatDateTime(currentDetail.updatedAt, t("detail.emptyValue"))}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.startTime")}</span><strong>{formatDateTime(currentDetail.startTime, t("detail.emptyValue"))}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.endTime")}</span><strong>{formatDateTime(currentDetail.endTime, t("detail.emptyValue"))}</strong></div>
                <div className={styles.infoTile}><span>{t("fields.location")}</span><strong>{currentDetail.location || t("detail.emptyValue")}</strong></div>
                <div className={`${styles.infoTile} ${styles.infoTileWide}`}><span>{t("fields.description")}</span><strong>{currentDetail.description || t("detail.emptyDescription")}</strong></div>
              </div>}
            </section>

            {panelMode !== "create" ? <section className={styles.sectionCard}>
              <div className={styles.sectionHead}>
                <div>
                  <h3>{t("detail.registrationsTitle")}</h3>
                  <p className={styles.sectionDescription}>{t("panel.registrationHint")}</p>
                </div>
              </div>
              {currentDetail.registrations.length ? <div className={styles.tableWrapper}><table className={styles.dataTable}><thead><tr><th>{t("table.registration.displayName")}</th><th>{t("table.registration.username")}</th><th>{t("table.registration.studentNo")}</th><th>{t("table.registration.grade")}</th><th>{t("table.registration.className")}</th><th>{t("table.registration.registeredAt")}</th></tr></thead><tbody>{currentDetail.registrations.map((registration) => <tr key={registration.registrationId}><td>{registration.displayName || t("detail.emptyValue")}</td><td>{registration.username || t("detail.emptyValue")}</td><td>{registration.studentNo || t("detail.emptyValue")}</td><td>{registration.grade ? t(`grades.${registration.grade}`) : t("detail.emptyValue")}</td><td>{registration.className ? formatStudentClassName(registration.className) : t("detail.emptyValue")}</td><td>{formatDateTime(registration.registeredAt, t("detail.emptyValue"))}</td></tr>)}</tbody></table></div> : <div className={styles.emptyBlock}>{t("empty.registrations")}</div>}
            </section> : null}
          </div>}
        </aside>
      </div> : null}
    </main>
  );

  function openCreatePanel() {
    setPanelOpen(true);
    setPanelMode("create");
    setCurrentDetail(EMPTY_DETAIL);
    setFormState(EMPTY_FORM);
    setDetailLoading(false);
  }

  function closePanel() {
    if (saving) return;
    setPanelOpen(false);
    setPanelMode("view");
    setCurrentDetail(EMPTY_DETAIL);
    setFormState(EMPTY_FORM);
  }

  function applyDetail(detail: AdminActivityDetail) {
    setCurrentDetail(detail);
    setFormState({
      clubId: detail.clubId ? String(detail.clubId) : "",
      title: detail.title,
      description: detail.description,
      location: detail.location,
      startTime: toDateTimeLocalValue(detail.startTime),
      endTime: toDateTimeLocalValue(detail.endTime),
      capacity: detail.capacity > 0 ? String(detail.capacity) : "1"
    });
  }

  async function loadActivities(page: number, keyword: string, status: StatusFilter, clubId: string) {
    setListLoading(true);
    try {
      const query = new URLSearchParams({ page: String(page), pageSize: String(PAGE_SIZE), keyword });
      if (status !== "ALL") query.set("status", status);
      if (clubId !== "ALL") query.set("clubId", clubId);
      const response = await fetchWithAuthorization(`/api/admin/activities?${query.toString()}`, { method: "GET" });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as ActivityPageResponse | null;
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

  async function loadClubOptions() {
    try {
      const response = await fetchWithAuthorization("/api/admin/clubs/options", { method: "GET" });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as ClubOptionsResponse | null;
      setClubOptions(response.ok && result?.success === true && Array.isArray(result.data) ? result.data : []);
    } catch {
      setClubOptions([]);
    }
  }

  async function openDetailPanel(activityId: number, mode: PanelMode) {
    setPanelOpen(true);
    setPanelMode(mode);
    setDetailLoading(true);
    try {
      const response = await fetchWithAuthorization(`/api/admin/activities/${activityId}`, { method: "GET" });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as ActivityDetailResponse | null;
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

  async function handleSave() {
    const payload = buildPayload(formState, currentDetail.registrationCount, currentDetail.status, notify, t);
    if (!payload) return;
    setSaving(true);
    try {
      const isCreate = panelMode === "create";
      const response = await fetchWithAuthorization(isCreate ? "/api/admin/activities" : `/api/admin/activities/${currentDetail.id}`, {
        method: isCreate ? "POST" : "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as ActivityDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveFailed"));
        return;
      }
      applyDetail(result.data);
      setPanelMode("view");
      notify.success(t(isCreate ? "messages.createSuccess" : "messages.updateSuccess"));
      const nextPage = isCreate ? 1 : pageNumber;
      if (isCreate) setPageNumber(1);
      await loadActivities(nextPage, searchQuery, statusFilter, clubFilter);
    } catch {
      notify.error(t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusAction(activityId: number, action: Exclude<PendingAction, null>) {
    setActionId(activityId);
    setActionType(action);
    try {
      const response = await fetchWithAuthorization(`/api/admin/activities/${activityId}/${action}`, { method: "POST" });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as ActivityDetailResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t(`messages.${action}Failed`));
        return;
      }
      if (panelOpen && currentDetail.id === activityId) applyDetail(result.data);
      notify.success(t(`messages.${action}Success`));
      await loadActivities(pageNumber, searchQuery, statusFilter, clubFilter);
    } catch {
      notify.error(t(`messages.${action}Failed`));
    } finally {
      setActionId(null);
      setActionType(null);
    }
  }

  async function handleDelete(activityId: number, title: string) {
    const accepted = await confirm.confirm({ title: t("confirm.deleteTitle"), message: t("confirm.deleteMessage", { name: title }), confirmText: t("actions.delete"), cancelText: t("actions.cancel"), tone: "danger" });
    if (!accepted) return;
    setDeletingId(activityId);
    try {
      const response = await fetchWithAuthorization(`/api/admin/activities/${activityId}`, { method: "DELETE" });
      if (!response) return;
      if (!(await handleAuthStatus(response.status))) return;
      const result = (await response.json().catch(() => null)) as { success?: boolean; code?: string; message?: string } | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.code === "DEPENDENCY_EXISTS" ? t("messages.deleteBlocked") : result?.message || t("messages.deleteFailed"));
        return;
      }
      notify.success(t("messages.deleteSuccess"));
      if (panelOpen && currentDetail.id === activityId) closePanel();
      const nextPage = pageData.items.length === 1 && pageNumber > 1 ? pageNumber - 1 : pageNumber;
      if (nextPage !== pageNumber) setPageNumber(nextPage);
      await loadActivities(nextPage, searchQuery, statusFilter, clubFilter);
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
}

function buildVisiblePages(totalPages: number, currentPage: number) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1);
  const start = Math.max(1, Math.min(currentPage - 2, totalPages - 4));
  return Array.from({ length: 5 }, (_, index) => start + index);
}

function resolveStatusKey(status: AdminActivityStatus) {
  return status === "PUBLISHED" ? "status.published" : status === "CLOSED" ? "status.closed" : "status.draft";
}

function resolveStatusClass(css: Record<string, string>, status: AdminActivityStatus) {
  return status === "PUBLISHED" ? css.statusActive : status === "CLOSED" ? css.statusInactive : css.statusPending;
}

function formatDateTime(value: string | null, fallback: string) {
  if (!value) return fallback;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;
  return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date);
}

function toDateTimeLocalValue(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function buildPayload(
  formState: ActivityForm,
  registrationCount: number,
  status: AdminActivityStatus,
  notify: { warning: (message: string) => void },
  t: (key: string) => string
): AdminActivityMutationPayload | null {
  const clubId = Number(formState.clubId);
  const title = formState.title.trim();
  const description = formState.description.trim();
  const location = formState.location.trim();
  const capacity = Number(formState.capacity);
  const startTime = toIsoString(formState.startTime);
  const endTime = toIsoString(formState.endTime);
  if (!clubId) return notify.warning(t("validation.clubRequired")), null;
  if (!title) return notify.warning(t("validation.titleRequired")), null;
  if (!startTime) return notify.warning(t("validation.startTimeRequired")), null;
  if (!endTime) return notify.warning(t("validation.endTimeRequired")), null;
  if (!Number.isFinite(capacity) || capacity < 1) return notify.warning(t("validation.capacityInvalid")), null;
  if (new Date(endTime).getTime() <= new Date(startTime).getTime()) return notify.warning(t("validation.timeRangeInvalid")), null;
  if (status === "PUBLISHED" && capacity < registrationCount) return notify.warning(t("validation.capacityBelowRegistrations")), null;
  return { clubId, title, description, location, startTime, endTime, capacity };
}

function toIsoString(value: string) {
  const normalized = String(value || "").trim();
  if (!normalized) return "";
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? "" : date.toISOString();
}
