import { proxyManagerAttendanceSessions } from "../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_SESSIONS_REQUEST_FAILED",
  failureMessage: "manager attendance sessions request failed",
  unavailableCode: "MANAGER_ATTENDANCE_SESSIONS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance sessions backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerAttendanceSessions(request, clubId, "GET", failure);
}

export async function POST(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerAttendanceSessions(request, clubId, "POST", failure);
}
