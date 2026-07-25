import type {
  AudioTrackInfo,
  EngineEvents,
  MediaEngine,
  QualityLevel,
  StreamKind,
  TextTrackInfo,
} from "./types";
import { EXTERNAL_TRACK_PREFIX } from "./subtitles";

/**
 * Plays whatever the browser can handle on its own: progressive MP4/WebM/OGG,
 * and HLS on Safari + iOS, where the platform demuxer beats hls.js (and is the
 * only option, since those builds do not expose MSE for HLS).
 *
 * There are no selectable bitrate ladders here — with native HLS the platform
 * owns ABR and does not report levels — so the quality menu is empty and the
 * UI hides it.
 */
export class NativeEngine implements MediaEngine {
  readonly kind: StreamKind;
  readonly name: string;

  private video: HTMLVideoElement;
  private events: EngineEvents;
  private url = "";
  private destroyed = false;
  private recoveries = 0;

  private onError = () => this.reportError();
  private onLoadedMetadata = () => {
    this.events.onReady();
    this.events.onTracksChanged();
  };
  private onTrackChange = () => this.events.onTracksChanged();

  constructor(video: HTMLVideoElement, events: EngineEvents, kind: StreamKind) {
    this.video = video;
    this.events = events;
    this.kind = kind;
    this.name = kind === "hls" ? "Native HLS (platform)" : "Native <video>";

    video.addEventListener("error", this.onError);
    video.addEventListener("loadedmetadata", this.onLoadedMetadata);
    video.textTracks.addEventListener?.("change", this.onTrackChange);
    video.textTracks.addEventListener?.("addtrack", this.onTrackChange);
  }

  private reportError() {
    if (this.destroyed) return;
    const err = this.video.error;
    const code = err?.code;

    const message = (() => {
      switch (code) {
        case MediaError.MEDIA_ERR_ABORTED:
          return "Playback was aborted.";
        case MediaError.MEDIA_ERR_NETWORK:
          return "The download failed part-way through.";
        case MediaError.MEDIA_ERR_DECODE:
          return "The file is corrupt, or uses a codec this browser cannot decode.";
        case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
          return "This browser cannot play that file. It may be an unsupported container (MKV and AVI are not playable in browsers), an unsupported codec, or the server may be blocking cross-origin requests.";
        default:
          return err?.message || "Playback failed.";
      }
    })();

    this.events.onError({
      message,
      code: code ? `MEDIA_ERR_${code}` : undefined,
      fatal: true,
      // A network drop is worth one retry; a codec mismatch never is.
      recoverable:
        code === MediaError.MEDIA_ERR_NETWORK && this.recoveries < 2,
      likelyCors: code === MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED,
    });
  }

  load(url: string) {
    this.url = url;
    this.video.src = url;
    this.video.load();
  }

  destroy() {
    this.destroyed = true;
    this.video.removeEventListener("error", this.onError);
    this.video.removeEventListener("loadedmetadata", this.onLoadedMetadata);
    this.video.textTracks.removeEventListener?.("change", this.onTrackChange);
    this.video.textTracks.removeEventListener?.("addtrack", this.onTrackChange);
    this.video.removeAttribute("src");
    this.video.load();
  }

  getQualityLevels(): QualityLevel[] {
    return [];
  }
  getActiveQualityId() {
    return null;
  }
  getSelectedQualityId() {
    return null;
  }
  setQuality() {
    /* no ladder to switch */
  }

  getAudioTracks(): AudioTrackInfo[] {
    const tracks = nativeAudioTracks(this.video);
    if (!tracks) return [];
    return Array.from({ length: tracks.length }, (_, i) => {
      const t = tracks[i];
      return {
        id: t.id || String(i),
        label: t.label || t.language || `Audio ${i + 1}`,
        lang: t.language || undefined,
      };
    });
  }

  getActiveAudioId() {
    const tracks = nativeAudioTracks(this.video);
    if (!tracks) return null;
    for (let i = 0; i < tracks.length; i++) {
      if (tracks[i].enabled) return tracks[i].id || String(i);
    }
    return null;
  }

  setAudioTrack(id: string) {
    const tracks = nativeAudioTracks(this.video);
    if (!tracks) return;
    for (let i = 0; i < tracks.length; i++) {
      tracks[i].enabled = (tracks[i].id || String(i)) === id;
    }
  }

  getTextTracks(): TextTrackInfo[] {
    const out: TextTrackInfo[] = [];
    const list = this.video.textTracks;
    for (let i = 0; i < list.length; i++) {
      const track = list[i];
      // Skip tracks the app itself attached — those are listed separately as
      // external subtitles — and metadata tracks, which are not displayable.
      if (track.id?.startsWith(EXTERNAL_TRACK_PREFIX)) continue;
      if (track.kind !== "subtitles" && track.kind !== "captions") continue;
      out.push({
        id: track.id || String(i),
        label: track.label || track.language || `Subtitles ${i + 1}`,
        lang: track.language || undefined,
        source: "embedded",
        kind: track.kind,
      });
    }
    return out;
  }

  setTextTrack(id: string | null) {
    const list = this.video.textTracks;
    for (let i = 0; i < list.length; i++) {
      const track = list[i];
      if (track.id?.startsWith(EXTERNAL_TRACK_PREFIX)) continue;
      if (track.kind !== "subtitles" && track.kind !== "captions") continue;
      track.mode = (track.id || String(i)) === id ? "showing" : "disabled";
    }
  }

  recover(): boolean {
    if (this.recoveries >= 2 || !this.url) return false;
    this.recoveries += 1;
    const resumeAt = this.video.currentTime;
    this.video.load();
    if (resumeAt > 0) {
      this.video.addEventListener(
        "loadedmetadata",
        () => {
          this.video.currentTime = resumeAt;
        },
        { once: true },
      );
    }
    return true;
  }

  getStats(): Record<string, string | number> {
    const buffered = this.video.buffered;
    const ahead =
      buffered.length > 0
        ? Math.max(0, buffered.end(buffered.length - 1) - this.video.currentTime)
        : 0;
    return {
      Engine: this.name,
      Resolution:
        this.video.videoWidth > 0
          ? `${this.video.videoWidth}×${this.video.videoHeight}`
          : "—",
      "Buffer ahead": `${ahead.toFixed(1)}s`,
      "Dropped frames":
        this.video.getVideoPlaybackQuality?.().droppedVideoFrames ?? 0,
    };
  }
}

interface NativeAudioTrack {
  id: string;
  label: string;
  language: string;
  enabled: boolean;
}

/** `audioTracks` is still unshipped in Chrome/Firefox, so it is optional. */
function nativeAudioTracks(
  video: HTMLVideoElement,
): { length: number; [i: number]: NativeAudioTrack } | null {
  const list = (video as HTMLVideoElement & { audioTracks?: unknown }).audioTracks;
  if (!list || typeof (list as { length?: number }).length !== "number") return null;
  return list as { length: number; [i: number]: NativeAudioTrack };
}
