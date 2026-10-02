"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { fetchManagerClubAppWorkspaceRequest, saveManagerClubAppWorkspaceRequest } from "../../lib/manager/clubClient";
import {
  ManagerClubAppWorkspace,
  ManagerClubAppWorkspaceGroup,
  ManagerClubAppWorkspaceMaterial,
  ManagerClubAppWorkspacePayload,
  ManagerClubAppWorkspaceProject
} from "../../lib/manager/appWorkspaceTypes";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import { PRIMARY_APP_CLUB_NAME } from "../../lib/club/appIdentity";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherAppWorkspace.module.css";

type WorkspaceResponse = {
  success?: boolean;
  data?: ManagerClubAppWorkspace;
  message?: string;
};

type EditorMaterial = ManagerClubAppWorkspaceMaterial & {
  key: string;
};

type EditorProject = Omit<ManagerClubAppWorkspaceProject, "materials"> & {
  key: string;
  materials: EditorMaterial[];
};

type EditorGroup = Omit<ManagerClubAppWorkspaceGroup, "projects"> & {
  key: string;
  projects: EditorProject[];
};

const MATERIAL_SECTION_KEYS = ["OVERVIEW", "REPORT", "ATTACHMENTS", "IMAGES", "POSTER", "VIDEOS", "OTHER"] as const;

