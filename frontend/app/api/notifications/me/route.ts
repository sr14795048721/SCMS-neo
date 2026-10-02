import { proxyNotifications } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "NOTIFICATIONS_FAILED",
  failureMessage: "notifications request failed",
  unavailableCode: "NOTIFICATIONS_UNAVAILABLE",
  unavailableMessage: "notifications service unavailable"
} as const;

export async function GET(request: Request) {
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";
  return proxyNotifications(request, `/api/v1/notifications/me${query}`, "GET", FAILURE);
}
