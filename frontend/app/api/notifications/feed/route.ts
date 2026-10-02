import { proxyNotificationFeed } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "NOTIFICATION_FEED_FAILED",
  failureMessage: "notification feed request failed",
  unavailableCode: "NOTIFICATION_FEED_UNAVAILABLE",
  unavailableMessage: "notification feed service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyNotificationFeed(request, FAILURE);
}
