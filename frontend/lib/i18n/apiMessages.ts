import zhApi from "../../locales/zh-CN/api.json";
import { resolveLocaleFromAcceptLanguage } from "./locale";
import { loadNamespaceRaw } from "./loadMessages";

export type ApiBanner = {
  type: "image" | "video";
  title?: string;
  desc?: string;
  src: string;
  poster?: string;
};

export type ApiNews = {
  id: number;
  title: string;
  publish_date: string;
  cover_image?: string;
  author_name?: string;
  views?: number;
  content?: string;
};

export type ApiClub = {
  id: string;
  name: string;
  description?: string;
  memberCount?: number;
};

export type ApiMessages = {
  feedback: {
    contentRequired: string;
    sendFailed: string;
    serviceUnavailable: string;
    mail: {
      anonymousName: string;
      unknownValue: string;
      emptyEmail: string;
      subjectPrefix: string;
      title: string;
      submittedAtLabel: string;
      sourceUrlLabel: string;
      nameLabel: string;
      emailLabel: string;
      clientIpLabel: string;
      userAgentLabel: string;
      contentLabel: string;
    };
  };
  auth: {
    loginRequired: string;
    loginFailed: string;
    registerRequired: string;
    usernameInvalid: string;
    emailInvalid: string;
    passwordWeak: string;
    registerFailed: string;
    serviceUnavailable: string;
  };
  banners: ApiBanner[];
  news: ApiNews[];
  clubs: ApiClub[];
};

const DEFAULT_API_MESSAGES = zhApi as ApiMessages;

export async function loadApiMessages(acceptLanguage?: string | null): Promise<ApiMessages> {
  const locale = resolveLocaleFromAcceptLanguage(acceptLanguage);
  try {
    const raw = await loadNamespaceRaw(locale, "api");
    return normalizeApiMessages(raw);
  } catch {
    return DEFAULT_API_MESSAGES;
  }
}

