import { I18nNamespace, LocaleCode, MessageMap } from "./types";
import { normalizeLocale } from "./locale";

type NamespaceLoader = () => Promise<Record<string, unknown>>;
type LocaleLoaders = Record<I18nNamespace, NamespaceLoader>;

const LOADERS: Record<LocaleCode, LocaleLoaders> = {
  "zh-CN": {
    common: () => import("../../locales/zh-CN/common.json"),
    home: () => import("../../locales/zh-CN/home.json"),
    login: () => import("../../locales/zh-CN/login.json"),
    news: () => import("../../locales/zh-CN/news.json"),
    api: () => import("../../locales/zh-CN/api.json"),
    portal: () => import("../../locales/zh-CN/portal.json"),
    pagination: () => import("../../locales/zh-CN/pagination.json"),
    notifications: () => import("../../locales/zh-CN/notifications.json"),
    adminDashboard: () => import("../../locales/zh-CN/admin-dashboard.json"),
    adminSystemSettings: () => import("../../locales/zh-CN/admin-system-settings.json"),
    adminDataStatistics: () => import("../../locales/zh-CN/admin-data-statistics.json"),
    adminScoreReward: () => import("../../locales/zh-CN/admin-score-reward.json"),
    adminClubManage: () => import("../../locales/zh-CN/adminClubManage.json"),
    adminActivityManage: () => import("../../locales/zh-CN/adminActivityManage.json"),
    adminClubRecommend: () => import("../../locales/zh-CN/adminClubRecommend.json"),
    adminNews: () => import("../../locales/zh-CN/admin-news.json"),
    adminUserManage: () => import("../../locales/zh-CN/admin-user-manage.json"),
    adminHomeConfig: () => import("../../locales/zh-CN/admin-home-config.json"),
    adminLoginConfig: () => import("../../locales/zh-CN/admin-login-config.json"),
    adminAppReleases: () => import("../../locales/zh-CN/admin-app-releases.json"),
    adminVersionChangelog: () => import("../../locales/zh-CN/admin-version-changelog.json"),
    teacherDashboard: () => import("../../locales/zh-CN/teacher-dashboard.json"),
    teacherClubs: () => import("../../locales/zh-CN/teacher-clubs.json"),
    teacherAttendance: () => import("../../locales/zh-CN/teacher-attendance.json"),
    teacherActivities: () => import("../../locales/zh-CN/teacher-activities.json"),
    teacherNews: () => import("../../locales/zh-CN/teacher-news.json"),
    teacherAppWorkspace: () => import("../../locales/zh-CN/teacher-app-workspace.json"),
    teacherRecommendations: () => import("../../locales/zh-CN/teacher-recommendations.json"),
    teacherClubCreation: () => import("../../locales/zh-CN/teacher-club-creation.json"),
    teacherProfile: () => import("../../locales/zh-CN/teacher-profile.json"),
    studentDashboard: () => import("../../locales/zh-CN/student-dashboard.json"),
    studentActivities: () => import("../../locales/zh-CN/student-activities.json"),
    studentClubCadre: () => import("../../locales/zh-CN/student-club-cadre.json"),
    studentScoreRewards: () => import("../../locales/zh-CN/student-score-rewards.json"),
    studentProfile: () => import("../../locales/zh-CN/student-profile.json"),
    publicAttendanceSign: () => import("../../locales/zh-CN/public-attendance-sign.json"),
    teacherVersionChangelog: () => import("../../locales/zh-CN/teacher-version-changelog.json")
  }
};

export async function loadMessages(locale: string, namespace: I18nNamespace): Promise<MessageMap> {
  const normalized = normalizeLocale(locale);
  const module = await LOADERS[normalized][namespace]();
  return flattenMessages(module.default as Record<string, unknown>);
}

export async function loadNamespaceRaw(locale: string, namespace: I18nNamespace): Promise<Record<string, unknown>> {
  const normalized = normalizeLocale(locale);
  const module = await LOADERS[normalized][namespace]();
  return module.default as Record<string, unknown>;
}

function flattenMessages(source: Record<string, unknown>): MessageMap {
  const output: MessageMap = {};

  const walk = (prefix: string, value: unknown) => {
    if (typeof value === "string") {
      output[prefix] = value;
      return;
    }
    if (Array.isArray(value)) {
      output[prefix] = JSON.stringify(value);
      return;
    }
    if (value && typeof value === "object") {
      for (const [key, nested] of Object.entries(value)) {
        const next = prefix ? `${prefix}.${key}` : key;
        walk(next, nested);
      }
    }
  };

  walk("", source);
  return output;
}
