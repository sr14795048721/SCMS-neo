"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { Button, Form, Input, Modal } from "antd";
import styles from "../styles/home.module.css";
import { useT } from "../lib/i18n/useT";
import { formatNewsDate, formatNewsMonthDayParts } from "../lib/news/date";
import { useNotify } from "../lib/notify/useNotify";

type BannerItem = {
  type: "image" | "video";
  title?: string;
  desc?: string;
  src: string;
  poster?: string;
};

type NewsItem = {
  id: number;
  title: string;
  publishedAt: string | null;
  cover?: string;
  authorName: string;
  viewCount: number;
};

type HomeNewsPayload = {
  carousel: NewsItem[];
  textList: NewsItem[];
};

type ClubItem = {
  id: string;
  name: string;
  description?: string;
  memberCount?: number;
};

type FeedbackForm = {
  name?: string;
  email?: string;
  content: string;
};

const { TextArea } = Input;
const BANNER_PLACEHOLDER = "https://via.placeholder.com/1920x1080/8B1A1A/ffffff";
const NEWS_COVER_PLACEHOLDER = "https://via.placeholder.com/700x360/8B1A1A/ffffff";
const LOGO_PLACEHOLDER = "https://via.placeholder.com/120x120/8B1A1A/ffffff";
const BANNER_INTERVAL_MS = 5000;
const DRAG_SWITCH_THRESHOLD = 40;

function buildClubTitleStyle(name: string): CSSProperties {
  const length = Array.from(name.trim()).length;
  let scale = 1;
  let letterSpacing = "-0.03em";

  if (length >= 12) {
    scale = 0.72;
    letterSpacing = "-0.08em";
  } else if (length >= 10) {
    scale = 0.8;
    letterSpacing = "-0.06em";
  } else if (length >= 8) {
    scale = 0.88;
    letterSpacing = "-0.04em";
  }

  return {
    "--club-title-scale": String(scale),
    "--club-title-letter-spacing": letterSpacing
  } as CSSProperties;
}

