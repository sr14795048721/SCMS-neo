export type NotificationItem = {
  id: number;
  category: string;
  title: string;
  content: string;
  status: "UNREAD" | "READ";
  createdAt: string | null;
  targetPath: string | null;
};

export type NotificationSummary = {
  unreadCount: number;
  latestUnread: NotificationItem[];
};

export type NotificationFeedPage = {
  items: NotificationItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};
