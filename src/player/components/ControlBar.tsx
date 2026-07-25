import {
  Maximize,
  Minimize,
  Pause,
  PictureInPicture2,
  Play,
  RotateCcw,
  RotateCw,
  Subtitles,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { PlayerState } from "../hooks/use-player";
import type { StreamKind, TextTrackInfo } from "../engine/types";
import { streamKindLabel } from "../engine/detect";
import { formatTime } from "../lib/format";
import { ControlButton } from "./ControlButton";
import { SeekBar } from "./SeekBar";
import { SettingsMenu } from "./SettingsMenu";
import { VolumeControl } from "./VolumeControl";

interface ControlBarProps {
  state: PlayerState;
  textTracks: TextTrackInfo[];
  title?: string;
  visible: boolean;
  isFullscreen: boolean;
  isPip: boolean;
  pipSupported: boolean;
  showStats: boolean;

  onTogglePlay: () => void;
  onSeek: (time: number) => void;
  onSeekBy: (delta: number) => void;
  onVolumeChange: (volume: number) => void;
  onToggleMute: () => void;
  onRateChange: (rate: number) => void;
  onQualityChange: (id: string | null) => void;
  onAudioChange: (id: string) => void;
  onTextTrackChange: (id: string | null) => void;
  onToggleCaptions: () => void;
  onAddSubtitle: () => void;
  onToggleStats: () => void;
  onToggleFullscreen: () => void;
  onTogglePip: () => void;
  onHold: (held: boolean) => void;
}

export function ControlBar({
  state,
  textTracks,
  title,
  visible,
  isFullscreen,
  isPip,
  pipSupported,
  showStats,
  onTogglePlay,
  onSeek,
  onSeekBy,
  onVolumeChange,
  onToggleMute,
  onRateChange,
  onQualityChange,
  onAudioChange,
  onTextTrackChange,
  onToggleCaptions,
  onAddSubtitle,
  onToggleStats,
  onToggleFullscreen,
  onTogglePip,
  onHold,
}: ControlBarProps) {
  const isPlaying = state.status === "playing" || state.status === "buffering";
  const hasCaptions = textTracks.length > 0;
  const captionsOn = state.activeTextTrackId !== null;

  return (
    <div
      className={cn(
        "absolute inset-x-0 bottom-0 z-20 transition-all duration-200",
        // The gradient keeps white controls legible over bright frames.
        "bg-gradient-to-t from-black/85 via-black/50 to-transparent pb-2 pt-12",
        visible ? "translate-y-0 opacity-100" : "translate-y-2 opacity-0",
      )}
      // Clicks in the bar must not reach the click-to-pause surface behind it.
      onClick={(event) => event.stopPropagation()}
      onDoubleClick={(event) => event.stopPropagation()}
    >
      <div className="px-3 sm:px-4">
        <SeekBar
          currentTime={state.currentTime}
          duration={state.duration}
          bufferedEnd={state.bufferedEnd}
          onSeek={onSeek}
          onScrubbingChange={onHold}
          disabled={state.isLive || state.duration === 0}
        />

        <div className="mt-1 flex items-center gap-0.5">
          <ControlButton
            label={isPlaying ? "Pause (k)" : "Play (k)"}
            onClick={onTogglePlay}
          >
            {isPlaying ? <Pause className="fill-current" /> : <Play className="fill-current" />}
          </ControlButton>

          <ControlButton label="Back 10 seconds (j)" onClick={() => onSeekBy(-10)}>
            <RotateCcw />
          </ControlButton>
          <ControlButton label="Forward 10 seconds (l)" onClick={() => onSeekBy(10)}>
            <RotateCw />
          </ControlButton>

          <VolumeControl
            volume={state.volume}
            muted={state.muted}
            onVolumeChange={onVolumeChange}
            onToggleMute={onToggleMute}
          />

          <div className="ml-2 shrink-0 font-mono text-xs text-white/85 tabular-nums">
            {state.isLive ? (
              <span className="flex items-center gap-1.5">
                <span className="size-1.5 rounded-full bg-destructive" />
                LIVE
              </span>
            ) : (
              <>
                {formatTime(state.currentTime)}
                <span className="text-white/45"> / {formatTime(state.duration)}</span>
              </>
            )}
          </div>

          {title && (
            <div className="mx-3 hidden min-w-0 flex-1 truncate text-sm text-white/70 md:block">
              {title}
            </div>
          )}
          <div className={cn("flex-1", title && "md:hidden")} />

          {state.kind && (
            <FormatBadge kind={state.kind} rate={state.playbackRate} />
          )}

          {hasCaptions && (
            <ControlButton
              label={captionsOn ? "Turn off subtitles (c)" : "Turn on subtitles (c)"}
              onClick={onToggleCaptions}
              active={captionsOn}
              className={cn(captionsOn && "after:absolute")}
            >
              <Subtitles />
            </ControlButton>
          )}

          <SettingsMenu
            qualityLevels={state.qualityLevels}
            selectedQualityId={state.selectedQualityId}
            activeQualityId={state.activeQualityId}
            onQualityChange={onQualityChange}
            audioTracks={state.audioTracks}
            activeAudioId={state.activeAudioId}
            onAudioChange={onAudioChange}
            textTracks={textTracks}
            activeTextTrackId={state.activeTextTrackId}
            onTextTrackChange={onTextTrackChange}
            onAddSubtitle={onAddSubtitle}
            playbackRate={state.playbackRate}
            onRateChange={onRateChange}
            showStats={showStats}
            onToggleStats={onToggleStats}
            onOpenChange={onHold}
          />

          {pipSupported && (
            <ControlButton
              label="Picture-in-picture (i)"
              onClick={onTogglePip}
              active={isPip}
            >
              <PictureInPicture2 />
            </ControlButton>
          )}

          <ControlButton
            label={isFullscreen ? "Exit fullscreen (f)" : "Fullscreen (f)"}
            onClick={onToggleFullscreen}
          >
            {isFullscreen ? <Minimize /> : <Maximize />}
          </ControlButton>
        </div>
      </div>
    </div>
  );
}

function FormatBadge({ kind, rate }: { kind: StreamKind; rate: number }) {
  return (
    <div className="mr-1 hidden items-center gap-1.5 sm:flex">
      {rate !== 1 && (
        <Badge
          variant="secondary"
          className="h-5 bg-white/15 px-1.5 font-mono text-[10px] text-white hover:bg-white/15"
        >
          {rate}×
        </Badge>
      )}
      <Badge
        variant="secondary"
        className="h-5 bg-white/10 px-1.5 text-[10px] font-medium uppercase tracking-wide text-white/70 hover:bg-white/10"
      >
        {streamKindLabel(kind)}
      </Badge>
    </div>
  );
}
