import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { StatCard } from "@/components/kit/stat-card";
import { ScoreBadge } from "@/components/kit/score-badge";
import { EmptyState } from "@/components/kit/empty-state";
import { Button } from "@/components/ui/button";
import { InsightCard } from "@/components/learning/insight-card";
import { Brain, CheckCircle2, Clock3, Gauge, Check, X } from "lucide-react";
import { channelById } from "@/lib/mock/data";
import {
  insights as insightsFixture, pendingAdjustments as pendingAdjustmentsFixture,
  INSIGHT_CATEGORIES, type InsightCategory, type PendingAdjustment,
} from "@/lib/mock/learning";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

export default function LearningLoop() {
  const [insights] = useState(insightsFixture);
  const [autoApply, setAutoApply] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(insightsFixture.map((i) => [i.id, i.status === "applied"])),
  );
  const [adjustments, setAdjustments] = useState<PendingAdjustment[]>(pendingAdjustmentsFixture);
  const [category, setCategory] = useState<InsightCategory | "all">("all");

  const visibleCategories = useMemo(
    () => INSIGHT_CATEGORIES.filter((c) => insights.some((i) => i.category === c)),
    [insights],
  );

  const filteredInsights = useMemo(
    () => (category === "all" ? insights : insights.filter((i) => i.category === category)),
    [insights, category],
  );

  const pending = useMemo(() => adjustments.filter((a) => a.status === "pending"), [adjustments]);

  const summary = useMemo(() => {
    const appliedThisMonth = insights.filter((i) => i.status === "applied").length;
    const avgConfidence = Math.round(
      insights.reduce((s, i) => s + i.confidence, 0) / insights.length,
    );
    return { appliedThisMonth, pendingReview: pending.length, avgConfidence };
  }, [insights, pending]);

  const handleToggleAutoApply = (id: string, next: boolean) => {
    setAutoApply((prev) => ({ ...prev, [id]: next }));
    toast(next ? "Auto-apply turned on" : "Auto-apply turned off", {
      description: next
        ? "This insight will be applied automatically to future content."
        : "You'll be asked to approve this insight going forward.",
    });
  };

  const resolveAdjustment = (id: string, status: "approved" | "dismissed") => {
    const adj = adjustments.find((a) => a.id === id);
    setAdjustments((prev) => prev.map((a) => (a.id === id ? { ...a, status } : a)));
    if (status === "approved") {
      toast.success("Adjustment approved", { description: adj?.title });
    } else {
      toast("Adjustment dismissed", { description: adj?.title });
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Learning Loop"
        subtitle="What's working, distilled into the next batch of ideas — with you in the loop."
      />

      {/* Summary */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="Insights applied this month" value={String(summary.appliedThisMonth)} icon={<CheckCircle2 className="size-4" />} accent="success" />
        <StatCard label="Pending review" value={String(summary.pendingReview)} icon={<Clock3 className="size-4" />} />
        <StatCard label="Avg confidence" value={`${summary.avgConfidence}%`} icon={<Gauge className="size-4" />} />
      </section>

      {/* Pending adjustments */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Pending adjustments</h2>
            <p className="text-xs text-muted-foreground">AI-proposed changes awaiting your approval.</p>
          </div>
          <span className="rounded-full border border-warning/30 bg-warning/10 px-2 py-0.5 text-[10px] font-medium text-warning">
            {pending.length} waiting
          </span>
        </div>
        {pending.length === 0 ? (
          <EmptyState
            icon={<CheckCircle2 className="size-6" />}
            title="All caught up"
            description="No AI-proposed adjustments are waiting on your review right now."
          />
        ) : (
          <ul className="divide-y divide-border/60">
            {pending.map((a) => {
              const ch = channelById(a.channelId);
              return (
                <li key={a.id} className="grid grid-cols-1 gap-3 py-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <ScoreBadge score={a.confidence} label="confidence" />
                      {ch ? (
                        <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                          <span>{ch.avatar}</span> {ch.name}
                        </span>
                      ) : (
                        <span className="text-[10px] uppercase tracking-wider text-muted-foreground">All channels</span>
                      )}
                    </div>
                    <div className="text-sm font-medium">{a.title}</div>
                    <p className="text-xs leading-relaxed text-muted-foreground">{a.rationale}</p>
                  </div>
                  <div className="flex gap-1.5 sm:justify-end">
                    <Button
                      size="sm" variant="ghost" className="h-8 gap-1 text-muted-foreground hover:text-destructive"
                      onClick={() => resolveAdjustment(a.id, "dismissed")}
                    >
                      <X className="size-3.5" /> Dismiss
                    </Button>
                    <Button
                      size="sm" className="h-8 gap-1 bg-success text-success-foreground hover:bg-success/90"
                      onClick={() => resolveAdjustment(a.id, "approved")}
                    >
                      <Check className="size-3.5" /> Approve
                    </Button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </GlassPanel>

      {/* Insights */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold">Performance insights</h2>
            <p className="text-xs text-muted-foreground">What the data says is working — and what to do about it.</p>
          </div>
          <div className="scrollbar-thin flex flex-wrap gap-1.5">
            <button
              type="button" onClick={() => setCategory("all")}
              className={cn(
                "shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors duration-150",
                category === "all"
                  ? "border-brand/40 bg-brand/10 text-brand"
                  : "border-border bg-surface/60 text-muted-foreground hover:border-brand/25 hover:text-foreground",
              )}
            >
              All
            </button>
            {visibleCategories.map((c) => (
              <button
                key={c} type="button" onClick={() => setCategory(c)}
                className={cn(
                  "shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors duration-150",
                  category === c
                    ? "border-brand/40 bg-brand/10 text-brand"
                    : "border-border bg-surface/60 text-muted-foreground hover:border-brand/25 hover:text-foreground",
                )}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        {filteredInsights.length === 0 ? (
          <EmptyState icon={<Brain className="size-6" />} description="No insights in this category yet." />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {filteredInsights.map((insight) => (
              <InsightCard
                key={insight.id}
                insight={insight}
                autoApply={!!autoApply[insight.id]}
                onToggleAutoApply={handleToggleAutoApply}
              />
            ))}
          </div>
        )}
      </GlassPanel>
    </div>
  );
}
