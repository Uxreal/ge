/**
 * Client for the Internet Archive's public JSON APIs.
 *
 * The Archive hosts a large, legally free library — public-domain features,
 * government and educational film, and works released under open licences —
 * and serves it over CORS-enabled HTTP with byte-range support, which is
 * exactly what a browser player needs. No key or account is required.
 *
 * Docs: https://archive.org/developers/
 */

const SEARCH_ENDPOINT = "https://archive.org/advancedsearch.php";
const METADATA_ENDPOINT = "https://archive.org/metadata";
const DOWNLOAD_ENDPOINT = "https://archive.org/download";
const THUMB_ENDPOINT = "https://archive.org/services/img";

export interface CatalogItem {
  identifier: string;
  title: string;
  year?: string;
  description?: string;
  creator?: string;
  downloads?: number;
  /** Seconds, when the Archive reports a runtime. */
  runtime?: number;
  poster: string;
}

export interface PlayableFile {
  name: string;
  url: string;
  /** Archive's own format label, e.g. "h.264", "512Kb MPEG4". */
  format: string;
  label: string;
  sizeBytes?: number;
  height?: number;
  width?: number;
  durationSeconds?: number;
  /** Higher is a better default pick. */
  score: number;
}

export interface SubtitleFile {
  name: string;
  url: string;
  label: string;
}

export interface ItemDetail {
  item: CatalogItem;
  files: PlayableFile[];
  subtitles: SubtitleFile[];
}

export interface SearchResult {
  items: CatalogItem[];
  total: number;
  page: number;
}

export class CatalogError extends Error {
  constructor(message: string, readonly cause?: unknown) {
    super(message);
    this.name = "CatalogError";
  }
}

export interface SearchOptions {
  /** Archive collection identifier, e.g. "feature_films". */
  collection?: string;
  /** Free-text query typed by the user. */
  query?: string;
  page?: number;
  rows?: number;
  sort?: "downloads desc" | "date desc" | "avg_rating desc" | "titleSorter asc";
  signal?: AbortSignal;
}

interface RawDoc {
  identifier: string;
  title?: string | string[];
  year?: string | number;
  description?: string | string[];
  creator?: string | string[];
  downloads?: number;
  runtime?: string;
}

/**
 * Lucene special characters would otherwise turn a stray apostrophe or colon
 * in a movie title into a syntax error.
 */
