import { Volume, Volume1, Volume2, VolumeX } from "lucide-react";
import { Slider } from "@/components/ui/slider";
import { ControlButton } from "./ControlButton";

interface VolumeControlProps {
  volume: number;
  muted: boolean;
  onVolumeChange: (volume: number) => void;
  onToggleMute: () => void;
}

export function VolumeControl({
  volume,
  muted,
  onVolumeChange,
  onToggleMute,
}: VolumeControlProps) {
  const level = muted ? 0 : volume;
  const Icon = muted || level === 0 ? VolumeX : level < 0.33 ? Volume : level < 0.7 ? Volume1 : Volume2;

  return (
    <div
      className="group/volume flex items-center"
      onWheel={(event) => {
        // Scrolling over the control is the fastest way to trim volume.
        event.preventDefault();
        onVolumeChange(volume + (event.deltaY < 0 ? 0.05 : -0.05));
      }}
    >
      <ControlButton
        label={muted || level === 0 ? "Unmute (m)" : "Mute (m)"}
        onClick={onToggleMute}
      >
        <Icon />
      </ControlButton>

      <div className="w-0 overflow-hidden opacity-0 transition-all duration-200 group-hover/volume:w-20 group-hover/volume:opacity-100 focus-within:w-20 focus-within:opacity-100">
        <Slider
          aria-label="Volume"
          className="mx-2 w-16 [&_[data-slot=track]]:bg-white/25"
          value={[level * 100]}
          max={100}
          step={1}
          onValueChange={([next]) => onVolumeChange(next / 100)}
        />
      </div>
    </div>
  );
}
