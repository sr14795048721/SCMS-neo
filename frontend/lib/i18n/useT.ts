"use client";

import { useEffect, useMemo } from "react";
import { useI18nContext } from "./provider";
import { I18nNamespace, TranslationVars } from "./types";

const VARIABLE_PATTERN = /\{(\w+)\}/g;

function interpolate(template: string, vars?: TranslationVars): string {
  if (!vars) {
    return template;
  }
  return template.replace(VARIABLE_PATTERN, (_, key: string) => String(vars[key] ?? `{${key}}`));
}

export function useT(namespace: I18nNamespace) {
  const { messages, ensureNamespace } = useI18nContext();

  useEffect(() => {
    void ensureNamespace(namespace);
  }, [namespace, ensureNamespace]);

  return useMemo(
    () =>
      (key: string, vars?: TranslationVars) =>
        interpolate(messages[namespace]?.[key] ?? key, vars),
    [messages, namespace]
  );
}

