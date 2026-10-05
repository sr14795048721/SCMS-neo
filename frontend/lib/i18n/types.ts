export const SUPPORTED_LOCALES = ["zh-CN"] as const;

export type LocaleCode = (typeof SUPPORTED_LOCALES)[number];
export type I18nNamespace =
  | "common"
  | "home"
  | "login"
  | "news"
  | "api"
  | "portal"
  | "pagination"
  | "notifications"
  | "adminDashboard"
  | "adminSystemSettings"
  | "adminDataStatistics"
  | "adminScoreReward"
  | "adminClubManage"
  | "adminActivityManage"
  | "adminClubRecommend"
  | "adminNews"
  | "adminUserManage"
  | "adminHomeConfig"
  | "adminLoginConfig"
  | "adminAppReleases"
  | "adminVersionChangelog"
  | "teacherDashboard"
  | "teacherClubs"
  | "teacherAttendance"
  | "teacherActivities"
  | "teacherNews"
  | "teacherAppWorkspace"
  | "teacherRecommendations"
  | "teacherClubCreation"
  | "teacherProfile"
  | "studentDashboard"
  | "studentActivities"
  | "studentClubCadre"
  | "studentScoreRewards"
  | "studentProfile"
  | "publicAttendanceSign"
  | "teacherVersionChangelog";
export type MessageMap = Record<string, string>;
export type NamespaceMessages = Record<I18nNamespace, MessageMap>;
export type TranslationVars = Record<string, string | number>;
export type PartialNamespaceMessages = Partial<Record<I18nNamespace, MessageMap>>;
