"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import styles from "../../styles/login.module.css";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import {
  extractRoleFromAccessToken,
  persistAuthTokens,
  resolveRoleRedirect,
  StoredAuthTokens
} from "../../lib/auth/session";
import {
  evaluatePasswordRules,
  isValidEmail,
  PasswordRuleState,
  USERNAME_MAX_LENGTH,
  USERNAME_MIN_LENGTH
} from "../../lib/auth/validation";
import { PublicBannerItem } from "../../lib/banner/types";

type AuthMode = "login" | "register";

type LoginFormState = {
  identifier: string;
  password: string;
  remember: boolean;
};

type RegisterFormState = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

type FeedbackFormState = {
  name: string;
  email: string;
  content: string;
};

type LoginRouteResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: {
    accessToken?: string;
    refreshToken?: string;
    accessExpiresInSeconds?: number;
    refreshExpiresInSeconds?: number;
  };
};

type RegisterRouteResponse = {
  success?: boolean;
  code?: string;
  message?: string;
};

const AUTOPLAY_MS = 4000;
const DRAG_SWITCH_THRESHOLD = 40;
const CARD_SWITCH_MS = 220;

type LoginFieldKey = "identifier" | "password";
type RegisterFieldKey = "username" | "email" | "password" | "confirmPassword";

type FormErrors<T extends string> = Partial<Record<T, string>>;

const emptyLoginForm: LoginFormState = {
  identifier: "",
  password: "",
  remember: true
};

const emptyRegisterForm: RegisterFormState = {
  username: "",
  email: "",
  password: "",
  confirmPassword: ""
};

const emptyFeedbackForm: FeedbackFormState = {
  name: "",
  email: "",
  content: ""
};

