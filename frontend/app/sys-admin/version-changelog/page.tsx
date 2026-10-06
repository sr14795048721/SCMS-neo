import type { Metadata } from "next";
import { SysAdminVersionChangelog } from "../../../components/sys-admin/SysAdminVersionChangelog";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminVersionChangelog"]);

  return {
    title: messages.adminVersionChangelog?.["meta.title"] || "Version Changelog",
    description: messages.adminVersionChangelog?.["meta.description"] || "Version Changelog"
  };
}

export default function SysAdminVersionChangelogPage() {
  return <SysAdminVersionChangelog />;
}
