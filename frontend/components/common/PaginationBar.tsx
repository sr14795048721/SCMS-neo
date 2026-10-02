"use client";

import { buildVisiblePages } from "../../lib/pagination";
import styles from "../../styles/pagination.module.css";

type PaginationBarProps = {
  currentPage: number;
  totalPages: number;
  prevLabel: string;
  nextLabel: string;
  pageLabel: string;
  onPageChange: (page: number) => void;
};

export function PaginationBar({
  currentPage,
  totalPages,
  prevLabel,
  nextLabel,
  pageLabel,
  onPageChange
}: PaginationBarProps) {
  if (totalPages <= 1) {
    return null;
  }

  const visiblePages = buildVisiblePages(totalPages, currentPage);

  return (
    <div className={styles.pagination}>
      <button
        type="button"
        className={styles.pagerButton}
        disabled={currentPage <= 1}
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
      >
        <i className="fas fa-angle-left" />
        {prevLabel}
      </button>
      <div className={styles.pageList}>
        {visiblePages.map((page) => (
          <button
            key={page}
            type="button"
            className={`${styles.pageChip} ${page === currentPage ? styles.pageChipActive : ""}`}
            onClick={() => onPageChange(page)}
          >
            {page}
          </button>
        ))}
      </div>
      <span className={styles.pageInfo}>{pageLabel}</span>
      <button
        type="button"
        className={styles.pagerButton}
        disabled={currentPage >= totalPages}
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
      >
        {nextLabel}
        <i className="fas fa-angle-right" />
      </button>
    </div>
  );
}
