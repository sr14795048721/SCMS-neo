"use client";

import { authorizedFetch } from "../auth/client";

export async function fetchActivityRegistrationsRequest(activityId: number) {
  return authorizedFetch(`/api/activities/${activityId}/registrations`, {
    method: "GET",
    cache: "no-store"
  });
}
