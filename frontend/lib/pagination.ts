export function buildVisiblePages(totalPages: number, currentPage: number, windowSize = 5) {
  if (totalPages <= 0) {
    return [];
  }

  if (totalPages <= windowSize) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const halfWindow = Math.floor(windowSize / 2);
  const start = Math.max(1, Math.min(currentPage - halfWindow, totalPages - windowSize + 1));
  return Array.from({ length: windowSize }, (_, index) => start + index);
}

export function paginateItems<T>(items: T[], currentPage: number, pageSize: number) {
  const normalizedPageSize = Math.max(1, pageSize);
  const totalPages = Math.max(1, Math.ceil(items.length / normalizedPageSize));
  const safePage = Math.min(Math.max(1, currentPage), totalPages);
  const start = (safePage - 1) * normalizedPageSize;

  return {
    items: items.slice(start, start + normalizedPageSize),
    page: safePage,
    pageSize: normalizedPageSize,
    total: items.length,
    totalPages
  };
}
