import { proxyManagerAttendanceSessionDetail } from "../../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_SESSION_DETAIL_REQUEST_FAILED",
  failureMessage: "manager attendance session detail request failed",
  unavailableCode: "MANAGER_ATTENDANCE_SESSION_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance session detail backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      sessionId: string;
    }>;
  }
) {
  const { clubId, sessionId } = await context.params;
  return proxyManagerAttendanceSessionDetail(request, clubId, sessionId, "detail", failure);
}
