import { proxyAdminScoreRuleDetail } from "../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_SCORE_RULE_DETAIL_FAILED",
  failureMessage: "admin score rule detail request failed",
  unavailableCode: "ADMIN_SCORE_RULE_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin score rule detail backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ ruleId: string }> }
) {
  const { ruleId } = await context.params;
  return proxyAdminScoreRuleDetail(request, `/api/v1/admin/score-rules/${ruleId}`, "PATCH", failure);
}

export async function DELETE(
  request: Request,
  context: { params: Promise<{ ruleId: string }> }
) {
  const { ruleId } = await context.params;
  return proxyAdminScoreRuleDetail(request, `/api/v1/admin/score-rules/${ruleId}`, "DELETE", failure);
}
