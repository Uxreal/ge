import { GlassPanel } from "@/components/kit/glass-panel";
import { SourceBadge } from "@/components/kit/source-badge";
import type { Platform } from "@/lib/mock/types";

interface Row {
  platform: Platform;
  primaryLabel: string; // "Title" or "Caption"
  primaryLength: number;
  tagLabel: string; // "Tags" or "Hashtags"
  tagCount: number;
}

export function ComparisonSummary({ rows }: { rows: Row[] }) {
  return (
    <GlassPanel className="p-5">
      <div className="mb-4">
        <h2 className="text-sm font-semibold">Cross-platform comparison</h2>
        <p className="text-xs text-muted-foreground">
          Each platform gets its own copy, length, and tagging strategy — never a copy-paste of the same post.
        </p>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        {rows.map((r) => (
          <div key={r.platform} className="rounded-xl border border-border/70 bg-surface/60 p-3">
            <div className="mb-2">
              <SourceBadge source={r.platform} />
            </div>
            <div className="grid grid-cols-2 gap-2 text-center">
              <div className="rounded-lg border border-border/60 p-2">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">{r.primaryLabel} length</div>
                <div className="mt-0.5 text-sm font-semibold tabular-nums">{r.primaryLength} chars</div>
              </div>
              <div className="rounded-lg border border-border/60 p-2">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">{r.tagLabel}</div>
                <div className="mt-0.5 text-sm font-semibold tabular-nums">{r.tagCount}</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </GlassPanel>
  );
}
