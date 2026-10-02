"use client";

import { useT } from "../../lib/i18n/useT";
import { presentNotification } from "../../lib/notifications/presenter";
import { NotificationSummary } from "../../lib/notifications/types";
import styles from "../../styles/notificationCenter.module.css";

type NotificationSummaryCardProps = {
  title: string;
  description: string;
  unreadTag: string;
  loadingLabel: string;
  emptyLabel: string;
  timeFallbackLabel: string;
  unreadBadgeLabel: string;
  viewAllLabel: string;
  summary: NotificationSummary;
  loading: boolean;
  onOpenAll: () => void;
  onItemClick: (notificationId: number) => void;
};

export function NotificationSummaryCard({
  title,
  description,
  unreadTag,
  loadingLabel,
  emptyLabel,
  timeFallbackLabel,
  unreadBadgeLabel,
  viewAllLabel,
  summary,
  loading,
  onOpenAll,
  onItemClick
}: NotificationSummaryCardProps) {
  const tNotifications = useT("notifications");

  return (
    <section className={styles.summaryCard}>
      <div className={styles.summaryHead}>
        <div>
          <h2 className={styles.title}>
            <i className="fas fa-bell" />
            {title}
          </h2>
          <p className={styles.description}>{description}</p>
        </div>
        <span className={styles.tag}>{unreadTag}</span>
      </div>

      {loading ? (
        <div className={styles.emptyPanel}>
          <p>{loadingLabel}</p>
        </div>
      ) : summary.latestUnread.length ? (
        <div className={styles.summaryList}>
          {summary.latestUnread.map((item) => {
            const display = presentNotification(item, tNotifications);
            return (
            <button key={item.id} type="button" className={styles.itemButton} onClick={() => onItemClick(item.id)}>
              <div className={styles.itemHead}>
                <h3 className={styles.itemTitle}>{display.title}</h3>
                <span className={styles.statusUnread}>{unreadBadgeLabel}</span>
              </div>
              <p className={styles.itemContent}>{display.content}</p>
              <p className={styles.itemTime}>
                {item.createdAt ? new Date(item.createdAt).toLocaleString("zh-CN") : timeFallbackLabel}
              </p>
            </button>
          );
          })}
        </div>
      ) : (
        <div className={styles.emptyPanel}>
          <p>{emptyLabel}</p>
        </div>
      )}

      <div className={styles.summaryFooter}>
        <button type="button" className={styles.primaryAction} onClick={onOpenAll}>
          <i className="fas fa-arrow-right" />
          {viewAllLabel}
        </button>
      </div>
    </section>
  );
}