function escapeLucene(input: string): string {
  return input.replace(/([+\-!(){}[\]^"~*?:\\/]|&&|\|\|)/g, "\\$1");
}

function buildQuery({ collection, query }: SearchOptions): string {
  const clauses = ["mediatype:(movies)"];
  if (collection) clauses.push(`collection:(${escapeLucene(collection)})`);
  if (query?.trim()) clauses.push(`(${escapeLucene(query.trim())})`);
  return clauses.join(" AND ");
}

function first(value: string | string[] | undefined): string | undefined {
  if (Array.isArray(value)) return value[0];
  return value;
}

/** Archive descriptions are HTML fragments; the UI renders plain text. */
function stripHtml(html?: string): string | undefined {
  if (!html) return undefined;
  const text = html
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&nbsp;/g, " ")
    .trim();
  return text || undefined;
}

/** "1:34:12" or "94 min" or "5642" -> seconds. */
function parseRuntime(runtime?: string): number | undefined {
  if (!runtime) return undefined;
  const clock = runtime.match(/^(?:(\d+):)?(\d{1,2}):(\d{2})/);
  if (clock) {
    const [, h = "0", m, s] = clock;
    return Number(h) * 3600 + Number(m) * 60 + Number(s);
  }
  const minutes = runtime.match(/^(\d+)\s*min/i);
  if (minutes) return Number(minutes[1]) * 60;
  const plain = Number(runtime);
  return Number.isFinite(plain) ? plain : undefined;
}

function toCatalogItem(doc: RawDoc): CatalogItem {
  return {
    identifier: doc.identifier,
    title: first(doc.title) || doc.identifier,
    year: doc.year !== undefined ? String(doc.year) : undefined,
    description: stripHtml(first(doc.description)),
    creator: first(doc.creator),
    downloads: doc.downloads,
    runtime: parseRuntime(doc.runtime),
    poster: `${THUMB_ENDPOINT}/${encodeURIComponent(doc.identifier)}`,
  };
}

export async function searchCatalog(
  options: SearchOptions = {},
): Promise<SearchResult> {
  const page = options.page ?? 1;
  const rows = options.rows ?? 24;

  const params = new URLSearchParams();
  params.set("q", buildQuery(options));
  for (const field of [
    "identifier",
    "title",
    "year",
    "description",
    "creator",
    "downloads",
    "runtime",
  ]) {
    params.append("fl[]", field);
  }
  params.append("sort[]", options.sort ?? "downloads desc");
  params.set("rows", String(rows));
  params.set("page", String(page));
  params.set("output", "json");

  let response: Response;
  try {
    response = await fetch(`${SEARCH_ENDPOINT}?${params}`, {
      signal: options.signal,
    });
  } catch (err) {
    if ((err as Error).name === "AbortError") throw err;
    throw new CatalogError(
      "Could not reach archive.org. Check your connection and try again.",
      err,
    );
  }

  if (!response.ok) {
    throw new CatalogError(
      `archive.org search failed (HTTP ${response.status}). It rate-limits bursts of requests — wait a moment and retry.`,
    );
  }

  let payload: { response?: { docs?: RawDoc[]; numFound?: number } };
  try {
    payload = await response.json();
  } catch (err) {
    throw new CatalogError("archive.org returned a response we could not read.", err);
  }

  const docs = payload.response?.docs ?? [];
  return {
    items: docs.filter((d) => d.identifier).map(toCatalogItem),
    total: payload.response?.numFound ?? docs.length,
    page,
  };
}

interface RawFile {
  name: string;
  format?: string;
  size?: string;
  length?: string;
  height?: string;
  width?: string;
  source?: string;
}

interface RawMetadata {
  metadata?: {
    identifier?: string;
    title?: string | string[];
    year?: string;
    date?: string;
    description?: string | string[];
    creator?: string | string[];
    runtime?: string;
  };
  files?: RawFile[];
}

/** Containers a browser can actually decode, best first. */
const VIDEO_EXT = /\.(mp4|m4v|webm|ogv|ogg)$/i;
/** Containers the Archive hosts that no browser can play. */
const UNPLAYABLE_EXT = /\.(mkv|avi|mpg|mpeg|flv|wmv|rm|asf|mov|iso|vob)$/i;
const SUBTITLE_EXT = /\.(vtt|srt)$/i;

/**
 * Rank derivatives so the default pick is a browser-friendly H.264 MP4 rather
 * than a 4 GB MPEG-2 original or a low-bitrate 240p preview.
 */
function scoreFile(file: RawFile): number {
  const format = (file.format ?? "").toLowerCase();
  const name = file.name.toLowerCase();
  let score = 0;

  if (name.endsWith(".mp4") || name.endsWith(".m4v")) score += 100;
  else if (name.endsWith(".webm")) score += 70;
  else if (name.endsWith(".ogv")) score += 40;

  if (format.includes("h.264")) score += 40;
  if (format.includes("mpeg4")) score += 20;
  // These are tiny preview derivatives — playable, but a poor default.
  if (/\b(64kb|256kb)\b/i.test(format)) score -= 40;
  if (format.includes("512kb")) score -= 10;

  const height = Number(file.height);
  if (Number.isFinite(height)) score += Math.min(height, 2160) / 100;

  return score;
}

function fileLabel(file: RawFile): string {
  const height = Number(file.height);
  const parts: string[] = [];
  if (Number.isFinite(height) && height > 0) parts.push(`${height}p`);
  if (file.format) parts.push(file.format);
  return parts.join(" · ") || file.name;
}

export async function fetchItem(
  identifier: string,
  signal?: AbortSignal,
): Promise<ItemDetail> {
  let response: Response;
  try {
    response = await fetch(`${METADATA_ENDPOINT}/${encodeURIComponent(identifier)}`, {
      signal,
    });
  } catch (err) {
    if ((err as Error).name === "AbortError") throw err;
    throw new CatalogError("Could not reach archive.org for this title.", err);
  }

  if (!response.ok) {
    throw new CatalogError(`archive.org returned HTTP ${response.status}.`);
  }

  const data = (await response.json()) as RawMetadata;
  if (!data.metadata || !data.files) {
    throw new CatalogError(
      "That item no longer exists on archive.org, or it has no downloadable files.",
    );
  }

  const meta = data.metadata;
  const base = `${DOWNLOAD_ENDPOINT}/${encodeURIComponent(identifier)}`;

  const files: PlayableFile[] = data.files
    .filter((f) => VIDEO_EXT.test(f.name) && !UNPLAYABLE_EXT.test(f.name))
    .map((f) => ({
      name: f.name,
      // Path segments are encoded individually so nested paths survive.
      url: `${base}/${f.name.split("/").map(encodeURIComponent).join("/")}`,
      format: f.format ?? "",
      label: fileLabel(f),
      sizeBytes: f.size ? Number(f.size) : undefined,
      height: f.height ? Number(f.height) : undefined,
      width: f.width ? Number(f.width) : undefined,
      durationSeconds: f.length ? parseRuntime(f.length) : undefined,
      score: scoreFile(f),
    }))
    .sort((a, b) => b.score - a.score);

  const subtitles: SubtitleFile[] = data.files
    .filter((f) => SUBTITLE_EXT.test(f.name))
    .map((f) => ({
      name: f.name,
      url: `${base}/${f.name.split("/").map(encodeURIComponent).join("/")}`,
      label: f.name.replace(/\.[^.]+$/, ""),
    }));

  return {
    item: {
      identifier,
      title: first(meta.title) || identifier,
      year: meta.year ?? meta.date?.slice(0, 4),
      description: stripHtml(first(meta.description)),
      creator: first(meta.creator),
      runtime: parseRuntime(meta.runtime),
      poster: `${THUMB_ENDPOINT}/${encodeURIComponent(identifier)}`,
    },
    files,
    subtitles,
  };
}

export function archiveDetailsUrl(identifier: string): string {
  return `https://archive.org/details/${encodeURIComponent(identifier)}`;
}
