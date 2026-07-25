/**
 * The watch route is fully described by its query string, so any playing video
 * can be linked to, bookmarked, or reloaded without losing its place.
 */
export interface WatchTarget {
  /** Direct stream URL. */
  url?: string;
  /** Internet Archive identifier — the player resolves the file itself. */
  id?: string;
  title?: string;
  /** Seconds to start at. */
  t?: number;
}

export function watchUrlFor(target: WatchTarget): string {
  const params = new URLSearchParams();
  if (target.id) params.set("id", target.id);
  if (target.url) params.set("url", target.url);
  if (target.title) params.set("title", target.title);
  if (target.t) params.set("t", String(Math.floor(target.t)));
  return `/watch?${params}`;
}

export function parseWatchTarget(search: URLSearchParams): WatchTarget {
  const t = Number(search.get("t"));
  return {
    url: search.get("url") ?? undefined,
    id: search.get("id") ?? undefined,
    title: search.get("title") ?? undefined,
    t: Number.isFinite(t) && t > 0 ? t : undefined,
  };
}

/** Mirrors the BrowserRouter basename — routes live under /player. */
export const BASENAME = "/player";

/** Shareable absolute URL, including the router basename. */
export function absoluteWatchUrl(target: WatchTarget): string {
  return new URL(
    `${BASENAME}${watchUrlFor(target)}`,
    window.location.origin,
  ).toString();
}

export function browseUrlFor(collectionId: string, query?: string): string {
  const params = new URLSearchParams();
  if (query?.trim()) params.set("q", query.trim());
  return `/browse/${collectionId}${params.size ? `?${params}` : ""}`;
}
