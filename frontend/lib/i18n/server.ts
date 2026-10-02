import { headers } from "next/headers";
import { resolveLocaleFromAcceptLanguage } from "./locale";
import { loadMessages } from "./loadMessages";
import { I18nNamespace, LocaleCode, PartialNamespaceMessages } from "./types";

export function resolveRequestLocale(): LocaleCode {
  const acceptLanguage = headers().get("accept-language");
  return resolveLocaleFromAcceptLanguage(acceptLanguage);
}

export async function loadInitialMessages(
  locale: LocaleCode,
  namespaces: I18nNamespace[]
): Promise<PartialNamespaceMessages> {
  const uniqueNamespaces = Array.from(new Set(namespaces));
  const entries = await Promise.all(
    uniqueNamespaces.map(async (namespace) => [namespace, await loadMessages(locale, namespace)] as const)
  );
  return Object.fromEntries(entries) as PartialNamespaceMessages;
}

