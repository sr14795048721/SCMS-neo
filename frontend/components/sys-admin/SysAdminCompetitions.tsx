"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { logoutAdminSession } from "../../lib/admin-system/session";
import {
  confirmCompetitionLeadRequest,
  createCompetitionRequest,
  createCompetitionSourceRequest,
  deleteCompetitionRequest,
  deleteCompetitionSourceRequest,
  fetchCompetitionLeadsRequest,
  fetchCompetitionSourcesRequest,
  fetchCompetitionsRequest,
  ignoreCompetitionLeadRequest,
  importCompetitionArticleRequest,
  scanCompetitionSourceRequest,
  updateCompetitionRequest,
  updateCompetitionSourceRequest
} from "../../lib/competition/client";
import {
  CompetitionItem,
  CompetitionLead,
  CompetitionLeadStatus,
  CompetitionPayload,
  CompetitionSource,
  CompetitionSourcePayload,
  CompetitionSourceType,
  CompetitionStatus
} from "../../lib/competition/types";
import styles from "../../styles/sysAdmin.module.css";
import css from "../../styles/competition.module.css";

type ResponseEnvelope = {
  success?: boolean;
  message?: string;
};

type LeadFilter = "ALL" | CompetitionLeadStatus;

const SOURCE_TYPES: { value: CompetitionSourceType; labelKey: string }[] = [
  { value: "OFFICIAL_WEBSITE", labelKey: "sources.typeWebsite" },
  { value: "WECHAT_OFFICIAL", labelKey: "sources.typeWechat" }
];

const STATUS_OPTIONS: { value: CompetitionStatus; labelKey: string }[] = [
  { value: "UPCOMING", labelKey: "competitions.statusUpcoming" },
  { value: "ONGOING", labelKey: "competitions.statusOngoing" },
  { value: "ENDED", labelKey: "competitions.statusEnded" }
];

