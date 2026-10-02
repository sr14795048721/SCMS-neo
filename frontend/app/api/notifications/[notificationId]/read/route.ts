import { proxyNotifications } from "../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "NOTIFICATION_READ_FAILED",
  failureMessage: "notification read failed",
  unavailableCode: "NOTIFICATION_READ_UNAVAILABLE",
  unavailableMessage: "notification read unavailable"
} as const;

export async function POST(request: Request, context: { params: Promise<{ notificationId: string }> }) {
  const { notificationId } = await context.params;
  return proxyNotifications(request, `/api/v1/notifications/${notificationId}/read`, "POST", FAILURE);
}
