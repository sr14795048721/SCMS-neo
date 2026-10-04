import { proxyPublicAttendanceSession } from "../../../../../../lib/api/publicAttendanceProxy";

const failure = {
  failureCode: "PUBLIC_ATTENDANCE_SESSION_FAILED",
  failureMessage: "public attendance session request failed",
  unavailableCode: "PUBLIC_ATTENDANCE_BACKEND_UNREACHABLE",
  unavailableMessage: "public attendance backend unavailable"
} as const;

export async function GET(
  _request: Request,
  context: { params: { token: string } }
) {
  return proxyPublicAttendanceSession(context.params.token, failure);
}
