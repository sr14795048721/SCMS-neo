const NEWS_TIME_ZONE = "Asia/Shanghai";

export function formatNewsDate(value?: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: NEWS_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(date);
}

export function formatNewsDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: NEWS_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(date);
}

export function formatNewsMonthDayParts(value?: string | null) {
  if (!value) {
    return { month: "--", day: "--" };
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return { month: "--", day: "--" };
  }

  const parts = new Intl.DateTimeFormat("zh-CN", {
    timeZone: NEWS_TIME_ZONE,
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(date);

  const month = parts.find((part) => part.type === "month")?.value || "--";
  const day = parts.find((part) => part.type === "day")?.value || "--";

  return { month, day };
}
