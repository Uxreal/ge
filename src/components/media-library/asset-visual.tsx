import type { ComponentType } from "react";
import { Image as ImageIcon, Video as VideoIcon, Mic, Music2, Zap } from "lucide-react";
import type { Asset } from "@/lib/mock/media";
import { cn } from "@/lib/utils";

function gradientFor(chart: number) {
  return `linear-gradient(135deg, color-mix(in oklab, var(--chart-${chart}) 50%, var(--surface-2)) 0%, color-mix(in oklab, var(--chart-${chart}) 16%, var(--surface)) 100%)`;
}

const TYPE_ICON: Record<Asset["type"], ComponentType<{ className?: string }>> = {
  image: ImageIcon, video: VideoIcon, voice: Mic, music: Music2, sfx: Zap,
};

function isPortrait(id: string) {
  return [...id].reduce((s, c) => s + c.charCodeAt(0), 0) % 2 === 0;
}

export function AssetVisual({ asset }: { asset: Asset }) {
  const Icon = TYPE_ICON[asset.type];
  const style = { background: gradientFor(asset.chart) };

  if (asset.type === "video") {
    return (
      <div className="relative flex aspect-video w-full items-center justify-center rounded-lg" style={style}>
        <div className="grid size-9 place-items-center rounded-full bg-background/30 ring-1 ring-white/25 backdrop-blur-sm">
          <Icon className="size-4 text-foreground" />
        </div>
      </div>
    );
  }

  if (asset.type === "image") {
    return (
      <div
        className={cn("relative flex w-full items-center justify-center rounded-lg", isPortrait(asset.id) ? "aspect-[3/4]" : "aspect-square")}
        style={style}
      >
        <Icon className="size-6 text-foreground/60" />
      </div>
    );
  }

  // voice / music / sfx — waveform-style bar illustration
  const bars = asset.waveform ?? [];
  return (
    <div className="relative flex aspect-[21/9] w-full flex-col overflow-hidden rounded-lg p-2.5" style={style}>
      <Icon className="size-3.5 shrink-0 text-foreground/70" />
      <div className="flex flex-1 items-end gap-[3px] pt-2">
        {bars.map((h, i) => (
          <span key={i} className="min-w-[2px] flex-1 rounded-full bg-foreground/50" style={{ height: `${h}%` }} />
        ))}
      </div>
    </div>
  );
}
