import type { Channel } from "@/lib/mock/types";
import { SourceRow } from "@/components/kit/source-badge";
import { CircleDot, ArrowUpRight } from "lucide-react";

function formatCompact(n: number) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

export function ChannelCard({ channel, onSelect }: { channel: Channel; onSelect: () => void }) {
  return (
    <button
      onClick={onSelect}
      className="group w-full rounded-2xl border border-border/70 bg-surface/60 p-4 text-left transition-all duration-200 hover:border-brand/40 hover:shadow-soft"
    >
      <div className="flex items-center gap-3">
        <div
          className="grid size-11 shrink-0 place-items-center rounded-xl text-xl"
          style={{ backgroundColor: channel.color + "33" }}
        >
          <span>{channel.avatar}</span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-semibold">{channel.name}</div>
          <div className="truncate text-xs text-muted-foreground">{channel.handle}</div>
        </div>
        <ArrowUpRight className="size-4 shrink-0 text-muted-foreground opacity-0 transition group-hover:opacity-100" />
      </div>

      <div className="mt-3 flex items-center justify-between text-[11px]">
        <span className="rounded-md border border-border bg-surface/70 px-1.5 py-0.5 text-[10px] uppercase tracking-wider text-muted-foreground">
          {channel.category}
        </span>
        <span className="flex items-center gap-1 text-muted-foreground">
          <CircleDot className="size-3" /> {formatCompact(channel.subscribers)}
        </span>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <SourceRow sources={channel.platforms} />
        <span className="text-xs font-medium tabular-nums text-success">${formatCompact(channel.weeklyRevenue)}/wk</span>
      </div>
    </button>
  );
}
