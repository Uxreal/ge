import { useCallback, useEffect, useMemo, useReducer, useRef } from "react";
import { createEngine, UnsupportedStreamError } from "../engine/create-engine";
import { detectStreamKind, fallbackOrder } from "../engine/detect";
import {
  EXTERNAL_TRACK_PREFIX,
  revokeExternalSubtitle,
  type ExternalSubtitle,
} from "../engine/subtitles";
import type {
  AudioTrackInfo,
  EngineError,
  MediaEngine,
  QualityLevel,
  StreamKind,
  TextTrackInfo,
} from "../engine/types";
import {
  getResumePoint,
  loadPreferences,
  savePreferences,
  setResumePoint,
} from "../lib/storage";

export interface PlayerSource {
  url: string;
  title?: string;
  poster?: string;
  /** Catalog identifier, carried through to history entries. */
  sourceId?: string;
  mimeHint?: string;
  /** Skip format detection entirely. */
  forceKind?: StreamKind;
  /** Seconds to start at, overriding any stored resume point. */
  startAt?: number;
}

export type PlaybackStatus =
  | "idle"
  | "loading"
  | "ready"
  | "playing"
  | "paused"
  | "buffering"
  | "ended"
  | "error";

export interface PlayerState {
  status: PlaybackStatus;
  kind: StreamKind | null;
  engineName: string | null;
  currentTime: number;
  duration: number;
  bufferedEnd: number;
  /** True while the stream has no known end (live edge). */
  isLive: boolean;
  volume: number;
  muted: boolean;
  playbackRate: number;
  qualityLevels: QualityLevel[];
  activeQualityId: string | null;
  selectedQualityId: string | null;
  audioTracks: AudioTrackInfo[];
  activeAudioId: string | null;
  textTracks: TextTrackInfo[];
  activeTextTrackId: string | null;
  externalSubtitles: ExternalSubtitle[];
  error: (EngineError & { canFallback: boolean }) | null;
  /** Bumped when the user asks for a retry, to re-run the load effect. */
  attempt: number;
}

type Action =
  | { type: "patch"; patch: Partial<PlayerState> }
  | { type: "retry" }
  | { type: "reset"; volume: number; muted: boolean; playbackRate: number };

const BASE_STATE: PlayerState = {
  status: "idle",
  kind: null,
  engineName: null,
  currentTime: 0,
  duration: 0,
  bufferedEnd: 0,
  isLive: false,
  volume: 1,
  muted: false,
  playbackRate: 1,
  qualityLevels: [],
  activeQualityId: null,
  selectedQualityId: null,
  audioTracks: [],
  activeAudioId: null,
  textTracks: [],
  activeTextTrackId: null,
  externalSubtitles: [],
  error: null,
  attempt: 0,
};

function reducer(state: PlayerState, action: Action): PlayerState {
  switch (action.type) {
    case "patch":
      return { ...state, ...action.patch };
    case "retry":
      return { ...state, error: null, status: "loading", attempt: state.attempt + 1 };
    case "reset":
      return {
        ...BASE_STATE,
        // Volume, mute and speed are user preferences, not per-video state.
        volume: action.volume,
        muted: action.muted,
        playbackRate: action.playbackRate,
        // External subtitles are deliberately dropped: they belong to the
        // video that was playing, not the next one.
        attempt: state.attempt,
      };
  }
}

/** Persist the resume point at most this often while playing. */
const RESUME_SAVE_INTERVAL_MS = 5000;
/** Throttle for the rAF-driven progress updates. ~15fps is smooth enough. */
const TIME_UPDATE_INTERVAL_MS = 66;

