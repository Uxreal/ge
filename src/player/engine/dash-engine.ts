import { MediaPlayer, type MediaPlayerClass, type Representation } from "dashjs";
import type {
  AudioTrackInfo,
  EngineEvents,
  MediaEngine,
  QualityLevel,
  TextTrackInfo,
} from "./types";
import { formatBitrate } from "../lib/format";

const MAX_AUTO_RECOVERIES = 2;

/**
 * dash.js exposes its event constants on the UMD namespace object, which the
 * ESM entry point does not re-export. The literal values are part of the public
 * API (they are in dashjs' own type declarations), so we name them directly.
 */
const DASH_EVENT = {
  STREAM_INITIALIZED: "streamInitialized",
  REPRESENTATION_SWITCH: "representationSwitch",
  QUALITY_CHANGE_RENDERED: "qualityChangeRendered",
  TRACK_CHANGE_RENDERED: "trackChangeRendered",
  TEXT_TRACKS_ADDED: "allTextTracksAdded",
  ERROR: "error",
  PLAYBACK_ERROR: "playbackError",
} as const;

export class DashEngine implements MediaEngine {
  readonly kind = "dash" as const;
  readonly name = "dash.js";

  private player: MediaPlayerClass;
  private video: HTMLVideoElement;
  private events: EngineEvents;
  private recoveries = 0;
  private destroyed = false;
  private url = "";
  private selectedQuality: string | null = null;

  constructor(video: HTMLVideoElement, events: EngineEvents) {
    this.video = video;
    this.events = events;
    this.player = MediaPlayer().create();

    this.player.updateSettings({
      streaming: {
        // We drive subtitles from the UI, so do not force one on at startup.
        text: { defaultEnabled: false },
        abr: { autoSwitchBitrate: { video: true, audio: true } },
        buffer: { fastSwitchEnabled: true },
      },
    });

    this.bind();
    this.player.initialize(video, undefined, false);
  }

  private bind() {
    const { player, events } = this;

    player.on(DASH_EVENT.STREAM_INITIALIZED, () => {
      this.recoveries = 0;
      events.onReady();
      events.onTracksChanged();
    });
    player.on(DASH_EVENT.REPRESENTATION_SWITCH, () => events.onLevelSwitched());
    player.on(DASH_EVENT.QUALITY_CHANGE_RENDERED, () => events.onLevelSwitched());
    player.on(DASH_EVENT.TRACK_CHANGE_RENDERED, () => events.onTracksChanged());
    player.on(DASH_EVENT.TEXT_TRACKS_ADDED, () => events.onTracksChanged());
    player.on(DASH_EVENT.ERROR, (e: unknown) => this.onDashError(e));
    player.on(DASH_EVENT.PLAYBACK_ERROR, (e: unknown) => this.onDashError(e));
  }

  private onDashError(raw: unknown) {
    if (this.destroyed) return;
    const err = (raw as { error?: { code?: number; message?: string } }).error;
    const code = err?.code;
    const message = err?.message ?? "DASH playback failed.";

    // 25–27 are the download-error family (manifest / init / media segment).
    const isDownload = typeof code === "number" && code >= 25 && code <= 27;

    this.events.onError({
      message: isDownload
        ? "Could not download the MPD manifest or its segments. The server may be unreachable or blocking cross-origin requests."
        : message,
      code: code !== undefined ? `DASH_${code}` : undefined,
      fatal: true,
      recoverable: isDownload && this.recoveries < MAX_AUTO_RECOVERIES,
      likelyCors: isDownload,
    });
  }

  load(url: string) {
    this.url = url;
    this.player.attachSource(url);
  }

  destroy() {
    this.destroyed = true;
    try {
      this.player.destroy();
    } catch {
      /* dash.js throws if destroyed before initialisation completes */
    }
  }

  private videoRepresentations(): Representation[] {
    try {
      return this.player.getRepresentationsByType("video") ?? [];
    } catch {
      return [];
    }
  }

