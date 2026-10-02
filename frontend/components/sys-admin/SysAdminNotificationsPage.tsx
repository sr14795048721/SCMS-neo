"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { NotificationFeedPanel } from "../notifications/NotificationFeedPanel";
import { logoutAdminSession } from "../../lib/admin-system/session";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import {
  fetchNotificationFeedRequest,
  fetchNotificationSummaryRequest,
  markNotificationReadRequest
} from "../../lib/notifications/client";
import { NotificationFeedPage, NotificationItem, NotificationSummary } from "../../lib/notifications/types";
import styles from "../../styles/sysAdmin.module.css";

type NotificationSummaryResponse = {
  success?: boolean;
  data?: NotificationSummary;
};

type NotificationFeedResponse = {
  success?: boolean;
  data?: NotificationFeedPage;
};

type FeedState = NotificationFeedPage & {
  loaded: boolean;
};

const PAGE_SIZE = 20;
const defaultSummary: NotificationSummary = {
  unreadCount: 0,
  latestUnread: []
};
const defaultFeed: FeedState = {
  items: [],
  page: 0,
  pageSize: PAGE_SIZE,
  total: 0,
  totalPages: 0,
  loaded: false
};

export function SysAdminNotificationsPage() {
  const tPortal = useT("portal");
  const tNotifications = useT("notifications");
  const notify = useNotify();
  const router = useRouter();
  const { authorized, checking, redirecting } = useRoleGate("ADMIN");
  const [summary, setSummary] = useState<NotificationSummary>(defaultSummary);
  const [status, setStatus] = useState<"UNREAD" | "READ">("UNREAD");
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [feeds, setFeeds] = useState<Record<"UNREAD" | "READ", FeedState>>({
    UNREAD: defaultFeed,
    READ: defaultFeed
  });

  useEffect(() => {
    if (!authorized) {
      return;
    }
    void loadSummary();
    void loadFeed("UNREAD", 0, false);
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

  const currentFeed = feeds[status];
  const hasMore = currentFeed.items.length < currentFeed.total;

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <h1>
            <i className="fas fa-bell" />
            {tNotifications("admin.pageTitle")}
          </h1>
          <div className={styles.headerActions}>
            <button type="button" className={styles.backButton} onClick={() => router.push("/sys-admin")}>
              <i className="fas fa-arrow-left" />
              {tNotifications("admin.backToDashboard")}
            </button>
          </div>
        </div>
      </header>

      <div className={styles.container}>
        <div className={styles.stack}>
          <NotificationFeedPanel
            title={tNotifications("admin.pageTitle")}
            description={tNotifications("admin.pageDescription")}
            unreadTag={tNotifications("admin.unreadTag", { count: summary.unreadCount })}
            unreadTabLabel={tNotifications("common.tabs.unread")}
            readTabLabel={tNotifications("common.tabs.read")}
            unreadBadgeLabel={tNotifications("common.badges.unread")}
            readBadgeLabel={tNotifications("common.badges.read")}
            loadingLabel={tNotifications("common.loading")}
            emptyUnreadLabel={tNotifications("common.emptyUnread")}
            emptyReadLabel={tNotifications("common.emptyRead")}
            timeFallbackLabel={tNotifications("common.timeFallback")}
            loadMoreLabel={tNotifications("common.loadMore")}
            loadingMoreLabel={tNotifications("common.loadingMore")}
            status={status}
            items={currentFeed.items}
            loading={loading}
            loadingMore={loadingMore}
            hasMore={hasMore}
            onStatusChange={(next) => {
              setStatus(next);
              if (!feeds[next].loaded) {
                void loadFeed(next, 0, false);
              }
            }}
            onItemClick={(notificationId) => void handleItemClick(notificationId)}
            onLoadMore={() => void loadFeed(status, feeds[status].page + 1, true)}
          />
        </div>
      </div>
    </main>
  );

  async function loadSummary() {
    const response = await fetchNotificationSummaryRequest();
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status))) {
      return;
    }
    const result = (await response.json().catch(() => null)) as NotificationSummaryResponse | null;
    if (!response.ok || result?.success !== true || !result.data) {
      return;
    }
    setSummary(result.data);
  }

  async function loadFeed(nextStatus: "UNREAD" | "READ", page: number, append: boolean) {
    if (append) {
      setLoadingMore(true);
    } else {
      setLoading(true);
    }

    try {
      const response = await fetchNotificationFeedRequest(nextStatus, page, PAGE_SIZE);
      if (!response) {
        return;
      }
      if (!(await handleAuthStatus(response.status))) {
        return;
      }
      const result = (await response.json().catch(() => null)) as NotificationFeedResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        return;
      }
      const pageData = result.data;

      setFeeds((current) => {
        const currentItems = append ? current[nextStatus].items : [];
        const mergedItems = [...currentItems, ...pageData.items].filter(
          (item, index, array) => array.findIndex((entry) => entry.id === item.id) === index
        );
        return {
          ...current,
          [nextStatus]: {
            ...pageData,
            items: mergedItems,
            loaded: true
          }
        };
      });
    } finally {
      if (append) {
        setLoadingMore(false);
      } else {
        setLoading(false);
      }
    }
  }

  async function handleItemClick(notificationId: number) {
    const item = feeds[status].items.find((entry) => entry.id === notificationId);
    if (!item) {
      return;
    }

    const updated = item.status === "UNREAD" ? await markItemAsRead(item) : item;
    if (updated.targetPath) {
      router.push(updated.targetPath);
    }
  }

  async function markItemAsRead(item: NotificationItem) {
    const response = await markNotificationReadRequest(item.id);
    if (!response) {
      return item;
    }
    if (!(await handleAuthStatus(response.status))) {
      return item;
    }
    if (response.ok) {
      setSummary((current) => ({
        unreadCount: Math.max(0, current.unreadCount - 1),
        latestUnread: current.latestUnread.filter((entry) => entry.id !== item.id)
      }));
      setFeeds((current) => ({
        UNREAD: {
          ...current.UNREAD,
          items: current.UNREAD.items.filter((entry) => entry.id !== item.id),
          total: Math.max(0, current.UNREAD.total - 1)
        },
        READ: {
          ...current.READ,
          items: [{ ...item, status: "READ" }, ...current.READ.items.filter((entry) => entry.id !== item.id)],
          total: current.READ.total + 1,
          loaded: current.READ.loaded
        }
      }));
      return { ...item, status: "READ" as const };
    }
    return item;
  }

  async function handleAuthStatus(statusCode: number) {
    if (statusCode === 401) {
      notify.warning(tPortal("auth.loginRequired"));
      await logoutAdminSession();
      router.replace("/login");
      return false;
    }

    if (statusCode === 403) {
      notify.error(tPortal("auth.noPermission"));
      router.replace("/login");
      return false;
    }

    return true;
  }
}