export function TeacherClubAppWorkspacePage({ clubId }: { clubId: number }) {
  const t = useT("teacherAppWorkspace");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(t("header.defaultName"));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [clubName, setClubName] = useState("");
  const [groups, setGroups] = useState<EditorGroup[]>([]);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadData();
  }, [clubId, portal.authorized]);

  const projectCount = useMemo(
    () => groups.reduce((total, group) => total + group.projects.length, 0),
    [groups]
  );

  const materialCount = useMemo(
    () =>
      groups.reduce(
        (total, group) =>
          total + group.projects.reduce((projectTotal, project) => projectTotal + project.materials.length, 0),
        0
      ),
    [groups]
  );

  const sectionOptions = useMemo(
    () =>
      MATERIAL_SECTION_KEYS.map((value) => ({
        value,
        label: t(`sections.${value.toLowerCase()}`)
      })),
    [t]
  );

  if (portal.checking) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{t("states.checking")}</p>
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
      subtitle={clubName || t("header.subtitle")}
      profileName={portal.profileName}
      profileHint={t("header.profileHint")}
      avatarUrl={portal.avatarUrl}
      profileLoading={!portal.managerInfo}
      onProfileClick={() => router.push("/club-admin/profile")}
      logoutLabel={t("header.logout")}
      logoutPendingLabel={t("header.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button
            type="button"
            className={styles.backButton}
            onClick={() => router.push(`/club-admin/clubs/${clubId}`, { scroll: true })}
          >
            <i className="fas fa-arrow-left" />
            {t("actions.back")}
          </button>
          <div className={styles.inlineActions}>
            <button type="button" className={styles.secondaryButton} onClick={handleAddGroup}>
              <i className="fas fa-layer-group" />
              {t("actions.addGroup")}
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => {
                void handleSave();
              }}
              disabled={saving}
            >
              <i className="fas fa-floppy-disk" />
              {saving ? t("actions.saving") : t("actions.save")}
            </button>
          </div>
        </div>

        <article className={styles.hero}>
          <div>
            <h2 className={styles.heroTitle}>{clubName || PRIMARY_APP_CLUB_NAME}</h2>
            <div className={styles.heroMeta}>
              <span className={styles.heroPill}>
                <i className="fas fa-layer-group" />
                {t("summary.groups", { count: groups.length })}
              </span>
              <span className={styles.heroPill}>
                <i className="fas fa-diagram-project" />
                {t("summary.projects", { count: projectCount })}
              </span>
              <span className={styles.heroPill}>
                <i className="fas fa-paperclip" />
                {t("summary.materials", { count: materialCount })}
              </span>
            </div>
          </div>
        </article>

        {loading ? (
          <article className={styles.emptyCard}>
            <p>{t("states.loading")}</p>
          </article>
        ) : groups.length ? (
          groups.map((group, groupIndex) => (
            <article key={group.key} className={styles.groupCard}>
              <div className={styles.groupHead}>
                <div className={styles.groupFields}>
                  <label className={styles.field}>
                    <span>{t("fields.groupTitle")}</span>
                    <input
                      value={group.title}
                      onChange={(event) => updateGroup(group.key, { title: event.target.value })}
                    />
                  </label>
                  <label className={styles.field}>
                    <span>{t("fields.groupSubtitle")}</span>
                    <input
                      value={group.subtitle}
                      onChange={(event) => updateGroup(group.key, { subtitle: event.target.value })}
                    />
                  </label>
                </div>
                <div className={styles.groupActions}>
                  <label className={styles.toggle}>
                    <input
                      type="checkbox"
                      checked={group.enabled}
                      onChange={(event) => updateGroup(group.key, { enabled: event.target.checked })}
                    />
                    <span>{group.enabled ? t("actions.visible") : t("actions.hidden")}</span>
                  </label>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => moveGroup(groupIndex, -1)}
                    disabled={groupIndex === 0}
                    aria-label={t("actions.moveUp")}
                  >
                    <i className="fas fa-arrow-up" />
                  </button>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => moveGroup(groupIndex, 1)}
                    disabled={groupIndex === groups.length - 1}
                    aria-label={t("actions.moveDown")}
                  >
                    <i className="fas fa-arrow-down" />
                  </button>
                  <button
                    type="button"
                    className={styles.dangerButton}
                    onClick={() => removeGroup(group.key)}
                  >
                    <i className="fas fa-trash" />
                    {t("actions.deleteGroup")}
                  </button>
                </div>
              </div>

              <div className={styles.projects}>
                {group.projects.map((project, projectIndex) => (
                  <article key={project.key} className={styles.projectCard}>
                    <div className={styles.projectCover}>
                      {project.coverUrl.trim() ? (
                        <img src={project.coverUrl.trim()} alt={project.title || t("preview.alt")} />
                      ) : (
                        <div className={styles.coverPlaceholder}>
                          <span>{t("preview.empty")}</span>
                        </div>
                      )}
                    </div>

                    <div className={styles.projectBody}>
                      <div className={styles.projectFields}>
                        <label className={styles.field}>
                          <span>{t("fields.projectTitle")}</span>
                          <input
                            value={project.title}
                            onChange={(event) =>
                              updateProject(group.key, project.key, { title: event.target.value })
                            }
                          />
                        </label>
                        <label className={styles.field}>
                          <span>{t("fields.projectSubtitle")}</span>
                          <input
                            value={project.subtitle}
                            onChange={(event) =>
                              updateProject(group.key, project.key, { subtitle: event.target.value })
                            }
                          />
                        </label>
                        <label className={styles.field}>
                          <span>{t("fields.projectKey")}</span>
                          <input
                            value={project.projectKey}
                            readOnly={project.id != null && project.projectKey.trim().length > 0}
                            placeholder={t("fields.projectKeyPlaceholder")}
                            onChange={(event) =>
                              updateProject(group.key, project.key, { projectKey: event.target.value })
                            }
                          />
                        </label>
                        <label className={`${styles.field} ${styles.fieldWide}`}>
                          <span>{t("fields.coverUrl")}</span>
                          <input
                            value={project.coverUrl}
                            onChange={(event) =>
                              updateProject(group.key, project.key, { coverUrl: event.target.value })
                            }
                          />
                        </label>
                      </div>

                      <label className={styles.field}>
                        <span>{t("fields.summary")}</span>
                        <textarea
                          value={project.summary}
                          onChange={(event) =>
                            updateProject(group.key, project.key, { summary: event.target.value })
                          }
                        />
                      </label>

                      <div className={styles.projectActions}>
                        <label className={`${styles.toggle} ${styles.compactToggle}`}>
                          <input
                            type="checkbox"
                            checked={project.enabled}
                            onChange={(event) =>
                              updateProject(group.key, project.key, { enabled: event.target.checked })
                            }
                          />
                          <span>{project.enabled ? t("actions.visible") : t("actions.hidden")}</span>
                        </label>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          disabled={!project.projectKey.trim()}
                          onClick={() =>
                            router.push(
                              `/club-admin/clubs/${clubId}/app-workspace/${encodeURIComponent(project.projectKey.trim())}/demo`,
                              { scroll: true }
                            )
                          }
                        >
                          <i className="fas fa-play-circle" />
                          {t("demo.actions.open")}
                        </button>
                        <div className={styles.actionCluster}>
                          <button
                            type="button"
                            className={styles.compactIconButton}
                            onClick={() => moveProject(group.key, projectIndex, -1)}
                            disabled={projectIndex === 0}
                            aria-label={t("actions.moveUp")}
                          >
                            <i className="fas fa-arrow-up" />
                          </button>
                          <button
                            type="button"
                            className={styles.compactIconButton}
                            onClick={() => moveProject(group.key, projectIndex, 1)}
                            disabled={projectIndex === group.projects.length - 1}
                            aria-label={t("actions.moveDown")}
                          >
                            <i className="fas fa-arrow-down" />
                          </button>
                        </div>
                        <button
                          type="button"
                          className={styles.inlineDangerButton}
                          onClick={() => removeProject(group.key, project.key)}
                        >
                          <i className="fas fa-xmark" />
                          {t("actions.deleteProject")}
                        </button>
                      </div>

                      <div className={styles.materialsSection}>
                        <div className={styles.materialsHead}>
                          <strong>{t("fields.materials")}</strong>
                          <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => handleAddMaterial(group.key, project.key)}
                          >
                            <i className="fas fa-paperclip" />
                            {t("actions.addMaterial")}
                          </button>
                        </div>

                        {project.materials.length ? (
                          <div className={styles.materialList}>
                            {project.materials.map((material, materialIndex) => (
                              <article key={material.key} className={styles.materialCard}>
                                <div className={styles.materialMain}>
                                  <div className={styles.materialTopRow}>
                                    <label className={styles.field}>
                                      <span>{t("fields.materialSection")}</span>
                                      <select
                                        value={material.sectionKey}
                                        onChange={(event) =>
                                          updateMaterial(group.key, project.key, material.key, {
                                            sectionKey: event.target.value
                                          })
                                        }
                                      >
                                        {sectionOptions.map((option) => (
                                          <option key={option.value} value={option.value}>
                                            {option.label}
                                          </option>
                                        ))}
                                      </select>
                                    </label>
                                    <label className={styles.field}>
                                      <span>{t("fields.materialTitle")}</span>
                                      <input
                                        value={material.title}
                                        onChange={(event) =>
                                          updateMaterial(group.key, project.key, material.key, {
                                            title: event.target.value
                                          })
                                        }
                                      />
                                    </label>
                                  </div>
                                  <label className={styles.field}>
                                    <span>{t("fields.storagePath")}</span>
                                    <input
                                      value={material.storagePath}
                                      placeholder={t("fields.storagePathPlaceholder")}
                                      onChange={(event) =>
                                        updateMaterial(group.key, project.key, material.key, {
                                          storagePath: event.target.value
                                        })
                                      }
                                    />
                                  </label>
                                </div>

                                <div className={styles.materialActions}>
                                  <label className={`${styles.toggle} ${styles.compactToggle}`}>
                                    <input
                                      type="checkbox"
                                      checked={material.enabled}
                                      onChange={(event) =>
                                        updateMaterial(group.key, project.key, material.key, {
                                          enabled: event.target.checked
                                        })
                                      }
                                    />
                                    <span>{material.enabled ? t("actions.visible") : t("actions.hidden")}</span>
                                  </label>
                                  <div className={styles.actionCluster}>
                                    <button
                                      type="button"
                                      className={styles.compactIconButton}
                                      onClick={() => moveMaterial(group.key, project.key, materialIndex, -1)}
                                      disabled={materialIndex === 0}
                                      aria-label={t("actions.moveUp")}
                                    >
                                      <i className="fas fa-arrow-up" />
                                    </button>
                                    <button
                                      type="button"
                                      className={styles.compactIconButton}
                                      onClick={() => moveMaterial(group.key, project.key, materialIndex, 1)}
                                      disabled={materialIndex === project.materials.length - 1}
                                      aria-label={t("actions.moveDown")}
                                    >
                                      <i className="fas fa-arrow-down" />
                                    </button>
                                  </div>
                                  <button
                                    type="button"
                                    className={styles.inlineDangerButton}
                                    onClick={() => removeMaterial(group.key, project.key, material.key)}
                                  >
                                    <i className="fas fa-xmark" />
                                    {t("actions.deleteMaterial")}
                                  </button>
                                </div>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <div className={styles.emptyInline}>{t("states.emptyMaterials")}</div>
                        )}
                      </div>
                    </div>
                  </article>
                ))}

                <button
                  type="button"
                  className={styles.addProjectButton}
                  onClick={() => handleAddProject(group.key)}
                >
                  <i className="fas fa-plus" />
                  {t("actions.addProject")}
                </button>
              </div>
            </article>
          ))
        ) : (
          <article className={styles.emptyCard}>
            <button type="button" className={styles.primaryButton} onClick={handleAddGroup}>
              <i className="fas fa-layer-group" />
              {t("actions.addGroup")}
            </button>
          </article>
        )}
      </section>
    </PortalShell>
  );

  async function loadData() {
    setLoading(true);
    try {
      const response = await fetchManagerClubAppWorkspaceRequest(clubId);
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as WorkspaceResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.loadFailed"));
        router.push(`/club-admin/clubs/${clubId}`, { scroll: true });
        return;
      }

      if (result.data.clubName !== PRIMARY_APP_CLUB_NAME) {
        notify.warning(t("messages.unavailable"));
        router.push(`/club-admin/clubs/${clubId}`, { scroll: true });
        return;
      }

      setClubName(result.data.clubName);
      setGroups(result.data.groups.map((group) => toEditorGroup(group)));
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    if (saving) {
      return;
    }

    setSaving(true);
    try {
      const response = await saveManagerClubAppWorkspaceRequest(clubId, toPayload(groups));
      if (!response) {
        notify.warning(t("messages.loginRequired"));
        router.replace("/login");
        return;
      }
      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as WorkspaceResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        notify.error(result?.message || t("messages.saveFailed"));
        return;
      }

      setClubName(result.data.clubName);
      setGroups(result.data.groups.map((group) => toEditorGroup(group)));
      notify.success(t("messages.saved"));
    } finally {
      setSaving(false);
    }
  }

  function handleAddGroup() {
    setGroups((current) => [
      ...current,
      {
        key: createEditorKey("group"),
        id: null,
        title: "",
        subtitle: "",
        sortOrder: current.length + 1,
        enabled: true,
        projects: []
      }
    ]);
  }

  function handleAddProject(groupKey: string) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: [
                ...group.projects,
                {
                  key: createEditorKey("project"),
                  id: null,
                  projectKey: "",
                  title: "",
                  subtitle: "",
                  summary: "",
                  coverUrl: "",
                  sortOrder: group.projects.length + 1,
                  enabled: true,
                  materials: []
                }
              ]
            }
          : group
      )
    );
  }

  function handleAddMaterial(groupKey: string, projectKey: string) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: group.projects.map((project) =>
                project.key === projectKey
                  ? {
                      ...project,
                      materials: [
                        ...project.materials,
                        {
                          key: createEditorKey("material"),
                          id: null,
                          sectionKey: "OVERVIEW",
                          title: "",
                          storagePath: "",
                          sortOrder: project.materials.length + 1,
                          enabled: true
                        }
                      ]
                    }
                  : project
              )
            }
          : group
      )
    );
  }

  function updateGroup(groupKey: string, patch: Partial<EditorGroup>) {
    setGroups((current) =>
      current.map((group) => (group.key === groupKey ? { ...group, ...patch } : group))
    );
  }

  function updateProject(groupKey: string, projectKey: string, patch: Partial<EditorProject>) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: group.projects.map((project) =>
                project.key === projectKey ? { ...project, ...patch } : project
              )
            }
          : group
      )
    );
  }

  function updateMaterial(
    groupKey: string,
    projectKey: string,
    materialKey: string,
    patch: Partial<EditorMaterial>
  ) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: group.projects.map((project) =>
                project.key === projectKey
                  ? {
                      ...project,
                      materials: project.materials.map((material) =>
                        material.key === materialKey ? { ...material, ...patch } : material
                      )
                    }
                  : project
              )
            }
          : group
      )
    );
  }

  function moveGroup(index: number, delta: -1 | 1) {
    setGroups((current) => reorder(current, index, delta));
  }

  function moveProject(groupKey: string, index: number, delta: -1 | 1) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey ? { ...group, projects: reorder(group.projects, index, delta) } : group
      )
    );
  }

  function moveMaterial(groupKey: string, projectKey: string, index: number, delta: -1 | 1) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: group.projects.map((project) =>
                project.key === projectKey
                  ? { ...project, materials: reorder(project.materials, index, delta) }
                  : project
              )
            }
          : group
      )
    );
  }

  function removeGroup(groupKey: string) {
    setGroups((current) => current.filter((group) => group.key !== groupKey));
  }

  function removeProject(groupKey: string, projectKey: string) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? { ...group, projects: group.projects.filter((project) => project.key !== projectKey) }
          : group
      )
    );
  }

  function removeMaterial(groupKey: string, projectKey: string, materialKey: string) {
    setGroups((current) =>
      current.map((group) =>
        group.key === groupKey
          ? {
              ...group,
              projects: group.projects.map((project) =>
                project.key === projectKey
                  ? {
                      ...project,
                      materials: project.materials.filter((material) => material.key !== materialKey)
                    }
                  : project
              )
            }
          : group
      )
    );
  }
}