export function usePlayer(
  videoRef: React.RefObject<HTMLVideoElement | null>,
  source: PlayerSource | null,
) {
  const prefsRef = useRef(loadPreferences());
  const [state, dispatch] = useReducer(reducer, {
    ...BASE_STATE,
    volume: prefsRef.current.volume,
    muted: prefsRef.current.muted,
    playbackRate: prefsRef.current.playbackRate,
  });

  const engineRef = useRef<MediaEngine | null>(null);
  /** Stream kinds already tried and failed for the current URL. */
  const triedKinds = useRef<StreamKind[]>([]);
  const lastResumeSave = useRef(0);
  const seekedToResume = useRef(false);

  const patch = useCallback(
    (next: Partial<PlayerState>) => dispatch({ type: "patch", patch: next }),
    [],
  );

  const readTracks = useCallback(() => {
    const engine = engineRef.current;
    if (!engine) return;
    patch({
      qualityLevels: engine.getQualityLevels(),
      activeQualityId: engine.getActiveQualityId(),
      selectedQualityId: engine.getSelectedQualityId(),
      audioTracks: engine.getAudioTracks(),
      activeAudioId: engine.getActiveAudioId(),
      textTracks: engine.getTextTracks(),
    });
  }, [patch]);

  /* ------------------------------------------------------- engine setup -- */

  const url = source?.url ?? null;
  const forceKind = source?.forceKind;
  const mimeHint = source?.mimeHint;
  const { attempt, externalSubtitles } = state;

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;

    triedKinds.current = [];
    seekedToResume.current = false;

    const detection = forceKind
      ? { kind: forceKind, confident: true }
      : detectStreamKind(url, mimeHint);

    let cancelled = false;

    const start = async (kind: StreamKind) => {
      triedKinds.current.push(kind);
      try {
        const engine = await createEngine(kind, video, {
          onReady: () => {
            if (cancelled) return;
            patch({ status: video.paused ? "ready" : "playing", error: null });
            readTracks();
          },
          onTracksChanged: () => {
            if (!cancelled) readTracks();
          },
          onLevelSwitched: () => {
            if (!cancelled) readTracks();
          },
          onError: (error) => {
            if (cancelled) return;

            // Detection was a guess and other formats remain — try the next
            // one before bothering the user with an error.
            const remaining = fallbackOrder(detection.kind).filter(
              (k) => !triedKinds.current.includes(k),
            );
            if (!detection.confident && remaining.length > 0) {
              engineRef.current?.destroy();
              engineRef.current = null;
              void start(remaining[0]);
              return;
            }

            if (error.recoverable && engineRef.current?.recover()) {
              patch({ status: "buffering" });
              return;
            }

            patch({
              status: "error",
              error: { ...error, canFallback: remaining.length > 0 },
            });
          },
        });

        // The engine module is fetched lazily, so the source may have changed
        // (or the component unmounted) while the import was in flight.
        if (cancelled) {
          engine.destroy();
          return;
        }

        engineRef.current = engine;
        patch({
          status: "loading",
          kind: engine.kind,
          engineName: engine.name,
          error: null,
        });
        engine.load(url);
      } catch (err) {
        if (cancelled) return;
        const message =
          err instanceof UnsupportedStreamError
            ? err.message
            : `Could not start playback: ${(err as Error).message}`;
        patch({
          status: "error",
          error: {
            message,
            fatal: true,
            recoverable: false,
            canFallback: fallbackOrder(detection.kind).some(
              (k) => !triedKinds.current.includes(k),
            ),
          },
        });
      }
    };

    start(detection.kind);

    return () => {
      cancelled = true;
      engineRef.current?.destroy();
      engineRef.current = null;
    };
  }, [url, forceKind, mimeHint, attempt, videoRef, patch, readTracks]);

  /* ------------------------------------------------- media element wiring -- */

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    // Apply stored preferences to the element before anything plays.
    video.volume = prefsRef.current.volume;
    video.muted = prefsRef.current.muted;
    video.playbackRate = prefsRef.current.playbackRate;

    const syncBuffered = () => {
      const ranges = video.buffered;
      let end = 0;
      for (let i = 0; i < ranges.length; i++) {
        if (ranges.start(i) <= video.currentTime && ranges.end(i) > end) {
          end = ranges.end(i);
        }
      }
      patch({ bufferedEnd: end });
    };

    const handlers: Array<[keyof HTMLMediaElementEventMap, () => void]> = [
      ["loadedmetadata", () => {
        patch({
          duration: video.duration,
          isLive: !Number.isFinite(video.duration),
        });
      }],
      ["durationchange", () => {
        patch({
          duration: video.duration,
          isLive: !Number.isFinite(video.duration),
        });
      }],
      ["play", () => patch({ status: "playing" })],
      ["playing", () => patch({ status: "playing" })],
      ["pause", () => {
        // `pause` also fires as part of seeking past the end; ignore that case.
        if (!video.ended) patch({ status: "paused" });
      }],
      ["waiting", () => patch({ status: "buffering" })],
      ["stalled", () => patch({ status: "buffering" })],
      ["canplay", () => syncBuffered()],
      ["ended", () => patch({ status: "ended" })],
      ["progress", syncBuffered],
      ["seeked", syncBuffered],
      ["ratechange", () => patch({ playbackRate: video.playbackRate })],
      ["volumechange", () => {
        patch({ volume: video.volume, muted: video.muted });
        prefsRef.current = {
          ...prefsRef.current,
          volume: video.volume,
          muted: video.muted,
        };
        savePreferences({ volume: video.volume, muted: video.muted });
      }],
    ];

    handlers.forEach(([event, handler]) => video.addEventListener(event, handler));
    return () =>
      handlers.forEach(([event, handler]) =>
        video.removeEventListener(event, handler),
      );
  }, [videoRef, patch]);

  /* ------------------------------------------ progress + resume bookkeeping -- */

  const isPlaying = state.status === "playing";

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !isPlaying) return;

    let raf = 0;
    let lastUpdate = 0;

    const tick = (now: number) => {
      raf = requestAnimationFrame(tick);
      if (now - lastUpdate < TIME_UPDATE_INTERVAL_MS) return;
      lastUpdate = now;
      patch({ currentTime: video.currentTime });

      if (url && now - lastResumeSave.current > RESUME_SAVE_INTERVAL_MS) {
        lastResumeSave.current = now;
        setResumePoint(url, video.currentTime, video.duration);
      }
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [isPlaying, url, videoRef, patch]);

  // Also record where the user stopped when playback pauses or the page closes.
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;

    const save = () => {
      if (video.currentTime > 0) {
        setResumePoint(url, video.currentTime, video.duration);
      }
    };
    window.addEventListener("pagehide", save);
    return () => {
      window.removeEventListener("pagehide", save);
      save();
    };
  }, [url, videoRef]);

  /* ------------------------------------------------------------- resume -- */

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url || seekedToResume.current) return;
    if (state.status !== "ready" && state.status !== "playing") return;
    if (!Number.isFinite(video.duration) || video.duration === 0) return;

    seekedToResume.current = true;
    const explicit = source?.startAt;
    const stored = explicit === undefined ? getResumePoint(url) : null;
    const target = explicit ?? stored?.position;

    if (target && target > 0 && target < video.duration) {
      video.currentTime = target;
      patch({ currentTime: target });
    }
  }, [state.status, url, source?.startAt, videoRef, patch]);

  /* ------------------------------------------------ text track selection -- */

  const { activeTextTrackId, textTracks } = state;

  useEffect(() => {
    const video = videoRef.current;
    const engine = engineRef.current;
    if (!video) return;

    const isExternal = activeTextTrackId?.startsWith(EXTERNAL_TRACK_PREFIX);

    // Engine-owned (embedded) tracks.
    engine?.setTextTrack(isExternal ? null : activeTextTrackId);

    // App-owned (external) <track> elements.
    for (let i = 0; i < video.textTracks.length; i++) {
      const track = video.textTracks[i];
      if (!track.id?.startsWith(EXTERNAL_TRACK_PREFIX)) continue;
      track.mode = track.id === activeTextTrackId ? "showing" : "disabled";
    }
  }, [activeTextTrackId, externalSubtitles, textTracks, videoRef]);

  // Auto-enable the preferred subtitle language once tracks are known.
  const autoSelectedFor = useRef<string | null>(null);
  useEffect(() => {
    const prefs = prefsRef.current;
    if (!url || !prefs.subtitlesEnabled || autoSelectedFor.current === url) return;
    if (textTracks.length === 0) return;

    autoSelectedFor.current = url;
    const match =
      textTracks.find((t) => prefs.subtitleLang && t.lang === prefs.subtitleLang) ??
      textTracks[0];
    if (match) patch({ activeTextTrackId: match.id });
  }, [textTracks, url, patch]);

  /* ------------------------------------------------ reset on source change -- */

  useEffect(() => {
    dispatch({
      type: "reset",
      volume: prefsRef.current.volume,
      muted: prefsRef.current.muted,
      playbackRate: prefsRef.current.playbackRate,
    });
    autoSelectedFor.current = null;
  }, [url]);

  // Revoke blob URLs for external subtitles when the component goes away.
  const subtitlesRef = useRef(externalSubtitles);
  subtitlesRef.current = externalSubtitles;
  useEffect(
    () => () => {
      subtitlesRef.current.forEach(revokeExternalSubtitle);
    },
    [],
  );

  /* ------------------------------------------------------------ actions -- */

  const actions = useMemo(() => {
    const el = () => videoRef.current;

    return {
      play: async () => {
        const video = el();
        if (!video) return;
        try {
          await video.play();
        } catch (err) {
          // Autoplay policies reject un-gestured play(); fall back to muted
          // playback rather than failing silently.
          if ((err as Error).name === "NotAllowedError" && !video.muted) {
            video.muted = true;
            await video.play().catch(() => undefined);
          }
        }
      },
      pause: () => el()?.pause(),
      togglePlay: () => {
        const video = el();
        if (!video) return;
        if (video.paused || video.ended) void actions.play();
        else video.pause();
      },
      seek: (time: number) => {
        const video = el();
        if (!video) return;
        const max = Number.isFinite(video.duration) ? video.duration : time;
        const next = Math.min(Math.max(0, time), max);
        video.currentTime = next;
        dispatch({ type: "patch", patch: { currentTime: next } });
      },
      seekBy: (delta: number) => {
        const video = el();
        if (video) actions.seek(video.currentTime + delta);
      },
      setVolume: (volume: number) => {
        const video = el();
        if (!video) return;
        const next = Math.min(1, Math.max(0, volume));
        video.volume = next;
        // Nudging the slider off zero should also unmute — otherwise the
        // control appears to do nothing.
        if (next > 0 && video.muted) video.muted = false;
      },
      toggleMute: () => {
        const video = el();
        if (video) video.muted = !video.muted;
      },
      setPlaybackRate: (rate: number) => {
        const video = el();
        if (!video) return;
        video.playbackRate = rate;
        savePreferences({ playbackRate: rate });
        prefsRef.current = { ...prefsRef.current, playbackRate: rate };
      },
      setQuality: (id: string | null) => {
        engineRef.current?.setQuality(id);
        dispatch({
          type: "patch",
          patch: { selectedQualityId: id },
        });
      },
      setAudioTrack: (id: string) => {
        engineRef.current?.setAudioTrack(id);
        dispatch({ type: "patch", patch: { activeAudioId: id } });
      },
      setTextTrack: (id: string | null) => {
        dispatch({ type: "patch", patch: { activeTextTrackId: id } });
        const lang = id
          ? (engineRef.current?.getTextTracks().find((t) => t.id === id)?.lang ??
            null)
          : null;
        savePreferences({ subtitlesEnabled: id !== null, subtitleLang: lang });
        prefsRef.current = {
          ...prefsRef.current,
          subtitlesEnabled: id !== null,
          subtitleLang: lang,
        };
      },
      addExternalSubtitle: (sub: ExternalSubtitle) => {
        dispatch({
          type: "patch",
          patch: {
            externalSubtitles: [...subtitlesRef.current, sub],
            activeTextTrackId: sub.id,
          },
        });
      },
      removeExternalSubtitle: (id: string) => {
        const target = subtitlesRef.current.find((s) => s.id === id);
        if (target) revokeExternalSubtitle(target);
        dispatch({
          type: "patch",
          patch: {
            externalSubtitles: subtitlesRef.current.filter((s) => s.id !== id),
          },
        });
      },
      retry: () => dispatch({ type: "retry" }),
      getStats: () => engineRef.current?.getStats() ?? {},
    };
  }, [videoRef]);

  /** External subtitles merged into the selectable track list. */
  const allTextTracks: TextTrackInfo[] = useMemo(
    () => [
      ...state.textTracks,
      ...state.externalSubtitles.map((sub) => ({
        id: sub.id,
        label: sub.label,
        lang: sub.lang || undefined,
        source: "external" as const,
      })),
    ],
    [state.textTracks, state.externalSubtitles],
  );

  return { state, actions, allTextTracks };
}

export type PlayerActions = ReturnType<typeof usePlayer>["actions"];
