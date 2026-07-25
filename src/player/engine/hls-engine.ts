import Hls, { Events, ErrorTypes, ErrorDetails, type ErrorData } from "hls.js";
import type {
  AudioTrackInfo,
  EngineEvents,
  MediaEngine,
  QualityLevel,
  TextTrackInfo,
} from "./types";
import { formatBitrate } from "../lib/format";

/** How many automatic recoveries we attempt before giving up on a stream. */
const MAX_AUTO_RECOVERIES = 3;

export function isHlsSupported(): boolean {
  return Hls.isSupported();
}

export class HlsEngine implements MediaEngine {
  readonly kind = "hls" as const;
  readonly name = `hls.js ${Hls.version}`;

  private hls: Hls;
  private video: HTMLVideoElement;
  private events: EngineEvents;
  private recoveries = 0;
  private lastRecoverAt = 0;
  private destroyed = false;
  private lastFatalType: string | null = null;

  constructor(video: HTMLVideoElement, events: EngineEvents) {
    this.video = video;
    this.events = events;
    this.hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      // Keep a generous back buffer so scrubbing backwards does not re-download.
      backBufferLength: 90,
      // Long-form content benefits from a deeper forward buffer than the default.
      maxBufferLength: 30,
      maxMaxBufferLength: 120,
      // Do not clamp quality to the element's CSS size — the user may go
      // fullscreen at any moment, and manual selection should be honoured.
      capLevelToPlayerSize: false,
      progressive: false,
    });

    this.hls.attachMedia(video);
    this.bind();
  }

  private bind() {
    const { hls, events } = this;

    hls.on(Events.MANIFEST_PARSED, () => {
      this.recoveries = 0;
      events.onReady();
      events.onTracksChanged();
    });
    hls.on(Events.LEVEL_SWITCHED, () => events.onLevelSwitched());
    hls.on(Events.LEVELS_UPDATED, () => events.onTracksChanged());
    hls.on(Events.AUDIO_TRACKS_UPDATED, () => events.onTracksChanged());
    hls.on(Events.AUDIO_TRACK_SWITCHED, () => events.onTracksChanged());
    hls.on(Events.SUBTITLE_TRACKS_UPDATED, () => events.onTracksChanged());
    hls.on(Events.SUBTITLE_TRACK_SWITCH, () => events.onTracksChanged());
    hls.on(Events.ERROR, (_e, data) => this.onHlsError(data));
  }

  private onHlsError(data: ErrorData) {
    if (this.destroyed) return;

    if (!data.fatal) {
      // Non-fatal errors are routine (a single failed segment, a gap). hls.js
      // handles them internally; surfacing them would be noise.
      return;
    }

    const likelyCors =
      data.type === ErrorTypes.NETWORK_ERROR &&
      (data.details === ErrorDetails.MANIFEST_LOAD_ERROR ||
        data.details === ErrorDetails.LEVEL_LOAD_ERROR) &&
      // hls.js reports status 0 when the browser blocks the response outright,
      // which in practice is almost always a CORS rejection.
      (data.response?.code === 0 || data.response?.code === undefined);

    // A playlist that will not load at all is a dead URL, a blocked origin, or
    // the wrong format — retrying just makes the user wait for the same
    // failure. Mid-stream errors are the ones worth recovering from.
    const manifestDead =
      data.details === ErrorDetails.MANIFEST_LOAD_ERROR ||
      data.details === ErrorDetails.MANIFEST_LOAD_TIMEOUT ||
      data.details === ErrorDetails.MANIFEST_PARSING_ERROR;

    const recoverable =
      !manifestDead &&
      this.recoveries < MAX_AUTO_RECOVERIES &&
      (data.type === ErrorTypes.NETWORK_ERROR ||
        data.type === ErrorTypes.MEDIA_ERROR);

    this.lastFatalType = data.type;

    this.events.onError({
      message: describeHlsError(data),
      code: data.details,
      fatal: true,
      recoverable,
      likelyCors,
    });
  }

  load(url: string) {
    this.hls.loadSource(url);
  }

  destroy() {
    this.destroyed = true;
    try {
      this.hls.destroy();
    } catch {
      /* hls.js can throw while tearing down a half-initialised transmuxer */
    }
  }

  getQualityLevels(): QualityLevel[] {
    return this.hls.levels.map((level, index) => ({
      id: String(index),
      label: qualityLabel(level.height, level.bitrate),
      height: level.height,
      width: level.width,
      bitrate: level.bitrate,
      codecs: [level.videoCodec, level.audioCodec].filter(Boolean).join(", "),
      frameRate: level.frameRate,
    }));
  }

  getActiveQualityId() {
    return this.hls.currentLevel >= 0 ? String(this.hls.currentLevel) : null;
  }

  getSelectedQualityId() {
    // hls.js uses -1 for "let ABR decide".
    const manual = this.hls.manualLevel;
    return manual >= 0 ? String(manual) : null;
  }

  setQuality(id: string | null) {
    this.hls.currentLevel = id === null ? -1 : Number(id);
  }

  getAudioTracks(): AudioTrackInfo[] {
    return this.hls.audioTracks.map((track, index) => ({
      id: String(index),
      label: track.name || track.lang || `Audio ${index + 1}`,
      lang: track.lang,
      isDescription: track.characteristics?.includes("describes-video"),
    }));
  }

  getActiveAudioId() {
    return this.hls.audioTrack >= 0 ? String(this.hls.audioTrack) : null;
  }

  setAudioTrack(id: string) {
    this.hls.audioTrack = Number(id);
  }

  getTextTracks(): TextTrackInfo[] {
    return this.hls.subtitleTracks.map((track, index) => ({
      id: String(index),
      label: track.name || track.lang || `Subtitles ${index + 1}`,
      lang: track.lang,
      source: "embedded" as const,
      kind: track.type?.toLowerCase(),
    }));
  }

  setTextTrack(id: string | null) {
    this.hls.subtitleDisplay = id !== null;
    this.hls.subtitleTrack = id === null ? -1 : Number(id);
  }

  recover(): boolean {
    const now = Date.now();
    if (this.recoveries >= MAX_AUTO_RECOVERIES) return false;
    this.recoveries += 1;

    try {
      if (this.lastFatalType === ErrorTypes.MEDIA_ERROR) {
        // Two media errors in quick succession mean the first
        // recoverMediaError() did not stick — escalate to a codec swap.
        if (now - this.lastRecoverAt < 3000) this.hls.swapAudioCodec();
        this.hls.recoverMediaError();
      } else {
        // Network errors just need the loader restarted.
        this.hls.startLoad();
      }
      this.lastRecoverAt = now;
      return true;
    } catch {
      return false;
    }
  }

  getStats(): Record<string, string | number> {
    const level = this.hls.levels[this.hls.currentLevel];
    const buffered = this.video.buffered;
    const ahead =
      buffered.length > 0
        ? Math.max(0, buffered.end(buffered.length - 1) - this.video.currentTime)
        : 0;

    return {
      Engine: this.name,
      Levels: this.hls.levels.length,
      Resolution: level ? `${level.width}×${level.height}` : "—",
      Bitrate: level ? formatBitrate(level.bitrate) : "—",
      "Est. bandwidth": formatBitrate(this.hls.bandwidthEstimate),
      "Buffer ahead": `${ahead.toFixed(1)}s`,
      "Dropped frames": this.video.getVideoPlaybackQuality?.().droppedVideoFrames ?? 0,
    };
  }
}

