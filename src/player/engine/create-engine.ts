import { canPlayNativeHls } from "./detect";
import { NativeEngine } from "./native-engine";
import type { EngineEvents, MediaEngine, StreamKind } from "./types";

/**
 * Pick the best engine for a stream kind on this browser.
 *
 * hls.js and dash.js are ~150 kB and ~250 kB gzipped respectively, and most
 * sessions need at most one of them, so both are code-split behind a dynamic
 * import. The native engine ships in the main bundle — it is a few hundred
 * lines and handles the common case of a plain MP4.
 *
 * HLS prefers hls.js wherever Media Source Extensions exist, because it
 * exposes the bitrate ladder and the audio/subtitle tracks that the platform
 * player keeps to itself. Safari and iOS have no MSE path for HLS, so they
 * fall through to the native demuxer.
 */
export async function createEngine(
  kind: StreamKind,
  video: HTMLVideoElement,
  events: EngineEvents,
): Promise<MediaEngine> {
  switch (kind) {
    case "hls": {
      // Skip the hls.js download entirely on browsers that cannot use it.
      if ("MediaSource" in window || "ManagedMediaSource" in window) {
        const { HlsEngine, isHlsSupported } = await import("./hls-engine");
        if (isHlsSupported()) return new HlsEngine(video, events);
      }
      if (canPlayNativeHls(video)) return new NativeEngine(video, events, "hls");
      throw new UnsupportedStreamError(
        "This browser cannot play HLS: it supports neither Media Source Extensions nor native HLS.",
      );
    }

    case "dash": {
      if (!("MediaSource" in window)) {
        throw new UnsupportedStreamError(
          "This browser cannot play MPEG-DASH: Media Source Extensions are unavailable.",
        );
      }
      const { DashEngine } = await import("./dash-engine");
      return new DashEngine(video, events);
    }

    case "progressive":
      return new NativeEngine(video, events, "progressive");
  }
}

export class UnsupportedStreamError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "UnsupportedStreamError";
  }
}
