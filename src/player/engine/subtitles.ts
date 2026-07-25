/**
 * External subtitle support: users can attach a local .srt/.vtt file or point
 * at a subtitle URL. Browsers only accept WebVTT in a <track>, so SubRip is
 * converted on the fly and handed over as a blob URL.
 */

export const EXTERNAL_TRACK_PREFIX = "ext:";

export interface ExternalSubtitle {
  /** Always starts with EXTERNAL_TRACK_PREFIX so engines can skip it. */
  id: string;
  label: string;
  lang: string;
  /** Blob URL (converted) or the original URL when it is already WebVTT. */
  src: string;
  /** Set when we created a blob URL that must be revoked on removal. */
  objectUrl?: string;
}

let counter = 0;

function nextId() {
  counter += 1;
  return `${EXTERNAL_TRACK_PREFIX}${counter}`;
}

export function isWebVtt(text: string): boolean {
  // The spec requires the signature at the very start, after an optional BOM.
  return /^﻿?WEBVTT/.test(text.trimStart());
}

/**
 * Convert SubRip to WebVTT.
 *
 * The two formats differ in three ways that matter for playback: the WEBVTT
 * signature, `,` vs `.` as the millisecond separator, and SubRip's numeric cue
 * counters (legal in VTT as cue identifiers, so they are simply kept).
 */
export function srtToVtt(srt: string): string {
  const body = srt
    .replace(/^﻿/, "")
    .replace(/\r\n|\r/g, "\n")
    // 00:00:12,345 --> 00:00:14,000
    .replace(
      /(\d{1,2}:\d{2}:\d{2}),(\d{1,3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}),(\d{1,3})/g,
      (_m, a, ams, b, bms) => `${pad(a)}.${ams} --> ${pad(b)}.${bms}`,
    );
  return `WEBVTT\n\n${body.trim()}\n`;
}

/** WebVTT requires two-digit hours; SubRip files in the wild often use one. */
function pad(stamp: string): string {
  const parts = stamp.split(":");
  if (parts.length === 3 && parts[0].length === 1) return `0${stamp}`;
  return stamp;
}

export function subtitleTextToSrc(text: string): {
  src: string;
  objectUrl: string;
} {
  const vtt = isWebVtt(text) ? text : srtToVtt(text);
  const objectUrl = URL.createObjectURL(
    new Blob([vtt], { type: "text/vtt" }),
  );
  return { src: objectUrl, objectUrl };
}

export async function externalSubtitleFromFile(
  file: File,
): Promise<ExternalSubtitle> {
  const text = await file.text();
  const { src, objectUrl } = subtitleTextToSrc(text);
  return {
    id: nextId(),
    label: file.name.replace(/\.[^.]+$/, ""),
    lang: guessLang(file.name),
    src,
    objectUrl,
  };
}

export async function externalSubtitleFromUrl(
  url: string,
): Promise<ExternalSubtitle> {
  const label = decodeURIComponent(url.split("/").pop() || "Subtitles").replace(
    /\.[^.]+$/,
    "",
  );

  // A remote .vtt can be handed to <track> directly, but only if the server
  // allows the cross-origin read; fetching first surfaces that failure here,
  // with a clear message, instead of as a silent empty track.
  const response = await fetch(url, { mode: "cors" });
  if (!response.ok) {
    throw new Error(`Subtitle server responded ${response.status}.`);
  }
  const text = await response.text();
  const { src, objectUrl } = subtitleTextToSrc(text);
  return { id: nextId(), label, lang: guessLang(url), src, objectUrl };
}

/** Pull a language code out of names like "movie.en.srt" / "movie_fr.vtt". */
function guessLang(name: string): string {
  const match = name.match(/[._-]([a-z]{2,3})(?:[._-][A-Za-z]{2})?\.(?:srt|vtt)$/i);
  return match ? match[1].toLowerCase() : "";
}

export function revokeExternalSubtitle(sub: ExternalSubtitle) {
  if (sub.objectUrl) URL.revokeObjectURL(sub.objectUrl);
}
