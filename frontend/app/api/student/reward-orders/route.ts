import {
  proxyStudentRewardOrderCreate,
  proxyStudentRewardOrders
} from "../../../../lib/api/studentScoreRewardProxy";

const listFailure = {
  failureCode: "STUDENT_REWARD_ORDERS_FAILED",
  failureMessage: "student reward orders request failed",
  unavailableCode: "STUDENT_REWARD_ORDERS_UNAVAILABLE",
  unavailableMessage: "student reward orders service unavailable"
} as const;

const createFailure = {
  failureCode: "STUDENT_REWARD_ORDER_CREATE_FAILED",
  failureMessage: "student reward order create request failed",
  unavailableCode: "STUDENT_REWARD_ORDER_CREATE_UNAVAILABLE",
  unavailableMessage: "student reward order create service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentRewardOrders(request, listFailure);
}

export async function POST(request: Request) {
  return proxyStudentRewardOrderCreate(request, createFailure);
}
