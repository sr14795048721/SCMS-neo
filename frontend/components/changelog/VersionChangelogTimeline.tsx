"use client";

import type { ReactNode } from "react";
import { VersionChangelogItem } from "../../lib/version-changelog/types";
import styles from "../../styles/versionChangelog.module.css";

type VersionChangelogTimelineProps = {
  items: VersionChangelogItem[];
  loading?: boolean;
  loadingLabel?: string;
  emptyLabel: string;
  timeFallbackLabel: string;
  renderActions?: (item: VersionChangelogItem) => ReactNode;
};

export function VersionChangelogTimeline({
  items,
  loading,
  loadingLabel,
  emptyLabel,
  timeFallbackLabel,
  renderActions
}: VersionChangelogTimelineProps) {
  if (loading) {
    return <p className={styles.versionEmpty}>{loadingLabel || "加载中..."}</p>;
  }

  if (!items.length) {
    return <p className={styles.versionEmpty}>{emptyLabel}</p>;
  }

  return (
    <div className={styles.timeline}>
      {items.map((item) => {
        const lines = item.content
          .split("\n")
          .map((line) => line.trim())
          .filter(Boolean);

        return (
          <article key={item.id} className={styles.versionCard}>
            <div className={styles.versionHead}>
              <span className={styles.versionBadge}>{item.version}</span>
              <h3 className={styles.versionTitle}>{item.title}</h3>
              <span className={styles.versionMeta}>{formatDate(item.releasedAt, timeFallbackLabel)}</span>
              {renderActions ? <div className={styles.versionActions}>{renderActions(item)}</div> : null}
            </div>
            {lines.length ? (
              <ul className={styles.versionContent}>
                {lines.map((line, index) => (
                  <li key={`${item.id}-${index}`} className={styles.versionLine}>
                    {line}
                  </li>
                ))}
              </ul>
            ) : null}
          </article>
        );
      })}
    </div>
  );
}

function formatDate(value: string | null, fallback: string) {
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
    day: "2-digit"
  }).format(date);
}
