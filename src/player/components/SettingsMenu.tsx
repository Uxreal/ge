import { Check, Gauge, Languages, Settings, SlidersHorizontal, Subtitles, Upload } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import type { AudioTrackInfo, QualityLevel, TextTrackInfo } from "../engine/types";
import { formatBitrate, languageName } from "../lib/format";

export const PLAYBACK_RATES = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 3, 4];

interface SettingsMenuProps {
  qualityLevels: QualityLevel[];
  selectedQualityId: string | null;
  activeQualityId: string | null;
  onQualityChange: (id: string | null) => void;

  audioTracks: AudioTrackInfo[];
  activeAudioId: string | null;
  onAudioChange: (id: string) => void;

  textTracks: TextTrackInfo[];
  activeTextTrackId: string | null;
  onTextTrackChange: (id: string | null) => void;
  onAddSubtitle: () => void;

  playbackRate: number;
  onRateChange: (rate: number) => void;

  showStats: boolean;
  onToggleStats: () => void;

  onOpenChange?: (open: boolean) => void;
}

export function SettingsMenu({
  qualityLevels,
  selectedQualityId,
  activeQualityId,
  onQualityChange,
  audioTracks,
  activeAudioId,
  onAudioChange,
  textTracks,
  activeTextTrackId,
  onTextTrackChange,
  onAddSubtitle,
  playbackRate,
  onRateChange,
  showStats,
  onToggleStats,
  onOpenChange,
}: SettingsMenuProps) {
  const activeLevel = qualityLevels.find((l) => l.id === activeQualityId);

  return (
    <DropdownMenu onOpenChange={onOpenChange}>
      <Tooltip>
        <TooltipTrigger asChild>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              aria-label="Playback settings"
              className={cn(
                "inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-white/90 transition-colors",
                "hover:bg-white/15 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70",
                "data-[state=open]:bg-white/15 [&_svg]:size-5",
              )}
            >
              <Settings />
            </button>
          </DropdownMenuTrigger>
        </TooltipTrigger>
        <TooltipContent side="top" className="text-xs">
          Settings
        </TooltipContent>
      </Tooltip>

      <DropdownMenuContent align="end" side="top" className="w-60">
        {/* Quality — hidden when the engine exposes no ladder (native HLS,
            progressive files), where there is nothing to choose. */}
        {qualityLevels.length > 0 && (
          <DropdownMenuSub>
            <DropdownMenuSubTrigger className="gap-2">
              <SlidersHorizontal className="size-4 opacity-70" />
              <span className="flex-1">Quality</span>
              <span className="text-xs text-muted-foreground">
                {selectedQualityId === null
                  ? `Auto${activeLevel ? ` · ${activeLevel.label}` : ""}`
                  : (qualityLevels.find((l) => l.id === selectedQualityId)?.label ??
                    "—")}
              </span>
            </DropdownMenuSubTrigger>
            <DropdownMenuSubContent className="max-h-80 w-56 overflow-y-auto">
              <DropdownMenuRadioGroup
                value={selectedQualityId ?? "auto"}
                onValueChange={(value) =>
                  onQualityChange(value === "auto" ? null : value)
                }
              >
                <DropdownMenuRadioItem value="auto">
                  <span className="flex-1">Auto</span>
                  {activeLevel && (
                    <span className="text-xs text-muted-foreground">
                      {activeLevel.label}
                    </span>
                  )}
                </DropdownMenuRadioItem>
                <DropdownMenuSeparator />
                {qualityLevels
                  .slice()
                  .sort((a, b) => (b.height ?? 0) - (a.height ?? 0) || (b.bitrate ?? 0) - (a.bitrate ?? 0))
                  .map((level) => (
                    <DropdownMenuRadioItem key={level.id} value={level.id}>
                      <span className="flex-1">{level.label}</span>
                      <span className="text-xs text-muted-foreground">
                        {formatBitrate(level.bitrate)}
                      </span>
                    </DropdownMenuRadioItem>
                  ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
        )}

        {/* Speed */}
        <DropdownMenuSub>
          <DropdownMenuSubTrigger className="gap-2">
            <Gauge className="size-4 opacity-70" />
            <span className="flex-1">Speed</span>
            <span className="text-xs text-muted-foreground">
              {playbackRate === 1 ? "Normal" : `${playbackRate}×`}
            </span>
          </DropdownMenuSubTrigger>
          <DropdownMenuSubContent className="w-40">
            <DropdownMenuRadioGroup
              value={String(playbackRate)}
              onValueChange={(value) => onRateChange(Number(value))}
            >
              {PLAYBACK_RATES.map((rate) => (
                <DropdownMenuRadioItem key={rate} value={String(rate)}>
                  {rate === 1 ? "Normal" : `${rate}×`}
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </DropdownMenuSubContent>
        </DropdownMenuSub>

        {/* Audio tracks — only meaningful with more than one */}
        {audioTracks.length > 1 && (
          <DropdownMenuSub>
            <DropdownMenuSubTrigger className="gap-2">
              <Languages className="size-4 opacity-70" />
              <span className="flex-1">Audio</span>
              <span className="max-w-24 truncate text-xs text-muted-foreground">
                {audioTracks.find((t) => t.id === activeAudioId)?.label ?? "—"}
              </span>
            </DropdownMenuSubTrigger>
            <DropdownMenuSubContent className="max-h-80 w-56 overflow-y-auto">
              <DropdownMenuRadioGroup
                value={activeAudioId ?? ""}
                onValueChange={onAudioChange}
              >
                {audioTracks.map((track) => (
                  <DropdownMenuRadioItem key={track.id} value={track.id}>
                    <span className="flex-1 truncate">{track.label}</span>
                    {track.lang && (
                      <span className="text-xs text-muted-foreground">
                        {languageName(track.lang)}
                      </span>
                    )}
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
        )}

        {/* Subtitles */}
        <DropdownMenuSub>
          <DropdownMenuSubTrigger className="gap-2">
            <Subtitles className="size-4 opacity-70" />
            <span className="flex-1">Subtitles</span>
            <span className="max-w-24 truncate text-xs text-muted-foreground">
              {activeTextTrackId
                ? (textTracks.find((t) => t.id === activeTextTrackId)?.label ?? "On")
                : "Off"}
            </span>
          </DropdownMenuSubTrigger>
          <DropdownMenuSubContent className="max-h-80 w-64 overflow-y-auto">
            <DropdownMenuItem onSelect={() => onTextTrackChange(null)}>
              <span className="flex-1">Off</span>
              {activeTextTrackId === null && <Check className="size-4" />}
            </DropdownMenuItem>
            {textTracks.length > 0 && <DropdownMenuSeparator />}
            {textTracks.map((track) => (
              <DropdownMenuItem
                key={track.id}
                onSelect={() => onTextTrackChange(track.id)}
              >
                <span className="flex-1 truncate">{track.label}</span>
                {track.source === "external" && (
                  <span className="text-[10px] uppercase tracking-wide text-muted-foreground">
                    file
                  </span>
                )}
                {activeTextTrackId === track.id && <Check className="size-4" />}
              </DropdownMenuItem>
            ))}
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={onAddSubtitle} className="gap-2">
              <Upload className="size-4 opacity-70" />
              Add subtitle file…
            </DropdownMenuItem>
          </DropdownMenuSubContent>
        </DropdownMenuSub>

        <DropdownMenuSeparator />
        <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
          Diagnostics
        </DropdownMenuLabel>
        <DropdownMenuCheckboxItem
          checked={showStats}
          onCheckedChange={onToggleStats}
        >
          Show stats overlay
        </DropdownMenuCheckboxItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
