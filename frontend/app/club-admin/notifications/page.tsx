import type { Metadata } from "next";
import { TeacherNotificationsPage } from "../../../components/teacher/TeacherNotificationsPage";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["notifications"]);

  return {
    title: messages.notifications?.["teacher.pageTitle"] || "SCMS Plus",
    description: messages.notifications?.["teacher.pageDescription"] || "SCMS Plus"
  };
}

export default function TeacherNotificationsRoute() {
  return <TeacherNotificationsPage />;
}
