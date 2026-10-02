import type { Metadata } from "next";
import { ConfigProvider } from "antd";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import "antd/dist/reset.css";
import "./globals.css";
import { I18nProvider } from "../lib/i18n/provider";
import { loadInitialMessages, resolveRequestLocale } from "../lib/i18n/server";
import { I18nNamespace } from "../lib/i18n/types";
import { NotifyProvider } from "../components/notify/NotifyProvider";
import { ClientOnly } from "../components/system/ClientOnly";
import { TelemetryTracker } from "../components/system/TelemetryTracker";

const INITIAL_NAMESPACES: I18nNamespace[] = [
  "common",
  "home",
  "login",
  "news",
  "api",
  "portal",
  "adminDashboard",
  "adminNews",
  "adminUserManage",
  "adminHomeConfig",
  "adminLoginConfig",
  "teacherDashboard",
  "teacherProfile",
  "studentDashboard",
  "studentActivities",
  "studentClubCadre",
  "studentScoreRewards",
  "studentProfile"
];

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["common"]);
  return {
    title: messages.common?.["meta.title"] || "SCMS Plus",
    description: messages.common?.["meta.description"] || "SCMS Plus"
  };
}

export default async function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  const locale = resolveRequestLocale();
  const initialMessages = await loadInitialMessages(locale, INITIAL_NAMESPACES);

  return (
    <html lang={locale}>
      <head>
        <link
          rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"
        />
      </head>
      <body>
        <AntdRegistry>
          <ConfigProvider
            theme={{
              token: {
                colorPrimary: "#8B1A1A",
                borderRadius: 8
              }
            }}
          >
            <NotifyProvider>
              <I18nProvider initialLocale={locale} initialMessages={initialMessages}>
                <ClientOnly>
                  <TelemetryTracker />
                  {children}
                </ClientOnly>
              </I18nProvider>
            </NotifyProvider>
          </ConfigProvider>
        </AntdRegistry>
      </body>
    </html>
  );
}