export function SysAdminCompetitions() {
  const t = useT("adminCompetitions");
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");

  const [sources, setSources] = useState<CompetitionSource[]>([]);
  const [leads, setLeads] = useState<CompetitionLead[]>([]);
  const [competitions, setCompetitions] = useState<CompetitionItem[]>([]);
  const [loading, setLoading] = useState(true);

  const [leadFilter, setLeadFilter] = useState<LeadFilter>("ALL");

  const [sourceFormOpen, setSourceFormOpen] = useState(false);
  const [editingSourceId, setEditingSourceId] = useState<number | null>(null);
  const [sourceName, setSourceName] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [sourceType, setSourceType] = useState<CompetitionSourceType>("OFFICIAL_WEBSITE");
  const [sourceWechatName, setSourceWechatName] = useState("");
  const [sourceEnabled, setSourceEnabled] = useState(true);
  const [sourceScanEnabled, setSourceScanEnabled] = useState(false);

  const [competitionFormOpen, setCompetitionFormOpen] = useState(false);
  const [editingCompetitionId, setEditingCompetitionId] = useState<number | null>(null);
  const [confirmingLeadId, setConfirmingLeadId] = useState<number | null>(null);
  const [cName, setCName] = useState("");
  const [cCategory, setCCategory] = useState("");
  const [cScope, setCScope] = useState("");
  const [cApplyDeadline, setCApplyDeadline] = useState("");
  const [cStartDate, setCStartDate] = useState("");
  const [cEndDate, setCEndDate] = useState("");
  const [cUrl, setCUrl] = useState("");
  const [cStatus, setCStatus] = useState<CompetitionStatus>("UPCOMING");
  const [cNote, setCNote] = useState("");

  const [importUrl, setImportUrl] = useState("");
  const [importTitle, setImportTitle] = useState("");
  const [importContent, setImportContent] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    if (authorized) {
      void loadAll();
    }
  }, [authorized]);

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

  const filteredLeads = leadFilter === "ALL" ? leads : leads.filter((lead) => lead.status === leadFilter);

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <h1>
            <i className="fas fa-trophy" />
            {t("header.title")}
          </h1>
          <button type="button" className={styles.logoutButton} onClick={() => void handleLogout()}>
            <i className="fas fa-sign-out-alt" />
            {t("header.logout")}
          </button>
        </div>
      </header>

      <div className={styles.container}>
        <div className={styles.stack}>
          {/* sources section */}
          <section className={css.section}>
            <div className={css.sectionHead}>
              <h2>
                <i className="fas fa-rss" />
                {t("sources.title")}
              </h2>
              <div className={css.headActions}>
                <button
                  type="button"
                  className={css.primaryButton}
                  onClick={openSourceCreate}
                  disabled={submitting}
                >
                  <i className="fas fa-plus" />
                  {t("sources.add")}
                </button>
              </div>
            </div>

            {sourceFormOpen ? (
              <form className={css.form} onSubmit={(event) => void handleSourceSubmit(event)}>
                <div className={css.formRow}>
                  <label className={css.formLabel}>
                    {t("sources.form.name")}
                    <input
                      className={css.formInput}
                      value={sourceName}
                      onChange={(event) => setSourceName(event.target.value)}
                      required
                    />
                  </label>
                  <label className={css.formLabel}>
                    {t("sources.form.type")}
                    <select
                      className={css.formSelect}
                      value={sourceType}
                      onChange={(event) => setSourceType(event.target.value as CompetitionSourceType)}
                    >
                      {SOURCE_TYPES.map((option) => (
                        <option key={option.value} value={option.value}>
                          {t(option.labelKey)}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <label className={css.formLabel}>
                  {t("sources.form.url")}
                  <input
                    className={css.formInput}
                    value={sourceUrl}
                    onChange={(event) => setSourceUrl(event.target.value)}
                    placeholder="https://..."
                  />
                </label>
                <label className={css.formLabel}>
                  {t("sources.form.wechatName")}
                  <input
                    className={css.formInput}
                    value={sourceWechatName}
                    onChange={(event) => setSourceWechatName(event.target.value)}
                    placeholder={t("sources.form.wechatNamePlaceholder")}
                  />
                </label>
                <div className={css.formRow}>
                  <label className={css.formCheckRow}>
                    <input
                      type="checkbox"
                      checked={sourceEnabled}
                      onChange={(event) => setSourceEnabled(event.target.checked)}
                    />
                    {t("sources.form.enabled")}
                  </label>
                  <label className={css.formCheckRow}>
                    <input
                      type="checkbox"
                      checked={sourceScanEnabled}
                      onChange={(event) => setSourceScanEnabled(event.target.checked)}
                    />
                    {t("sources.form.scanEnabled")}
                  </label>
                </div>
                <div className={css.formActions}>
                  <button
                    type="button"
                    className={css.secondaryButton}
                    onClick={() => setSourceFormOpen(false)}
                    disabled={submitting}
                  >
                    {t("actions.cancel")}
                  </button>
                  <button type="submit" className={css.primaryButton} disabled={submitting}>
                    {submitting ? t("actions.saving") : t("actions.save")}
                  </button>
                </div>
              </form>
            ) : null}

            {loading ? (
              <p className={css.empty}>{t("common.loading")}</p>
            ) : sources.length === 0 ? (
              <p className={css.empty}>{t("sources.empty")}</p>
            ) : (
              <div className={css.list}>
                {sources.map((source) => (
                  <div key={source.id} className={css.item}>
                    <div className={css.itemHead}>
                      <p className={css.itemTitle}>{source.name}</p>
                      <span className={`${css.tag} ${source.sourceType === "OFFICIAL_WEBSITE" ? css.tagBlue : css.tagGreen}`}>
                        {source.sourceType === "OFFICIAL_WEBSITE" ? t("sources.typeWebsite") : t("sources.typeWechat")}
                      </span>
                      <span className={`${css.tag} ${source.enabled ? css.tagGreen : css.tagGray}`}>
                        {source.enabled ? t("sources.enabled") : t("sources.disabled")}
                      </span>
                      {source.scanEnabled ? (
                        <span className={`${css.tag} ${css.tagOrange}`}>{t("sources.scanOn")}</span>
                      ) : null}
                    </div>
                    <div className={css.itemMeta}>
                      {source.url ? (
                        <a
                          className={css.link}
                          href={source.url}
                          target="_blank"
                          rel="noreferrer"
                          onClick={(event) => event.stopPropagation()}
                        >
                          {source.url}
                        </a>
                      ) : (
                        <span>{source.wechatName || "-"}</span>
                      )}
                    </div>
                    <div className={css.itemActions}>
                      {source.sourceType === "OFFICIAL_WEBSITE" ? (
                        <button
                          type="button"
                          className={css.actionButton}
                          onClick={() => void handleScanSource(source)}
                          disabled={busyId === source.id}
                        >
                          <i className="fas fa-sync-alt" />
                          {busyId === source.id ? t("sources.scanning") : t("sources.scan")}
                        </button>
                      ) : null}
                      <button type="button" className={css.actionButton} onClick={() => openSourceEdit(source)}>
                        <i className="fas fa-pen" />
                        {t("actions.edit")}
                      </button>
                      <button
                        type="button"
                        className={css.dangerButton}
                        onClick={() => void handleDeleteSource(source)}
                        disabled={busyId === source.id}
                      >
                        <i className="fas fa-trash" />
                        {t("actions.delete")}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* leads section */}
          <section className={css.section}>
            <div className={css.sectionHead}>
              <h2>
                <i className="fas fa-inbox" />
                {t("leads.title")}
              </h2>
              <div className={css.filters}>
                {(["ALL", "PENDING", "CONFIRMED", "IGNORED"] as LeadFilter[]).map((filter) => (
                  <button
                    key={filter}
                    type="button"
                    className={`${css.filterButton} ${leadFilter === filter ? css.filterButtonActive : ""}`}
                    onClick={() => setLeadFilter(filter)}
                  >
                    {filterLabel(filter)}
                  </button>
                ))}
              </div>
            </div>

            {loading ? (
              <p className={css.empty}>{t("common.loading")}</p>
            ) : filteredLeads.length === 0 ? (
              <p className={css.empty}>{t("leads.empty")}</p>
            ) : (
              <div className={css.list}>
                {filteredLeads.map((lead) => (
                  <div key={lead.id} className={css.item}>
                    <div className={css.itemHead}>
                      <p className={css.itemTitle}>{lead.title}</p>
                      <span className={`${css.tag} ${leadStatusTag(lead.status)}`}>{leadStatusLabel(lead.status)}</span>
                    </div>
                    <div className={css.itemMeta}>
                      {lead.sourceName ? (
                        <span>
                          <i className="fas fa-rss" /> {lead.sourceName}
                        </span>
                      ) : null}
                      {lead.detectedAt ? <span>{formatDate(lead.detectedAt)}</span> : null}
                      {lead.url ? (
                        <a className={css.link} href={lead.url} target="_blank" rel="noreferrer">
                          {t("leads.original")}
                        </a>
                      ) : null}
                    </div>
                    {lead.snippet ? <p className={css.itemBody}>{lead.snippet}</p> : null}
                    {lead.status === "PENDING" ? (
                      <div className={css.itemActions}>
                        <button
                          type="button"
                          className={css.primaryButton}
                          onClick={() => openLeadConfirm(lead)}
                          disabled={busyId === lead.id}
                        >
                          <i className="fas fa-check" />
                          {t("leads.confirm")}
                        </button>
                        <button
                          type="button"
                          className={css.dangerButton}
                          onClick={() => void handleIgnoreLead(lead)}
                          disabled={busyId === lead.id}
                        >
                          <i className="fas fa-ban" />
                          {t("leads.ignore")}
                        </button>
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* competitions section */}
          <section className={css.section}>
            <div className={css.sectionHead}>
              <h2>
                <i className="fas fa-trophy" />
                {t("competitions.title")}
              </h2>
              <div className={css.headActions}>
                <button
                  type="button"
                  className={css.primaryButton}
                  onClick={openCompetitionCreate}
                  disabled={submitting}
                >
                  <i className="fas fa-plus" />
                  {t("competitions.add")}
                </button>
              </div>
            </div>

            {competitionFormOpen ? (
              <form className={css.form} onSubmit={(event) => void handleCompetitionSubmit(event)}>
                <div className={css.formRow}>
                  <label className={css.formLabel}>
                    {t("competitions.form.name")}
                    <input
                      className={css.formInput}
                      value={cName}
                      onChange={(event) => setCName(event.target.value)}
                      required
                    />
                  </label>
                  <label className={css.formLabel}>
                    {t("competitions.form.status")}
                    <select
                      className={css.formSelect}
                      value={cStatus}
                      onChange={(event) => setCStatus(event.target.value as CompetitionStatus)}
                    >
                      {STATUS_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {t(option.labelKey)}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <div className={css.formRow}>
                  <label className={css.formLabel}>
                    {t("competitions.form.category")}
                    <input
                      className={css.formInput}
                      value={cCategory}
                      onChange={(event) => setCCategory(event.target.value)}
                    />
                  </label>
                  <label className={css.formLabel}>
                    {t("competitions.form.scope")}
                    <input
                      className={css.formInput}
                      value={cScope}
                      onChange={(event) => setCScope(event.target.value)}
                    />
                  </label>
                </div>
                <div className={css.formRow}>
                  <label className={css.formLabel}>
                    {t("competitions.form.applyDeadline")}
                    <input
                      className={css.formInput}
                      type="date"
                      value={cApplyDeadline}
                      onChange={(event) => setCApplyDeadline(event.target.value)}
                    />
                  </label>
                  <label className={css.formLabel}>
                    {t("competitions.form.startDate")}
                    <input
                      className={css.formInput}
                      type="date"
                      value={cStartDate}
                      onChange={(event) => setCStartDate(event.target.value)}
                    />
                  </label>
                  <label className={css.formLabel}>
                    {t("competitions.form.endDate")}
                    <input
                      className={css.formInput}
                      type="date"
                      value={cEndDate}
                      onChange={(event) => setCEndDate(event.target.value)}
                    />
                  </label>
                </div>
                <label className={css.formLabel}>
                  {t("competitions.form.url")}
                  <input
                    className={css.formInput}
                    value={cUrl}
                    onChange={(event) => setCUrl(event.target.value)}
                    placeholder="https://..."
                  />
                </label>
                <label className={css.formLabel}>
                  {t("competitions.form.note")}
                  <textarea
                    className={css.formTextarea}
                    value={cNote}
                    onChange={(event) => setCNote(event.target.value)}
                    rows={3}
                  />
                </label>
                <div className={css.formActions}>
                  <button
                    type="button"
                    className={css.secondaryButton}
                    onClick={() => setCompetitionFormOpen(false)}
                    disabled={submitting}
                  >
                    {t("actions.cancel")}
                  </button>
                  <button type="submit" className={css.primaryButton} disabled={submitting}>
                    {submitting ? t("actions.saving") : t("actions.save")}
                  </button>
                </div>
              </form>
            ) : null}

            {loading ? (
              <p className={css.empty}>{t("common.loading")}</p>
            ) : competitions.length === 0 ? (
              <p className={css.empty}>{t("competitions.empty")}</p>
            ) : (
              <div className={css.list}>
                {competitions.map((competition) => (
                  <div key={competition.id} className={css.item}>
                    <div className={css.itemHead}>
                      <p className={css.itemTitle}>{competition.name}</p>
                      <span className={`${css.tag} ${competitionStatusTag(competition.status)}`}>
                        {competitionStatusLabel(competition.status)}
                      </span>
                      {competition.category ? (
                        <span className={`${css.tag} ${css.tagBlue}`}>{competition.category}</span>
                      ) : null}
                    </div>
                    <div className={css.itemMeta}>
                      {competition.participantScope ? <span>{competition.participantScope}</span> : null}
                      {competition.sourceName ? <span>{competition.sourceName}</span> : null}
                      {competition.applyDeadline ? (
                        <span>{t("competitions.meta.deadline")} {formatDate(competition.applyDeadline)}</span>
                      ) : null}
                      {competition.startDate ? (
                        <span>{t("competitions.meta.start")} {formatDate(competition.startDate)}</span>
                      ) : null}
                      {competition.url ? (
                        <a className={css.link} href={competition.url} target="_blank" rel="noreferrer">
                          {t("competitions.meta.original")}
                        </a>
                      ) : null}
                    </div>
                    {competition.note ? <p className={css.itemBody}>{competition.note}</p> : null}
                    <div className={css.itemActions}>
                      <button
                        type="button"
                        className={css.actionButton}
                        onClick={() => openCompetitionEdit(competition)}
                      >
                        <i className="fas fa-pen" />
                        {t("actions.edit")}
                      </button>
                      <button
                        type="button"
                        className={css.dangerButton}
                        onClick={() => void handleDeleteCompetition(competition)}
                        disabled={busyId === competition.id}
                      >
                        <i className="fas fa-trash" />
                        {t("actions.delete")}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* import section */}
          <section className={css.section}>
            <div className={css.sectionHead}>
              <h2>
                <i className="fas fa-file-import" />
                {t("import.title")}
              </h2>
            </div>
            <p className={css.itemMeta} style={{ marginBottom: 12 }}>
              {t("import.hint")}
            </p>
            <form className={css.form} onSubmit={(event) => void handleImportSubmit(event)}>
              <label className={css.formLabel}>
                {t("import.form.url")}
                <input
                  className={css.formInput}
                  value={importUrl}
                  onChange={(event) => setImportUrl(event.target.value)}
                  placeholder="https://..."
                  required
                />
              </label>
              <label className={css.formLabel}>
                {t("import.form.title")}
                <input
                  className={css.formInput}
                  value={importTitle}
                  onChange={(event) => setImportTitle(event.target.value)}
                />
              </label>
              <label className={css.formLabel}>
                {t("import.form.content")}
                <textarea
                  className={css.formTextarea}
                  value={importContent}
                  onChange={(event) => setImportContent(event.target.value)}
                  rows={6}
                  placeholder={t("import.form.contentPlaceholder")}
                />
              </label>
              <div className={css.formActions}>
                <button type="submit" className={css.primaryButton} disabled={submitting}>
                  {submitting ? t("actions.saving") : t("import.submit")}
                </button>
              </div>
            </form>
          </section>
        </div>
      </div>
    </main>
  );

  // ---------------- data loading ----------------

  async function loadAll() {
    setLoading(true);
    await Promise.all([loadSources(), loadLeads(), loadCompetitions()]);
    setLoading(false);
  }

  async function loadSources() {
    const response = await fetchCompetitionSourcesRequest();
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as
      | { success?: boolean; data?: CompetitionSource[] }
      | null;
    setSources(response.ok && result?.success && Array.isArray(result.data) ? result.data : []);
  }

  async function loadLeads() {
    const response = await fetchCompetitionLeadsRequest();
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as
      | { success?: boolean; data?: CompetitionLead[] }
      | null;
    setLeads(response.ok && result?.success && Array.isArray(result.data) ? result.data : []);
  }

  async function loadCompetitions() {
    const response = await fetchCompetitionsRequest();
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as
      | { success?: boolean; data?: CompetitionItem[] }
      | null;
    setCompetitions(response.ok && result?.success && Array.isArray(result.data) ? result.data : []);
  }

  // ---------------- sources ----------------

  function openSourceCreate() {
    setEditingSourceId(null);
    setSourceName("");
    setSourceUrl("");
    setSourceType("OFFICIAL_WEBSITE");
    setSourceWechatName("");
    setSourceEnabled(true);
    setSourceScanEnabled(false);
    setSourceFormOpen(true);
  }

  function openSourceEdit(source: CompetitionSource) {
    setEditingSourceId(source.id);
    setSourceName(source.name);
    setSourceUrl(source.url || "");
    setSourceType(source.sourceType);
    setSourceWechatName(source.wechatName || "");
    setSourceEnabled(source.enabled);
    setSourceScanEnabled(source.scanEnabled);
    setSourceFormOpen(true);
  }

  async function handleSourceSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) {
      return;
    }
    const payload: CompetitionSourcePayload = {
      name: sourceName.trim(),
      url: sourceUrl.trim() || null,
      sourceType,
      wechatName: sourceWechatName.trim() || null,
      enabled: sourceEnabled,
      scanEnabled: sourceScanEnabled
    };
    if (!payload.name) {
      notify.warning(t("messages.fillRequired"));
      return;
    }

    setSubmitting(true);
    try {
      const response = editingSourceId == null
        ? await createCompetitionSourceRequest(payload)
        : await updateCompetitionSourceRequest(editingSourceId, payload);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ResponseEnvelope | null;
      if (!response.ok || result?.success !== true) {
        notify.error(String(result?.message || t("messages.saveFailed")));
        return;
      }
      notify.success(editingSourceId == null ? t("messages.created") : t("messages.updated"));
      setSourceFormOpen(false);
      await loadSources();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleScanSource(source: CompetitionSource) {
    if (busyId) {
      return;
    }
    setBusyId(source.id);
    try {
      const response = await scanCompetitionSourceRequest(source.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: { created: number }; message?: string }
        | null;
      if (!response.ok || result?.success !== true) {
        notify.error(String(result?.message || t("messages.scanFailed")));
        return;
      }
      notify.success(t("messages.scanned", { count: String(result.data?.created ?? 0) }));
      await loadLeads();
    } finally {
      setBusyId(null);
    }
  }

  async function handleDeleteSource(source: CompetitionSource) {
    if (busyId) {
      return;
    }
    if (!window.confirm(t("messages.deleteSourceConfirm", { name: source.name }))) {
      return;
    }
    setBusyId(source.id);
    try {
      const response = await deleteCompetitionSourceRequest(source.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      if (!response.ok) {
        const result = (await response.json().catch(() => null)) as ResponseEnvelope | null;
        notify.error(String(result?.message || t("messages.deleteFailed")));
        return;
      }
      notify.success(t("messages.deleted"));
      await loadSources();
    } finally {
      setBusyId(null);
    }
  }

  // ---------------- leads ----------------

  function openLeadConfirm(lead: CompetitionLead) {
    setConfirmingLeadId(lead.id);
    setCName(lead.title);
    setCCategory("");
    setCScope("");
    setCApplyDeadline("");
    setCStartDate("");
    setCEndDate("");
    setCUrl(lead.url || "");
    setCStatus("UPCOMING");
    setCNote("");
    setCompetitionFormOpen(true);
  }

  async function handleIgnoreLead(lead: CompetitionLead) {
    if (busyId) {
      return;
    }
    setBusyId(lead.id);
    try {
      const response = await ignoreCompetitionLeadRequest(lead.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      if (!response.ok) {
        notify.error(t("messages.ignoreFailed"));
        return;
      }
      notify.success(t("messages.ignored"));
      await loadLeads();
    } finally {
      setBusyId(null);
    }
  }

  // ---------------- competitions ----------------

  function openCompetitionCreate() {
    setConfirmingLeadId(null);
    setEditingCompetitionId(null);
    resetCompetitionForm();
    setCompetitionFormOpen(true);
  }

  function openCompetitionEdit(competition: CompetitionItem) {
    setConfirmingLeadId(null);
    setEditingCompetitionId(competition.id);
    setCName(competition.name);
    setCCategory(competition.category || "");
    setCScope(competition.participantScope || "");
    setCApplyDeadline(toDateInput(competition.applyDeadline));
    setCStartDate(toDateInput(competition.startDate));
    setCEndDate(toDateInput(competition.endDate));
    setCUrl(competition.url || "");
    setCStatus(competition.status);
    setCNote(competition.note || "");
    setCompetitionFormOpen(true);
  }

  function resetCompetitionForm() {
    setCName("");
    setCCategory("");
    setCScope("");
    setCApplyDeadline("");
    setCStartDate("");
    setCEndDate("");
    setCUrl("");
    setCStatus("UPCOMING");
    setCNote("");
  }

  async function handleCompetitionSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) {
      return;
    }
    const payload: CompetitionPayload = {
      name: cName.trim(),
      category: cCategory.trim() || null,
      participantScope: cScope.trim() || null,
      applyDeadline: toIso(cApplyDeadline),
      startDate: toIso(cStartDate),
      endDate: toIso(cEndDate),
      url: cUrl.trim() || null,
      status: cStatus,
      note: cNote.trim() || null
    };
    if (!payload.name) {
      notify.warning(t("messages.fillRequired"));
      return;
    }

    setSubmitting(true);
    try {
      const response = confirmingLeadId != null
        ? await confirmCompetitionLeadRequest(confirmingLeadId, payload)
        : editingCompetitionId == null
          ? await createCompetitionRequest(payload)
          : await updateCompetitionRequest(editingCompetitionId, payload);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as ResponseEnvelope | null;
      if (!response.ok || result?.success !== true) {
        notify.error(String(result?.message || t("messages.saveFailed")));
        return;
      }
      notify.success(
        confirmingLeadId != null
          ? t("messages.confirmed")
          : editingCompetitionId == null
            ? t("messages.created")
            : t("messages.updated")
      );
      setCompetitionFormOpen(false);
      await Promise.all([loadCompetitions(), loadLeads()]);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteCompetition(competition: CompetitionItem) {
    if (busyId) {
      return;
    }
    if (!window.confirm(t("messages.deleteCompetitionConfirm", { name: competition.name }))) {
      return;
    }
    setBusyId(competition.id);
    try {
      const response = await deleteCompetitionRequest(competition.id);
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      if (!response.ok) {
        notify.error(t("messages.deleteFailed"));
        return;
      }
      notify.success(t("messages.deleted"));
      await loadCompetitions();
    } finally {
      setBusyId(null);
    }
  }

  // ---------------- import ----------------

  async function handleImportSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) {
      return;
    }
    if (!importUrl.trim()) {
      notify.warning(t("messages.fillRequired"));
      return;
    }
    setSubmitting(true);
    try {
      const response = await importCompetitionArticleRequest({
        url: importUrl.trim(),
        title: importTitle.trim() || null,
        content: importContent.trim() || null
      });
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: { created: number }; message?: string }
        | null;
      if (!response.ok || result?.success !== true) {
        notify.error(String(result?.message || t("messages.importFailed")));
        return;
      }
      notify.success(t("messages.imported", { count: String(result.data?.created ?? 0) }));
      setImportUrl("");
      setImportTitle("");
      setImportContent("");
      await loadLeads();
    } finally {
      setSubmitting(false);
    }
  }

  // ---------------- helpers ----------------

  function filterLabel(filter: LeadFilter) {
    if (filter === "ALL") {
      return t("leads.filterAll");
    }
    return leadStatusLabel(filter);
  }

  function leadStatusLabel(status: CompetitionLeadStatus) {
    if (status === "PENDING") {
      return t("leads.statusPending");
    }
    if (status === "CONFIRMED") {
      return t("leads.statusConfirmed");
    }
    return t("leads.statusIgnored");
  }

  function leadStatusTag(status: CompetitionLeadStatus) {
    if (status === "PENDING") {
      return css.tagOrange;
    }
    if (status === "CONFIRMED") {
      return css.tagGreen;
    }
    return css.tagGray;
  }

  function competitionStatusLabel(status: CompetitionStatus) {
    if (status === "UPCOMING") {
      return t("competitions.statusUpcoming");
    }
    if (status === "ONGOING") {
      return t("competitions.statusOngoing");
    }
    return t("competitions.statusEnded");
  }

  function competitionStatusTag(status: CompetitionStatus) {
    if (status === "UPCOMING") {
      return css.tagBlue;
    }
    if (status === "ONGOING") {
      return css.tagGreen;
    }
    return css.tagGray;
  }

  function formatDate(iso: string) {
    return iso.slice(0, 10);
  }

  function toDateInput(iso: string | null) {
    return iso ? iso.slice(0, 10) : "";
  }

  function toIso(dateInput: string): string | null {
    if (!dateInput) {
      return null;
    }
    return new Date(`${dateInput}T00:00:00`).toISOString();
  }

  async function handleLogout() {
    await logoutAdminSession();
    router.replace("/login");
  }

  async function handleAuthStatus(status: number) {
    if (status === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      await logoutAdminSession();
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
}
