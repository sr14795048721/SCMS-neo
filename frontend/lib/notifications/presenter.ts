import { TranslationVars } from "../i18n/types";
import { NotificationItem } from "./types";

type NotificationTranslate = (key: string, vars?: TranslationVars) => string;

type NotificationDisplay = {
  title: string;
  content: string;
};

const CLUB_CREATION_REQUEST_PREFIX = /A club manager submitted a new club creation request:\s*(.+)$/i;
const CLUB_JOIN_REQUEST_PREFIX = /A student submitted a new join request for\s+(.+?)\.?$/i;
const REWARD_ORDER_CREATED_PREFIX = /(.+?) requested reward redemption:\s*(.+)$/i;
const ACTIVITY_PUBLISHED_PREFIX = /A teacher published a new activity(?: for (.+?))?:\s*(.+)$/i;

export function presentNotification(item: NotificationItem, t: NotificationTranslate): NotificationDisplay {
  const normalizedTitle = String(item.title || "").trim().toLowerCase();
  const rawContent = String(item.content || "").trim();

  if (item.category === "CLUB_CREATION_REQUEST") {
    if (normalizedTitle === "new club creation request") {
      const matchedName = rawContent.match(CLUB_CREATION_REQUEST_PREFIX)?.[1]?.trim();
      return {
        title: t("display.clubCreationRequest.newTitle"),
        content: t("display.clubCreationRequest.newContent", { name: matchedName || rawContent || "-" })
      };
    }
    if (normalizedTitle === "club creation approved") {
      return {
        title: t("display.clubCreationRequest.approvedTitle"),
        content: t("display.clubCreationRequest.approvedContent")
      };
    }
    if (normalizedTitle === "club creation rejected") {
      return {
        title: t("display.clubCreationRequest.rejectedTitle"),
        content: t("display.clubCreationRequest.rejectedContent")
      };
    }
  }

  if (item.category === "CLUB_JOIN_REQUEST" && normalizedTitle === "new join request") {
    const matchedClubName = rawContent.match(CLUB_JOIN_REQUEST_PREFIX)?.[1]?.trim();
    return {
      title: t("display.clubJoinRequest.newTitle"),
      content: t("display.clubJoinRequest.newContent", { clubName: matchedClubName || rawContent || "-" })
    };
  }

  if (item.category === "NEWS" && normalizedTitle === "teacher news published") {
    return {
      title: t("display.news.publishedTitle"),
      content: t("display.news.publishedContent")
    };
  }

  if (item.category === "ACTIVITY" && normalizedTitle === "activity published") {
    const matched = rawContent.match(ACTIVITY_PUBLISHED_PREFIX);
    const clubName = matched?.[1]?.trim() || "";
    const activityTitle = matched?.[2]?.trim() || rawContent || "-";
    return {
      title: t("display.activity.publishedTitle"),
      content: clubName
        ? t("display.activity.publishedContentWithClub", { clubName, activityTitle })
        : t("display.activity.publishedContent", { activityTitle })
    };
  }

  if (item.category === "REWARD_ORDER") {
    if (normalizedTitle === "student reward order created") {
      const matched = rawContent.match(REWARD_ORDER_CREATED_PREFIX);
      return {
        title: t("display.rewardOrder.createdTitle"),
        content: t("display.rewardOrder.createdContent", {
          studentName: matched?.[1]?.trim() || "-",
          rewardName: matched?.[2]?.trim() || rawContent || "-"
        })
      };
    }
    if (normalizedTitle === "reward order completed") {
      return {
        title: t("display.rewardOrder.completedTitle"),
        content: t("display.rewardOrder.completedContent")
      };
    }
    if (normalizedTitle === "reward order rejected") {
      return {
        title: t("display.rewardOrder.rejectedTitle"),
        content: t("display.rewardOrder.rejectedContent")
      };
    }
  }

  return {
    title: item.title,
    content: item.content
  };
}
