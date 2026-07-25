import type { StreamKind } from "./types";

const HLS_EXT = /\.(m3u8|m3u)$/i;
const DASH_EXT = /\.(mpd)$/i;
const PROGRESSIVE_EXT =
  /\.(mp4|m4v|mov|webm|ogv|ogg|ogm|mkv|mp3|m4a|aac|flac|wav|opus)$/i;

const HLS_MIME = /(application|video)\/(x-mpegurl|vnd\.apple\.mpegurl)/i;
const DASH_MIME = /application\/dash\+xml/i;

/** Strip query string and fragment so extension sniffing works on signed URLs. */
export function urlPath(raw: string): string {
  try {
    return new URL(raw, window.location.href).pathname;
  } catch {
    return raw.split("#")[0].split("?")[0];
  }
}

export function detectStreamKind(
  url: string,
  mimeHint?: string,
): { kind: StreamKind; confident: boolean } {
  if (mimeHint) {
    if (HLS_MIME.test(mimeHint)) return { kind: "hls", confident: true };
    if (DASH_MIME.test(mimeHint)) return { kind: "dash", confident: true };
    if (/^(video|audio)\//i.test(mimeHint))
      return { kind: "progressive", confident: true };
  }

  const path = urlPath(url);
  if (HLS_EXT.test(path)) return { kind: "hls", confident: true };
  if (DASH_EXT.test(path)) return { kind: "dash", confident: true };
  if (PROGRESSIVE_EXT.test(path)) return { kind: "progressive", confident: true };

  // Extensionless URLs are common for CDN/manifest endpoints. Look for the
  // usual giveaways in the whole URL before falling back.
  if (/m3u8/i.test(url)) return { kind: "hls", confident: false };
  if (/(\bdash\b|manifest\.mpd|\.mpd)/i.test(url))
    return { kind: "dash", confident: false };

  return { kind: "progressive", confident: false };
}

/**
 * Order to try engines in when detection was not confident. The player walks
 * this list on fatal load errors so an unlabelled HLS manifest still plays.
 */
export function fallbackOrder(first: StreamKind): StreamKind[] {
  const all: StreamKind[] = ["progressive", "hls", "dash"];
  return [first, ...all.filter((k) => k !== first)];
}

export function canPlayNativeHls(video: HTMLVideoElement): boolean {
  return (
    video.canPlayType("application/vnd.apple.mpegurl") !== "" ||
    video.canPlayType("application/x-mpegURL") !== ""
  );
}

const KIND_LABEL: Record<StreamKind, string> = {
  hls: "HLS",
  dash: "MPEG-DASH",
  progressive: "Progressive",
};

export function streamKindLabel(kind: StreamKind): string {
  return KIND_LABEL[kind];
}
