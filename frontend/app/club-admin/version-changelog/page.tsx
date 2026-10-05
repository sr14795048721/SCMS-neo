import type { Metadata } from "next";
import { TeacherVersionChangelogPage } from "../../../components/teacher/TeacherVersionChangelog";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["teacherVersionChangelog"]);

  return {
    title: messages.teacherVersionChangelog?.["meta.title"] || "版本更新日志",
    description: messages.teacherVersionChangelog?.["meta.description"] || "版本更新日志"
  };
}

export default function ClubAdminVersionChangelogPage() {
  return <TeacherVersionChangelogPage />;
}