function toEditorGroup(group: ManagerClubAppWorkspaceGroup): EditorGroup {
  return {
    ...group,
    key: createEditorKey("group", group.id),
    projects: group.projects.map((project) => ({
      ...project,
      key: createEditorKey("project", project.id),
      materials: project.materials.map((material) => ({
        ...material,
        key: createEditorKey("material", material.id)
      }))
    }))
  };
}

function toPayload(groups: EditorGroup[]): ManagerClubAppWorkspacePayload {
  return {
    groups: groups.map((group) => ({
      ...(group.id ? { id: group.id } : {}),
      title: group.title.trim(),
      subtitle: group.subtitle.trim(),
      enabled: group.enabled,
      projects: group.projects.map((project) => ({
        ...(project.id ? { id: project.id } : {}),
        projectKey: project.projectKey.trim(),
        title: project.title.trim(),
        subtitle: project.subtitle.trim(),
        summary: project.summary.trim(),
        coverUrl: project.coverUrl.trim(),
        enabled: project.enabled,
        materials: project.materials.map((material) => ({
          ...(material.id ? { id: material.id } : {}),
          sectionKey: material.sectionKey,
          title: material.title.trim(),
          storagePath: material.storagePath.trim(),
          enabled: material.enabled
        }))
      }))
    }))
  };
}

function reorder<T>(items: T[], index: number, delta: -1 | 1) {
  const targetIndex = index + delta;
  if (targetIndex < 0 || targetIndex >= items.length) {
    return items;
  }
  const next = [...items];
  const [moved] = next.splice(index, 1);
  next.splice(targetIndex, 0, moved);
  return next;
}

function createEditorKey(prefix: string, id?: number | null) {
  if (id != null) {
    return `${prefix}-${id}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
