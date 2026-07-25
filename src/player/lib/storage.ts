/**
 * All persistence is local: preferences, resume positions and history live in
 * localStorage. Nothing leaves the browser.
 */

const PREFIX = "mediaplayer:";
const KEY_PREFS = `${PREFIX}prefs`;
const KEY_RESUME = `${PREFIX}resume`;
const KEY_HISTORY = `${PREFIX}history`;
const KEY_SAVED = `${PREFIX}saved`;

const MAX_HISTORY = 60;
const MAX_RESUME_ENTRIES = 200;

/** Below this we assume the user barely started; above it, they finished. */
export const RESUME_MIN_SECONDS = 15;
export const RESUME_MAX_FRACTION = 0.97;

export interface Preferences {
  volume: number;
  muted: boolean;
  playbackRate: number;
  /** Preferred subtitle language code — auto-selected on load when present. */
  subtitleLang: string | null;
  subtitlesEnabled: boolean;
  showStats: boolean;
}

export const DEFAULT_PREFERENCES: Preferences = {
  volume: 1,
  muted: false,
  playbackRate: 1,
  subtitleLang: null,
  subtitlesEnabled: false,
  showStats: false,
};

export interface ResumePoint {
  position: number;
  duration: number;
  updatedAt: number;
}

export interface HistoryEntry {
  url: string;
  title: string;
  poster?: string;
  /** Internet Archive identifier, when the item came from the catalog. */
  sourceId?: string;
  watchedAt: number;
}

export interface SavedEntry extends HistoryEntry {
  savedAt: number;
}

function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return { ...fallback, ...(JSON.parse(raw) as T) };
  } catch {
    return fallback;
  }
}

function readList<T>(key: string): T[] {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as T[]) : [];
  } catch {
    return [];
  }
}

function write(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Quota exceeded or storage disabled (private mode). Preferences are a
    // nicety, not a requirement — carry on without them.
  }
}

export function loadPreferences(): Preferences {
  return read<Preferences>(KEY_PREFS, DEFAULT_PREFERENCES);
}

export function savePreferences(prefs: Partial<Preferences>) {
  write(KEY_PREFS, { ...loadPreferences(), ...prefs });
}

/* ---------------------------------------------------------------- resume -- */

type ResumeMap = Record<string, ResumePoint>;

export function getResumePoint(url: string): ResumePoint | null {
  const map = read<ResumeMap>(KEY_RESUME, {});
  const entry = map[url];
  if (!entry) return null;
  if (entry.position < RESUME_MIN_SECONDS) return null;
  if (entry.duration > 0 && entry.position / entry.duration > RESUME_MAX_FRACTION) {
    return null;
  }
  return entry;
}

export function setResumePoint(url: string, position: number, duration: number) {
  const map = read<ResumeMap>(KEY_RESUME, {});

  const finished = duration > 0 && position / duration > RESUME_MAX_FRACTION;
  if (position < RESUME_MIN_SECONDS || finished) {
    delete map[url];
  } else {
    map[url] = { position, duration, updatedAt: Date.now() };
  }

  // Keep the map bounded — drop the oldest entries once it gets large.
  const keys = Object.keys(map);
  if (keys.length > MAX_RESUME_ENTRIES) {
    keys
      .sort((a, b) => map[a].updatedAt - map[b].updatedAt)
      .slice(0, keys.length - MAX_RESUME_ENTRIES)
      .forEach((key) => delete map[key]);
  }

  write(KEY_RESUME, map);
}

export function getAllResumePoints(): ResumeMap {
  return read<ResumeMap>(KEY_RESUME, {});
}

export function clearResumePoint(url: string) {
  const map = read<ResumeMap>(KEY_RESUME, {});
  delete map[url];
  write(KEY_RESUME, map);
}

/* --------------------------------------------------------------- history -- */

export function getHistory(): HistoryEntry[] {
  return readList<HistoryEntry>(KEY_HISTORY);
}

export function recordHistory(entry: Omit<HistoryEntry, "watchedAt">) {
  const next = [
    { ...entry, watchedAt: Date.now() },
    ...getHistory().filter((e) => e.url !== entry.url),
  ].slice(0, MAX_HISTORY);
  write(KEY_HISTORY, next);
}

export function removeHistory(url: string) {
  write(
    KEY_HISTORY,
    getHistory().filter((e) => e.url !== url),
  );
}

export function clearHistory() {
  write(KEY_HISTORY, []);
}

/* ----------------------------------------------------------------- saved -- */

export function getSaved(): SavedEntry[] {
  return readList<SavedEntry>(KEY_SAVED);
}

export function isSaved(url: string): boolean {
  return getSaved().some((e) => e.url === url);
}

export function toggleSaved(entry: Omit<HistoryEntry, "watchedAt">): boolean {
  const saved = getSaved();
  const existing = saved.find((e) => e.url === entry.url);
  if (existing) {
    write(
      KEY_SAVED,
      saved.filter((e) => e.url !== entry.url),
    );
    return false;
  }
  const now = Date.now();
  write(KEY_SAVED, [{ ...entry, watchedAt: now, savedAt: now }, ...saved]);
  return true;
}
