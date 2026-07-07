import { AlertTriangle } from "lucide-react";
import type { Asset } from "@/lib/mock/media";
import { cn } from "@/lib/utils";
import { AssetVisual } from "./asset-visual";

const SOURCE_TONE: Record<Asset["sourceLabel"], string> = {
  "AI-generated": "border-brand/30 bg-brand/10 text-brand",
  Stock: "border-border bg-surface/70 text-muted-foreground",
  "Voice clone": "border-success/30 bg-success/10 text-success",
  Uploaded: "border-warning/30 bg-warning/10 text-warning",
};

export function AssetCard({ asset, onClick }: { asset: Asset; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="group flex flex-col overflow-hidden rounded-xl border border-border/70 bg-surface/60 text-left transition-all duration-150 hover:border-brand/40 hover:shadow-soft"
    >
      <div className="relative p-2 pb-0">
        <AssetVisual asset={asset} />
        {asset.similarDetected && (
          <span className="absolute left-3.5 top-3.5 inline-flex items-center gap-1 rounded-full border border-warning/40 bg-warning/20 px-1.5 py-0.5 text-[9px] font-medium text-warning backdrop-blur-sm">
            <AlertTriangle className="size-2.5" /> Similar asset
          </span>
        )}
        <span className="absolute bottom-2.5 right-3.5 rounded-md border border-border/60 bg-background/60 px-1.5 py-0.5 text-[10px] font-medium tabular-nums text-foreground/80 backdrop-blur-sm">
          {asset.meta}
        </span>
      </div>
      <div className="space-y-1.5 px-3 pb-3 pt-2">
        <div className="truncate text-sm font-medium">{asset.title}</div>
        <div className="flex flex-wrap items-center gap-1.5">
          <span className={cn("rounded-full border px-1.5 py-0.5 text-[9px] font-medium uppercase tracking-wider", SOURCE_TONE[asset.sourceLabel])}>
            {asset.sourceLabel}
          </span>
          {asset.projectTitle && <span className="truncate text-[10px] text-muted-foreground">{asset.projectTitle}</span>}
        </div>
        <div className="flex flex-wrap gap-1">
          {asset.tags.slice(0, 3).map((t) => (
            <span key={t} className="rounded border border-border/60 bg-surface/50 px-1.5 py-0.5 text-[9px] text-muted-foreground">
              #{t}
            </span>
          ))}
        </div>
      </div>
    </button>
  );
}
