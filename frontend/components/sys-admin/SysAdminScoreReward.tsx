"use client";

import { ChangeEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { PaginationBar } from "../common/PaginationBar";
import { AdminClubDetail, AdminClubOption, AdminClubStudent } from "../../lib/admin-club/types";
import { authorizedFetch } from "../../lib/auth/client";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import {
  AdminRewardItem,
  AdminRewardMutationPayload,
  AdminRewardOrder,
  AdminScoreRecord,
  AdminScoreRecordMutationPayload,
  AdminScoreRule,
  AdminScoreRuleMutationPayload,
  RewardVisibilityScope
} from "../../lib/admin-score-reward/types";
import { useT } from "../../lib/i18n/useT";
import { paginateItems } from "../../lib/pagination";
import { useConfirm } from "../../lib/notify/useConfirm";
import { useNotify } from "../../lib/notify/useNotify";
import styles from "../../styles/sysAdminScoreReward.module.css";

const IMAGE_MAX_SIZE_BYTES = 10 * 1024 * 1024;
const ACCEPTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

const EMPTY_RULE_FORM: AdminScoreRuleMutationPayload = {
  name: "",
  scoreDelta: 0,
  scopeType: "GLOBAL",
  clubId: null,
  status: "ACTIVE"
};

const EMPTY_REWARD_FORM: AdminRewardMutationPayload = {
  name: "",
  scoreCost: 0,
  stock: 0,
  visibilityScope: "GLOBAL",
  clubIds: [],
  status: "ACTIVE"
};

const EMPTY_RECORD_FORM: AdminScoreRecordMutationPayload = {
  clubId: 0,
  userId: 0,
  ruleId: null,
  scoreDelta: 0,
  reason: ""
};

type RuleViewFilter = "ALL" | "GLOBAL" | "CLUB";
type RewardViewFilter = "ALL" | RewardVisibilityScope;
type AdminPage = "rules" | "rewards" | "orders" | "records";

type ApiResponse<T> = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: T;
};

type BasicResponse = {
  success?: boolean;
  code?: string;
  message?: string;
};

