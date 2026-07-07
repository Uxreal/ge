import type { Trend } from "@/lib/mock/types";
import { GlassPanel } from "@/components/kit/glass-panel";
import { ScoreBadge, MetricPill } from "@/components/kit/score-badge";
import { SourceRow } from "@/components/kit/source-badge";
import { Button } from "@/components/ui/button";
import { Line, LineChart, ResponsiveContainer } from "recharts";
import { Sparkles, ArrowRight, Eye, Swords, Leaf, Clock } from "lucide-react";
import { toast } from "sonner";

function cap(s: string) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function TrendCard({ trend, onView }: { trend: Trend; onView: (t: Trend) => void }) {
  return (
    <GlassPanel className="group flex flex-col gap-3 p-5 transition-all duration-200 hover:border-brand/40 hover:shadow-soft">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <ScoreBadge score={trend.viralityScore} />
            <span className="text-[10px] uppercase tracking-wider text-muted-foreground">{trend.category}</span>
            <span className="text-[10px] text-muted-foreground">· {trend.ageHours}h ago · {trend.volume}</span>
          </div>
          <h3 className="text-sm font-semibold leading-snug">{trend.topic}</h3>
          <SourceRow sources={trend.sources} />
        </div>
        <div className="h-10 w-24 shrink-0">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={trend.growth.map((v, i) => ({ i, v }))}>
              <Line type="monotone" dataKey="v" stroke="var(--brand)" strokeWidth={1.75} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="flex items-start gap-2 rounded-lg border border-brand/15 bg-brand/5 px-3 py-2">
        <Sparkles className="mt-0.5 size-3.5 shrink-0 text-brand" />
        <p className="text-xs leading-relaxed text-foreground/80">{trend.whyItsWorking}</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <MetricPill icon={<Swords className="size-3" />} label="Competition" value={cap(trend.competition)} />
        <MetricPill icon={<Leaf className="size-3" />} label="Evergreen" value={String(trend.evergreenScore)} />
        <MetricPill icon={<Clock className="size-3" />} label="Lifespan" value={trend.estimatedLifespan} />
      </div>

      <div className="mt-auto flex items-center gap-2 pt-1">
        <Button
          size="sm"
          className="h-8 flex-1 text-brand-foreground shadow-glow"
          style={{ background: "var(--gradient-brand)" }}
          onClick={() => toast.success("Idea drafted", { description: trend.topic })}
        >
          <ArrowRight className="mr-1.5 size-3.5" /> Turn into idea
        </Button>
        <Button size="sm" variant="outline" className="h-8" onClick={() => onView(trend)}>
          <Eye className="mr-1.5 size-3.5" /> View
        </Button>
      </div>
    </GlassPanel>
  );
}
