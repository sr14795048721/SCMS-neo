import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["studentScoreRewards"]);

  return {
    title: messages.studentScoreRewards?.["meta.title"] || "SCMS Plus",
    description: messages.studentScoreRewards?.["meta.description"] || "SCMS Plus"
  };
}

export default function StudentScoreRewardsLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
