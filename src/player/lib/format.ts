/** hh:mm:ss for anything over an hour, m:ss below it. */
export function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
  const total = Math.floor(seconds);
  const s = total % 60;
  const m = Math.floor(total / 60) % 60;
  const h = Math.floor(total / 3600);
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${ss}` : `${m}:${ss}`;
}

/** Signed offset used by the seek toast: "+10s" / "−10s". */
export function formatOffset(seconds: number): string {
  const sign = seconds < 0 ? "−" : "+";
  return `${sign}${Math.abs(Math.round(seconds))}s`;
}

export function formatBitrate(bps?: number): string {
  if (!bps || !Number.isFinite(bps)) return "—";
  if (bps >= 1_000_000) return `${(bps / 1_000_000).toFixed(1)} Mbps`;
  return `${Math.round(bps / 1000)} kbps`;
}

export function formatBytes(bytes?: number): string {
  if (!bytes || !Number.isFinite(bytes)) return "—";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value < 10 && unit > 0 ? value.toFixed(1) : Math.round(value)} ${units[unit]}`;
}

export function formatCount(n?: number): string {
  if (!n || !Number.isFinite(n)) return "0";
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return String(n);
}

const DISPLAY_NAMES =
  typeof Intl !== "undefined" && "DisplayNames" in Intl
    ? new Intl.DisplayNames(undefined, { type: "language" })
    : null;

/** "en" -> "English". Falls back to the raw code for anything unrecognised. */
export function languageName(code?: string): string {
  if (!code) return "";
  try {
    return DISPLAY_NAMES?.of(code) ?? code;
  } catch {
    return code;
  }
}

/** Shorten a URL for display in a card or a title bar. */
export function prettyUrl(raw: string, max = 64): string {
  let text = raw;
  try {
    const url = new URL(raw);
    text = `${url.host}${url.pathname}`;
  } catch {
    /* not a parseable URL — show it as typed */
  }
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}

/** Filename without extension, URL-decoded — used as a fallback title. */
export function titleFromUrl(raw: string): string {
  try {
    const path = new URL(raw, window.location.href).pathname;
    const file = decodeURIComponent(path.split("/").filter(Boolean).pop() || "");
    const base = file.replace(/\.[^.]+$/, "").replace(/[._-]+/g, " ").trim();
    return base || prettyUrl(raw, 48);
  } catch {
    return prettyUrl(raw, 48);
  }
}
