import type { Platform } from "@/lib/mock/types";
import { Youtube, Instagram, Rss, Newspaper, TrendingUp, MessageSquare } from "lucide-react";
import { cn } from "@/lib/utils";

const MAP: Record<Platform, { label: string; icon: React.ComponentType<{ className?: string }>; color: string }> = {
  youtube: { label: "YouTube", icon: Youtube, color: "text-[#ff4d4d]" },
  tiktok: { label: "TikTok", icon: MessageSquare, color: "text-foreground" },
  instagram: { label: "Instagram", icon: Instagram, color: "text-[#e26a9c]" },
  reddit: { label: "Reddit", icon: MessageSquare, color: "text-[#ff7a3d]" },
  x: { label: "X", icon: MessageSquare, color: "text-foreground" },
  "google-trends": { label: "Trends", icon: TrendingUp, color: "text-[#5b8def]" },
  news: { label: "News", icon: Newspaper, color: "text-[#22c1a8]" },
  rss: { label: "RSS", icon: Rss, color: "text-[#f5b342]" },
};

export function SourceBadge({ source, compact = false }: { source: Platform; compact?: boolean }) {
  const cfg = MAP[source];
  const Icon = cfg.icon;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border border-border bg-surface/70 px-1.5 py-0.5 text-[10px] font-medium",
        cfg.color,
      )}
      title={cfg.label}
    >
      <Icon className="size-3" />
      {!compact && <span className="text-foreground/80">{cfg.label}</span>}
    </span>
  );
}

export function SourceRow({ sources }: { sources: Platform[] }) {
  return (
    <div className="flex flex-wrap gap-1">
      {sources.map((s) => <SourceBadge key={s} source={s} compact />)}
    </div>
  );
}
