import type { Trend } from "@/lib/mock/types";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { ScoreBadge } from "@/components/kit/score-badge";
import { SourceRow } from "@/components/kit/source-badge";
import { Button } from "@/components/ui/button";
import { Line, LineChart, ResponsiveContainer, Tooltip } from "recharts";
import { Sparkles, Wand2 } from "lucide-react";
import { toast } from "sonner";

function DetailStat({ label, value, className = "" }: { label: string; value: string; className?: string }) {
  return (
    <div className="rounded-lg border border-border/60 bg-surface/50 p-2.5">
      <div className="text-[10px] uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className={`mt-0.5 text-sm font-semibold ${className}`}>{value}</div>
    </div>
  );
}

export function TrendDetailSheet({
  trend, onOpenChange,
}: { trend: Trend | null; onOpenChange: (open: boolean) => void }) {
  return (
    <Sheet open={!!trend} onOpenChange={onOpenChange}>
      <SheetContent className="scrollbar-thin w-full overflow-y-auto sm:max-w-lg">
        {trend && (
          <div className="space-y-6 pb-4">
            <SheetHeader>
              <div className="flex items-center gap-2">
                <ScoreBadge score={trend.viralityScore} label="Virality" />
                <span className="text-[10px] uppercase tracking-wider text-muted-foreground">{trend.category}</span>
              </div>
              <SheetTitle className="text-left text-lg leading-snug">{trend.topic}</SheetTitle>
              <SheetDescription className="text-left">
                {trend.volume} · {trend.ageHours}h old · {trend.seasonalRelevance}
              </SheetDescription>
            </SheetHeader>

            <div className="h-24 rounded-xl border border-border/60 bg-surface/40 p-2">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trend.growth.map((v, i) => ({ i, v }))}>
                  <Tooltip
                    contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 12, fontSize: 12 }}
                    labelFormatter={() => ""}
                    formatter={(v: number) => [v, "Index"]}
                  />
                  <Line type="monotone" dataKey="v" stroke="var(--brand)" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="rounded-xl border border-brand/20 bg-brand/5 p-4">
              <div className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-brand">
                <Sparkles className="size-3.5" /> Why it's working
              </div>
              <p className="text-sm leading-relaxed text-foreground/85">{trend.whyItsWorking}</p>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <DetailStat label="Growth velocity" value={`${trend.growthVelocity}%/day`} />
              <DetailStat label="Competition" value={trend.competition} className="capitalize" />
              <DetailStat label="Audience overlap" value={`${trend.audienceOverlap}%`} />
              <DetailStat label="Estimated lifespan" value={trend.estimatedLifespan} />
              <DetailStat label="Revenue potential" value={trend.revenuePotential} />
              <DetailStat label="Evergreen score" value={String(trend.evergreenScore)} />
              <DetailStat label="Seasonal relevance" value={trend.seasonalRelevance} />
              <DetailStat label="Search volume" value={trend.volume} />
            </div>

            <div>
              <div className="mb-2 text-xs font-semibold text-muted-foreground">Sources scanned</div>
              <SourceRow sources={trend.sources} />
            </div>

            <Button
              className="w-full text-brand-foreground shadow-glow"
              style={{ background: "var(--gradient-brand)" }}
              onClick={() => toast.success("Generating ideas…", { description: `New concepts from "${trend.topic}"` })}
            >
              <Wand2 className="mr-2 size-4" /> Generate ideas from this trend
            </Button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
