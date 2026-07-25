import { AlertTriangle, Loader2, Play, RotateCcw, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { EngineError, StreamKind } from "../engine/types";
import { streamKindLabel } from "../engine/detect";

export function BufferingOverlay({ visible }: { visible: boolean }) {
  if (!visible) return null;
  return (
    <div className="pointer-events-none absolute inset-0 grid place-items-center">
      <Loader2 className="size-12 animate-spin text-white/80 drop-shadow-lg" />
    </div>
  );
}

export function CenterPlayButton({
  visible,
  onClick,
}: {
  visible: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      aria-label="Play"
      onClick={onClick}
      className={cn(
        "absolute inset-0 grid place-items-center transition-opacity duration-200",
        visible ? "opacity-100" : "pointer-events-none opacity-0",
      )}
    >
      <span className="grid size-20 place-items-center rounded-full bg-black/55 backdrop-blur-sm ring-1 ring-white/20 transition-transform hover:scale-105">
        <Play className="size-9 translate-x-0.5 fill-white text-white" />
      </span>
    </button>
  );
}

interface ErrorOverlayProps {
  error: EngineError & { canFallback: boolean };
  kind: StreamKind | null;
  onRetry: () => void;
  onForceKind: (kind: StreamKind) => void;
}

export function ErrorOverlay({
  error,
  kind,
  onRetry,
  onForceKind,
}: ErrorOverlayProps) {
  const otherKinds = (["hls", "dash", "progressive"] as StreamKind[]).filter(
    (k) => k !== kind,
  );

  return (
    <div className="absolute inset-0 grid place-items-center bg-black/85 p-6 text-center backdrop-blur-sm">
      <div className="max-w-lg space-y-4">
        <div className="mx-auto grid size-12 place-items-center rounded-full bg-destructive/15 text-destructive">
          {error.likelyCors ? (
            <ShieldAlert className="size-6" />
          ) : (
            <AlertTriangle className="size-6" />
          )}
        </div>

        <div className="space-y-2">
          <h2 className="text-lg font-semibold text-white">
            {error.likelyCors ? "Could not load this stream" : "Playback failed"}
          </h2>
          <p className="text-sm leading-relaxed text-white/70">{error.message}</p>
          {error.likelyCors && (
            <p className="text-sm leading-relaxed text-white/60">
              The browser gave no reason, which usually means one of three
              things: the URL is wrong, the host is unreachable, or the server
              does not send the cross-origin (CORS) headers a web page needs in
              order to play it. Only the first is fixable from here — the rest
              belong to the server.
            </p>
          )}
          {error.code && (
            <p className="font-mono text-xs text-white/40">{error.code}</p>
          )}
        </div>

        <div className="flex flex-wrap items-center justify-center gap-2">
          <Button onClick={onRetry} variant="secondary" size="sm">
            <RotateCcw className="size-4" />
            Try again
          </Button>
          {otherKinds.map((k) => (
            <Button
              key={k}
              onClick={() => onForceKind(k)}
              variant="ghost"
              size="sm"
              className="text-white/80 hover:text-white"
            >
              Force {streamKindLabel(k)}
            </Button>
          ))}
        </div>
      </div>
    </div>
  );
}

/** Brief "+10s" flash after a keyboard or double-tap seek. */
export function SeekFeedback({ label }: { label: string | null }) {
  if (!label) return null;
  return (
    <div className="pointer-events-none absolute inset-0 grid place-items-center">
      <span className="animate-in fade-in zoom-in-95 rounded-full bg-black/70 px-4 py-2 font-mono text-lg text-white tabular-nums shadow-lg">
        {label}
      </span>
    </div>
  );
}
