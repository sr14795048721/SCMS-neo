import { proxyStudentActivityMutation } from "../../../../../lib/api/studentActivityProxy";

const failure = {
  failureCode: "STUDENT_ACTIVITY_UPDATE_FAILED",
  failureMessage: "student activity update failed",
  unavailableCode: "STUDENT_ACTIVITY_UPDATE_UNAVAILABLE",
  unavailableMessage: "student activity update service unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: {
    params: Promise<{
      activityId: string;
    }>;
  }
) {
  const { activityId } = await context.params;
  return proxyStudentActivityMutation(request, `/api/v1/students/me/activities/${activityId}`, "PATCH", failure);
}
