"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import styles from "./notify.module.css";

export type NotifyType = "success" | "warning" | "error" | "info";
export type ConfirmTone = "primary" | "danger";

type NotifyOptions = {
  type: NotifyType;
  message: string;
  duration?: number;
};

type ConfirmOptions = {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  tone?: ConfirmTone;
};

type NotifyApi = {
  notify: (options: NotifyOptions) => void;
  success: (message: string, duration?: number) => void;
  warning: (message: string, duration?: number) => void;
  error: (message: string, duration?: number) => void;
  info: (message: string, duration?: number) => void;
};

type ConfirmApi = {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
};

type Notice = {
  id: string;
  type: NotifyType;
  message: string;
  leaving: boolean;
};

type ConfirmState = {
  title?: string;
  message: string;
  confirmText: string;
  cancelText: string;
  tone: ConfirmTone;
};

const DEFAULT_DURATION_MS = 3000;
const EXIT_ANIMATION_MS = 220;

const NotifyContext = createContext<NotifyApi | null>(null);
const ConfirmContext = createContext<ConfirmApi | null>(null);

export function NotifyProvider({ children }: { children: React.ReactNode }) {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [confirmState, setConfirmState] = useState<ConfirmState | null>(null);
  const seedRef = useRef(0);
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());
  const exitTimersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());
  const confirmResolverRef = useRef<((value: boolean) => void) | null>(null);

  const clearNoticeTimers = useCallback((id: string) => {
    const closeTimer = timersRef.current.get(id);
    if (closeTimer) {
      clearTimeout(closeTimer);
      timersRef.current.delete(id);
    }
    const exitTimer = exitTimersRef.current.get(id);
    if (exitTimer) {
      clearTimeout(exitTimer);
      exitTimersRef.current.delete(id);
    }
  }, []);

  const dismiss = useCallback(
    (id: string) => {
      clearNoticeTimers(id);
      setNotices((prev) => prev.map((item) => (item.id === id ? { ...item, leaving: true } : item)));

      const exitTimer = setTimeout(() => {
        setNotices((prev) => prev.filter((item) => item.id !== id));
        exitTimersRef.current.delete(id);
      }, EXIT_ANIMATION_MS);

      exitTimersRef.current.set(id, exitTimer);
    },
    [clearNoticeTimers]
  );

  const notify = useCallback(
    ({ type, message, duration }: NotifyOptions) => {
      const text = String(message || "").trim();
      if (!text) {
        return;
      }

      seedRef.current += 1;
      const id = `${Date.now()}-${seedRef.current}`;
      setNotices((prev) => [...prev, { id, type, message: text, leaving: false }]);

      const closeTimer = setTimeout(() => {
        dismiss(id);
      }, duration ?? DEFAULT_DURATION_MS);

      timersRef.current.set(id, closeTimer);
    },
    [dismiss]
  );

  const api = useMemo<NotifyApi>(
    () => ({
      notify,
      success: (message, duration) => notify({ type: "success", message, duration }),
      warning: (message, duration) => notify({ type: "warning", message, duration }),
      error: (message, duration) => notify({ type: "error", message, duration }),
      info: (message, duration) => notify({ type: "info", message, duration })
    }),
    [notify]
  );

  const closeConfirm = useCallback((accepted: boolean) => {
    const resolver = confirmResolverRef.current;
    confirmResolverRef.current = null;
    setConfirmState(null);
    resolver?.(accepted);
  }, []);

  const confirm = useCallback((options: ConfirmOptions) => {
    const message = String(options.message || "").trim();
    if (!message) {
      return Promise.resolve(false);
    }

    if (confirmResolverRef.current) {
      confirmResolverRef.current(false);
      confirmResolverRef.current = null;
    }

    setConfirmState({
      title: String(options.title || "").trim() || undefined,
      message,
      confirmText: String(options.confirmText || "").trim() || "Confirm",
      cancelText: String(options.cancelText || "").trim() || "Cancel",
      tone: options.tone === "danger" ? "danger" : "primary"
    });

    return new Promise<boolean>((resolve) => {
      confirmResolverRef.current = resolve;
    });
  }, []);

  const confirmApi = useMemo<ConfirmApi>(
    () => ({
      confirm
    }),
    [confirm]
  );

  useEffect(
    () => () => {
      if (confirmResolverRef.current) {
        confirmResolverRef.current(false);
        confirmResolverRef.current = null;
      }
      timersRef.current.forEach((timer) => clearTimeout(timer));
      exitTimersRef.current.forEach((timer) => clearTimeout(timer));
      timersRef.current.clear();
      exitTimersRef.current.clear();
    },
    []
  );

  useEffect(() => {
    if (!confirmState) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeConfirm(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [closeConfirm, confirmState]);

  return (
    <ConfirmContext.Provider value={confirmApi}>
      <NotifyContext.Provider value={api}>
        {children}
        <div className={styles.stack}>
          {notices.map((item) => (
            <div
              key={item.id}
              className={`${styles.item} ${styles[item.type]} ${item.leaving ? styles.leaving : ""}`}
              role="status"
              aria-live="polite"
            >
              <i className={`fas ${typeToIcon(item.type)} ${styles.icon}`} />
              <p className={styles.text}>{item.message}</p>
            </div>
          ))}
        </div>
        {confirmState ? (
          <div className={styles.confirmLayer} role="presentation" onClick={() => closeConfirm(false)}>
            <div
              className={styles.confirmCard}
              role="alertdialog"
              aria-modal="true"
              aria-labelledby={confirmState.title ? "notify-confirm-title" : undefined}
              aria-describedby="notify-confirm-message"
              onClick={(event) => event.stopPropagation()}
            >
              <div className={styles.confirmHeader}>
                <div className={`${styles.confirmBadge} ${styles[confirmState.tone]}`}>
                  <i className={`fas ${confirmState.tone === "danger" ? "fa-trash-can" : "fa-circle-question"}`} />
                </div>
                <div className={styles.confirmCopy}>
                  {confirmState.title ? (
                    <h3 id="notify-confirm-title" className={styles.confirmTitle}>
                      {confirmState.title}
                    </h3>
                  ) : null}
                  <p id="notify-confirm-message" className={styles.confirmMessage}>
                    {confirmState.message}
                  </p>
                </div>
              </div>
              <div className={styles.confirmActions}>
                <button
                  type="button"
                  className={styles.confirmCancel}
                  onClick={() => closeConfirm(false)}
                >
                  {confirmState.cancelText}
                </button>
                <button
                  type="button"
                  className={`${styles.confirmSubmit} ${styles[confirmState.tone]}`}
                  onClick={() => closeConfirm(true)}
                >
                  {confirmState.confirmText}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </NotifyContext.Provider>
    </ConfirmContext.Provider>
  );
}

export function useNotify(): NotifyApi {
  const context = useContext(NotifyContext);
  if (!context) {
    throw new Error("useNotify must be used within NotifyProvider");
  }
  return context;
}

export function useConfirm(): ConfirmApi {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error("useConfirm must be used within NotifyProvider");
  }
  return context;
}

function typeToIcon(type: NotifyType): string {
  if (type === "success") {
    return "fa-circle-check";
  }
  if (type === "warning") {
    return "fa-triangle-exclamation";
  }
  if (type === "error") {
    return "fa-circle-xmark";
  }
  return "fa-circle-info";
}
