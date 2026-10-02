"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import {
  RecommendedClubItem,
  RecommendedClubOption,
  UpdateRecommendedClubsPayload
} from "../../lib/admin-club-recommend/types";
import { useT } from "../../lib/i18n/useT";
import { paginateItems } from "../../lib/pagination";
import { TeacherRecommendationGroup } from "../../lib/manager/teacherWorkflowTypes";
import { useNotify } from "../../lib/notify/useNotify";
import styles from "../../styles/sysAdminClubRecommend.module.css";

type RecommendationResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: RecommendedClubItem[];
};

type ClubOptionResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: RecommendedClubOption[];
};

type TeacherRecommendationResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: TeacherRecommendationGroup[];
};

const MAX_RECOMMENDATIONS = 4;

export function SysAdminClubRecommend() {
  const t = useT("adminClubRecommend");
  const tPortal = useT("portal");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [pageLoading, setPageLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [candidatePage, setCandidatePage] = useState(1);
  const [teacherPage, setTeacherPage] = useState(1);
  const [selectedClubs, setSelectedClubs] = useState<RecommendedClubOption[]>([]);
  const [candidateClubs, setCandidateClubs] = useState<RecommendedClubOption[]>([]);
  const [viewMode, setViewMode] = useState<"homepage" | "teacher">("homepage");
  const [teacherLoading, setTeacherLoading] = useState(false);
  const [teacherGroups, setTeacherGroups] = useState<TeacherRecommendationGroup[]>([]);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadInitialData();
  }, [authorized]);

  useEffect(() => {
    if (!authorized) {
      return;
    }
    const timer = window.setTimeout(() => {
      void loadCandidateClubs(searchKeyword);
    }, 180);
    return () => window.clearTimeout(timer);
  }, [authorized, searchKeyword]);

  const visibleCandidates = useMemo(() => {
    const selectedIds = new Set(selectedClubs.map((item) => item.id));
    return candidateClubs.filter((item) => !selectedIds.has(item.id));
  }, [candidateClubs, selectedClubs]);
  const pagedCandidates = useMemo(() => paginateItems(visibleCandidates, candidatePage, 10), [candidatePage, visibleCandidates]);
  const pagedTeacherGroups = useMemo(() => paginateItems(teacherGroups, teacherPage, 10), [teacherGroups, teacherPage]);

  if (checking || pageLoading) {
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
              <i className="fas fa-star" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
            <div className={styles.headerActions}>
              <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
                <i className="fas fa-arrow-left" />
                {t("actions.back")}
              </button>
            <button type="button" className={styles.primaryButton} onClick={() => void saveRecommendations()} disabled={saving}>
              <i className="fas fa-floppy-disk" />
              {saving ? t("actions.saving") : t("actions.save")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.viewTabs}>
          <button
            type="button"
            className={`${styles.tabButton} ${viewMode === "homepage" ? styles.tabButtonActive : ""}`}
            onClick={() => setViewMode("homepage")}
          >
            {t("tabs.homepage")}
          </button>
          <button
            type="button"
            className={`${styles.tabButton} ${viewMode === "teacher" ? styles.tabButtonActive : ""}`}
            onClick={() => {
              setViewMode("teacher");
              if (!teacherGroups.length) {
                void loadTeacherSubmissions();
              }
            }}
          >
            {t("tabs.teacher")}
          </button>
        </div>
        {viewMode === "homepage" ? (
        <div className={styles.layout}>
          <section className={styles.card}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("current.title")}</h2>
                <p>{t("current.subtitle")}</p>
              </div>
              <span className={styles.countBadge}>
                {t("current.count", { count: selectedClubs.length, total: MAX_RECOMMENDATIONS })}
              </span>
            </div>

            <div className={styles.slotGrid}>
              {Array.from({ length: MAX_RECOMMENDATIONS }, (_, index) => {
                const item = selectedClubs[index];
                const slotNo = index + 1;

                return (
                  <article key={slotNo} className={`${styles.slotCard} ${item ? styles.slotFilled : styles.slotEmpty}`}>
                    <div className={styles.slotHead}>
                      <span className={styles.slotBadge}>{t("current.slot", { slotNo })}</span>
                      {item ? (
                        <div className={styles.slotActions}>
                          <button type="button" className={styles.iconButton} onClick={() => moveClub(index, -1)} disabled={index === 0 || saving}>
                            <i className="fas fa-arrow-up" />
                          </button>
                          <button
                            type="button"
                            className={styles.iconButton}
                            onClick={() => moveClub(index, 1)}
                            disabled={index === selectedClubs.length - 1 || saving}
                          >
                            <i className="fas fa-arrow-down" />
                          </button>
                          <button type="button" className={styles.iconButton} onClick={() => removeClub(item.id)} disabled={saving}>
                            <i className="fas fa-xmark" />
                          </button>
                        </div>
                      ) : null}
                    </div>

                    {item ? (
                      <div className={styles.slotBody}>
                        <h3>{item.name}</h3>
                        <div className={styles.metaRow}>
                          <span>{item.type || t("common.emptyValue")}</span>
                          <span>{t("current.memberCount", { count: item.memberCount })}</span>
                        </div>
                        <p>{item.description || t("current.emptyDescription")}</p>
                      </div>
                    ) : (
                      <div className={styles.emptySlot}>
                        <i className="fas fa-plus-circle" />
                        <p>{t("current.emptySlot")}</p>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          </section>

          <section className={styles.card}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("candidate.title")}</h2>
                <p>{t("candidate.subtitle")}</p>
              </div>
            </div>

            <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
              <i className={`fas fa-search ${styles.searchIcon}`} />
              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                className={styles.searchInput}
                placeholder={t("candidate.searchPlaceholder")}
              />
              <button type="submit" className={styles.secondaryButton} disabled={optionsLoading}>
                {t("actions.search")}
              </button>
            </form>

            <div className={styles.candidateList}>
              {optionsLoading ? (
                <div className={styles.emptyState}>{t("states.loadingCandidates")}</div>
              ) : visibleCandidates.length ? (
                pagedCandidates.items.map((club) => (
                  <article key={club.id} className={styles.candidateCard}>
                    <div className={styles.candidateInfo}>
                      <h3>{club.name}</h3>
                      <div className={styles.metaRow}>
                        <span>{club.type || t("common.emptyValue")}</span>
                        <span>{t("current.memberCount", { count: club.memberCount })}</span>
                      </div>
                      <p>{club.description || t("current.emptyDescription")}</p>
                    </div>
                    <button
                      type="button"
                      className={styles.primaryButton}
                      onClick={() => addClub(club)}
                      disabled={selectedClubs.length >= MAX_RECOMMENDATIONS || saving}
                    >
                      <i className="fas fa-plus" />
                      {t("actions.add")}
                    </button>
                  </article>
                ))
              ) : (
                <div className={styles.emptyState}>{t("candidate.empty")}</div>
              )}
            </div>
            <PaginationBar
              currentPage={pagedCandidates.page}
              totalPages={pagedCandidates.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedCandidates.page, total: pagedCandidates.totalPages })}
              onPageChange={setCandidatePage}
            />
          </section>
        </div>
        ) : (
          <section className={styles.card}>
            <div className={styles.cardHead}>
              <div>
                <h2>{t("teacher.title")}</h2>
                <p>{t("teacher.subtitle")}</p>
              </div>
            </div>
            <div className={styles.candidateList}>
              {teacherLoading ? (
                <div className={styles.emptyState}>{t("teacher.loading")}</div>
              ) : teacherGroups.length ? (
                pagedTeacherGroups.items.map((group) => (
                  <article key={group.managerUserId} className={styles.candidateCard}>
                    <div className={styles.candidateInfo}>
                      <h3>{group.managerName}</h3>
                      <div className={styles.metaRow}>
                        <span>{group.managerNo || t("common.emptyValue")}</span>
                        <span>{t("teacher.count", { count: group.recommendations.length })}</span>
                      </div>
                      <p>
                        {group.recommendations.length
                          ? group.recommendations.map((item) => `${item.orderNo}. ${item.clubName}`).join(" / ")
                          : t("teacher.emptyGroup")}
                      </p>
                    </div>
                  </article>
                ))
              ) : (
                <div className={styles.emptyState}>{t("teacher.empty")}</div>
              )}
            </div>
            <PaginationBar
              currentPage={pagedTeacherGroups.page}
              totalPages={pagedTeacherGroups.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedTeacherGroups.page, total: pagedTeacherGroups.totalPages })}
              onPageChange={setTeacherPage}
            />
          </section>
        )}
      </section>
    </main>
  );

  async function loadInitialData() {
    setPageLoading(true);
    try {
      await Promise.all([loadRecommendations(), loadCandidateClubs("")]);
    } finally {
      setPageLoading(false);
    }
  }

  async function loadRecommendations() {
    try {
      const response = await fetchWithAuthorization("/api/admin/club-recommendations", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as RecommendationResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadFailed"));
        setSelectedClubs([]);
        return;
      }
      setSelectedClubs(
        result.data.map((item) => ({
          id: item.clubId,
          name: item.name,
          type: item.type,
          description: item.description,
          memberCount: item.memberCount
        }))
      );
    } catch {
      notify.error(t("messages.loadFailed"));
      setSelectedClubs([]);
    }
  }

  async function loadCandidateClubs(keyword: string) {
    setOptionsLoading(true);
    try {
      const query = new URLSearchParams();
      if (keyword.trim()) {
        query.set("keyword", keyword.trim());
      }
      const response = await fetchWithAuthorization(`/api/admin/clubs/options?${query.toString()}`, {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ClubOptionResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        setCandidateClubs([]);
        return;
      }
      setCandidateClubs(result.data);
    } catch {
      setCandidateClubs([]);
    } finally {
      setOptionsLoading(false);
    }
  }

  async function saveRecommendations() {
    const payload: UpdateRecommendedClubsPayload = {
      clubIds: selectedClubs.map((item) => item.id)
    };

    setSaving(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/club-recommendations", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as RecommendationResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.saveFailed"));
        return;
      }
      setSelectedClubs(
        result.data.map((item) => ({
          id: item.clubId,
          name: item.name,
          type: item.type,
          description: item.description,
          memberCount: item.memberCount
        }))
      );
      notify.success(t("messages.saveSuccess"));
    } catch {
      notify.error(t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCandidatePage(1);
    setSearchKeyword(searchInput.trim());
  }

  function addClub(club: RecommendedClubOption) {
    if (selectedClubs.length >= MAX_RECOMMENDATIONS || selectedClubs.some((item) => item.id === club.id)) {
      return;
    }
    setSelectedClubs((prev) => [...prev, club]);
  }

  function removeClub(clubId: number) {
    setSelectedClubs((prev) => prev.filter((item) => item.id !== clubId));
  }

  function moveClub(index: number, offset: -1 | 1) {
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= selectedClubs.length) {
      return;
    }
    setSelectedClubs((prev) => {
      const next = [...prev];
      const [current] = next.splice(index, 1);
      next.splice(targetIndex, 0, current);
      return next;
    });
  }

  async function loadTeacherSubmissions() {
    setTeacherLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/club-recommendations/teacher-submissions", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as TeacherRecommendationResponse | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        setTeacherGroups([]);
        return;
      }
      setTeacherPage(1);
      setTeacherGroups(result.data);
    } finally {
      setTeacherLoading(false);
    }
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
