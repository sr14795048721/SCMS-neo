import { proxyManagerAttendanceSessionDetail } from "../../../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_CHECK_IN_REQUEST_FAILED",
  failureMessage: "manager attendance check-in request failed",
  unavailableCode: "MANAGER_ATTENDANCE_CHECK_IN_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance check-in backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      sessionId: string;
    }>;
  }
) {
  const { clubId, sessionId } = await context.params;
  return proxyManagerAttendanceSessionDetail(request, clubId, sessionId, "check-in", failure);
}
