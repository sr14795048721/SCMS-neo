import { proxyManagerAttendanceSignature } from "../../../../../../../../../../lib/api/managerAttendanceProxy";

const failure = {
  failureCode: "MANAGER_ATTENDANCE_SIGNATURE_REQUEST_FAILED",
  failureMessage: "manager attendance signature request failed",
  unavailableCode: "MANAGER_ATTENDANCE_SIGNATURE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager attendance signature backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      sessionId: string;
      studentUserId: string;
    }>;
  }
) {
  const { clubId, sessionId, studentUserId } = await context.params;
  return proxyManagerAttendanceSignature(request, clubId, sessionId, studentUserId, failure);
}
