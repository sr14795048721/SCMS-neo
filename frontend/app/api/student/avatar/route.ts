import { proxyStudentAvatarGet, proxyStudentAvatarUpload } from "../../../../lib/api/studentProxy";

const failure = {
  failureCode: "STUDENT_AVATAR_REQUEST_FAILED",
  failureMessage: "student avatar request failed",
  unavailableCode: "STUDENT_AVATAR_BACKEND_UNREACHABLE",
  unavailableMessage: "student avatar backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentAvatarGet(request, failure);
}

export async function POST(request: Request) {
  return proxyStudentAvatarUpload(request, failure);
}