export default function LoginPage() {
  const t = useT("login");
  const notify = useNotify();
  const [mode, setMode] = useState<AuthMode>("login");
  const [cardTransitioning, setCardTransitioning] = useState(false);
  const [slides, setSlides] = useState<PublicBannerItem[]>([]);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [dragDeltaX, setDragDeltaX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [isPointerActive, setIsPointerActive] = useState(false);
  const [timerVersion, setTimerVersion] = useState(0);

  const [loginSubmitting, setLoginSubmitting] = useState(false);
  const [registerSubmitting, setRegisterSubmitting] = useState(false);
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [loginForm, setLoginForm] = useState<LoginFormState>(emptyLoginForm);
  const [registerForm, setRegisterForm] = useState<RegisterFormState>(emptyRegisterForm);
  const [feedbackForm, setFeedbackForm] = useState<FeedbackFormState>(emptyFeedbackForm);
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showRegisterPassword, setShowRegisterPassword] = useState(false);
  const [showRegisterConfirmPassword, setShowRegisterConfirmPassword] = useState(false);
  const [loginTouched, setLoginTouched] = useState<Record<LoginFieldKey, boolean>>({
    identifier: false,
    password: false
  });
  const [registerTouched, setRegisterTouched] = useState<Record<RegisterFieldKey, boolean>>({
    username: false,
    email: false,
    password: false,
    confirmPassword: false
  });

  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const videoRefs = useRef<Array<HTMLVideoElement | null>>([]);
  const dragStartRef = useRef<number | null>(null);
  const dragDeltaRef = useRef(0);
  const pointerActiveRef = useRef(false);

  const slideCount = slides.length;
  const hasMultiSlides = slideCount > 1;
  const activeSlide = slides[currentSlide] || slides[0];
  const loginErrors = useMemo<FormErrors<LoginFieldKey>>(() => validateLoginForm(loginForm), [loginForm]);
  const registerPasswordRules = useMemo<PasswordRuleState>(
    () => evaluatePasswordRules(registerForm.password),
    [registerForm.password]
  );
  const registerErrors = useMemo<FormErrors<RegisterFieldKey>>(
    () => validateRegisterForm(registerForm, registerPasswordRules),
    [registerForm, registerPasswordRules]
  );
  const loginFormValid = useMemo(() => Object.keys(loginErrors).length === 0, [loginErrors]);
  const registerFormValid = useMemo(() => Object.keys(registerErrors).length === 0, [registerErrors]);

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const goNext = useCallback(() => {
    if (!hasMultiSlides) {
      return;
    }
    setCurrentSlide((prev) => (prev + 1) % slideCount);
  }, [hasMultiSlides, slideCount]);

  const goPrev = useCallback(() => {
    if (!hasMultiSlides) {
      return;
    }
    setCurrentSlide((prev) => (prev - 1 + slideCount) % slideCount);
  }, [hasMultiSlides, slideCount]);

  const restartTimer = useCallback(() => {
    clearTimer();
    setTimerVersion((prev) => prev + 1);
  }, [clearTimer]);

  const switchMode = useCallback((nextMode: AuthMode) => {
    setCardTransitioning(true);
    window.setTimeout(() => {
      setMode(nextMode);
      if (nextMode === "login") {
        setLoginTouched({
          identifier: false,
          password: false
        });
      } else {
        setRegisterTouched({
          username: false,
          email: false,
          password: false,
          confirmPassword: false
        });
      }
      setCardTransitioning(false);
    }, CARD_SWITCH_MS);
  }, []);

  const dragOffsetPercent = useMemo(() => {
    if (!isDragging || !hasMultiSlides) {
      return 0;
    }
    const width = viewportRef.current?.clientWidth || 1;
    return (dragDeltaX / width) * 100;
  }, [dragDeltaX, hasMultiSlides, isDragging]);

  const trackTranslate = useMemo(
    () => -currentSlide * 100 + dragOffsetPercent,
    [currentSlide, dragOffsetPercent]
  );

  const finishDrag = useCallback(
    (endX: number | null) => {
      if (!pointerActiveRef.current) {
        return;
      }

      const startX = dragStartRef.current;
      const delta = startX === null || endX === null ? dragDeltaRef.current : endX - startX;
      if (Math.abs(delta) >= DRAG_SWITCH_THRESHOLD) {
        if (delta > 0) {
          goPrev();
        } else {
          goNext();
        }
      }

      dragStartRef.current = null;
      dragDeltaRef.current = 0;
      pointerActiveRef.current = false;
      setDragDeltaX(0);
      setIsDragging(false);
      setIsPointerActive(false);
      restartTimer();
    },
    [goNext, goPrev, restartTimer]
  );

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!hasMultiSlides) {
      return;
    }
    if (event.pointerType === "mouse" && event.button !== 0) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    dragStartRef.current = event.clientX;
    dragDeltaRef.current = 0;
    pointerActiveRef.current = true;
    setDragDeltaX(0);
    setIsDragging(true);
    setIsPointerActive(true);
    clearTimer();
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    const startX = dragStartRef.current;
    if (!pointerActiveRef.current || startX === null) {
      return;
    }
    const delta = event.clientX - startX;
    dragDeltaRef.current = delta;
    setDragDeltaX(delta);
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    finishDrag(event.clientX);
  };

  const handlePointerCancel = (event: React.PointerEvent<HTMLDivElement>) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    dragStartRef.current = null;
    dragDeltaRef.current = 0;
    pointerActiveRef.current = false;
    setDragDeltaX(0);
    setIsDragging(false);
    setIsPointerActive(false);
    restartTimer();
  };

  const jumpSlide = (index: number) => {
    if (!hasMultiSlides) {
      return;
    }
    setCurrentSlide(((index % slideCount) + slideCount) % slideCount);
    restartTimer();
  };

  const submitLogin = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (loginSubmitting) {
      return;
    }

    if (!loginFormValid) {
      setLoginTouched({
        identifier: true,
        password: true
      });
      notify.warning(resolveValidationMessage(loginErrors, t, "errors.loginRequired"));
      return;
    }

    setLoginSubmitting(true);
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username: loginForm.identifier.trim(),
          password: loginForm.password
        })
      });

      const result = (await response.json()) as LoginRouteResponse;
      if (!response.ok || result.success !== true || !result.data?.accessToken) {
        notify.error(resolveAuthRouteMessage(t, result.code, result.message, "errors.loginFailed"));
        return;
      }

      persistAuthTokens(result.data as StoredAuthTokens, loginForm.remember);

      notify.success(t("login.success"));
      const role = extractRoleFromAccessToken(result.data.accessToken);
      window.location.href = resolveRoleRedirect(role);
    } catch {
      notify.error(t("errors.authServiceUnavailable"));
    } finally {
      setLoginSubmitting(false);
    }
  };

  const submitRegister = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (registerSubmitting) {
      return;
    }

    if (!registerFormValid) {
      setRegisterTouched({
        username: true,
        email: true,
        password: true,
        confirmPassword: true
      });
      notify.warning(resolveValidationMessage(registerErrors, t, "errors.registerRequired"));
      return;
    }

    setRegisterSubmitting(true);
    try {
      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username: registerForm.username.trim(),
          email: registerForm.email.trim(),
          password: registerForm.password
        })
      });

      const result = (await response.json().catch(() => null)) as RegisterRouteResponse | null;
      if (!response.ok || result?.success !== true) {
        notify.error(resolveAuthRouteMessage(t, result?.code, result?.message, "errors.registerFailed"));
        return;
      }

      notify.success(t("register.success"));
      setRegisterForm(emptyRegisterForm);
      setRegisterTouched({
        username: false,
        email: false,
        password: false,
        confirmPassword: false
      });
      setLoginForm((prev) => ({
        ...prev,
        identifier: registerForm.username.trim(),
        password: ""
      }));
      switchMode("login");
    } catch {
      notify.error(t("errors.authServiceUnavailable"));
    } finally {
      setRegisterSubmitting(false);
    }
  };

  const submitFeedback = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (feedbackSubmitting) {
      return;
    }
    if (!feedbackForm.content.trim()) {
      notify.warning(t("errors.feedbackContentRequired"));
      return;
    }

    setFeedbackSubmitting(true);
    try {
      const response = await fetch("/api/feedback", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          name: feedbackForm.name.trim() || t("feedback.anonymousName"),
          email: feedbackForm.email.trim(),
          content: feedbackForm.content.trim()
        })
      });
      const result = (await response.json()) as { success?: boolean };
      if (!response.ok || result.success !== true) {
        throw new Error("FEEDBACK_SUBMIT_FAILED");
      }

      notify.success(t("messages.feedbackSuccess"));
      setFeedbackForm(emptyFeedbackForm);
      setFeedbackOpen(false);
    } catch {
      notify.error(t("messages.feedbackFailed"));
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  useEffect(() => {
    const loadBanners = async () => {
      try {
        const response = await fetch("/api/login-banners", { cache: "no-store" });
        const data = (await response.json()) as {
          success?: boolean;
          banners?: Array<Record<string, unknown>>;
        };

        if (response.ok && data.success && Array.isArray(data.banners)) {
          setSlides(normalizeBannerList(data.banners));
          setCurrentSlide(0);
        }
      } catch {
        setSlides([]);
      }
    };

    void loadBanners();
  }, []);

  useEffect(() => {
    if (currentSlide >= slideCount) {
      setCurrentSlide(0);
    }
  }, [currentSlide, slideCount]);

  useEffect(() => {
    if (!hasMultiSlides || isPointerActive) {
      clearTimer();
      return;
    }

    timerRef.current = setInterval(() => {
      goNext();
    }, AUTOPLAY_MS);

    return () => {
      clearTimer();
    };
  }, [clearTimer, goNext, hasMultiSlides, isPointerActive, timerVersion]);

  useEffect(() => {
    videoRefs.current.forEach((video, index) => {
      if (!video) {
        return;
      }
      const isActive = index === currentSlide && slides[index]?.type === "video";
      if (isActive) {
        video.muted = true;
        video.defaultMuted = true;
        const playPromise = video.play();
        if (playPromise && typeof playPromise.catch === "function") {
          playPromise.catch(() => undefined);
        }
      } else {
        video.pause();
        try {
          video.currentTime = 0;
        } catch {
          // no-op
        }
      }
    });
  }, [currentSlide, slides]);

  useEffect(
    () => () => {
      clearTimer();
    },
    [clearTimer]
  );

  return (
    <main className={styles.page}>
      <section className={styles.hero}>
        <div
          ref={viewportRef}
          className={`${styles.carouselViewport} ${isDragging ? styles.carouselViewportDragging : ""}`}
          onDragStart={(event) => {
            event.preventDefault();
          }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerCancel}
        >
          <ul
            className={`${styles.carouselTrack} ${isDragging ? styles.carouselTrackDragging : ""}`}
            style={{ transform: `translate3d(${trackTranslate}%, 0, 0)` }}
          >
            {slides.map((slide, index) => (
              <li key={`${slide.src}-${index}`} className={styles.slide}>
                {slide.type === "video" ? (
                  <video
                    ref={(element) => {
                      videoRefs.current[index] = element;
                    }}
                    className={styles.slideMedia}
                    src={slide.src}
                    poster={slide.poster}
                    muted
                    loop
                    playsInline
                    preload="metadata"
                  />
                ) : (
                  <img className={styles.slideMedia} src={slide.src} alt={slide.title || ""} />
                )}
                <div className={styles.slideOverlay} />
              </li>
            ))}
          </ul>
        </div>

        {(activeSlide?.title || activeSlide?.desc) && (
          <div className={styles.slideCaption}>
            {activeSlide?.title && <h2>{activeSlide.title}</h2>}
            {activeSlide?.desc && <p>{activeSlide.desc}</p>}
          </div>
        )}

        {hasMultiSlides && (
          <div className={styles.pagination}>
            {slides.map((slide, index) => (
              <button
                key={`dot-${slide.src}-${index}`}
                type="button"
                className={`${styles.paginationDot} ${index === currentSlide ? styles.paginationDotActive : ""}`}
                onClick={() => jumpSlide(index)}
                aria-label={`slide-${index + 1}`}
              />
            ))}
          </div>
        )}

        <div className={`${styles.loginCard} ${cardTransitioning ? styles.cardTransitioning : ""}`}>
          <div className={styles.animatedBlock}>
            <div className={styles.loginHeader}>
              <h2>{mode === "login" ? t("login.headline") : t("register.headline")}</h2>
              <p>{mode === "login" ? t("login.subtitle") : t("register.subtitle")}</p>
            </div>

            {mode === "login" ? (
              <form onSubmit={submitLogin}>
                <div className={styles.inputGroup}>
                  <label htmlFor="login-identifier">{t("login.identifierLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      loginTouched.identifier && loginErrors.identifier ? styles.inputFieldInvalid : ""
                    }`}
                  >
                    <i className="far fa-envelope" />
                    <input
                      id="login-identifier"
                      type="text"
                      value={loginForm.identifier}
                      placeholder={t("login.identifierPlaceholder")}
                      onChange={(event) =>
                        {
                          setLoginForm((prev) => ({
                            ...prev,
                            identifier: event.target.value
                          }));
                          setLoginTouched((prev) => ({
                            ...prev,
                            identifier: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setLoginTouched((prev) => ({
                          ...prev,
                          identifier: true
                        }));
                      }}
                      autoComplete="username"
                    />
                  </div>
                  {loginTouched.identifier && loginErrors.identifier && (
                    <p className={styles.fieldError}>{t(loginErrors.identifier)}</p>
                  )}
                </div>

                <div className={styles.inputGroup}>
                  <label htmlFor="login-password">{t("login.passwordLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      loginTouched.password && loginErrors.password ? styles.inputFieldInvalid : ""
                    }`}
                  >
                    <i className="fas fa-lock" />
                    <input
                      id="login-password"
                      type={showLoginPassword ? "text" : "password"}
                      value={loginForm.password}
                      placeholder={t("login.passwordPlaceholder")}
                      onChange={(event) =>
                        {
                          setLoginForm((prev) => ({
                            ...prev,
                            password: event.target.value
                          }));
                          setLoginTouched((prev) => ({
                            ...prev,
                            password: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setLoginTouched((prev) => ({
                          ...prev,
                          password: true
                        }));
                      }}
                      autoComplete="current-password"
                    />
                    <button
                      type="button"
                      className={styles.inputAction}
                      aria-label={showLoginPassword ? t("password.hide") : t("password.show")}
                      onClick={() => {
                        setShowLoginPassword((prev) => !prev);
                      }}
                    >
                      <i className={`fas ${showLoginPassword ? "fa-eye-slash" : "fa-eye"}`} />
                    </button>
                  </div>
                  {loginTouched.password && loginErrors.password && (
                    <p className={styles.fieldError}>{t(loginErrors.password)}</p>
                  )}
                </div>

                <div className={styles.options}>
                  <label className={styles.rememberMe}>
                    <input
                      type="checkbox"
                      checked={loginForm.remember}
                      onChange={(event) =>
                        setLoginForm((prev) => ({
                          ...prev,
                          remember: event.target.checked
                        }))
                      }
                    />
                    {t("login.rememberMe")}
                  </label>
                  <a
                    href="#"
                    className={styles.textLink}
                    onClick={(event) => {
                      event.preventDefault();
                      notify.info(t("errors.forgotNotAvailable"));
                    }}
                  >
                    {t("login.forgot")}
                  </a>
                </div>

                <button
                  type="submit"
                  className={styles.submitBtn}
                  disabled={loginSubmitting || !loginFormValid}
                >
                  <span>{loginSubmitting ? t("login.loading") : t("login.button")}</span>
                  <i className={`fas ${loginSubmitting ? "fa-spinner fa-spin" : "fa-arrow-right"}`} />
                </button>

                <p className={styles.switchPrompt}>
                  {t("login.switchText")}{" "}
                  <button
                    type="button"
                    onClick={() => {
                      switchMode("register");
                    }}
                  >
                    {t("login.switchAction")}
                  </button>
                </p>
              </form>
            ) : (
              <form onSubmit={submitRegister}>
                <div className={styles.inputGroup}>
                  <label htmlFor="register-username">{t("register.usernameLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      registerTouched.username && registerErrors.username ? styles.inputFieldInvalid : ""
                    }`}
                  >
                    <i className="fas fa-user" />
                    <input
                      id="register-username"
                      type="text"
                      value={registerForm.username}
                      placeholder={t("register.usernamePlaceholder")}
                      onChange={(event) =>
                        {
                          setRegisterForm((prev) => ({
                            ...prev,
                            username: event.target.value
                          }));
                          setRegisterTouched((prev) => ({
                            ...prev,
                            username: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setRegisterTouched((prev) => ({
                          ...prev,
                          username: true
                        }));
                      }}
                      autoComplete="username"
                    />
                  </div>
                  {registerTouched.username && registerErrors.username && (
                    <p className={styles.fieldError}>{t(registerErrors.username)}</p>
                  )}
                </div>

                <div className={styles.inputGroup}>
                  <label htmlFor="register-email">{t("register.emailLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      registerTouched.email && registerErrors.email ? styles.inputFieldInvalid : ""
                    }`}
                  >
                    <i className="far fa-envelope" />
                    <input
                      id="register-email"
                      type="email"
                      value={registerForm.email}
                      placeholder={t("register.emailPlaceholder")}
                      onChange={(event) =>
                        {
                          setRegisterForm((prev) => ({
                            ...prev,
                            email: event.target.value
                          }));
                          setRegisterTouched((prev) => ({
                            ...prev,
                            email: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setRegisterTouched((prev) => ({
                          ...prev,
                          email: true
                        }));
                      }}
                      autoComplete="email"
                    />
                  </div>
                  {registerTouched.email && registerErrors.email && (
                    <p className={styles.fieldError}>{t(registerErrors.email)}</p>
                  )}
                </div>

                <div className={styles.inputGroup}>
                  <label htmlFor="register-password">{t("register.passwordLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      registerTouched.password && registerErrors.password ? styles.inputFieldInvalid : ""
                    }`}
                  >
                    <i className="fas fa-lock" />
                    <input
                      id="register-password"
                      type={showRegisterPassword ? "text" : "password"}
                      value={registerForm.password}
                      placeholder={t("register.passwordPlaceholder")}
                      onChange={(event) =>
                        {
                          setRegisterForm((prev) => ({
                            ...prev,
                            password: event.target.value
                          }));
                          setRegisterTouched((prev) => ({
                            ...prev,
                            password: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setRegisterTouched((prev) => ({
                          ...prev,
                          password: true
                        }));
                      }}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className={styles.inputAction}
                      aria-label={showRegisterPassword ? t("password.hide") : t("password.show")}
                      onClick={() => {
                        setShowRegisterPassword((prev) => !prev);
                      }}
                    >
                      <i className={`fas ${showRegisterPassword ? "fa-eye-slash" : "fa-eye"}`} />
                    </button>
                  </div>
                  {registerTouched.password && registerErrors.password && (
                    <p className={styles.fieldError}>{t(registerErrors.password)}</p>
                  )}
                  <ul className={styles.passwordRules}>
                    <li className={registerPasswordRules.minLength ? styles.passwordRuleDone : ""}>
                      {t("validation.passwordRule.minLength")}
                    </li>
                    <li className={registerPasswordRules.hasUpper ? styles.passwordRuleDone : ""}>
                      {t("validation.passwordRule.hasUpper")}
                    </li>
                    <li className={registerPasswordRules.hasLower ? styles.passwordRuleDone : ""}>
                      {t("validation.passwordRule.hasLower")}
                    </li>
                    <li className={registerPasswordRules.hasDigit ? styles.passwordRuleDone : ""}>
                      {t("validation.passwordRule.hasDigit")}
                    </li>
                    <li className={registerPasswordRules.hasSpecial ? styles.passwordRuleDone : ""}>
                      {t("validation.passwordRule.hasSpecial")}
                    </li>
                  </ul>
                </div>

                <div className={styles.inputGroup}>
                  <label htmlFor="register-confirm">{t("register.confirmLabel")}</label>
                  <div
                    className={`${styles.inputField} ${
                      registerTouched.confirmPassword && registerErrors.confirmPassword
                        ? styles.inputFieldInvalid
                        : ""
                    }`}
                  >
                    <i className="fas fa-lock" />
                    <input
                      id="register-confirm"
                      type={showRegisterConfirmPassword ? "text" : "password"}
                      value={registerForm.confirmPassword}
                      placeholder={t("register.confirmPlaceholder")}
                      onChange={(event) =>
                        {
                          setRegisterForm((prev) => ({
                            ...prev,
                            confirmPassword: event.target.value
                          }));
                          setRegisterTouched((prev) => ({
                            ...prev,
                            confirmPassword: true
                          }));
                        }
                      }
                      onBlur={() => {
                        setRegisterTouched((prev) => ({
                          ...prev,
                          confirmPassword: true
                        }));
                      }}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className={styles.inputAction}
                      aria-label={showRegisterConfirmPassword ? t("password.hide") : t("password.show")}
                      onClick={() => {
                        setShowRegisterConfirmPassword((prev) => !prev);
                      }}
                    >
                      <i className={`fas ${showRegisterConfirmPassword ? "fa-eye-slash" : "fa-eye"}`} />
                    </button>
                  </div>
                  {registerTouched.confirmPassword && registerErrors.confirmPassword && (
                    <p className={styles.fieldError}>{t(registerErrors.confirmPassword)}</p>
                  )}
                </div>

                <button
                  type="submit"
                  className={styles.submitBtn}
                  disabled={registerSubmitting || !registerFormValid}
                >
                  <span>{registerSubmitting ? t("register.loading") : t("register.button")}</span>
                  <i className={`fas ${registerSubmitting ? "fa-spinner fa-spin" : "fa-arrow-right"}`} />
                </button>

                <p className={styles.switchPrompt}>
                  {t("register.switchText")}{" "}
                  <button
                    type="button"
                    onClick={() => {
                      switchMode("login");
                    }}
                  >
                    {t("register.switchAction")}
                  </button>
                </p>
              </form>
            )}
          </div>
        </div>

        <div className={styles.footerNote}>{t("footer.copyright")}</div>

        <a href="/" className={styles.backHomeEntry}>
          <i className="fas fa-house" />
          {t("footer.backHome")}
        </a>

        <button
          type="button"
          className={styles.feedbackEntry}
          onClick={() => {
            setFeedbackOpen(true);
          }}
        >
          <i className="fas fa-comment-dots" />
          {t("feedback.entry")}
        </button>
      </section>

      {feedbackOpen && (
        <div
          className={styles.feedbackModal}
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setFeedbackOpen(false);
            }
          }}
        >
          <div className={styles.feedbackCard}>
            <div className={styles.feedbackHead}>
              <h3>
                <i className="fas fa-paper-plane" /> {t("feedback.title")}
              </h3>
              <button
                type="button"
                className={styles.feedbackClose}
                onClick={() => {
                  setFeedbackOpen(false);
                }}
                aria-label="close-feedback"
              >
                <i className="fas fa-xmark" />
              </button>
            </div>
            <div className={styles.feedbackBody}>
              <form onSubmit={submitFeedback}>
                <div className={styles.inputGroup}>
                  <label htmlFor="feedback-name">{t("feedback.nameLabel")}</label>
                  <div className={styles.inputField}>
                    <input
                      id="feedback-name"
                      type="text"
                      maxLength={30}
                      value={feedbackForm.name}
                      placeholder={t("feedback.namePlaceholder")}
                      onChange={(event) =>
                        setFeedbackForm((prev) => ({
                          ...prev,
                          name: event.target.value
                        }))
                      }
                    />
                  </div>
                </div>
                <div className={styles.inputGroup}>
                  <label htmlFor="feedback-email">{t("feedback.emailLabel")}</label>
                  <div className={styles.inputField}>
                    <input
                      id="feedback-email"
                      type="email"
                      maxLength={80}
                      value={feedbackForm.email}
                      placeholder={t("feedback.emailPlaceholder")}
                      onChange={(event) =>
                        setFeedbackForm((prev) => ({
                          ...prev,
                          email: event.target.value
                        }))
                      }
                    />
                  </div>
                </div>
                <div className={styles.inputGroup}>
                  <label htmlFor="feedback-content">{t("feedback.contentLabel")}</label>
                  <div className={styles.inputField}>
                    <textarea
                      id="feedback-content"
                      rows={4}
                      maxLength={400}
                      value={feedbackForm.content}
                      placeholder={t("feedback.contentPlaceholder")}
                      onChange={(event) =>
                        setFeedbackForm((prev) => ({
                          ...prev,
                          content: event.target.value
                        }))
                      }
                    />
                  </div>
                </div>
                <div className={styles.feedbackActions}>
                  <button
                    type="button"
                    className={styles.feedbackCancel}
                    onClick={() => {
                      setFeedbackOpen(false);
                    }}
                  >
                    {t("feedback.cancel")}
                  </button>
                  <button type="submit" className={styles.feedbackSubmit} disabled={feedbackSubmitting}>
                    {feedbackSubmitting ? t("feedback.submitting") : t("feedback.submit")}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

function toMediaUrl(value: unknown): string {
  const text = String(value || "").trim().replace(/\\/g, "/");
  if (!text) {
    return "";
  }
  if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
    return text;
  }
  return `/${text.replace(/^\.?\//, "")}`;
}

function normalizeBannerList(items: Array<Record<string, unknown>>): PublicBannerItem[] {
  return items
    .map((item) => {
      const type = item.type === "video" ? "video" : "image";
      const src = toMediaUrl(item.src);
      const poster = toMediaUrl(item.poster);
      const title = typeof item.title === "string" ? item.title : undefined;
      const desc = typeof item.desc === "string" ? item.desc : undefined;

      if (!src) {
        return null;
      }

      return {
        type,
        src,
        poster: poster || undefined,
        title,
        desc
      } as PublicBannerItem;
    })
    .filter((item): item is PublicBannerItem => item !== null);
}

function validateLoginForm(form: LoginFormState): FormErrors<LoginFieldKey> {
  const errors: FormErrors<LoginFieldKey> = {};
  if (!form.identifier.trim()) {
    errors.identifier = "validation.identifierRequired";
  }
  if (!form.password) {
    errors.password = "validation.passwordRequired";
  }
  return errors;
}

function validateRegisterForm(
  form: RegisterFormState,
  passwordRules: PasswordRuleState
): FormErrors<RegisterFieldKey> {
  const errors: FormErrors<RegisterFieldKey> = {};
  const username = form.username.trim();
  const email = form.email.trim();

  if (!username) {
    errors.username = "validation.usernameRequired";
  } else if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
    errors.username = "validation.usernameLength";
  }

  if (!email) {
    errors.email = "validation.emailRequired";
  } else if (!isValidEmail(email)) {
    errors.email = "validation.emailInvalid";
  }

  if (!form.password) {
    errors.password = "validation.passwordRequired";
  } else if (!passwordRules.minLength || !passwordRules.maxLength) {
    errors.password = "validation.passwordLength";
  } else if (
    !passwordRules.hasUpper ||
    !passwordRules.hasLower ||
    !passwordRules.hasDigit ||
    !passwordRules.hasSpecial
  ) {
    errors.password = "validation.passwordWeak";
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = "validation.confirmPasswordRequired";
  } else if (form.confirmPassword !== form.password) {
    errors.confirmPassword = "validation.confirmPasswordMismatch";
  }

  return errors;
}

function resolveValidationMessage<T extends string>(
  errors: FormErrors<T>,
  t: (key: string) => string,
  fallbackKey: string
): string {
  for (const key of Object.keys(errors) as T[]) {
    const errorKey = errors[key];
    if (errorKey) {
      return t(errorKey);
    }
  }
  return t(fallbackKey);
}

function resolveAuthRouteMessage(
  t: (key: string) => string,
  code: string | undefined,
  message: string | undefined,
  fallbackKey: string
): string {
  const text = String(message || "").trim();
  if (text) {
    return text;
  }
  const codeKeyMap: Record<string, string> = {
    AUTH_LOGIN_INVALID_INPUT: "errors.loginRequired",
    AUTH_REGISTER_INVALID_INPUT: "errors.registerRequired",
    AUTH_REGISTER_USERNAME_INVALID: "validation.usernameLength",
    AUTH_REGISTER_INVALID_EMAIL: "validation.emailInvalid",
    AUTH_REGISTER_WEAK_PASSWORD: "validation.passwordWeak",
    AUTH_BACKEND_UNREACHABLE: "errors.authServiceUnavailable"
  };
  return t(codeKeyMap[code || ""] || fallbackKey);
}
