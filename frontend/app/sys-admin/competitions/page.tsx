import type { Metadata } from "next";
import { SysAdminCompetitions } from "../../../components/sys-admin/SysAdminCompetitions";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminCompetitions"]);

  return {
    title: messages.adminCompetitions?.["meta.title"] || "Competition Tracking",
    description: messages.adminCompetitions?.["meta.description"] || "Competition Tracking"
  };
}

export default function SysAdminCompetitionsPage() {
  return <SysAdminCompetitions />;
}
