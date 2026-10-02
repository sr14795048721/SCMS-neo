"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import { TeacherRecommendationItem } from "../../lib/manager/teacherWorkflowTypes";
import { fetchManagerRecommendationsRequest, updateManagerRecommendationsRequest } from "../../lib/manager/teacherWorkflowClient";
import { paginateItems } from "../../lib/pagination";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherWorkflows.module.css";

type ClubOption = {
  id: number;
  name: string;
  type: string;
  description: string;
  memberCount: number;
};

type ClubsResponse = {
  success?: boolean;
  data?: ClubOption[];
};

type RecommendationResponse = {
  success?: boolean;
  data?: TeacherRecommendationItem[];
};

const MAX_RECOMMENDATIONS = 4;

export function TeacherClubRecommendationsPage() {
  const t = useT("teacherRecommendations");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("common.defaultName"));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [candidatePage, setCandidatePage] = useState(1);
  const [selected, setSelected] = useState<ClubOption[]>([]);
  const [clubs, setClubs] = useState<ClubOption[]>([]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [portal.authorized]);

  const visibleCandidates = useMemo(() => {
    const selectedIds = new Set(selected.map((item) => item.id));
    const keyword = searchKeyword.trim().toLowerCase();
    return clubs.filter((item) => {
      if (selectedIds.has(item.id)) {
        return false;
      }
      if (!keyword) {
        return true;
      }
      return [item.name, item.type, item.description].join(" ").toLowerCase().includes(keyword);
    });
  }, [clubs, searchKeyword, selected]);
  const pagedCandidates = useMemo(() => paginateItems(visibleCandidates, candidatePage, 6), [candidatePage, visibleCandidates]);

  if (portal.checking || loading) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{portal.checking ? t("states.checking") : t("states.loading")}</p>
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
          <button type="button" className={styles.secondaryButton} onClick={() => router.push("/club-admin")}>
            <i className="fas fa-arrow-left" /> {t("actions.back")}
          </button>
          <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={saving}>
            <i className="fas fa-floppy-disk" /> {saving ? t("actions.saving") : t("actions.save")}
          </button>
        </div>

        <section className={styles.summaryGrid}>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("summary.selected.label")}</p>
            <p className={styles.summaryValue}>{selected.length}</p>
            <p className={styles.summaryHint}>{t("summary.selected.hint", { total: MAX_RECOMMENDATIONS })}</p>
          </article>
          <article className={styles.summaryCard}>
            <p className={styles.summaryLabel}>{t("summary.candidates.label")}</p>
            <p className={styles.summaryValue}>{visibleCandidates.length}</p>
            <p className={styles.summaryHint}>{t("summary.candidates.hint")}</p>
          </article>
        </section>

        <section className={styles.twoColumn}>
          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h2 className={styles.panelTitle}>
                  <i className="fas fa-star" />
                  {t("current.title")}
                </h2>
                <p className={styles.panelHint}>{t("current.subtitle")}</p>
              </div>
              <span className={styles.tag}>{t("current.count", { count: selected.length, total: MAX_RECOMMENDATIONS })}</span>
            </div>

            <div className={styles.list}>
              {Array.from({ length: MAX_RECOMMENDATIONS }, (_, index) => {
                const item = selected[index];
                if (!item) {
                  return (
                    <div key={index} className={styles.emptyState}>
                      {t("current.emptySlot", { slotNo: index + 1 })}
                    </div>
                  );
                }
                return (
                  <article key={item.id} className={styles.card}>
                    <div className={styles.cardHead}>
                      <div>
                        <h3 className={styles.cardTitle}>{item.name}</h3>
                        <div className={styles.cardMeta}>
                          <span>{t("current.slot", { slotNo: index + 1 })}</span>
                          <span>{item.type || t("common.empty")}</span>
                          <span>{t("current.memberCount", { count: item.memberCount })}</span>
                        </div>
                      </div>
                      <div className={styles.actions}>
                        <button type="button" className={styles.secondaryButton} onClick={() => moveItem(index, -1)} disabled={index === 0}>
                          {t("actions.up")}
                        </button>
                        <button type="button" className={styles.secondaryButton} onClick={() => moveItem(index, 1)} disabled={index === selected.length - 1}>
                          {t("actions.down")}
                        </button>
                        <button type="button" className={styles.dangerButton} onClick={() => removeItem(item.id)}>
                          {t("actions.remove")}
                        </button>
                      </div>
                    </div>
                    <p className={styles.cardDescription}>{item.description || t("common.emptyDescription")}</p>
                  </article>
                );
              })}
            </div>
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHead}>
              <div>
                <h2 className={styles.panelTitle}>
                  <i className="fas fa-compass" />
                  {t("candidate.title")}
                </h2>
                <p className={styles.panelHint}>{t("candidate.subtitle")}</p>
              </div>
            </div>

            <form className={styles.searchRow} onSubmit={handleSearch}>
              <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} className={styles.searchInput} placeholder={t("candidate.searchPlaceholder")} />
              <button type="submit" className={styles.secondaryButton}>{t("actions.search")}</button>
            </form>

            <div className={`${styles.list} ${styles.searchResults}`}>
              {visibleCandidates.length ? (
                pagedCandidates.items.map((club) => (
                  <article key={club.id} className={styles.card}>
                    <div className={styles.cardHead}>
                      <div>
                        <h3 className={styles.cardTitle}>{club.name}</h3>
                        <div className={styles.cardMeta}>
                          <span>{club.type || t("common.empty")}</span>
                          <span>{t("current.memberCount", { count: club.memberCount })}</span>
                        </div>
                      </div>
                      <button type="button" className={styles.primaryButton} onClick={() => addItem(club)} disabled={selected.length >= MAX_RECOMMENDATIONS}>
                        {t("actions.add")}
                      </button>
                    </div>
                    <p className={styles.cardDescription}>{club.description || t("common.emptyDescription")}</p>
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
          </article>
        </section>
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const [recommendResponse, clubsResponse] = await Promise.all([
        fetchManagerRecommendationsRequest(),
        fetch("/api/clubs", { cache: "no-store" })
      ]);
      if (!recommendResponse) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(recommendResponse.status))) {
        return;
      }

      const recommendationResult = (await recommendResponse.json().catch(() => null)) as RecommendationResponse | null;
      const clubsResult = (await clubsResponse.json().catch(() => null)) as ClubsResponse | null;

      const allClubs = Array.isArray(clubsResult?.data) ? clubsResult.data : [];
      setClubs(allClubs);

      const selectedIds = Array.isArray(recommendationResult?.data)
        ? recommendationResult.data.map((item) => item.clubId)
        : [];
      const selectedClubs = selectedIds
        .map((clubId) => allClubs.find((club) => club.id === clubId))
        .filter((item): item is ClubOption => Boolean(item));
      setSelected(selectedClubs);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    setSaving(true);
    try {
      const response = await updateManagerRecommendationsRequest(selected.map((item) => item.id));
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as RecommendationResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(t("messages.saveFailed"));
        return;
      }
      notify.success(t("messages.saveSuccess"));
    } catch {
      notify.error(t("messages.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCandidatePage(1);
    setSearchKeyword(searchInput.trim());
  }

  function addItem(item: ClubOption) {
    if (selected.length >= MAX_RECOMMENDATIONS || selected.some((entry) => entry.id === item.id)) {
      return;
    }
    setSelected((current) => [...current, item]);
  }

  function removeItem(clubId: number) {
    setSelected((current) => current.filter((item) => item.id !== clubId));
  }

  function moveItem(index: number, offset: -1 | 1) {
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= selected.length) {
      return;
    }
    setSelected((current) => {
      const next = [...current];
      const [item] = next.splice(index, 1);
      next.splice(targetIndex, 0, item);
      return next;
    });
  }
}
