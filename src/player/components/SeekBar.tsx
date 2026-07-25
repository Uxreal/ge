import { useCallback, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { formatTime } from "../lib/format";

interface SeekBarProps {
  currentTime: number;
  duration: number;
  bufferedEnd: number;
  onSeek: (time: number) => void;
  /** Fired on pointer down/up so the chrome can stay visible while scrubbing. */
  onScrubbingChange?: (scrubbing: boolean) => void;
  disabled?: boolean;
}

/**
 * Custom scrubber rather than a range input: it has to show the buffered
 * range behind the played range, preview the time under the cursor, and stay
 * responsive while the pointer is dragged outside the element.
 */
export function SeekBar({
  currentTime,
  duration,
  bufferedEnd,
  onSeek,
  onScrubbingChange,
  disabled,
}: SeekBarProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [scrubTime, setScrubTime] = useState<number | null>(null);
  const [hoverTime, setHoverTime] = useState<number | null>(null);

  const usable = duration > 0 && Number.isFinite(duration) && !disabled;
  const displayTime = scrubTime ?? currentTime;
  const percent = usable ? Math.min(100, (displayTime / duration) * 100) : 0;
  const bufferedPercent = usable
    ? Math.min(100, (bufferedEnd / duration) * 100)
    : 0;

  const timeFromEvent = useCallback(
    (clientX: number) => {
      const track = trackRef.current;
      if (!track || !usable) return 0;
      const rect = track.getBoundingClientRect();
      const ratio = (clientX - rect.left) / rect.width;
      return Math.min(duration, Math.max(0, ratio * duration));
    },
    [duration, usable],
  );

  const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!usable) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    onScrubbingChange?.(true);
    setScrubTime(timeFromEvent(event.clientX));
  };

  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!usable) return;
    const time = timeFromEvent(event.clientX);
    setHoverTime(time);
    // Only track the drag when this element captured the pointer.
    if (scrubTime !== null) setScrubTime(time);
  };

  const commit = (event: React.PointerEvent<HTMLDivElement>) => {
    if (scrubTime === null) return;
    event.currentTarget.releasePointerCapture?.(event.pointerId);
    onSeek(scrubTime);
    setScrubTime(null);
    onScrubbingChange?.(false);
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!usable) return;
    const step = event.shiftKey ? 60 : 5;
    if (event.key === "ArrowLeft") onSeek(currentTime - step);
    else if (event.key === "ArrowRight") onSeek(currentTime + step);
    else if (event.key === "Home") onSeek(0);
    else if (event.key === "End") onSeek(duration);
    else return;
    event.preventDefault();
    event.stopPropagation();
  };

  const tooltipTime = scrubTime ?? hoverTime;
  const tooltipPercent =
    tooltipTime !== null && usable ? (tooltipTime / duration) * 100 : 0;

  return (
    <div
      ref={trackRef}
      role="slider"
      tabIndex={usable ? 0 : -1}
      aria-label="Seek"
      aria-valuemin={0}
      aria-valuemax={Math.round(duration) || 0}
      aria-valuenow={Math.round(displayTime)}
      aria-valuetext={`${formatTime(displayTime)} of ${formatTime(duration)}`}
      aria-disabled={!usable}
      className={cn(
        "group relative flex h-5 w-full items-center outline-none",
        usable ? "cursor-pointer" : "cursor-default opacity-60",
      )}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={commit}
      onPointerCancel={commit}
      onPointerLeave={() => setHoverTime(null)}
      onKeyDown={onKeyDown}
    >
      {/* Track */}
      <div className="relative h-1 w-full overflow-hidden rounded-full bg-white/25 transition-[height] duration-150 group-hover:h-1.5 group-focus-visible:h-1.5">
        <div
          className="absolute inset-y-0 left-0 bg-white/30"
          style={{ width: `${bufferedPercent}%` }}
        />
        <div
          className="absolute inset-y-0 left-0 bg-brand"
          style={{ width: `${percent}%` }}
        />
      </div>

      {/* Handle */}
      <div
        className={cn(
          "pointer-events-none absolute h-3.5 w-3.5 -translate-x-1/2 rounded-full bg-brand shadow-[0_0_0_1px_rgba(0,0,0,0.35)] transition-transform duration-150",
          scrubTime !== null
            ? "scale-100"
            : "scale-0 group-hover:scale-100 group-focus-visible:scale-100",
        )}
        style={{ left: `${percent}%` }}
      />

      {/* Hover / scrub time readout */}
      {tooltipTime !== null && usable && (
        <div
          className="pointer-events-none absolute bottom-6 -translate-x-1/2 rounded bg-black/85 px-1.5 py-0.5 font-mono text-[11px] text-white tabular-nums shadow-lg"
          style={{ left: `clamp(1.5rem, ${tooltipPercent}%, calc(100% - 1.5rem))` }}
        >
          {formatTime(tooltipTime)}
        </div>
      )}
    </div>
  );
}