function qualityLabel(height?: number, bitrate?: number): string {
  if (height) {
    const tag =
      height >= 2160 ? " 4K" : height >= 1440 ? " QHD" : height >= 1080 ? " HD" : "";
    return `${height}p${tag}`;
  }
  return bitrate ? formatBitrate(bitrate) : "Unknown";
}

function describeHlsError(data: ErrorData): string {
  switch (data.details) {
    case ErrorDetails.MANIFEST_LOAD_ERROR:
      return "Could not load the playlist. The server refused the request or is unreachable.";
    case ErrorDetails.MANIFEST_LOAD_TIMEOUT:
      return "The playlist request timed out.";
    case ErrorDetails.MANIFEST_PARSING_ERROR:
      return "That URL did not return a valid HLS playlist.";
    case ErrorDetails.LEVEL_LOAD_ERROR:
    case ErrorDetails.LEVEL_LOAD_TIMEOUT:
      return "Could not load the media playlist for the selected quality.";
    case ErrorDetails.FRAG_LOAD_ERROR:
    case ErrorDetails.FRAG_LOAD_TIMEOUT:
      return "A media segment failed to download.";
    case ErrorDetails.FRAG_PARSING_ERROR:
      return "A media segment could not be parsed.";
    case ErrorDetails.BUFFER_APPEND_ERROR:
    case ErrorDetails.BUFFER_ADD_CODEC_ERROR:
      return "This browser cannot decode the stream's codec.";
    case ErrorDetails.KEY_LOAD_ERROR:
      return "The stream is encrypted and its decryption key could not be loaded.";
    default:
      return data.reason || `Playback failed (${data.details}).`;
  }
}
