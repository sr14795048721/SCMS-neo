import type { Metadata } from "next";
import { SysAdminNotificationsPage } from "../../../components/sys-admin/SysAdminNotificationsPage";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["notifications"]);

  return {
    title: messages.notifications?.["admin.pageTitle"] || "SCMS Plus",
    description: messages.notifications?.["admin.pageDescription"] || "SCMS Plus"
  };
}

export default function SysAdminNotificationsRoute() {
  return <SysAdminNotificationsPage />;
}