export default function HomePage() {
  const tCommon = useT("common");
  const tHome = useT("home");
  const notify = useNotify();
  const [isScrolled, setIsScrolled] = useState(false);
  const [banners, setBanners] = useState<BannerItem[]>([]);
  const [homeNews, setHomeNews] = useState<HomeNewsPayload>({ carousel: [], textList: [] });
  const [clubs, setClubs] = useState<ClubItem[]>([]);
  const [currentBanner, setCurrentBanner] = useState(0);
  const [currentNews, setCurrentNews] = useState(0);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
  const [homeLoading, setHomeLoading] = useState(true);
  const [clubsLoading, setClubsLoading] = useState(true);
  const [form] = Form.useForm<FeedbackForm>();

  const [dragStartX, setDragStartX] = useState<number | null>(null);
  const [dragDeltaX, setDragDeltaX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [isPointerActive, setIsPointerActive] = useState(false);
  const [bannerTimerVersion, setBannerTimerVersion] = useState(0);

  const videoRefs = useRef<Array<HTMLVideoElement | null>>([]);
  const bannerTimerRef = useRef<NodeJS.Timeout | null>(null);
  const newsTimerRef = useRef<NodeJS.Timeout | null>(null);
  const bannerViewportRef = useRef<HTMLDivElement | null>(null);
  const topBarRef = useRef<HTMLDivElement | null>(null);
  const navBarRef = useRef<HTMLElement | null>(null);
  const bannerBoxRef = useRef<HTMLDivElement | null>(null);

  const featuredNews = useMemo(() => homeNews.carousel, [homeNews.carousel]);
  const textNews = useMemo(() => homeNews.textList, [homeNews.textList]);
  const currentFeaturedNewsDate = useMemo(
    () => formatNewsDate(featuredNews[currentNews]?.publishedAt),
    [currentNews, featuredNews]
  );
  const hasBanners = banners.length > 0;
  const hasMultipleBanners = banners.length > 1;

  const clearBannerTimer = useCallback(() => {
    if (bannerTimerRef.current) {
      clearInterval(bannerTimerRef.current);
      bannerTimerRef.current = null;
    }
  }, []);

  const goNext = useCallback(() => {
    if (!hasMultipleBanners) {
      return;
    }
    setCurrentBanner((prev) => (prev + 1) % banners.length);
  }, [banners.length, hasMultipleBanners]);

  const goPrev = useCallback(() => {
    if (!hasMultipleBanners) {
      return;
    }
    setCurrentBanner((prev) => (prev - 1 + banners.length) % banners.length);
  }, [banners.length, hasMultipleBanners]);

  const restartBannerTimer = useCallback(() => {
    clearBannerTimer();
    setBannerTimerVersion((prev) => prev + 1);
  }, [clearBannerTimer]);

  const jumpBanner = useCallback(
    (index: number) => {
      if (!hasMultipleBanners) {
        return;
      }
      setCurrentBanner(((index % banners.length) + banners.length) % banners.length);
      restartBannerTimer();
    },
    [banners.length, hasMultipleBanners, restartBannerTimer]
  );

  const finishDrag = useCallback(
    (endX: number | null) => {
      if (!isPointerActive) {
        return;
      }

      const delta = dragStartX === null || endX === null ? dragDeltaX : endX - dragStartX;

      if (Math.abs(delta) >= DRAG_SWITCH_THRESHOLD) {
        if (delta > 0) {
          goPrev();
        } else {
          goNext();
        }
      }

      setDragStartX(null);
      setDragDeltaX(0);
      setIsDragging(false);
      setIsPointerActive(false);
      restartBannerTimer();
    },
    [dragDeltaX, dragStartX, goNext, goPrev, isPointerActive, restartBannerTimer]
  );

  const handleBannerPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!hasMultipleBanners) {
      return;
    }
    if (event.pointerType === "mouse" && event.button !== 0) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    setDragStartX(event.clientX);
    setDragDeltaX(0);
    setIsDragging(true);
    setIsPointerActive(true);
    clearBannerTimer();
  };

  const handleBannerPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!isPointerActive || dragStartX === null) {
      return;
    }
    setDragDeltaX(event.clientX - dragStartX);
  };

  const handleBannerPointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    finishDrag(event.clientX);
  };

  const handleBannerPointerCancel = (event: React.PointerEvent<HTMLDivElement>) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    setDragStartX(null);
    setDragDeltaX(0);
    setIsDragging(false);
    setIsPointerActive(false);
    restartBannerTimer();
  };

  const handleQuickLink = (action: "login" | "clubs" | "activity" | "feedback") => {
    if (action === "login") {
      window.location.href = "/login";
      return;
    }
    if (action === "activity") {
      window.location.href = "/student/activities";
      return;
    }
    if (action === "feedback") {
      setFeedbackOpen(true);
      return;
    }
    if (action === "clubs") {
      document.getElementById("clubs")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }
  };

  const submitFeedback = async () => {
    try {
      const values = await form.validateFields();
      setFeedbackSubmitting(true);
      const resp = await fetch("/api/feedback", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          name: values.name || tHome("modal.feedback.anonymousName"),
          email: values.email || "",
          content: values.content
        })
      });
      const result = (await resp.json()) as { success?: boolean };
      if (result.success) {
        notify.success(tHome("messages.feedbackSuccess"));
        setFeedbackOpen(false);
        form.resetFields();
      } else {
        notify.error(tHome("messages.feedbackFailed"));
      }
    } catch {
      notify.error(tHome("messages.feedbackSubmitFailed"));
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  const dragOffsetPercent = useMemo(() => {
    if (!isDragging || !hasMultipleBanners) {
      return 0;
    }
    const viewportWidth = bannerViewportRef.current?.clientWidth || 1;
    return (dragDeltaX / viewportWidth) * 100;
  }, [dragDeltaX, hasMultipleBanners, isDragging]);

  const trackTranslate = useMemo(
    () => -currentBanner * 100 + dragOffsetPercent,
    [currentBanner, dragOffsetPercent]
  );

  useEffect(() => {
    const updateScrolledState = () => {
      const topBarHeight = topBarRef.current?.offsetHeight ?? 0;
      const navBarHeight = navBarRef.current?.offsetHeight ?? 0;
      const headerHeight = topBarHeight + navBarHeight;
      const bannerBottom =
        (bannerBoxRef.current?.offsetTop ?? 0) + (bannerBoxRef.current?.offsetHeight ?? window.innerHeight);

      setIsScrolled(window.scrollY + headerHeight >= bannerBottom - 1);
    };

    updateScrolledState();
    window.addEventListener("scroll", updateScrolledState, { passive: true });
    window.addEventListener("resize", updateScrolledState);
    return () => {
      window.removeEventListener("scroll", updateScrolledState);
      window.removeEventListener("resize", updateScrolledState);
    };
  }, []);

  useEffect(() => {
    void loadHomepageData();
  }, []);

  useEffect(() => {
    if (featuredNews.length <= 1) {
      return;
    }
    newsTimerRef.current = setInterval(() => {
      setCurrentNews((prev) => (prev + 1) % featuredNews.length);
    }, 4000);
    return () => {
      if (newsTimerRef.current) {
        clearInterval(newsTimerRef.current);
      }
    };
  }, [featuredNews.length]);

  useEffect(() => {
    if (!hasMultipleBanners || isPointerActive) {
      clearBannerTimer();
      return;
    }

    const current = banners[currentBanner];
    const activeVideo = videoRefs.current[currentBanner];

    clearBannerTimer();

    if (current?.type === "video" && activeVideo) {
      activeVideo.currentTime = 0;
      const playPromise = activeVideo.play();
      if (playPromise && typeof playPromise.catch === "function") {
        playPromise.catch(() => {
          bannerTimerRef.current = setInterval(goNext, BANNER_INTERVAL_MS);
        });
      }
      activeVideo.onended = goNext;
      activeVideo.onerror = () => {
        bannerTimerRef.current = setInterval(goNext, BANNER_INTERVAL_MS);
      };

      return () => {
        clearBannerTimer();
        activeVideo.onended = null;
        activeVideo.onerror = null;
      };
    }

    bannerTimerRef.current = setInterval(goNext, BANNER_INTERVAL_MS);

    return () => {
      clearBannerTimer();
      if (activeVideo) {
        activeVideo.onended = null;
        activeVideo.onerror = null;
      }
    };
  }, [
    bannerTimerVersion,
    banners,
    clearBannerTimer,
    currentBanner,
    goNext,
    hasMultipleBanners,
    isPointerActive
  ]);

  useEffect(
    () => () => {
      clearBannerTimer();
      if (newsTimerRef.current) {
        clearInterval(newsTimerRef.current);
      }
    },
    [clearBannerTimer]
  );

  return (
    <main className={`${styles.page} min-h-screen`}>
      <div id="top" />

      <div ref={topBarRef} className={styles.topBar}>
        <div className={`${styles.container} ${styles.clearfix}`}>
          <div className={`${styles.pullLeft} ${styles.headerLink}`}>
            <i className="fas fa-phone" /> {tCommon("contact.phone")}
            <span>|</span>
            <i className="fas fa-envelope" /> {tCommon("contact.email")}
          </div>
          <div className={`${styles.pullRight} ${styles.headerLink}`}>
            <a href="/login">
              <i className="fas fa-sign-in-alt" /> {tHome("top.login")}
            </a>
            <span>|</span>
            <a href="#">{tHome("top.service")}</a>
          </div>
        </div>
      </div>

      <nav ref={navBarRef} className={`${styles.navBar} ${isScrolled ? styles.scrolled : ""}`}>
        <div className={styles.navContainer}>
          <ul className={styles.navList}>
            <li>
              <a href="#news">{tHome("nav.news")}</a>
            </li>
            <li>
              <a href="#clubs">{tHome("nav.clubs")}</a>
            </li>
          </ul>
          <a
            href="#top"
            className={styles.logoNav}
            onClick={(event) => {
              event.preventDefault();
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
          >
            <img
              src="/image/main/%E6%A0%A1%E5%BE%BD.png"
              alt={tHome("nav.logoAlt")}
              onError={(event) => {
                event.currentTarget.src = LOGO_PLACEHOLDER;
              }}
            />
          </a>
          <ul className={styles.navList}>
            <li>
              <a href="#activities">{tHome("nav.activities")}</a>
            </li>
            <li>
              <a href="#about">{tHome("nav.about")}</a>
            </li>
          </ul>
        </div>
      </nav>

      <div ref={bannerBoxRef} className={styles.bannerBox}>
        <section className={styles.banner}>
          <div
            ref={bannerViewportRef}
            className={`${styles.bannerViewport} ${isDragging ? styles.bannerViewportDragging : ""}`}
            onPointerDown={handleBannerPointerDown}
            onPointerMove={handleBannerPointerMove}
            onPointerUp={handleBannerPointerUp}
            onPointerCancel={handleBannerPointerCancel}
          >
            {hasBanners ? (
              <ul
                className={`${styles.bannerList} ${isDragging ? styles.bannerListDragging : ""}`}
                style={{ transform: `translate3d(${trackTranslate}%, 0, 0)` }}
              >
                {banners.map((banner, index) => (
                  <li key={`${banner.src}-${index}`} className={styles.bannerItem}>
                    {banner.type === "video" ? (
                      <video
                        ref={(el) => {
                          videoRefs.current[index] = el;
                        }}
                        src={banner.src}
                        poster={banner.poster}
                        muted
                        playsInline
                        preload={index === currentBanner ? "metadata" : "none"}
                      />
                    ) : (
                      <img
                        src={banner.src}
                        alt={banner.title || ""}
                        loading={index === 0 ? "eager" : "lazy"}
                        fetchPriority={index === 0 ? "high" : "auto"}
                        decoding="async"
                        onError={(event) => {
                          event.currentTarget.src = BANNER_PLACEHOLDER;
                        }}
                      />
                    )}
                    {(banner.title || banner.desc) && (
                      <div className={styles.slideContent}>
                        {banner.title && <h2>{banner.title}</h2>}
                        {banner.desc && <p>{banner.desc}</p>}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              <div className={styles.bannerEmpty}>
                <div className={styles.bannerEmptyInner}>
                  <i className={`fas ${homeLoading ? "fa-spinner fa-spin" : "fa-images"}`} />
                  <p>{homeLoading ? tHome("banner.loading") : tHome("banner.empty")}</p>
                </div>
              </div>
            )}
          </div>

          <button
            type="button"
            className={`${styles.bannerArrow} ${styles.bannerArrowPrev}`}
            onPointerDown={(event) => event.stopPropagation()}
            onClick={() => {
              goPrev();
              restartBannerTimer();
            }}
            aria-label={tHome("carousel.prev")}
            disabled={!hasMultipleBanners}
          >
            <i className="fas fa-angle-left" />
          </button>
          <button
            type="button"
            className={`${styles.bannerArrow} ${styles.bannerArrowNext}`}
            onPointerDown={(event) => event.stopPropagation()}
            onClick={() => {
              goNext();
              restartBannerTimer();
            }}
            aria-label={tHome("carousel.next")}
            disabled={!hasMultipleBanners}
          >
            <i className="fas fa-angle-right" />
          </button>

          {hasMultipleBanners && (
            <div className={styles.bannerDots}>
              {banners.map((_, index) => (
                <span
                  key={`dot-${index}`}
                  className={index === currentBanner ? styles.bannerDotActive : ""}
                  onClick={() => jumpBanner(index)}
                />
              ))}
            </div>
          )}
        </section>
      </div>

      <section id="news" className={`${styles.section} ${styles.sectionWhite}`}>
        <div className={styles.container}>
          <div className={`${styles.titleWrapper} ${styles.clearfix}`}>
            <div className={`${styles.pullLeft} ${styles.ttBox}`}>
              <span>{tHome("sections.news.title")}</span>
              <span className={styles.ttEn}>{tHome("sections.news.en")}</span>
            </div>
            <a className={`${styles.pullRight} ${styles.ttMore}`} href="/news">
              {tCommon("action.viewMore")} <i className="fa fa-angle-right" />
            </a>
          </div>

          <div className={styles.newsRow}>
            <div>
              {featuredNews[currentNews] ? (
                <a href={`/news/${featuredNews[currentNews].id}.html`} className={styles.imgSlickItem}>
                  <img
                    src={featuredNews[currentNews].cover || NEWS_COVER_PLACEHOLDER}
                    alt={featuredNews[currentNews].title}
                    loading={currentNews === 0 ? "eager" : "lazy"}
                    decoding="async"
                    onError={(event) => {
                      event.currentTarget.src = NEWS_COVER_PLACEHOLDER;
                    }}
                  />
                  <div className={styles.imgSlickBottom}>
                    <div className={styles.imgSlickDate}>{currentFeaturedNewsDate}</div>
                    <div className={styles.imgSlickTitle}>{featuredNews[currentNews].title}</div>
                  </div>
                </a>
              ) : (
                <div className={styles.loadingBlock}>
                  <i className="fas fa-spinner fa-spin" />
                  <p>{homeLoading ? tCommon("state.loading") : tHome("news.emptyCarousel")}</p>
                </div>
              )}
            </div>
            <div className={styles.newsRight}>
              <ul className={styles.imgNewsList}>
                {textNews.map((item) => {
                  const { month, day } = formatNewsMonthDayParts(item.publishedAt);

                  return (
                    <li key={item.id}>
                      <a href={`/news/${item.id}.html`}>
                        <div className={styles.imgNewsDate}>
                          <span>{day}</span>
                          {month}
                          {tHome("news.monthSuffix")}
                        </div>
                        <div className={styles.imgNewsTitle}>{item.title}</div>
                      </a>
                    </li>
                  );
                })}
                {!textNews.length && !homeLoading && (
                  <li>
                    <div className={styles.loadingBlock}>
                      <p>{tHome("news.emptyTextList")}</p>
                    </div>
                  </li>
                )}
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section id="activities" className={styles.section}>
        <div className={styles.container}>
          <div className={`${styles.titleWrapper} ${styles.clearfix}`}>
            <div className={`${styles.pullLeft} ${styles.ttBox}`}>
              <span>{tHome("sections.quickLinks.title")}</span>
              <span className={styles.ttEn}>{tHome("sections.quickLinks.en")}</span>
            </div>
            <a className={`${styles.pullRight} ${styles.ttMore}`} href="#">
              {tCommon("action.all")}
              <i className="fa fa-angle-right" />
            </a>
          </div>

          <div className={styles.quickLinks}>
            <div className={styles.quickLink} onClick={() => handleQuickLink("login")}>
              <i className="fas fa-user-circle" />
              <h3>{tHome("quick.login.title")}</h3>
              <p>{tHome("quick.login.desc")}</p>
            </div>
            <div className={styles.quickLink} onClick={() => handleQuickLink("clubs")}>
              <i className="fas fa-users" />
              <h3>{tHome("quick.clubs.title")}</h3>
              <p>{tHome("quick.clubs.desc")}</p>
            </div>
            <div className={styles.quickLink} onClick={() => handleQuickLink("activity")}>
              <i className="fas fa-calendar-check" />
              <h3>{tHome("quick.activity.title")}</h3>
              <p>{tHome("quick.activity.desc")}</p>
            </div>
            <div className={styles.quickLink} onClick={() => handleQuickLink("feedback")}>
              <i className="fas fa-comments" />
              <h3>{tHome("quick.feedback.title")}</h3>
              <p>{tHome("quick.feedback.desc")}</p>
            </div>
          </div>
        </div>
      </section>

      <section id="clubs" className={`${styles.section} ${styles.sectionWhite}`}>
        <div className={styles.container}>
          <div className={`${styles.titleWrapper} ${styles.clearfix}`}>
            <div className={`${styles.pullLeft} ${styles.ttBox}`}>
              <span>{tHome("sections.clubs.title")}</span>
              <span className={styles.ttEn}>{tHome("sections.clubs.en")}</span>
            </div>
            <a className={`${styles.pullRight} ${styles.ttMore}`} href="#clubs">
              {tCommon("action.viewMore")} <i className="fa fa-angle-right" />
            </a>
          </div>

          <div className={styles.quickLinks}>
            {clubs.length > 0 ? (
              clubs.map((club) => (
                <div key={club.id} className={styles.quickLink}>
                  <i className="fas fa-users" />
                  <h3 className={styles.clubCardTitle} style={buildClubTitleStyle(club.name)} title={club.name}>
                    {club.name}
                  </h3>
                  <p>{club.description || tHome("clubs.fallbackDescription")}</p>
                  <div style={{ marginTop: "10px", color: "#8B1A1A", fontSize: "14px" }}>
                    <i className="fas fa-user" /> {tHome("clubs.memberCount", { count: club.memberCount || 0 })}
                  </div>
                </div>
              ))
            ) : clubsLoading ? (
              <div className={styles.loadingBlock}>
                <i className="fas fa-spinner fa-spin" />
                <p>{tHome("loading.clubs")}</p>
              </div>
            ) : (
              <div className={styles.loadingBlock}>
                <i className="fas fa-users-slash" />
                <p>{tHome("empty.recommendedClubs")}</p>
              </div>
            )}
          </div>
        </div>
      </section>

      <footer id="about" className={styles.footer}>
        <div className={styles.container}>
          <div className={styles.footerContent}>
            <div>
              <h4>{tHome("footer.about.title")}</h4>
              <a href="#">{tHome("footer.about.intro")}</a>
              <a href="#">{tHome("footer.about.contact")}</a>
              <a href="#">{tHome("footer.about.join")}</a>
            </div>
            <div>
              <h4>{tHome("footer.links.title")}</h4>
              <a href="#">{tHome("footer.links.clubManagement")}</a>
              <a href="#">{tHome("footer.links.activityCenter")}</a>
              <a href="#">{tHome("footer.links.statistics")}</a>
            </div>
            <div>
              <h4>{tHome("footer.help.title")}</h4>
              <a href="#">{tHome("footer.help.guide")}</a>
              <a href="#">{tHome("footer.help.faq")}</a>
              <a href="#">{tHome("footer.help.feedback")}</a>
            </div>
            <div>
              <h4>{tHome("footer.contact.title")}</h4>
              <p>
                <i className="fas fa-map-marker-alt" /> {tCommon("contact.address")}
              </p>
              <p>
                <i className="fas fa-phone" /> {tCommon("contact.phone")}
              </p>
              <p>
                <i className="fas fa-envelope" /> {tCommon("contact.email")}
              </p>
            </div>
          </div>
          <div className={styles.footerBottom}>
            <p>{tHome("footer.copyright")}</p>
            <p>
              <a href="#" style={{ color: "#888", display: "inline-block", marginRight: "10px" }}>
                {tHome("footer.signature.dev")}
              </a>
              <a href="#" style={{ color: "#888", display: "inline-block" }}>
                {tHome("footer.signature.cn")}
              </a>
            </p>
          </div>
        </div>
      </footer>

      <Modal
        open={feedbackOpen}
        title={
          <span className={styles.feedbackTitle}>
            <i className="fas fa-comments" /> {tHome("modal.feedback.title")}
          </span>
        }
        onCancel={() => setFeedbackOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setFeedbackOpen(false)}>
            {tCommon("action.cancel")}
          </Button>,
          <Button key="submit" type="primary" loading={feedbackSubmitting} onClick={submitFeedback}>
            {tCommon("action.submit")}
          </Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item<FeedbackForm> name="name" label={tHome("modal.feedback.nameLabel")}>
            <Input placeholder={tHome("modal.feedback.namePlaceholder")} />
          </Form.Item>
          <Form.Item<FeedbackForm> name="email" label={tHome("modal.feedback.emailLabel")}>
            <Input placeholder={tHome("modal.feedback.emailPlaceholder")} />
          </Form.Item>
          <Form.Item<FeedbackForm>
            name="content"
            label={tHome("modal.feedback.contentLabel")}
            rules={[{ required: true, message: tHome("modal.feedback.contentRequired") }]}
          >
            <TextArea rows={5} placeholder={tHome("modal.feedback.contentPlaceholder")} />
          </Form.Item>
        </Form>
      </Modal>
    </main>
  );

  async function loadHomepageData() {
    setHomeLoading(true);
    setClubsLoading(true);

    const clubsPromise = fetch("/api/home/recommended-clubs", { cache: "no-store" })
      .then((res) => res.json())
      .catch(() => null);

    const [bannerData, newsData] = await Promise.allSettled([
      fetch("/api/banners", { cache: "no-store" }).then((res) => res.json()),
      fetch("/api/news/home", { cache: "no-store" }).then((res) => res.json())
    ]);

    if (
      bannerData.status === "fulfilled" &&
      bannerData.value &&
      bannerData.value.success &&
      Array.isArray(bannerData.value.banners)
    ) {
      setBanners(bannerData.value.banners as BannerItem[]);
      setCurrentBanner(0);
    } else {
      setBanners([]);
      setCurrentBanner(0);
    }

    if (
      newsData.status === "fulfilled" &&
      newsData.value &&
      newsData.value.success &&
      newsData.value.data
    ) {
      setHomeNews({
        carousel: Array.isArray(newsData.value.data.carousel) ? (newsData.value.data.carousel as NewsItem[]) : [],
        textList: Array.isArray(newsData.value.data.textList) ? (newsData.value.data.textList as NewsItem[]) : []
      });
      setCurrentNews(0);
    } else {
      setHomeNews({ carousel: [], textList: [] });
      setCurrentNews(0);
    }

    if (
      bannerData.status === "rejected" &&
      newsData.status === "rejected"
    ) {
      setBanners([]);
      setHomeNews({ carousel: [], textList: [] });
    }

    setHomeLoading(false);

    const clubData = await clubsPromise;
    if (clubData && clubData.success && Array.isArray(clubData.clubs)) {
      setClubs(clubData.clubs as ClubItem[]);
    } else {
      setClubs([]);
    }
    setClubsLoading(false);
  }
}