export function SysAdminScoreReward() {
  const t = useT("adminScoreReward");
  const tPortal = useT("portal");
  const tPagination = useT("pagination");
  const notify = useNotify();
  const confirm = useConfirm();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [clubs, setClubs] = useState<AdminClubOption[]>([]);
  const [rules, setRules] = useState<AdminScoreRule[]>([]);
  const [rewards, setRewards] = useState<AdminRewardItem[]>([]);
  const [orders, setOrders] = useState<AdminRewardOrder[]>([]);

  const [pageLoading, setPageLoading] = useState(true);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [rewardsLoading, setRewardsLoading] = useState(false);
  const [ordersLoading, setOrdersLoading] = useState(false);

  const [ruleForm, setRuleForm] = useState<AdminScoreRuleMutationPayload>(EMPTY_RULE_FORM);
  const [rewardForm, setRewardForm] = useState<AdminRewardMutationPayload>(EMPTY_REWARD_FORM);

  const [editingRuleId, setEditingRuleId] = useState<number | null>(null);
  const [editingRewardId, setEditingRewardId] = useState<number | null>(null);

  const [ruleScopeFilter, setRuleScopeFilter] = useState<RuleViewFilter>("ALL");
  const [ruleClubFilter, setRuleClubFilter] = useState<string>("ALL");
  const [rewardVisibilityFilter, setRewardVisibilityFilter] = useState<RewardViewFilter>("ALL");
  const [rewardClubFilter, setRewardClubFilter] = useState<string>("ALL");
  const [activePage, setActivePage] = useState<AdminPage>("rules");
  const [rulePage, setRulePage] = useState(1);
  const [rewardPage, setRewardPage] = useState(1);
  const [orderPage, setOrderPage] = useState(1);

  const [savingRule, setSavingRule] = useState(false);
  const [savingReward, setSavingReward] = useState(false);
  const [deletingRuleId, setDeletingRuleId] = useState<number | null>(null);
  const [deletingRewardId, setDeletingRewardId] = useState<number | null>(null);
  const [completingOrderId, setCompletingOrderId] = useState<number | null>(null);
  const [rejectingOrderId, setRejectingOrderId] = useState<number | null>(null);
  const [deletingOrderId, setDeletingOrderId] = useState<number | null>(null);

  const [rewardImageFile, setRewardImageFile] = useState<File | null>(null);
  const [rewardImagePreviewUrl, setRewardImagePreviewUrl] = useState("");
  const [rewardAssetUrls, setRewardAssetUrls] = useState<Record<number, string>>({});

  const [scoreRecords, setScoreRecords] = useState<AdminScoreRecord[]>([]);
  const [scoreRecordsLoading, setScoreRecordsLoading] = useState(false);
  const [recordClubFilter, setRecordClubFilter] = useState<string>("ALL");
  const [recordPage, setRecordPage] = useState(1);
  const [recordForm, setRecordForm] = useState<AdminScoreRecordMutationPayload>(EMPTY_RECORD_FORM);
  const [clubStudents, setClubStudents] = useState<AdminClubStudent[]>([]);
  const [clubStudentsLoading, setClubStudentsLoading] = useState(false);
  const [savingRecord, setSavingRecord] = useState(false);
  const [deletingRecordId, setDeletingRecordId] = useState<number | null>(null);

  useEffect(() => {
    if (authorized) {
      void loadInitialData();
    }
  }, [authorized]);

  useEffect(
    () => () => {
      if (rewardImagePreviewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(rewardImagePreviewUrl);
      }
    },
    [rewardImagePreviewUrl]
  );

  useEffect(
    () => () => {
      Object.values(rewardAssetUrls).forEach((url) => {
        if (url.startsWith("blob:")) {
          URL.revokeObjectURL(url);
        }
      });
    },
    [rewardAssetUrls]
  );

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

  if (pageLoading) {
    return (
      <main className={styles.page}>
        <section className={styles.loadingCard}>
          <p>{t("states.loadingPage")}</p>
        </section>
      </main>
    );
  }

  const visibleRules = rules.filter((rule) => {
    if (ruleScopeFilter === "GLOBAL" && rule.scopeType !== "GLOBAL") {
      return false;
    }
    if (ruleScopeFilter === "CLUB" && rule.scopeType !== "CLUB") {
      return false;
    }
    if (ruleScopeFilter === "CLUB" && ruleClubFilter !== "ALL" && String(rule.clubId || "") !== ruleClubFilter) {
      return false;
    }
    return true;
  });
  const visibleRewards = rewards.filter((reward) => {
    if (rewardVisibilityFilter !== "ALL" && reward.visibilityScope !== rewardVisibilityFilter) {
      return false;
    }
    if (
      rewardVisibilityFilter === "CLUB" &&
      rewardClubFilter !== "ALL" &&
      !reward.clubIds.includes(Number(rewardClubFilter))
    ) {
      return false;
    }
    return true;
  });

  const currentRewardPreview = rewardImagePreviewUrl || (editingRewardId ? rewardAssetUrls[editingRewardId] || "" : "");
  const pendingOrderCount = orders.filter((item) => item.status === "PENDING").length;
  const completedOrderCount = orders.filter((item) => item.status === "COMPLETED").length;
  const rejectedOrderCount = orders.filter((item) => item.status === "REJECTED").length;
  const editingReward = editingRewardId == null ? null : rewards.find((item) => item.id === editingRewardId) || null;
  const pagedRules = paginateItems(visibleRules, rulePage, 10);
  const pagedRewards = paginateItems(visibleRewards, rewardPage, 10);
  const pagedOrders = paginateItems(orders, orderPage, 10);
  const visibleScoreRecords = scoreRecords.filter((record) => {
    return recordClubFilter === "ALL" || String(record.clubId) === recordClubFilter;
  });
  const pagedScoreRecords = paginateItems(visibleScoreRecords, recordPage, 10);
  const recordRuleOptions = rules.filter(
    (rule) => rule.status === "ACTIVE" && (rule.scopeType === "GLOBAL" || rule.clubId === recordForm.clubId)
  );

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerCopy}>
            <h1>
              <i className="fas fa-coins" />
              {t("header.title")}
            </h1>
            <p>{t("header.subtitle")}</p>
          </div>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {t("actions.back")}
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => {
                void loadInitialData();
              }}
              disabled={pageLoading}
            >
              <i className="fas fa-rotate-right" />
              {t("actions.refreshAll")}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.container}>
        <div className={styles.overviewGrid}>
          <article className={styles.overviewCard}>
            <span>{t("overview.rulesLabel")}</span>
            <strong>{rules.length}</strong>
            <p>{t("overview.rulesHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("overview.rewardsLabel")}</span>
            <strong>{rewards.length}</strong>
            <p>{t("overview.rewardsHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("overview.pendingOrdersLabel")}</span>
            <strong>{pendingOrderCount}</strong>
            <p>{t("overview.pendingOrdersHint")}</p>
          </article>
          <article className={styles.overviewCard}>
            <span>{t("overview.completedOrdersLabel")}</span>
            <strong>{completedOrderCount}</strong>
            <p>{t("overview.completedOrdersHint")}</p>
          </article>
        </div>

        <section className={styles.pageSwitchCard}>
          <div className={styles.pageSwitchHead}>
            <h2>{t("navigation.title")}</h2>
            <p>{t(`navigation.${activePage}Hint`)}</p>
          </div>
          <div className={styles.pageSwitchGroup}>
            <button
              type="button"
              className={`${styles.filterChip} ${activePage === "rules" ? styles.filterChipActive : ""}`}
              onClick={() => setActivePage("rules")}
            >
              {t("rules.title")}
            </button>
            <button
              type="button"
              className={`${styles.filterChip} ${activePage === "rewards" ? styles.filterChipActive : ""}`}
              onClick={() => setActivePage("rewards")}
            >
              {t("rewards.title")}
            </button>
            <button
              type="button"
              className={`${styles.filterChip} ${activePage === "orders" ? styles.filterChipActive : ""}`}
              onClick={() => setActivePage("orders")}
            >
              {t("orders.title")}
            </button>
            <button
              type="button"
              className={`${styles.filterChip} ${activePage === "records" ? styles.filterChipActive : ""}`}
              onClick={() => setActivePage("records")}
            >
              {t("scoreRecords.title")}
            </button>
          </div>
        </section>

        {activePage === "rules" ? (
        <section className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{t("rules.title")}</h2>
              <p>{t("rules.description")}</p>
            </div>
            <div className={styles.sectionActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void loadRules()}
                disabled={rulesLoading}
              >
                <i className="fas fa-rotate-right" />
                {rulesLoading ? t("actions.loading") : t("actions.refresh")}
              </button>
            </div>
          </div>

          <div className={styles.sectionGrid}>
            <article className={styles.editorCard}>
              <div className={styles.editorHead}>
                <div>
                  <h3>{editingRuleId ? t("rules.editor.editTitle") : t("rules.editor.createTitle")}</h3>
                </div>
                {editingRuleId ? (
                  <button type="button" className={styles.ghostButton} onClick={resetRuleForm} disabled={savingRule}>
                    <i className="fas fa-xmark" />
                    {t("actions.cancelEdit")}
                  </button>
                ) : null}
              </div>

              <form
                className={styles.formGrid}
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleRuleSave();
                }}
              >
                <label className={styles.field}>
                  <span>{t("fields.ruleName")}</span>
                  <input
                    value={ruleForm.name}
                    onChange={(event) => setRuleForm((prev) => ({ ...prev, name: event.target.value.slice(0, 120) }))}
                    className={styles.input}
                    maxLength={120}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.scoreDelta")}</span>
                  <input
                    type="number"
                    value={String(ruleForm.scoreDelta)}
                    onChange={(event) => setRuleForm((prev) => ({ ...prev, scoreDelta: toInteger(event.target.value) }))}
                    className={styles.input}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.scopeType")}</span>
                  <select
                    value={ruleForm.scopeType}
                    onChange={(event) =>
                      setRuleForm((prev) => ({
                        ...prev,
                        scopeType: event.target.value === "CLUB" ? "CLUB" : "GLOBAL",
                        clubId: event.target.value === "CLUB" ? prev.clubId : null
                      }))
                    }
                    className={styles.select}
                  >
                    <option value="GLOBAL">{t("rules.scope.global")}</option>
                    <option value="CLUB">{t("rules.scope.club")}</option>
                  </select>
                </label>

                <label className={styles.field}>
                  <span>{t("fields.ruleStatus")}</span>
                  <select
                    value={ruleForm.status}
                    onChange={(event) =>
                      setRuleForm((prev) => ({
                        ...prev,
                        status: event.target.value === "INACTIVE" ? "INACTIVE" : "ACTIVE"
                      }))
                    }
                    className={styles.select}
                  >
                    <option value="ACTIVE">{t("status.active")}</option>
                    <option value="INACTIVE">{t("status.inactive")}</option>
                  </select>
                </label>

                {ruleForm.scopeType === "CLUB" ? (
                  <label className={`${styles.field} ${styles.fieldWide}`}>
                    <span>{t("fields.club")}</span>
                    <select
                      value={ruleForm.clubId == null ? "" : String(ruleForm.clubId)}
                      onChange={(event) =>
                        setRuleForm((prev) => ({
                          ...prev,
                          clubId: event.target.value ? Number(event.target.value) : null
                        }))
                      }
                      className={styles.select}
                    >
                      <option value="">{t("rules.editor.clubPlaceholder")}</option>
                      {clubs.map((club) => (
                        <option key={club.id} value={club.id}>
                          {club.name}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : null}

                <div className={`${styles.formActions} ${styles.fieldWide}`}>
                  <button type="button" className={styles.secondaryButton} onClick={resetRuleForm} disabled={savingRule}>
                    <i className="fas fa-eraser" />
                    {t("actions.reset")}
                  </button>
                  <button type="submit" className={styles.primaryButton} disabled={savingRule}>
                    <i className="fas fa-floppy-disk" />
                    {savingRule ? t("actions.saving") : editingRuleId ? t("actions.saveChanges") : t("actions.createRule")}
                  </button>
                </div>
              </form>
            </article>

            <article className={styles.listCard}>
              <div className={styles.listToolbar}>
                <div className={styles.filterGroup}>
                  {(["ALL", "GLOBAL", "CLUB"] as RuleViewFilter[]).map((value) => (
                    <button
                      key={value}
                      type="button"
                      className={`${styles.filterChip} ${ruleScopeFilter === value ? styles.filterChipActive : ""}`}
                      onClick={() => {
                        setRuleScopeFilter(value);
                        setRulePage(1);
                      }}
                    >
                      {t(`filters.scope.${value.toLowerCase()}`)}
                    </button>
                  ))}
                </div>
                {ruleScopeFilter === "CLUB" ? (
                  <select
                    value={ruleClubFilter}
                    onChange={(event) => {
                      setRuleClubFilter(event.target.value);
                      setRulePage(1);
                    }}
                    className={styles.filterSelect}
                  >
                    <option value="ALL">{t("filters.allClubs")}</option>
                    {clubs.map((club) => (
                      <option key={club.id} value={club.id}>
                        {club.name}
                      </option>
                    ))}
                  </select>
                ) : null}
              </div>

              {rulesLoading ? (
                <div className={styles.inlineState}>{t("states.loadingRules")}</div>
              ) : !visibleRules.length ? (
                <div className={styles.emptyState}>
                  <i className="fas fa-layer-group" />
                  <h3>{t("empty.rulesTitle")}</h3>
                  <p>{t("empty.rulesDescription")}</p>
                </div>
              ) : (
                <>
                <div className={styles.cardList}>
                  {pagedRules.items.map((rule) => (
                    <article key={rule.id} className={styles.itemCard}>
                      <div className={styles.itemHead}>
                        <div>
                          <div className={styles.itemTitleRow}>
                            <h3>{rule.name}</h3>
                            <span className={`${styles.statusBadge} ${rule.status === "ACTIVE" ? styles.statusActive : styles.statusInactive}`}>
                              {t(rule.status === "ACTIVE" ? "status.active" : "status.inactive")}
                            </span>
                          </div>
                          <div className={styles.metaRow}>
                            <span>{t(`rules.scope.${rule.scopeType.toLowerCase()}`)}</span>
                            <span>{t("rules.scoreDeltaValue", { value: formatSigned(rule.scoreDelta) })}</span>
                            <span>{rule.scopeType === "CLUB" ? rule.clubName || t("states.notSet") : t("rules.scope.global")}</span>
                            <span>{formatDateTime(rule.updatedAt || rule.createdAt, t("states.notSet"))}</span>
                          </div>
                        </div>
                        <div className={styles.itemActions}>
                          <button type="button" className={styles.rowAction} onClick={() => populateRuleForm(rule)}>
                            <i className="fas fa-pen" />
                            {t("actions.edit")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void handleRuleDelete(rule)}
                            disabled={deletingRuleId === rule.id}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingRuleId === rule.id ? t("actions.deleting") : t("actions.delete")}
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <PaginationBar
                  currentPage={pagedRules.page}
                  totalPages={pagedRules.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedRules.page, total: pagedRules.totalPages })}
                  onPageChange={setRulePage}
                />
                </>
              )}
            </article>
          </div>
        </section>
        ) : null}

        {activePage === "rewards" ? (
        <section className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{t("rewards.title")}</h2>
              <p>{t("rewards.description")}</p>
            </div>
            <div className={styles.sectionActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void loadRewards()}
                disabled={rewardsLoading}
              >
                <i className="fas fa-rotate-right" />
                {rewardsLoading ? t("actions.loading") : t("actions.refresh")}
              </button>
            </div>
          </div>

          <div className={styles.sectionGrid}>
            <article className={styles.editorCard}>
              <div className={styles.editorHead}>
                <div>
                  <h3>{editingRewardId ? t("rewards.editor.editTitle") : t("rewards.editor.createTitle")}</h3>
                </div>
                {editingRewardId ? (
                  <button type="button" className={styles.ghostButton} onClick={resetRewardForm} disabled={savingReward}>
                    <i className="fas fa-xmark" />
                    {t("actions.cancelEdit")}
                  </button>
                ) : null}
              </div>

              <form
                className={styles.formGrid}
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleRewardSave();
                }}
              >
                <div className={`${styles.field} ${styles.fieldWide}`}>
                  <span>{t("fields.rewardImage")}</span>
                  <div className={styles.imageEditor}>
                    <div className={styles.imagePreview}>
                      {currentRewardPreview ? (
                        <img src={currentRewardPreview} alt={t("rewards.imageAlt")} className={styles.previewImage} />
                      ) : (
                        <div className={styles.previewPlaceholder}>
                          <i className="fas fa-image" />
                          <p>{t("rewards.noImage")}</p>
                        </div>
                      )}
                    </div>
                    <div className={styles.imageActions}>
                      <label className={styles.uploadButton}>
                        <i className="fas fa-upload" />
                        {t("actions.selectImage")}
                        <input type="file" hidden accept={ACCEPTED_IMAGE_TYPES.join(",")} onChange={handleRewardImageChange} />
                      </label>
                      <p>{t("rewards.imageHint")}</p>
                    </div>
                  </div>
                </div>

                <label className={styles.field}>
                  <span>{t("fields.rewardName")}</span>
                  <input
                    value={rewardForm.name}
                    onChange={(event) => setRewardForm((prev) => ({ ...prev, name: event.target.value.slice(0, 120) }))}
                    className={styles.input}
                    maxLength={120}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.scoreCost")}</span>
                  <input
                    type="number"
                    min={0}
                    value={String(rewardForm.scoreCost)}
                    onChange={(event) => setRewardForm((prev) => ({ ...prev, scoreCost: toNonNegativeInteger(event.target.value) }))}
                    className={styles.input}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.stock")}</span>
                  <input
                    type="number"
                    min={0}
                    value={String(rewardForm.stock)}
                    onChange={(event) => setRewardForm((prev) => ({ ...prev, stock: toNonNegativeInteger(event.target.value) }))}
                    className={styles.input}
                  />
                </label>

                <label className={styles.field}>
                  <span>{t("fields.rewardStatus")}</span>
                  <select
                    value={rewardForm.status}
                    onChange={(event) =>
                      setRewardForm((prev) => ({
                        ...prev,
                        status: event.target.value === "INACTIVE" ? "INACTIVE" : "ACTIVE"
                      }))
                    }
                    className={styles.select}
                  >
                    <option value="ACTIVE">{t("status.active")}</option>
                    <option value="INACTIVE">{t("status.inactive")}</option>
                  </select>
                </label>

                <label className={styles.field}>
                  <span>{t("fields.rewardVisibility")}</span>
                  <select
                    value={rewardForm.visibilityScope}
                    onChange={(event) => {
                      const nextScope = toRewardVisibilityScope(event.target.value);
                      setRewardForm((prev) => ({
                        ...prev,
                        visibilityScope: nextScope,
                        clubIds: nextScope === "CLUB" ? prev.clubIds : []
                      }));
                    }}
                    className={styles.select}
                  >
                    <option value="GLOBAL">{t("rewards.visibility.global")}</option>
                    <option value="CLUB">{t("rewards.visibility.club")}</option>
                    {editingReward?.visibilityScope === "UNASSIGNED" ? (
                      <option value="UNASSIGNED">{t("rewards.editor.unassignedOption")}</option>
                    ) : null}
                  </select>
                </label>

                {rewardForm.visibilityScope === "CLUB" ? (
                  <div className={`${styles.field} ${styles.fieldWide}`}>
                    <span>{t("fields.rewardTargetClubs")}</span>
                    {clubs.length ? (
                      <div className={styles.checkboxGrid}>
                        {clubs.map((club) => {
                          const checked = rewardForm.clubIds.includes(club.id);
                          return (
                            <label key={club.id} className={`${styles.checkboxCard} ${checked ? styles.checkboxCardActive : ""}`}>
                              <input
                                type="checkbox"
                                checked={checked}
                                onChange={() => toggleRewardTargetClub(club.id)}
                              />
                              <span>{club.name}</span>
                            </label>
                          );
                        })}
                      </div>
                    ) : (
                      <div className={styles.inlineHint}>{t("rewards.clubTargetEmpty")}</div>
                    )}
                    <p className={styles.helperText}>{t("rewards.clubHint")}</p>
                  </div>
                ) : null}

                <div className={`${styles.formActions} ${styles.fieldWide}`}>
                  <button type="button" className={styles.secondaryButton} onClick={resetRewardForm} disabled={savingReward}>
                    <i className="fas fa-eraser" />
                    {t("actions.reset")}
                  </button>
                  <button type="submit" className={styles.primaryButton} disabled={savingReward}>
                    <i className="fas fa-floppy-disk" />
                    {savingReward ? t("actions.saving") : editingRewardId ? t("actions.saveChanges") : t("actions.createReward")}
                  </button>
                </div>
              </form>
            </article>

            <article className={styles.listCard}>
              <div className={styles.listToolbar}>
                <div className={styles.filterGroup}>
                  {(["ALL", "GLOBAL", "CLUB", "UNASSIGNED"] as RewardViewFilter[]).map((value) => (
                    <button
                      key={value}
                      type="button"
                      className={`${styles.filterChip} ${rewardVisibilityFilter === value ? styles.filterChipActive : ""}`}
                      onClick={() => {
                        setRewardVisibilityFilter(value);
                        setRewardPage(1);
                        if (value !== "CLUB") {
                          setRewardClubFilter("ALL");
                        }
                      }}
                    >
                      {t(`filters.rewardVisibility.${value.toLowerCase()}`)}
                    </button>
                  ))}
                </div>
                {rewardVisibilityFilter === "CLUB" ? (
                  <select
                    value={rewardClubFilter}
                    onChange={(event) => {
                      setRewardClubFilter(event.target.value);
                      setRewardPage(1);
                    }}
                    className={styles.filterSelect}
                  >
                    <option value="ALL">{t("filters.allClubs")}</option>
                    {clubs.map((club) => (
                      <option key={club.id} value={club.id}>
                        {club.name}
                      </option>
                    ))}
                  </select>
                ) : null}
              </div>
              {rewardsLoading ? (
                <div className={styles.inlineState}>{t("states.loadingRewards")}</div>
              ) : !visibleRewards.length ? (
                <div className={styles.emptyState}>
                  <i className="fas fa-gift" />
                  <h3>{t("empty.rewardsTitle")}</h3>
                  <p>{t("empty.rewardsDescription")}</p>
                </div>
              ) : (
                <>
                <div className={styles.cardList}>
                  {pagedRewards.items.map((reward) => (
                    <article key={reward.id} className={styles.itemCard}>
                      <div className={styles.rewardCard}>
                        <div className={styles.rewardThumb}>
                          {rewardAssetUrls[reward.id] ? (
                            <img src={rewardAssetUrls[reward.id]} alt={reward.name || t("rewards.imageAlt")} className={styles.thumbImage} />
                          ) : (
                            <div className={styles.thumbPlaceholder}>
                              <i className="fas fa-box-open" />
                            </div>
                          )}
                        </div>
                        <div className={styles.rewardCopy}>
                          <div className={styles.itemTitleRow}>
                            <h3>{reward.name}</h3>
                            <span className={`${styles.statusBadge} ${reward.status === "ACTIVE" ? styles.statusActive : styles.statusInactive}`}>
                              {t(reward.status === "ACTIVE" ? "status.active" : "status.inactive")}
                            </span>
                          </div>
                          <div className={styles.metaRow}>
                            <span>{t("rewards.scoreCostValue", { value: reward.scoreCost })}</span>
                            <span>{t("rewards.stockValue", { value: reward.stock })}</span>
                            <span>{reward.hasImage ? t("rewards.imageReady") : t("rewards.imageMissing")}</span>
                            <span>{t(`rewards.visibility.${reward.visibilityScope.toLowerCase()}`)}</span>
                            {reward.visibilityScope === "CLUB" && reward.clubNames.length ? (
                              <span>{t("rewards.visibilitySummary.clubNames", { names: reward.clubNames.join("、") })}</span>
                            ) : (
                              <span>{t(`rewards.visibilitySummary.${reward.visibilityScope.toLowerCase()}`)}</span>
                            )}
                            <span>{formatDateTime(reward.updatedAt || reward.createdAt, t("states.notSet"))}</span>
                          </div>
                        </div>
                        <div className={styles.itemActions}>
                          <button type="button" className={styles.rowAction} onClick={() => populateRewardForm(reward)}>
                            <i className="fas fa-pen" />
                            {t("actions.edit")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void handleRewardDelete(reward)}
                            disabled={deletingRewardId === reward.id}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingRewardId === reward.id ? t("actions.deleting") : t("actions.delete")}
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <PaginationBar
                  currentPage={pagedRewards.page}
                  totalPages={pagedRewards.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedRewards.page, total: pagedRewards.totalPages })}
                  onPageChange={setRewardPage}
                />
                </>
              )}
            </article>
          </div>
        </section>
        ) : null}

        {activePage === "orders" ? (
        <section className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{t("orders.title")}</h2>
              <p>{t("orders.description")}</p>
            </div>
            <div className={styles.sectionActions}>
              <span className={styles.summaryPill}>{t("orders.pendingValue", { value: pendingOrderCount })}</span>
              <span className={styles.summaryPill}>{t("orders.completedValue", { value: completedOrderCount })}</span>
              <span className={styles.summaryPill}>{t("orders.rejectedValue", { value: rejectedOrderCount })}</span>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void loadOrders()}
                disabled={ordersLoading}
              >
                <i className="fas fa-rotate-right" />
                {ordersLoading ? t("actions.loading") : t("actions.refresh")}
              </button>
            </div>
          </div>

          {ordersLoading ? (
            <div className={styles.inlineState}>{t("states.loadingOrders")}</div>
          ) : !orders.length ? (
            <div className={styles.emptyState}>
              <i className="fas fa-inbox" />
              <h3>{t("empty.ordersTitle")}</h3>
              <p>{t("empty.ordersDescription")}</p>
            </div>
          ) : (
            <>
            <div className={styles.tableWrapper}>
              <table className={styles.dataTable}>
                <thead>
                  <tr>
                    <th>{t("orders.table.reward")}</th>
                    <th>{t("orders.table.student")}</th>
                    <th>{t("orders.table.scoreCost")}</th>
                    <th>{t("orders.table.status")}</th>
                    <th>{t("orders.table.createdAt")}</th>
                    <th>{t("orders.table.lifecycle")}</th>
                    <th>{t("orders.table.actions")}</th>
                  </tr>
                </thead>
                <tbody>
                  {pagedOrders.items.map((order) => (
                    <tr key={order.id}>
                      <td>
                        <div className={styles.tablePrimary}>{order.rewardName || t("states.notSet")}</div>
                        <div className={styles.tableSecondary}>#{order.rewardId}</div>
                      </td>
                      <td>
                        <div className={styles.tablePrimary}>{resolveStudentName(order)}</div>
                      </td>
                      <td>{order.scoreCost}</td>
                      <td>
                        <span
                          className={`${styles.statusBadge} ${
                            order.status === "COMPLETED"
                              ? styles.statusActive
                              : order.status === "REJECTED"
                                ? styles.statusInactive
                                : styles.statusPending
                          }`}
                        >
                          {t(
                            order.status === "COMPLETED"
                              ? "status.completed"
                              : order.status === "REJECTED"
                                ? "status.rejected"
                                : "status.pending"
                          )}
                        </span>
                      </td>
                      <td>{formatDateTime(order.createdAt, t("states.notSet"))}</td>
                      <td>
                        <div className={styles.tablePrimary}>
                          {formatDateTime(
                            order.status === "REJECTED" ? order.rejectedAt : order.completedAt,
                            t("states.notSet")
                          )}
                        </div>
                        <div className={styles.tableSecondary}>
                          {order.status === "REJECTED"
                            ? order.rejectedByName || t("states.notSet")
                            : order.completedByName || t("states.notSet")}
                        </div>
                      </td>
                      <td>
                        <div className={styles.itemActions}>
                          <button
                            type="button"
                            className={styles.rowAction}
                            onClick={() => void handleCompleteOrder(order)}
                            disabled={completingOrderId === order.id || order.status !== "PENDING"}
                          >
                            <i className="fas fa-circle-check" />
                            {completingOrderId === order.id
                              ? t("actions.processing")
                              : order.status === "COMPLETED"
                                ? t("actions.completed")
                                : t("actions.complete")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void handleRejectOrder(order)}
                            disabled={rejectingOrderId === order.id || order.status !== "PENDING"}
                          >
                            <i className="fas fa-ban" />
                            {rejectingOrderId === order.id
                              ? t("actions.processing")
                              : order.status === "REJECTED"
                                ? t("actions.rejected")
                                : t("actions.reject")}
                          </button>
                          <button
                            type="button"
                            className={`${styles.rowAction} ${styles.rowDangerAction}`}
                            onClick={() => void handleDeleteOrder(order)}
                            disabled={deletingOrderId === order.id || order.status !== "COMPLETED"}
                          >
                            <i className="fas fa-trash-can" />
                            {deletingOrderId === order.id ? t("actions.deleting") : t("actions.delete")}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationBar
              currentPage={pagedOrders.page}
              totalPages={pagedOrders.totalPages}
              prevLabel={tPagination("prev")}
              nextLabel={tPagination("next")}
              pageLabel={tPagination("status", { page: pagedOrders.page, total: pagedOrders.totalPages })}
              onPageChange={setOrderPage}
            />
            </>
          )}
        </section>
        ) : null}

        {activePage === "records" ? (
        <section className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <div>
              <h2>{t("scoreRecords.title")}</h2>
              <p>{t("scoreRecords.description")}</p>
            </div>
            <div className={styles.sectionActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void loadScoreRecords()}
                disabled={scoreRecordsLoading}
              >
                <i className="fas fa-rotate-right" />
                {scoreRecordsLoading ? t("actions.loading") : t("actions.refresh")}
              </button>
            </div>
          </div>

          <div className={styles.sectionGrid}>
            <article className={styles.editorCard}>
              <div className={styles.editorHead}>
                <div>
                  <h3>{t("scoreRecords.form.title")}</h3>
                </div>
              </div>

              <form
                className={styles.formGrid}
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleRecordSave();
                }}
              >
                <label className={styles.field}>
                  <span>{t("scoreRecords.form.club")}</span>
                  <select
                    value={recordForm.clubId > 0 ? String(recordForm.clubId) : ""}
                    onChange={(event) => void handleRecordClubChange(event.target.value)}
                    className={styles.select}
                  >
                    <option value="">{t("scoreRecords.form.clubPlaceholder")}</option>
                    {clubs.map((club) => (
                      <option key={club.id} value={club.id}>
                        {club.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={styles.field}>
                  <span>{t("scoreRecords.form.student")}</span>
                  <select
                    value={recordForm.userId > 0 ? String(recordForm.userId) : ""}
                    onChange={(event) =>
                      setRecordForm((prev) => ({
                        ...prev,
                        userId: event.target.value ? Number(event.target.value) : 0
                      }))
                    }
                    className={styles.select}
                    disabled={!recordForm.clubId || clubStudentsLoading}
                  >
                    <option value="">
                      {clubStudentsLoading
                        ? t("scoreRecords.form.studentLoading")
                        : t("scoreRecords.form.studentPlaceholder")}
                    </option>
                    {clubStudents.map((student) => (
                      <option key={student.userId} value={student.userId}>
                        {student.displayName
                          ? `${student.displayName}（${student.studentNo || student.username}）`
                          : student.username}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={styles.field}>
                  <span>{t("scoreRecords.form.rule")}</span>
                  <select
                    value={recordForm.ruleId == null ? "" : String(recordForm.ruleId)}
                    onChange={(event) => {
                      const ruleId = event.target.value ? Number(event.target.value) : null;
                      setRecordForm((prev) => {
                        const rule = ruleId == null ? null : rules.find((item) => item.id === ruleId) || null;
                        return {
                          ...prev,
                          ruleId,
                          scoreDelta: rule ? rule.scoreDelta : prev.scoreDelta
                        };
                      });
                    }}
                    className={styles.select}
                    disabled={!recordForm.clubId}
                  >
                    <option value="">{t("scoreRecords.form.rulePlaceholder")}</option>
                    {recordRuleOptions.map((rule) => (
                      <option key={rule.id} value={rule.id}>
                        {`${rule.name}（${formatSigned(rule.scoreDelta)}）`}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={styles.field}>
                  <span>{t("scoreRecords.form.delta")}</span>
                  <input
                    type="number"
                    value={String(recordForm.scoreDelta)}
                    onChange={(event) => setRecordForm((prev) => ({ ...prev, scoreDelta: toInteger(event.target.value) }))}
                    className={styles.input}
                    disabled={recordForm.ruleId != null}
                  />
                </label>

                <label className={`${styles.field} ${styles.fieldWide}`}>
                  <span>{t("scoreRecords.form.reason")}</span>
                  <input
                    value={recordForm.reason}
                    onChange={(event) => setRecordForm((prev) => ({ ...prev, reason: event.target.value.slice(0, 200) }))}
                    className={styles.input}
                    maxLength={200}
                    placeholder={t("scoreRecords.form.reasonPlaceholder")}
                  />
                </label>

                <p className={styles.formHint}>{t("scoreRecords.form.deltaHint")}</p>

                <div className={`${styles.formActions} ${styles.fieldWide}`}>
                  <button type="button" className={styles.secondaryButton} onClick={resetRecordForm} disabled={savingRecord}>
                    <i className="fas fa-eraser" />
                    {t("actions.reset")}
                  </button>
                  <button type="submit" className={styles.primaryButton} disabled={savingRecord}>
                    <i className="fas fa-plus" />
                    {savingRecord ? t("actions.saving") : t("scoreRecords.form.submit")}
                  </button>
                </div>
              </form>
            </article>

            <article className={styles.listCard}>
              <div className={styles.listToolbar}>
                <div className={styles.filterGroup}>
                  <select
                    value={recordClubFilter}
                    onChange={(event) => {
                      setRecordClubFilter(event.target.value);
                      setRecordPage(1);
                    }}
                    className={styles.filterSelect}
                  >
                    <option value="ALL">{t("scoreRecords.filter.allClubs")}</option>
                    {clubs.map((club) => (
                      <option key={club.id} value={club.id}>
                        {club.name}
                      </option>
                    ))}
                  </select>
                </div>
                <span className={styles.summaryPill}>{t("scoreRecords.totalValue", { value: visibleScoreRecords.length })}</span>
              </div>

              {scoreRecordsLoading ? (
                <div className={styles.inlineState}>{t("states.loadingScoreRecords")}</div>
              ) : !visibleScoreRecords.length ? (
                <div className={styles.emptyState}>
                  <i className="fas fa-list-ul" />
                  <h3>{t("scoreRecords.empty.title")}</h3>
                  <p>{t("scoreRecords.empty.description")}</p>
                </div>
              ) : (
                <>
                <div className={styles.tableWrapper}>
                  <table className={styles.dataTable}>
                    <thead>
                      <tr>
                        <th>{t("scoreRecords.table.createdAt")}</th>
                        <th>{t("scoreRecords.table.club")}</th>
                        <th>{t("scoreRecords.table.student")}</th>
                        <th>{t("scoreRecords.table.delta")}</th>
                        <th>{t("scoreRecords.table.rule")}</th>
                        <th>{t("scoreRecords.table.reason")}</th>
                        <th>{t("scoreRecords.table.operator")}</th>
                        <th>{t("scoreRecords.table.actions")}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagedScoreRecords.items.map((record) => (
                        <tr key={record.id}>
                          <td>{formatDateTime(record.createdAt, t("states.notSet"))}</td>
                          <td>
                            <div className={styles.tablePrimary}>{record.clubName || t("states.notSet")}</div>
                          </td>
                          <td>
                            <div className={styles.tablePrimary}>
                              {record.displayName || record.username || `#${record.userId}`}
                            </div>
                            <div className={styles.tableSecondary}>{record.studentNo || record.username || ""}</div>
                          </td>
                          <td>
                            <span
                              className={`${styles.statusBadge} ${
                                record.scoreDelta >= 0 ? styles.statusActive : styles.statusInactive
                              }`}
                            >
                              {formatSigned(record.scoreDelta)}
                            </span>
                          </td>
                          <td>{record.ruleName || t("scoreRecords.table.manual")}</td>
                          <td>{record.reason || t("states.notSet")}</td>
                          <td>{record.operatorName || t("states.notSet")}</td>
                          <td>
                            <div className={styles.itemActions}>
                              <button
                                type="button"
                                className={`${styles.rowAction} ${styles.rowDangerAction}`}
                                onClick={() => void handleRecordDelete(record)}
                                disabled={deletingRecordId === record.id}
                              >
                                <i className="fas fa-rotate-left" />
                                {deletingRecordId === record.id ? t("actions.deleting") : t("actions.revoke")}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <PaginationBar
                  currentPage={pagedScoreRecords.page}
                  totalPages={pagedScoreRecords.totalPages}
                  prevLabel={tPagination("prev")}
                  nextLabel={tPagination("next")}
                  pageLabel={tPagination("status", { page: pagedScoreRecords.page, total: pagedScoreRecords.totalPages })}
                  onPageChange={setRecordPage}
                />
                </>
              )}
            </article>
          </div>
        </section>
        ) : null}
      </section>
    </main>
  );

  async function loadInitialData() {
    setPageLoading(true);
    await Promise.all([loadClubs(), loadRules(), loadRewards(), loadOrders(), loadScoreRecords()]);
    setPageLoading(false);
  }

  async function loadClubs() {
    try {
      const response = await fetchWithAuthorization("/api/admin/clubs/options", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminClubOption[]> | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadClubsFailed"));
        return;
      }
      setClubs(result.data);
    } catch {
      notify.error(t("messages.loadClubsFailed"));
    }
  }

  async function loadRules() {
    setRulesLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/score-rules", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminScoreRule[]> | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadRulesFailed"));
        return;
      }
      setRules(result.data);
    } catch {
      notify.error(t("messages.loadRulesFailed"));
    } finally {
      setRulesLoading(false);
    }
  }

  async function loadRewards() {
    setRewardsLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/rewards", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminRewardItem[]> | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadRewardsFailed"));
        return;
      }
      setRewards(result.data);
      void loadRewardAssets(result.data);
    } catch {
      notify.error(t("messages.loadRewardsFailed"));
    } finally {
      setRewardsLoading(false);
    }
  }

  async function loadOrders() {
    setOrdersLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/reward-orders", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminRewardOrder[]> | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadOrdersFailed"));
        return;
      }
      setOrders(result.data);
    } catch {
      notify.error(t("messages.loadOrdersFailed"));
    } finally {
      setOrdersLoading(false);
    }
  }

  async function loadScoreRecords() {
    setScoreRecordsLoading(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/score-records", {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminScoreRecord[]> | null;
      if (!response.ok || result?.success !== true || !Array.isArray(result.data)) {
        notify.error(result?.message || t("messages.loadScoreRecordsFailed"));
        return;
      }
      setScoreRecords(result.data);
    } catch {
      notify.error(t("messages.loadScoreRecordsFailed"));
    } finally {
      setScoreRecordsLoading(false);
    }
  }

  async function loadClubStudents(clubId: number) {
    setClubStudentsLoading(true);
    try {
      const response = await fetchWithAuthorization(`/api/admin/clubs/${clubId}`, {
        method: "GET"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminClubDetail> | null;
      if (!response.ok || result?.success !== true || !result.data || !Array.isArray(result.data.students)) {
        notify.error(result?.message || t("messages.loadClubStudentsFailed"));
        return;
      }
      setClubStudents(result.data.students);
    } catch {
      notify.error(t("messages.loadClubStudentsFailed"));
    } finally {
      setClubStudentsLoading(false);
    }
  }

  async function handleRuleSave() {
    const payload = buildRulePayload();
    if (!payload) {
      return;
    }

    setSavingRule(true);
    try {
      const isCreate = editingRuleId == null;
      const response = await fetchWithAuthorization(isCreate ? "/api/admin/score-rules" : `/api/admin/score-rules/${editingRuleId}`, {
        method: isCreate ? "POST" : "PATCH",
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
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminScoreRule> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveRuleFailed"));
        return;
      }

      notify.success(t(isCreate ? "messages.createRuleSuccess" : "messages.updateRuleSuccess"));
      resetRuleForm();
      await loadRules();
    } catch {
      notify.error(t("messages.saveRuleFailed"));
    } finally {
      setSavingRule(false);
    }
  }

  async function handleRuleDelete(rule: AdminScoreRule) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteRuleTitle"),
      message: t("confirm.deleteRuleMessage", { name: rule.name }),
      confirmText: t("actions.delete"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingRuleId(rule.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/score-rules/${rule.id}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as BasicResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.code === "DEPENDENCY_EXISTS" ? t("messages.deleteRuleBlocked") : result?.message || t("messages.deleteRuleFailed"));
        return;
      }
      if (editingRuleId === rule.id) {
        resetRuleForm();
      }
      notify.success(t("messages.deleteRuleSuccess"));
      await loadRules();
    } catch {
      notify.error(t("messages.deleteRuleFailed"));
    } finally {
      setDeletingRuleId(null);
    }
  }

  async function handleRewardSave() {
    const payload = buildRewardPayload();
    if (!payload) {
      return;
    }

    setSavingReward(true);
    try {
      const isCreate = editingRewardId == null;
      const response = await fetchWithAuthorization(isCreate ? "/api/admin/rewards" : `/api/admin/rewards/${editingRewardId}`, {
        method: isCreate ? "POST" : "PATCH",
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
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminRewardItem> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveRewardFailed"));
        return;
      }

      const savedReward = result.data;
      if (rewardImageFile) {
        const formData = new FormData();
        formData.append("file", rewardImageFile);
        const uploadResponse = await fetchWithAuthorization(`/api/admin/rewards/${savedReward.id}/image`, {
          method: "POST",
          body: formData
        });
        if (!uploadResponse) {
          return;
        }
        if (!(await handleAuthStatus(uploadResponse.status))) {
          return;
        }
        const uploadResult = (await uploadResponse.json().catch(() => null)) as ApiResponse<AdminRewardItem> | null;
        if (!uploadResponse.ok || uploadResult?.success !== true || !uploadResult.data) {
          notify.error(uploadResult?.message || t("messages.uploadRewardImageFailed"));
          await loadRewards();
          return;
        }
      }

      notify.success(t(isCreate ? "messages.createRewardSuccess" : "messages.updateRewardSuccess"));
      resetRewardForm();
      await loadRewards();
    } catch {
      notify.error(t("messages.saveRewardFailed"));
    } finally {
      setSavingReward(false);
    }
  }

  async function handleRewardDelete(reward: AdminRewardItem) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteRewardTitle"),
      message: t("confirm.deleteRewardMessage", { name: reward.name }),
      confirmText: t("actions.delete"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingRewardId(reward.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/rewards/${reward.id}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as BasicResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.code === "DEPENDENCY_EXISTS" ? t("messages.deleteRewardBlocked") : result?.message || t("messages.deleteRewardFailed"));
        return;
      }
      if (editingRewardId === reward.id) {
        resetRewardForm();
      }
      notify.success(t("messages.deleteRewardSuccess"));
      await loadRewards();
    } catch {
      notify.error(t("messages.deleteRewardFailed"));
    } finally {
      setDeletingRewardId(null);
    }
  }

  async function handleCompleteOrder(order: AdminRewardOrder) {
    if (order.status !== "PENDING") {
      return;
    }

    setCompletingOrderId(order.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/reward-orders/${order.id}/complete`, {
        method: "POST"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminRewardOrder> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.completeOrderFailed"));
        return;
      }
      setOrders((prev) => prev.map((item) => (item.id === result.data?.id ? result.data : item)));
      notify.success(t("messages.completeOrderSuccess"));
    } catch {
      notify.error(t("messages.completeOrderFailed"));
    } finally {
      setCompletingOrderId(null);
    }
  }

  async function handleRejectOrder(order: AdminRewardOrder) {
    if (order.status !== "PENDING") {
      return;
    }

    const accepted = await confirm.confirm({
      title: t("confirm.rejectOrderTitle"),
      message: t("confirm.rejectOrderMessage", { name: order.rewardName || `#${order.id}` }),
      confirmText: t("actions.reject"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setRejectingOrderId(order.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/reward-orders/${order.id}/reject`, {
        method: "POST"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminRewardOrder> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.rejectOrderFailed"));
        return;
      }
      setOrders((prev) => prev.map((item) => (item.id === result.data?.id ? result.data : item)));
      notify.success(t("messages.rejectOrderSuccess"));
    } catch {
      notify.error(t("messages.rejectOrderFailed"));
    } finally {
      setRejectingOrderId(null);
    }
  }

  async function handleDeleteOrder(order: AdminRewardOrder) {
    if (order.status !== "COMPLETED") {
      notify.warning(t("messages.deleteOrderPendingBlocked"));
      return;
    }

    const accepted = await confirm.confirm({
      title: t("confirm.deleteOrderTitle"),
      message: t("confirm.deleteOrderMessage", { name: order.rewardName || `#${order.id}` }),
      confirmText: t("actions.delete"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingOrderId(order.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/reward-orders/${order.id}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as BasicResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.deleteOrderFailed"));
        return;
      }
      setOrders((prev) => prev.filter((item) => item.id !== order.id));
      notify.success(t("messages.deleteOrderSuccess"));
    } catch {
      notify.error(t("messages.deleteOrderFailed"));
    } finally {
      setDeletingOrderId(null);
    }
  }

  async function handleRecordClubChange(value: string) {
    const clubId = value ? Number(value) : 0;
    setRecordForm((prev) => ({ ...prev, clubId, userId: 0, ruleId: null }));
    setClubStudents([]);
    if (clubId > 0) {
      await loadClubStudents(clubId);
    }
  }

  async function handleRecordSave() {
    const payload = buildRecordPayload();
    if (!payload) {
      return;
    }

    setSavingRecord(true);
    try {
      const response = await fetchWithAuthorization("/api/admin/score-records", {
        method: "POST",
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
      const result = (await response.json().catch(() => null)) as ApiResponse<AdminScoreRecord> | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveRecordFailed"));
        return;
      }
      notify.success(t("messages.saveRecordSuccess"));
      setRecordForm((prev) => ({ ...EMPTY_RECORD_FORM, clubId: prev.clubId }));
      await loadScoreRecords();
    } catch {
      notify.error(t("messages.saveRecordFailed"));
    } finally {
      setSavingRecord(false);
    }
  }

  async function handleRecordDelete(record: AdminScoreRecord) {
    const accepted = await confirm.confirm({
      title: t("confirm.deleteRecordTitle"),
      message: t("confirm.deleteRecordMessage", {
        name: record.displayName || record.username || `#${record.userId}`
      }),
      confirmText: t("actions.delete"),
      cancelText: t("actions.cancel"),
      tone: "danger"
    });
    if (!accepted) {
      return;
    }

    setDeletingRecordId(record.id);
    try {
      const response = await fetchWithAuthorization(`/api/admin/score-records/${record.id}`, {
        method: "DELETE"
      });
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as BasicResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(result?.message || t("messages.deleteRecordFailed"));
        return;
      }
      notify.success(t("messages.deleteRecordSuccess"));
      setScoreRecords((prev) => prev.filter((item) => item.id !== record.id));
    } catch {
      notify.error(t("messages.deleteRecordFailed"));
    } finally {
      setDeletingRecordId(null);
    }
  }

  async function loadRewardAssets(items: AdminRewardItem[]) {
    const nextEntries = await Promise.all(
      items
        .filter((item) => item.hasImage && item.imageUrl)
        .map(async (item) => {
          try {
            const response = await authorizedFetch(item.imageUrl as string, {
              method: "GET",
              cache: "no-store"
            });
            if (!response?.ok) {
              return null;
            }
            const blob = await response.blob();
            return [item.id, URL.createObjectURL(blob)] as const;
          } catch {
            return null;
          }
        })
    );

    const nextUrls: Record<number, string> = {};
    nextEntries.forEach((entry) => {
      if (entry) {
        nextUrls[entry[0]] = entry[1];
      }
    });
    setRewardAssetUrls(nextUrls);
  }

  function populateRuleForm(rule: AdminScoreRule) {
    setEditingRuleId(rule.id);
    setRuleForm({
      name: rule.name,
      scoreDelta: rule.scoreDelta,
      scopeType: rule.scopeType,
      clubId: rule.clubId,
      status: rule.status
    });
  }

  function populateRewardForm(reward: AdminRewardItem) {
    clearRewardImageSelection();
    setEditingRewardId(reward.id);
    setRewardForm({
      name: reward.name,
      scoreCost: reward.scoreCost,
      stock: reward.stock,
      visibilityScope: reward.visibilityScope,
      clubIds: reward.clubIds,
      status: reward.status
    });
  }

  function resetRuleForm() {
    setEditingRuleId(null);
    setRuleForm(EMPTY_RULE_FORM);
  }

  function resetRewardForm() {
    setEditingRewardId(null);
    setRewardForm(EMPTY_REWARD_FORM);
    clearRewardImageSelection();
  }

  function clearRewardImageSelection() {
    setRewardImageFile(null);
    setRewardImagePreviewUrl((current) => {
      if (current.startsWith("blob:")) {
        URL.revokeObjectURL(current);
      }
      return "";
    });
  }

  function toggleRewardTargetClub(clubId: number) {
    setRewardForm((prev) => {
      const nextClubIds = prev.clubIds.includes(clubId)
        ? prev.clubIds.filter((id) => id !== clubId)
        : [...prev.clubIds, clubId];
      return {
        ...prev,
        clubIds: nextClubIds
      };
    });
  }

  function handleRewardImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.currentTarget.value = "";
    if (!file) {
      return;
    }
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      notify.warning(t("validation.rewardImageType"));
      return;
    }
    if (file.size > IMAGE_MAX_SIZE_BYTES) {
      notify.warning(t("validation.rewardImageSize"));
      return;
    }
    setRewardImageFile(file);
    setRewardImagePreviewUrl((current) => {
      if (current.startsWith("blob:")) {
        URL.revokeObjectURL(current);
      }
      return URL.createObjectURL(file);
    });
  }

  function buildRulePayload() {
    const name = ruleForm.name.trim();
    if (!name) {
      notify.warning(t("validation.ruleNameRequired"));
      return null;
    }
    if (ruleForm.scopeType === "CLUB" && (!ruleForm.clubId || ruleForm.clubId <= 0)) {
      notify.warning(t("validation.ruleClubRequired"));
      return null;
    }
    return {
      name,
      scoreDelta: ruleForm.scoreDelta,
      scopeType: ruleForm.scopeType,
      clubId: ruleForm.scopeType === "CLUB" ? ruleForm.clubId : null,
      status: ruleForm.status
    } satisfies AdminScoreRuleMutationPayload;
  }

  function buildRewardPayload() {
    const name = rewardForm.name.trim();
    if (!name) {
      notify.warning(t("validation.rewardNameRequired"));
      return null;
    }
    if (rewardForm.scoreCost < 0) {
      notify.warning(t("validation.scoreCostInvalid"));
      return null;
    }
    if (rewardForm.stock < 0) {
      notify.warning(t("validation.stockInvalid"));
      return null;
    }
    if (editingRewardId == null && rewardForm.visibilityScope === "UNASSIGNED") {
      notify.warning(t("validation.rewardUnassignedCreateBlocked"));
      return null;
    }
    if (rewardForm.visibilityScope === "CLUB" && rewardForm.clubIds.length === 0) {
      notify.warning(t("validation.rewardClubRequired"));
      return null;
    }
    return {
      name,
      scoreCost: rewardForm.scoreCost,
      stock: rewardForm.stock,
      visibilityScope: rewardForm.visibilityScope,
      clubIds: rewardForm.visibilityScope === "CLUB" ? rewardForm.clubIds : [],
      status: rewardForm.status
    } satisfies AdminRewardMutationPayload;
  }

  function resetRecordForm() {
    setRecordForm(EMPTY_RECORD_FORM);
    setClubStudents([]);
  }

  function buildRecordPayload() {
    if (!recordForm.clubId || recordForm.clubId <= 0) {
      notify.warning(t("validation.recordClubRequired"));
      return null;
    }
    if (!recordForm.userId || recordForm.userId <= 0) {
      notify.warning(t("validation.recordStudentRequired"));
      return null;
    }
    if (!recordForm.scoreDelta) {
      notify.warning(t("validation.recordDeltaRequired"));
      return null;
    }
    const rule = recordForm.ruleId == null ? null : rules.find((item) => item.id === recordForm.ruleId) || null;
    const reason = recordForm.reason.trim() || (rule ? `Applied rule: ${rule.name}` : "Manual score adjustment");
    return {
      clubId: recordForm.clubId,
      userId: recordForm.userId,
      ruleId: recordForm.ruleId,
      scoreDelta: recordForm.scoreDelta,
      reason
    } satisfies AdminScoreRecordMutationPayload;
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

function toInteger(value: string) {
  const next = Number.parseInt(value, 10);
  return Number.isFinite(next) ? next : 0;
}

function toNonNegativeInteger(value: string) {
  return Math.max(0, toInteger(value));
}

function formatSigned(value: number) {
  return value > 0 ? `+${value}` : String(value);
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

function resolveStudentName(order: AdminRewardOrder) {
  return order.displayName || "";
}

function toRewardVisibilityScope(value: string): RewardVisibilityScope {
  if (value === "CLUB") {
    return "CLUB";
  }
  if (value === "UNASSIGNED") {
    return "UNASSIGNED";
  }
  return "GLOBAL";
}
