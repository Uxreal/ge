import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { TooltipProvider } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import type { StreamKind } from "../engine/types";
import { usePlayer, type PlayerSource } from "../hooks/use-player";
import { useFullscreen, usePictureInPicture } from "../hooks/use-fullscreen";
import { useIdle } from "../hooks/use-idle";
import { useShortcuts } from "../hooks/use-shortcuts";
import { formatOffset } from "../lib/format";
import { loadPreferences, savePreferences } from "../lib/storage";
import { PLAYBACK_RATES } from "./SettingsMenu";
import { AddSubtitleDialog } from "./AddSubtitleDialog";
import { ControlBar } from "./ControlBar";
import {
  BufferingOverlay,
  CenterPlayButton,
  ErrorOverlay,
  SeekFeedback,
} from "./PlayerOverlays";
import { ShortcutsDialog } from "./ShortcutsDialog";
import { StatsPanel } from "./StatsPanel";

interface VideoPlayerProps {
  source: PlayerSource | null;
  /** Called when playback reaches the end (used to advance a queue). */
  onEnded?: () => void;
  className?: string;
  autoPlay?: boolean;
}

export function VideoPlayer({
  source,
  onEnded,
  className,
  autoPlay = true,
}: VideoPlayerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);

  const [forcedKind, setForcedKind] = useState<StreamKind | undefined>();

  // A forced format is a per-source override; drop it when the source changes.
  const sourceUrl = source?.url;
  useEffect(() => setForcedKind(undefined), [sourceUrl]);

  const effectiveSource = useMemo(
    () => (source ? { ...source, forceKind: forcedKind ?? source.forceKind } : null),
    [source, forcedKind],
  );

  const { state, actions, allTextTracks } = usePlayer(videoRef, effectiveSource);
  const { isFullscreen, toggle: toggleFullscreen } = useFullscreen(containerRef);
  const {
    isPip,
    supported: pipSupported,
    toggle: togglePip,
  } = usePictureInPicture(videoRef);

  const [showStats, setShowStats] = useState(() => loadPreferences().showStats);
  const [helpOpen, setHelpOpen] = useState(false);
  const [subtitleDialogOpen, setSubtitleDialogOpen] = useState(false);
  const [seekLabel, setSeekLabel] = useState<string | null>(null);

  // Controls stay put while a dialog is open — otherwise they fade out under it.
  const dialogOpen = helpOpen || subtitleDialogOpen;
  const isPlaying = state.status === "playing";
  const { isIdle, wake, hold } = useIdle(2800, isPlaying && !dialogOpen);

  useEffect(() => hold(dialogOpen), [dialogOpen, hold]);

  useEffect(() => {
    if (state.status === "ended") onEnded?.();
  }, [state.status, onEnded]);

  // Kick off playback once the stream is ready.
  const readyToPlay = state.status === "ready";
  useEffect(() => {
    if (autoPlay && readyToPlay) void actions.play();
  }, [autoPlay, readyToPlay, actions]);

  const flashSeek = useCallback((delta: number) => {
    setSeekLabel(formatOffset(delta));
    window.setTimeout(() => setSeekLabel(null), 700);
  }, []);

  const seekBy = useCallback(
    (delta: number) => {
      actions.seekBy(delta);
      flashSeek(delta);
    },
    [actions, flashSeek],
  );

  const toggleCaptions = useCallback(() => {
    if (allTextTracks.length === 0) return;
    actions.setTextTrack(
      state.activeTextTrackId === null ? allTextTracks[0].id : null,
    );
  }, [actions, allTextTracks, state.activeTextTrackId]);

  const rateBy = useCallback(
    (direction: 1 | -1) => {
      const index = PLAYBACK_RATES.indexOf(state.playbackRate);
      const nextIndex = Math.min(
        PLAYBACK_RATES.length - 1,
        Math.max(0, (index === -1 ? PLAYBACK_RATES.indexOf(1) : index) + direction),
      );
      actions.setPlaybackRate(PLAYBACK_RATES[nextIndex]);
    },
    [actions, state.playbackRate],
  );

  const shortcutHandlers = useMemo(
    () => ({
      togglePlay: actions.togglePlay,
      seekBy,
      seekToFraction: (fraction: number) => {
        if (state.duration > 0) actions.seek(state.duration * fraction);
      },
      volumeBy: (delta: number) => actions.setVolume(state.volume + delta),
      toggleMute: actions.toggleMute,
      toggleFullscreen: () => void toggleFullscreen(),
      togglePip: () => void togglePip(),
      toggleCaptions,
      rateBy,
      toggleHelp: () => setHelpOpen((open) => !open),
      onActivity: wake,
    }),
    [
      actions,
      seekBy,
      state.duration,
      state.volume,
      toggleFullscreen,
      togglePip,
      toggleCaptions,
      rateBy,
      wake,
    ],
  );

  useShortcuts(shortcutHandlers, Boolean(source) && !dialogOpen);

  const toggleStats = useCallback(() => {
    setShowStats((visible) => {
      savePreferences({ showStats: !visible });
      return !visible;
    });
  }, []);

  const controlsVisible = !isPlaying || !isIdle || state.status === "error";

  return (
    <TooltipProvider delayDuration={400}>
      <div
        ref={containerRef}
        data-player-root
        className={cn(
          "group/player relative isolate w-full overflow-hidden bg-black select-none",
          isFullscreen ? "h-screen" : "aspect-video",
          isPlaying && isIdle && "cursor-none",
          className,
        )}
        onPointerMove={wake}
        onPointerLeave={() => isPlaying && hold(false)}
      >
        {/* Click surface: tap to toggle play, double-tap for fullscreen. */}
        <div
          className="absolute inset-0 z-10"
          onClick={() => {
            wake();
            actions.togglePlay();
          }}
          onDoubleClick={() => void toggleFullscreen()}
        />

        <video
          ref={videoRef}
          className="h-full w-full bg-black"
          poster={source?.poster}
          playsInline
          // Cross-origin subtitle tracks and canvas access both need this, and
          // servers that allow playback at all allow it too.
          crossOrigin="anonymous"
          preload="metadata"
        >
          {state.externalSubtitles.map((sub) => (
            <track
              key={sub.id}
              id={sub.id}
              kind="subtitles"
              label={sub.label}
              srcLang={sub.lang || "und"}
              src={sub.src}
            />
          ))}
        </video>

        <BufferingOverlay
          visible={state.status === "loading" || state.status === "buffering"}
        />
        <SeekFeedback label={seekLabel} />
        <CenterPlayButton
          visible={
            state.status === "paused" ||
            state.status === "ready" ||
            state.status === "ended"
          }
          onClick={() => void actions.play()}
        />

        {showStats && state.status !== "error" && (
          <StatsPanel
            getStats={actions.getStats}
            extra={{ Status: state.status }}
            onClose={toggleStats}
          />
        )}

        {state.error && (
          <ErrorOverlay
            error={state.error}
            kind={state.kind}
            onRetry={actions.retry}
            onForceKind={(kind) => {
              setForcedKind(kind);
              actions.retry();
            }}
          />
        )}

        {source && (
          <ControlBar
            state={state}
            textTracks={allTextTracks}
            title={effectiveSource?.title}
            visible={controlsVisible}
            isFullscreen={isFullscreen}
            isPip={isPip}
            pipSupported={pipSupported}
            showStats={showStats}
            onTogglePlay={actions.togglePlay}
            onSeek={actions.seek}
            onSeekBy={seekBy}
            onVolumeChange={actions.setVolume}
            onToggleMute={actions.toggleMute}
            onRateChange={actions.setPlaybackRate}
            onQualityChange={actions.setQuality}
            onAudioChange={actions.setAudioTrack}
            onTextTrackChange={actions.setTextTrack}
            onToggleCaptions={toggleCaptions}
            onAddSubtitle={() => setSubtitleDialogOpen(true)}
            onToggleStats={toggleStats}
            onToggleFullscreen={() => void toggleFullscreen()}
            onTogglePip={() => void togglePip()}
            onHold={hold}
          />
        )}
      </div>

      <ShortcutsDialog open={helpOpen} onOpenChange={setHelpOpen} />
      <AddSubtitleDialog
        open={subtitleDialogOpen}
        onOpenChange={setSubtitleDialogOpen}
        onAdd={actions.addExternalSubtitle}
      />
    </TooltipProvider>
  );
}
