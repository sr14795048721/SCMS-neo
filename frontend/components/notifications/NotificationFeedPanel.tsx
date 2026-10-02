"use client";

import { useT } from "../../lib/i18n/useT";
import { presentNotification } from "../../lib/notifications/presenter";
import { NotificationItem } from "../../lib/notifications/types";
import styles from "../../styles/notificationCenter.module.css";

type NotificationFeedPanelProps = {
  title: string;
  description: string;
  unreadTag: string;
  unreadTabLabel: string;
  readTabLabel: string;
  unreadBadgeLabel: string;
  readBadgeLabel: string;
  loadingLabel: string;
  emptyUnreadLabel: string;
  emptyReadLabel: string;
  timeFallbackLabel: string;
  loadMoreLabel: string;
  loadingMoreLabel: string;
  status: "UNREAD" | "READ";
  items: NotificationItem[];
  loading: boolean;
  loadingMore: boolean;
  hasMore: boolean;
  onStatusChange: (status: "UNREAD" | "READ") => void;
  onItemClick: (notificationId: number) => void;
  onLoadMore: () => void;
};

export function NotificationFeedPanel({
  title,
  description,
  unreadTag,
  unreadTabLabel,
  readTabLabel,
  unreadBadgeLabel,
  readBadgeLabel,
  loadingLabel,
  emptyUnreadLabel,
  emptyReadLabel,
  timeFallbackLabel,
  loadMoreLabel,
  loadingMoreLabel,
  status,
  items,
  loading,
  loadingMore,
  hasMore,
  onStatusChange,
  onItemClick,
  onLoadMore
}: NotificationFeedPanelProps) {
  const tNotifications = useT("notifications");

  return (
    <section className={styles.feedPanel}>
      <div className={styles.feedHead}>
        <div>
          <h2 className={styles.title}>
            <i className="fas fa-inbox" />
            {title}
          </h2>
          <p className={styles.description}>{description}</p>
        </div>
        <span className={styles.tag}>{unreadTag}</span>
      </div>

      <div className={styles.tabRow}>
        <button
          type="button"
          className={`${styles.tabButton} ${status === "UNREAD" ? styles.tabButtonActive : ""}`}
          onClick={() => onStatusChange("UNREAD")}
        >
          {unreadTabLabel}
        </button>
        <button
          type="button"
          className={`${styles.tabButton} ${status === "READ" ? styles.tabButtonActive : ""}`}
          onClick={() => onStatusChange("READ")}
        >
          {readTabLabel}
        </button>
      </div>

      {loading ? (
        <div className={styles.emptyPanel}>
          <p>{loadingLabel}</p>
        </div>
      ) : items.length ? (
        <>
          <div className={styles.feedList}>
            {items.map((item) => {
              const display = presentNotification(item, tNotifications);
              return (
              <button key={item.id} type="button" className={styles.itemButton} onClick={() => onItemClick(item.id)}>
                <div className={styles.itemHead}>
                  <h3 className={styles.itemTitle}>{display.title}</h3>
                  <span className={status === "UNREAD" ? styles.statusUnread : styles.statusRead}>
                    {status === "UNREAD" ? unreadBadgeLabel : readBadgeLabel}
                  </span>
                </div>
                <p className={styles.itemContent}>{display.content}</p>
                <p className={styles.itemTime}>
                  {item.createdAt ? new Date(item.createdAt).toLocaleString("zh-CN") : timeFallbackLabel}
                </p>
              </button>
            );
            })}
          </div>

          {hasMore ? (
            <div className={styles.feedFooter}>
              <button type="button" className={styles.secondaryAction} onClick={onLoadMore} disabled={loadingMore}>
                <i className="fas fa-angles-down" />
                {loadingMore ? loadingMoreLabel : loadMoreLabel}
              </button>
            </div>
          ) : null}
        </>
      ) : (
        <div className={styles.emptyPanel}>
          <p>{status === "UNREAD" ? emptyUnreadLabel : emptyReadLabel}</p>
        </div>
      )}
    </section>
  );
}
