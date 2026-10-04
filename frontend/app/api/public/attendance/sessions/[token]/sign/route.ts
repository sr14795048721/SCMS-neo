import { proxyPublicAttendanceSign } from "../../../../../../../lib/api/publicAttendanceProxy";

const failure = {
  failureCode: "PUBLIC_ATTENDANCE_SIGN_FAILED",
  failureMessage: "public attendance sign request failed",
  unavailableCode: "PUBLIC_ATTENDANCE_BACKEND_UNREACHABLE",
  unavailableMessage: "public attendance sign backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: { token: string } }
) {
  return proxyPublicAttendanceSign(request, context.params.token, failure);
}
