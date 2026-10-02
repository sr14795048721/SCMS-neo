import { proxyNotificationSummary } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "NOTIFICATION_SUMMARY_FAILED",
  failureMessage: "notification summary request failed",
  unavailableCode: "NOTIFICATION_SUMMARY_UNAVAILABLE",
  unavailableMessage: "notification summary service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyNotificationSummary(request, FAILURE);
}
