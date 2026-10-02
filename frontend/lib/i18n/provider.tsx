"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { loadMessages } from "./loadMessages";
import { normalizeLocale, resolveBrowserLocale } from "./locale";
import { I18nNamespace, LocaleCode, PartialNamespaceMessages } from "./types";

type I18nContextValue = {
  locale: LocaleCode;
  messages: PartialNamespaceMessages;
  ensureNamespace: (namespace: I18nNamespace) => Promise<void>;
};

const I18nContext = createContext<I18nContextValue | null>(null);

type I18nProviderProps = {
  children: React.ReactNode;
  initialLocale?: LocaleCode;
  initialMessages?: PartialNamespaceMessages;
};

export function I18nProvider({
  children,
  initialLocale = "zh-CN",
  initialMessages = {}
}: I18nProviderProps) {
  const [locale, setLocale] = useState<LocaleCode>(normalizeLocale(initialLocale));
  const [messages, setMessages] = useState<PartialNamespaceMessages>(initialMessages);
  const messagesRef = useRef(messages);
  const loadingRef = useRef<Set<I18nNamespace>>(new Set());

  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  useEffect(() => {
    const browserLocale = resolveBrowserLocale();
    if (browserLocale !== locale) {
      setLocale(browserLocale);
      setMessages({});
      messagesRef.current = {};
      loadingRef.current.clear();
    }
  }, [locale]);

  const ensureNamespace = useCallback(
    async (namespace: I18nNamespace) => {
      if (messagesRef.current[namespace] || loadingRef.current.has(namespace)) {
        return;
      }

      loadingRef.current.add(namespace);
      try {
        const loaded = await loadMessages(locale, namespace);
        setMessages((prev) => {
          const next = { ...prev, [namespace]: loaded };
          messagesRef.current = next;
          return next;
        });
      } finally {
        loadingRef.current.delete(namespace);
      }
    },
    [locale]
  );

  const value = useMemo(
    () => ({
      locale,
      messages,
      ensureNamespace
    }),
    [locale, messages, ensureNamespace]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18nContext(): I18nContextValue {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18nContext must be used within I18nProvider");
  }
  return context;
}

