import { proxyManagerAttendanceSessionDetail } from "../../../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_SETTLE_REQUEST_FAILED",
  failureMessage: "manager attendance settle request failed",
  unavailableCode: "MANAGER_ATTENDANCE_SETTLE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance settle backend unavailable"
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
  return proxyManagerAttendanceSessionDetail(request, clubId, sessionId, "settle", failure);
}