  getQualityLevels(): QualityLevel[] {
    return this.videoRepresentations().map((rep) => ({
      id: rep.id,
      label: rep.height ? `${rep.height}p` : formatBitrate(rep.bandwidth),
      height: rep.height,
      width: rep.width,
      bitrate: rep.bandwidth,
      codecs: rep.codecs ?? undefined,
      frameRate: rep.frameRate,
    }));
  }

  getActiveQualityId() {
    try {
      return this.player.getCurrentRepresentationForType("video")?.id ?? null;
    } catch {
      return null;
    }
  }

  getSelectedQualityId() {
    return this.selectedQuality;
  }

  setQuality(id: string | null) {
    this.selectedQuality = id;
    this.player.updateSettings({
      streaming: { abr: { autoSwitchBitrate: { video: id === null } } },
    });
    if (id !== null) {
      this.player.setRepresentationForTypeById("video", id, true);
    }
  }

  getAudioTracks(): AudioTrackInfo[] {
    let tracks;
    try {
      tracks = this.player.getTracksFor("audio");
    } catch {
      return [];
    }
    return tracks.map((track, index) => ({
      id: trackKey(track, index),
      label: track.labels?.[0]?.text || track.lang || `Audio ${index + 1}`,
      lang: track.lang ?? undefined,
      isDescription: track.roles?.some((r) => r.value === "description"),
    }));
  }

  getActiveAudioId() {
    try {
      const current = this.player.getCurrentTrackFor("audio");
      if (!current) return null;
      const index = this.player
        .getTracksFor("audio")
        .findIndex((t) => t.index === current.index);
      return trackKey(current, index);
    } catch {
      return null;
    }
  }

  setAudioTrack(id: string) {
    const tracks = this.player.getTracksFor("audio");
    const match = tracks.find((t, i) => trackKey(t, i) === id);
    if (match) this.player.setCurrentTrack(match);
  }

  getTextTracks(): TextTrackInfo[] {
    let tracks;
    try {
      tracks = this.player.getTracksFor("text");
    } catch {
      return [];
    }
    return tracks.map((track, index) => ({
      // dash.js selects text tracks by ordinal index, so that is the id.
      id: String(index),
      label: track.labels?.[0]?.text || track.lang || `Subtitles ${index + 1}`,
      lang: track.lang ?? undefined,
      source: "embedded" as const,
    }));
  }

  setTextTrack(id: string | null) {
    if (id === null) {
      this.player.enableText(false);
      return;
    }
    this.player.enableText(true);
    this.player.setTextTrack(Number(id));
  }

  recover(): boolean {
    if (this.recoveries >= MAX_AUTO_RECOVERIES || !this.url) return false;
    this.recoveries += 1;
    const resumeAt = this.video.currentTime;
    try {
      this.player.attachSource(this.url);
      if (resumeAt > 0) this.player.seek(resumeAt);
      return true;
    } catch {
      return false;
    }
  }

  getStats(): Record<string, string | number> {
    const rep = (() => {
      try {
        return this.player.getCurrentRepresentationForType("video");
      } catch {
        return null;
      }
    })();
    const buffered = this.video.buffered;
    const ahead =
      buffered.length > 0
        ? Math.max(0, buffered.end(buffered.length - 1) - this.video.currentTime)
        : 0;

    return {
      Engine: this.name,
      Representations: this.videoRepresentations().length,
      Resolution: rep ? `${rep.width}×${rep.height}` : "—",
      Bitrate: rep ? formatBitrate(rep.bandwidth) : "—",
      "Buffer ahead": `${ahead.toFixed(1)}s`,
      "Dropped frames": this.video.getVideoPlaybackQuality?.().droppedVideoFrames ?? 0,
    };
  }
}

/** dash.js MediaInfo has no stable public id, so fall back to the ordinal. */
function trackKey(track: { id?: string | null; lang?: string | null }, index: number) {
  return track.id ?? `${track.lang ?? "track"}-${index}`;
}
