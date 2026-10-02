"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useT } from "../../lib/i18n/useT";
import { formatNewsDate, formatNewsMonthDayParts } from "../../lib/news/date";
import { PublicNewsPage } from "../../lib/news/types";
import styles from "../../styles/newsListPage.module.css";

const DEFAULT_PAGE: PublicNewsPage = {
  items: [],
  page: 1,
  pageSize: 9,
  total: 0,
  totalPages: 0
};

export function PublicNewsListPage() {
  const t = useT("news");
  const router = useRouter();
  const searchParams = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [newsPage, setNewsPage] = useState<PublicNewsPage>(DEFAULT_PAGE);

  const currentPage = Math.max(Number(searchParams.get("page") || 1), 1);

  useEffect(() => {
    void loadNews(currentPage);
  }, [currentPage]);

  return (
    <main className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.container}>
          <h1>{t("list.title")}</h1>
          <p>{t("list.subtitle")}</p>
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.container}>
          <div className={styles.toolbar}>
            <p className={styles.stats}>{t("list.count", { count: newsPage.total })}</p>
          </div>

          {loading ? (
            <div className={styles.loading}>
              <i className="fas fa-spinner fa-spin" />
              <p>{t("state.loading")}</p>
            </div>
          ) : !newsPage.items.length ? (
            <div className={styles.empty}>
              <i className="fas fa-newspaper" />
              <h2>{t("list.empty.title")}</h2>
              <p>{t("list.empty.description")}</p>
            </div>
          ) : (
            <>
              <div className={styles.grid}>
                {newsPage.items.map((item) => {
                  const hasCover = Boolean(item.cover);
                  const { month, day } = formatNewsMonthDayParts(item.publishedAt);

                  return (
                    <a
                      key={item.id}
                      href={`/news/${item.id}.html`}
                      className={`${styles.item} ${!hasCover ? styles.itemTextOnly : ""}`}
                    >
                      {hasCover ? (
                        <img src={item.cover} alt={item.title} className={styles.cover} />
                      ) : (
                        <div className={styles.coverTextOnly}>
                          <div className={styles.coverTextOnlyTop}>
                            <div className={styles.coverTextOnlyIcon}>
                              <i className="fas fa-newspaper" />
                            </div>
                            <div className={styles.coverTextOnlyDate}>
                              <strong>{day}</strong>
                              <span>{month}</span>
                            </div>
                          </div>
                          <div className={styles.coverTextOnlyLines}>
                            <span />
                            <span />
                            <span />
                            <span />
                          </div>
                        </div>
                      )}
                      <div className={styles.body}>
                        <div className={styles.meta}>
                          <span>
                            <i className="fas fa-user" /> {item.authorName || t("meta.authorFallback")}
                          </span>
                          <span>
                            <i className="fas fa-calendar-day" />{" "}
                            {formatNewsDate(item.publishedAt)}
                          </span>
                          <span>
                            <i className="fas fa-eye" /> {t("meta.views", { count: item.viewCount })}
                          </span>
                        </div>
                        <h2 className={styles.title}>{item.title}</h2>
                        <span className={styles.more}>
                          {t("list.readMore")} <i className="fas fa-arrow-right" />
                        </span>
                      </div>
                    </a>
                  );
                })}
              </div>

              <div className={styles.pager}>
                {buildPageNumbers(newsPage.page, newsPage.totalPages).map((pageNo) =>
                  pageNo === currentPage ? (
                    <button key={pageNo} type="button" className={styles.pagerActive}>
                      {pageNo}
                    </button>
                  ) : (
                    <button key={pageNo} type="button" className={styles.pagerButton} onClick={() => jumpToPage(pageNo)}>
                      {pageNo}
                    </button>
                  )
                )}
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  );

  async function loadNews(page: number) {
    setLoading(true);
    try {
      const response = await fetch(`/api/news?page=${page}&pageSize=9`, {
        cache: "no-store"
      });
      const result = (await response.json().catch(() => null)) as
        | { success?: boolean; data?: PublicNewsPage }
        | null;

      if (!response.ok || result?.success !== true || !result.data) {
        setNewsPage(DEFAULT_PAGE);
        return;
      }

      setNewsPage(result.data);
    } finally {
      setLoading(false);
    }
  }

  function jumpToPage(page: number) {
    router.push(page <= 1 ? "/news" : `/news?page=${page}`);
  }
}

function buildPageNumbers(current: number, total: number) {
  if (total <= 5) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }
  const start = Math.max(Math.min(current - 2, total - 4), 1);
  return Array.from({ length: 5 }, (_, index) => start + index);
}
