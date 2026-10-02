import { proxyManagerAttendanceSessionDetail } from "../../../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_CHECK_OUT_REQUEST_FAILED",
  failureMessage: "manager attendance check-out request failed",
  unavailableCode: "MANAGER_ATTENDANCE_CHECK_OUT_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance check-out backend unavailable"
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
  return proxyManagerAttendanceSessionDetail(request, clubId, sessionId, "check-out", failure);
}
