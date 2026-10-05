"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { PortalShell } from "../portal/PortalShell";
import { VersionChangelogTimeline } from "../changelog/VersionChangelogTimeline";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { useManagerPortalState } from "../../lib/manager/useManagerPortalState";
import { fetchVersionChangelogsRequest } from "../../lib/version-changelog/client";
import { VersionChangelogItem } from "../../lib/version-changelog/types";
import shellStyles from "../../styles/teacherShell.module.css";
import styles from "../../styles/teacherClubs.module.css";

type ChangelogResponse = {
  success?: boolean;
  data?: VersionChangelogItem[];
  message?: string;
};

export function TeacherVersionChangelogPage() {
  const t = useT("teacherVersionChangelog");
  const tClubs = useT("teacherClubs");
  const notify = useNotify();
  const router = useRouter();
  const portal = useManagerPortalState(tClubs("common.defaultName"));
  const [items, setItems] = useState<VersionChangelogItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!portal.authorized) {
      return;
    }
    void loadChangelogs();
  }, [portal.authorized]);

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
      title={t("header.title")}
      subtitle={t("meta.description")}
      profileName={portal.profileName}
      profileHint={tClubs("common.profileHint")}
      avatarUrl={portal.avatarUrl}
      profileLoading={!portal.managerInfo}
      onProfileClick={() => router.push("/club-admin/profile")}
      logoutLabel={tClubs("common.logout")}
      logoutPendingLabel={tClubs("common.loggingOut")}
      logoutSubmitting={portal.logoutSubmitting}
      onLogout={portal.handleLogout}
    >
      <section className={styles.stack}>
        <div className={styles.topBar}>
          <button type="button" className={styles.backButton} onClick={() => router.push("/club-admin", { scroll: true })}>
            <i className="fas fa-arrow-left" />
            {tClubs("common.back")}
          </button>
        </div>

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <div>
              <h3 className={styles.panelTitle}>
                <i className="fas fa-history" />
                {t("list.title")}
              </h3>
              <p className={styles.panelHint}>{t("list.description")}</p>
            </div>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => void loadChangelogs()}
              disabled={loading}
            >
              <i className="fas fa-rotate-right" />
              {loading ? t("common.loading") : t("common.refresh")}
            </button>
          </div>

          <div className={styles.panelContent}>
            <VersionChangelogTimeline
              items={items}
              loading={loading}
              loadingLabel={t("common.loading")}
              emptyLabel={t("list.empty")}
              timeFallbackLabel={t("common.timeFallback")}
            />
          </div>
        </section>
      </section>
    </PortalShell>
  );

  async function loadChangelogs() {
    setLoading(true);
    try {
      const response = await fetchVersionChangelogsRequest();
      if (!response) {
        notify.warning(t("common.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await portal.handleAuthStatus(response.status))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as ChangelogResponse | null;
      setItems(response.ok && result?.success && Array.isArray(result.data) ? result.data : []);
    } finally {
      setLoading(false);
    }
  }
}
