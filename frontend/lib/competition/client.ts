"use client";

import { authorizedFetch } from "../auth/client";
import {
  CompetitionImportPayload,
  CompetitionPayload,
  CompetitionSourcePayload
} from "./types";

const JSON_HEADERS = { "Content-Type": "application/json" };

export async function fetchCompetitionSourcesRequest() {
  return authorizedFetch("/api/admin/competition-sources", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createCompetitionSourceRequest(payload: CompetitionSourcePayload) {
  return authorizedFetch("/api/admin/competition-sources", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateCompetitionSourceRequest(sourceId: number, payload: CompetitionSourcePayload) {
  return authorizedFetch(`/api/admin/competition-sources/${sourceId}`, {
    method: "PATCH",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function deleteCompetitionSourceRequest(sourceId: number) {
  return authorizedFetch(`/api/admin/competition-sources/${sourceId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function scanCompetitionSourceRequest(sourceId: number) {
  return authorizedFetch(`/api/admin/competition-sources/${sourceId}/scan`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function fetchCompetitionLeadsRequest(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return authorizedFetch(`/api/admin/competition-leads${query}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function confirmCompetitionLeadRequest(leadId: number, payload: CompetitionPayload) {
  return authorizedFetch(`/api/admin/competition-leads/${leadId}/confirm`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function ignoreCompetitionLeadRequest(leadId: number) {
  return authorizedFetch(`/api/admin/competition-leads/${leadId}/ignore`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function fetchCompetitionsRequest() {
  return authorizedFetch("/api/admin/competitions", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createCompetitionRequest(payload: CompetitionPayload) {
  return authorizedFetch("/api/admin/competitions", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateCompetitionRequest(competitionId: number, payload: CompetitionPayload) {
  return authorizedFetch(`/api/admin/competitions/${competitionId}`, {
    method: "PATCH",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function deleteCompetitionRequest(competitionId: number) {
  return authorizedFetch(`/api/admin/competitions/${competitionId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function importCompetitionArticleRequest(payload: CompetitionImportPayload) {
  return authorizedFetch("/api/admin/competitions/import-article", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}
