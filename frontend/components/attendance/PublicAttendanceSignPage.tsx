"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { PointerEvent as ReactPointerEvent } from "react";
import { useParams } from "next/navigation";
import { useT } from "../../lib/i18n/useT";
import { PublicAttendanceSession, PublicAttendanceSignResult } from "../../lib/attendance/publicAttendanceTypes";
import styles from "../../styles/publicAttendanceSign.module.css";

type PageState = "loading" | "invalid" | "ended" | "ready" | "success";

const SIGNATURE_HEIGHT = 220;

export function PublicAttendanceSignPage() {
  const t = useT("publicAttendanceSign");
  const params = useParams<{ token: string }>();
  const token = String(params?.token || "");

  const [state, setState] = useState<PageState>("loading");
  const [stateMessage, setStateMessage] = useState("");
  const [session, setSession] = useState<PublicAttendanceSession | null>(null);
  const [result, setResult] = useState<PublicAttendanceSignResult | null>(null);

  const [displayName, setDisplayName] = useState("");
  const [studentNo, setStudentNo] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [hasSignature, setHasSignature] = useState(false);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const drawingRef = useRef(false);
  const sizeRef = useRef({ width: 0, height: SIGNATURE_HEIGHT });

  const initializeCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const container = canvas.parentElement;
    const width = Math.max(container ? container.clientWidth : 0, 280);
    const height = SIGNATURE_HEIGHT;
    const ratio = Math.min(window.devicePixelRatio || 1, 2);

    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
    sizeRef.current = { width, height };

    const ctx = canvas.getContext("2d");
    if (!ctx) {
      return;
    }
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    paintBackground(ctx, width, height);
    ctx.strokeStyle = "#1f2937";
    ctx.lineWidth = 2.4;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    setHasSignature(false);
  }, []);

  useEffect(() => {
    if (!token) {
      setState("invalid");
      return;
    }
    void loadSession(token);
  }, [token]);

  useEffect(() => {
    if (state !== "ready") {
      return;
    }
    initializeCanvas();
  }, [state, initializeCanvas]);

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <header className={styles.header}>
          <span className={styles.brand}>
            <i className="fas fa-fingerprint" /> SCMS Plus
          </span>
          <h1 className={styles.title}>{t("page.title")}</h1>
          <p className={styles.subtitle}>{t("page.subtitle")}</p>
        </header>

        {state === "loading" ? (
          <section className={styles.stateCard}>
            <i className={`fas fa-spinner fa-spin ${styles.stateIcon}`} />
            <p className={styles.stateTitle}>{t("state.loading")}</p>
          </section>
        ) : state === "invalid" || state === "ended" ? (
          <section className={styles.stateCard}>
            <i className={`fas ${state === "ended" ? "fa-flag-checkered" : "fa-triangle-exclamation"} ${styles.stateIconMuted}`} />
            <p className={styles.stateTitle}>
              {state === "ended" ? stateMessage || t("state.ended") : stateMessage || t("state.invalid")}
            </p>
            <p className={styles.stateHint}>{state === "ended" ? t("state.endedHint") : t("state.invalidHint")}</p>
          </section>
        ) : state === "success" && result ? (
          <section className={styles.card}>
            <div className={styles.successCard}>
              <i className={`fas fa-circle-check ${styles.successIcon}`} />
              <h2 className={styles.successTitle}>{t("result.title")}</h2>
              <div className={styles.successList}>
                <div className={styles.successRow}>
                  <span className={styles.successLabel}>{t("result.name")}</span>
                  <span className={styles.successValue}>{result.displayName || t("common.authorFallback")}</span>
                </div>
                <div className={styles.successRow}>
                  <span className={styles.successLabel}>{t("result.role")}</span>
                  <span className={styles.successValue}>{roleLabel(result.role, t)}</span>
                </div>
                <div className={styles.successRow}>
                  <span className={styles.successLabel}>{t("result.class")}</span>
                  <span className={styles.successValue}>{classLabel(result, t)}</span>
                </div>
                <div className={styles.successRow}>
                  <span className={styles.successLabel}>{t("result.time")}</span>
                  <span className={styles.successValue}>{formatDateTime(result.checkInAt, t("common.timeFallback"))}</span>
                </div>
              </div>
              <p className={styles.successHint}>{t("result.hint")}</p>
            </div>
          </section>
        ) : session ? (
          <section className={styles.card}>
            <div className={styles.sessionInfo}>
              <p className={styles.clubName}>{session.clubName}</p>
              <h2 className={styles.sessionTitle}>{session.title}</h2>
              <div className={styles.sessionMeta}>
                {session.scoreDelta > 0 ? (
                  <span className={styles.scoreBadge}>
                    <i className="fas fa-coins" /> {t("session.scoreValue", { delta: session.scoreDelta })}
                  </span>
                ) : null}
                {session.scoreRuleName ? <span>{session.scoreRuleName}</span> : null}
                <span className={`${styles.sessionStatus} ${styles.sessionStatusOpen}`}>
                  {t("session.statusOPEN")}
                </span>
              </div>
            </div>

            <form className={styles.form} onSubmit={(event) => event.preventDefault()}>
              <div className={styles.fieldRow}>
                <label className={styles.field}>
                  <span className={styles.label}>{t("form.name")}</span>
                  <input
                    className={styles.input}
                    value={displayName}
                    maxLength={120}
                    placeholder={t("form.namePlaceholder")}
                    onChange={(event) => setDisplayName(event.target.value)}
                  />
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>{t("form.studentNo")}</span>
                  <input
                    className={styles.input}
                    value={studentNo}
                    maxLength={64}
                    inputMode="text"
                    autoCapitalize="characters"
                    placeholder={t("form.studentNoPlaceholder")}
                    onChange={(event) => setStudentNo(event.target.value)}
                  />
                </label>
              </div>

              <div className={styles.signatureField}>
                <div className={styles.signatureHeader}>
                  <span className={styles.label}>{t("form.signature")}</span>
                  <button type="button" className={styles.clearButton} onClick={initializeCanvas}>
                    <i className="fas fa-eraser" /> {t("form.clear")}
                  </button>
                </div>
                <div className={styles.signaturePad}>
                  <canvas
                    ref={canvasRef}
                    className={styles.canvas}
                    onPointerDown={handlePointerDown}
                    onPointerMove={handlePointerMove}
                    onPointerUp={handlePointerEnd}
                    onPointerLeave={handlePointerEnd}
                    onPointerCancel={handlePointerEnd}
                  />
                </div>
                <p className={styles.signatureHint}>
                  <i className="fas fa-pen" /> {t("form.signatureHint")}
                </p>
              </div>

              {errorMessage ? (
                <p className={styles.error} role="alert">
                  <i className="fas fa-circle-exclamation" /> {errorMessage}
                </p>
              ) : null}

              <button type="button" className={styles.submitButton} disabled={submitting} onClick={() => void handleSubmit()}>
                {submitting ? (
                  <>
                    <i className="fas fa-spinner fa-spin" /> {t("form.submitting")}
                  </>
                ) : (
                  <>
                    <i className="fas fa-pen-nib" /> {t("form.submit")}
                  </>
                )}
              </button>
            </form>
          </section>
        ) : null}
      </div>
    </main>
  );

  async function loadSession(shareToken: string) {
    setState("loading");
    setStateMessage("");
    try {
      const response = await fetch(`/api/public/attendance/sessions/${encodeURIComponent(shareToken)}`, {
        cache: "no-store"
      });
      const payload = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: PublicAttendanceSession; message?: string }
        | null;

      if (!response.ok || payload?.success !== true || !payload.data) {
        setState("invalid");
        setStateMessage(payload?.message && payload.message !== "public attendance session request failed" ? payload.message : "");
        return;
      }

      setSession(payload.data);
      if (payload.data.status === "COMPLETED") {
        setState("ended");
        return;
      }
      setState("ready");
    } catch {
      setState("invalid");
      setStateMessage(t("state.networkFailed"));
    }
  }

  function paintBackground(ctx: CanvasRenderingContext2D, width: number, height: number) {
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, width, height);
  }

  function getPoint(event: ReactPointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    if (!canvas) {
      return null;
    }
    const rect = canvas.getBoundingClientRect();
    const { width, height } = sizeRef.current;
    if (!rect.width || !rect.height) {
      return null;
    }
    return {
      x: ((event.clientX - rect.left) / rect.width) * width,
      y: ((event.clientY - rect.top) / rect.height) * height
    };
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLCanvasElement>) {
    const point = getPoint(event);
    const ctx = canvasRef.current?.getContext("2d");
    if (!point || !ctx) {
      return;
    }
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    drawingRef.current = true;
    ctx.beginPath();
    ctx.moveTo(point.x, point.y);
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) {
      return;
    }
    const point = getPoint(event);
    const ctx = canvasRef.current?.getContext("2d");
    if (!point || !ctx) {
      return;
    }
    event.preventDefault();
    ctx.lineTo(point.x, point.y);
    ctx.stroke();
    if (!hasSignature) {
      setHasSignature(true);
    }
  }

  function handlePointerEnd(event: ReactPointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) {
      return;
    }
    drawingRef.current = false;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  }

  async function handleSubmit() {
    if (!session || submitting) {
      return;
    }
    const name = displayName.trim();
    const studentNoValue = studentNo.trim();
    const canvas = canvasRef.current;

    if (!name) {
      setErrorMessage(t("form.nameRequired"));
      return;
    }
    if (!studentNoValue) {
      setErrorMessage(t("form.studentNoRequired"));
      return;
    }
    if (!canvas || !hasSignature) {
      setErrorMessage(t("form.signatureRequired"));
      return;
    }

    setSubmitting(true);
    setErrorMessage("");
    try {
      const response = await fetch(`/api/public/attendance/sessions/${encodeURIComponent(token)}/sign`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          displayName: name,
          studentNo: studentNoValue,
          signatureImage: canvas.toDataURL("image/png")
        })
      });
      const payload = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: PublicAttendanceSignResult; message?: string }
        | null;

      if (!response.ok || payload?.success !== true || !payload.data) {
        setErrorMessage(payload?.message || t("state.signFailed"));
        return;
      }

      setResult(payload.data);
      setState("success");
    } catch {
      setErrorMessage(t("state.networkFailed"));
    } finally {
      setSubmitting(false);
    }
  }
}

function roleLabel(role: string, t: (key: string, vars?: Record<string, string | number>) => string) {
  if (!role) {
    return t("role.MEMBER");
  }
  return t(`role.${role}`);
}

function classLabel(result: PublicAttendanceSignResult, t: (key: string) => string) {
  const gradeLabel = t(`common.gradeLabels.${result.grade}`);
  const parts = [gradeLabel.startsWith("common.gradeLabels") ? "" : gradeLabel, result.className].filter(Boolean);
  return parts.join(" ") || t("common.classFallback");
}

function formatDateTime(value: string | null, fallback: string) {
  if (!value) {
    return fallback;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
