import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminScoreReward"]);

  return {
    title: messages.adminScoreReward?.["meta.title"] || "SCMS Plus",
    description: messages.adminScoreReward?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminScoreRewardLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
