import { LocaleCode } from "./types";

const DEFAULT_LOCALE: LocaleCode = "zh-CN";

function detectSupportedLocale(input?: string | null): LocaleCode | null {
  const value = (input || "").trim().toLowerCase();
  if (!value) {
    return null;
  }
  if (value.startsWith("zh")) {
    return "zh-CN";
  }
  return null;
}

export function normalizeLocale(input?: string | null): LocaleCode {
  return detectSupportedLocale(input) ?? DEFAULT_LOCALE;
}

export function resolveBrowserLocale(): LocaleCode {
  if (typeof navigator === "undefined") {
    return DEFAULT_LOCALE;
  }
  const candidates = [...(navigator.languages || []), navigator.language].filter(Boolean);
  for (const item of candidates) {
    const locale = detectSupportedLocale(item);
    if (locale !== null) {
      return locale;
    }
  }
  return DEFAULT_LOCALE;
}

export function resolveLocaleFromAcceptLanguage(headerValue?: string | null): LocaleCode {
  if (!headerValue) {
    return DEFAULT_LOCALE;
  }
  const tokens = headerValue.split(",");
  for (const token of tokens) {
    const locale = detectSupportedLocale(token.split(";")[0]?.trim());
    if (locale !== null) {
      return locale;
    }
  }
  return DEFAULT_LOCALE;
}
