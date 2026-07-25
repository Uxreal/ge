/**
 * Engine-agnostic types.
 *
 * A "media engine" wraps one playback technology (hls.js, dash.js, or the
 * browser's own <video> loader) behind a single interface so the UI never has
 * to branch on stream format.
 */

export type StreamKind = "hls" | "dash" | "progressive";

/** How we decided which engine to use. */
export type EngineChoiceReason = "extension" | "mime" | "forced" | "fallback";

export interface QualityLevel {
  /** Engine-local id. Stable for the lifetime of the engine. */
  id: string;
  label: string;
  height?: number;
  width?: number;
  /** bits per second */
  bitrate?: number;
  codecs?: string;
  frameRate?: number;
}

export interface AudioTrackInfo {
  id: string;
  label: string;
  lang?: string;
  /** Descriptive / audio-description track. */
  isDescription?: boolean;
}

export interface TextTrackInfo {
  id: string;
  label: string;
  lang?: string;
  source: "embedded" | "external";
  kind?: string;
}

export interface EngineError {
  message: string;
  /** Short machine-ish code, surfaced in the UI's details panel. */
  code?: string;
  fatal: boolean;
  /** True when calling engine.recover() has a realistic chance of helping. */
  recoverable: boolean;
  /** Set when the failure looks like a cross-origin (CORS) rejection. */
  likelyCors?: boolean;
}

export interface EngineEvents {
  /** Quality levels / audio tracks / text tracks changed. */
  onTracksChanged: () => void;
  /** Active (rendered) quality changed — includes ABR-driven switches. */
  onLevelSwitched: () => void;
  onError: (error: EngineError) => void;
  /** Manifest parsed / metadata known. */
  onReady: () => void;
}

export interface MediaEngine {
  readonly kind: StreamKind;
  /** Human-readable engine name for the stats panel. */
  readonly name: string;

  load(url: string): void;
  destroy(): void;

  getQualityLevels(): QualityLevel[];
  /** Currently rendering level id, or null when unknown. */
  getActiveQualityId(): string | null;
  /** User selection: null means "auto" (ABR). */
  getSelectedQualityId(): string | null;
  setQuality(id: string | null): void;

  getAudioTracks(): AudioTrackInfo[];
  getActiveAudioId(): string | null;
  setAudioTrack(id: string): void;

  /** Text tracks the engine itself owns (embedded in the stream). */
  getTextTracks(): TextTrackInfo[];
  /** null disables engine-owned subtitles. */
  setTextTrack(id: string | null): void;

  /** Best-effort recovery after a fatal error. Returns false if hopeless. */
  recover(): boolean;

  /** Live diagnostics for the stats overlay. */
  getStats(): Record<string, string | number>;
}

export type EngineFactory = (
  video: HTMLVideoElement,
  events: EngineEvents,
) => MediaEngine;
