import { proxyAdminScoreRecords, proxyAdminScoreRecordCreate } from "../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_SCORE_RECORDS_FAILED",
  failureMessage: "admin score records request failed",
  unavailableCode: "ADMIN_SCORE_RECORDS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin score records backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminScoreRecords(request, failure);
}

export async function POST(request: Request) {
  return proxyAdminScoreRecordCreate(request, failure);
}