function normalizeApiMessages(raw: Record<string, unknown>): ApiMessages {
  const feedbackRaw = asRecord(raw.feedback);
  const contentRequired = asString(feedbackRaw?.contentRequired) || DEFAULT_API_MESSAGES.feedback.contentRequired;
  const sendFailed = asString(feedbackRaw?.sendFailed) || DEFAULT_API_MESSAGES.feedback.sendFailed;
  const serviceUnavailable =
    asString(feedbackRaw?.serviceUnavailable) || DEFAULT_API_MESSAGES.feedback.serviceUnavailable;
  const feedbackMailRaw = asRecord(feedbackRaw?.mail);
  const authRaw = asRecord(raw.auth);

  return {
    feedback: {
      contentRequired,
      sendFailed,
      serviceUnavailable,
      mail: {
        anonymousName:
          asString(feedbackMailRaw?.anonymousName) || DEFAULT_API_MESSAGES.feedback.mail.anonymousName,
        unknownValue: asString(feedbackMailRaw?.unknownValue) || DEFAULT_API_MESSAGES.feedback.mail.unknownValue,
        emptyEmail: asString(feedbackMailRaw?.emptyEmail) || DEFAULT_API_MESSAGES.feedback.mail.emptyEmail,
        subjectPrefix:
          asString(feedbackMailRaw?.subjectPrefix) || DEFAULT_API_MESSAGES.feedback.mail.subjectPrefix,
        title: asString(feedbackMailRaw?.title) || DEFAULT_API_MESSAGES.feedback.mail.title,
        submittedAtLabel:
          asString(feedbackMailRaw?.submittedAtLabel) || DEFAULT_API_MESSAGES.feedback.mail.submittedAtLabel,
        sourceUrlLabel:
          asString(feedbackMailRaw?.sourceUrlLabel) || DEFAULT_API_MESSAGES.feedback.mail.sourceUrlLabel,
        nameLabel: asString(feedbackMailRaw?.nameLabel) || DEFAULT_API_MESSAGES.feedback.mail.nameLabel,
        emailLabel: asString(feedbackMailRaw?.emailLabel) || DEFAULT_API_MESSAGES.feedback.mail.emailLabel,
        clientIpLabel:
          asString(feedbackMailRaw?.clientIpLabel) || DEFAULT_API_MESSAGES.feedback.mail.clientIpLabel,
        userAgentLabel:
          asString(feedbackMailRaw?.userAgentLabel) || DEFAULT_API_MESSAGES.feedback.mail.userAgentLabel,
        contentLabel: asString(feedbackMailRaw?.contentLabel) || DEFAULT_API_MESSAGES.feedback.mail.contentLabel
      }
    },
    auth: {
      loginRequired: asString(authRaw?.loginRequired) || DEFAULT_API_MESSAGES.auth.loginRequired,
      loginFailed: asString(authRaw?.loginFailed) || DEFAULT_API_MESSAGES.auth.loginFailed,
      registerRequired: asString(authRaw?.registerRequired) || DEFAULT_API_MESSAGES.auth.registerRequired,
      usernameInvalid: asString(authRaw?.usernameInvalid) || DEFAULT_API_MESSAGES.auth.usernameInvalid,
      emailInvalid: asString(authRaw?.emailInvalid) || DEFAULT_API_MESSAGES.auth.emailInvalid,
      passwordWeak: asString(authRaw?.passwordWeak) || DEFAULT_API_MESSAGES.auth.passwordWeak,
      registerFailed: asString(authRaw?.registerFailed) || DEFAULT_API_MESSAGES.auth.registerFailed,
      serviceUnavailable: asString(authRaw?.serviceUnavailable) || DEFAULT_API_MESSAGES.auth.serviceUnavailable
    },
    banners: asBannerList(raw.banners) || DEFAULT_API_MESSAGES.banners,
    news: asNewsList(raw.news) || DEFAULT_API_MESSAGES.news,
    clubs: asClubList(raw.clubs) || DEFAULT_API_MESSAGES.clubs
  };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function asString(value: unknown): string | null {
  return typeof value === "string" ? value : null;
}

function asBannerList(value: unknown): ApiBanner[] | null {
  if (!Array.isArray(value)) {
    return null;
  }
  const banners: ApiBanner[] = [];
  for (const item of value) {
    const record = asRecord(item);
    if (!record) {
      continue;
    }
    const type = asString(record.type);
    const src = asString(record.src);
    if ((type !== "image" && type !== "video") || !src) {
      continue;
    }
    banners.push({
      type,
      src,
      title: asString(record.title) ?? undefined,
      desc: asString(record.desc) ?? undefined,
      poster: asString(record.poster) ?? undefined
    });
  }
  return banners.length > 0 ? banners : null;
}

function asNewsList(value: unknown): ApiNews[] | null {
  if (!Array.isArray(value)) {
    return null;
  }
  const news: ApiNews[] = [];
  for (const item of value) {
    const record = asRecord(item);
    if (!record) {
      continue;
    }
    const id = Number(record.id);
    const title = asString(record.title);
    const publishDate = asString(record.publish_date);
    if (!Number.isFinite(id) || !title || !publishDate) {
      continue;
    }
    news.push({
      id,
      title,
      publish_date: publishDate,
      cover_image: asString(record.cover_image) ?? undefined,
      author_name: asString(record.author_name) ?? undefined,
      views: Number.isFinite(Number(record.views)) ? Number(record.views) : undefined,
      content: asString(record.content) ?? undefined
    });
  }
  return news.length > 0 ? news : null;
}

function asClubList(value: unknown): ApiClub[] | null {
  if (!Array.isArray(value)) {
    return null;
  }
  const clubs: ApiClub[] = [];
  for (const item of value) {
    const record = asRecord(item);
    if (!record) {
      continue;
    }
    const id = record.id != null ? String(record.id) : "";
    const name = asString(record.name);
    if (!id || !name) {
      continue;
    }
    const memberCountNumber = Number(record.memberCount);
    clubs.push({
      id,
      name,
      description: asString(record.description) ?? undefined,
      memberCount: Number.isFinite(memberCountNumber) ? memberCountNumber : undefined
    });
  }
  return clubs.length > 0 ? clubs : null;
}
