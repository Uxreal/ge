import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { StageBadge, stageLabel } from "@/components/kit/stage-badge";
import { SourceRow } from "@/components/kit/source-badge";
import { channelById } from "@/lib/mock/data";
import type { PipelineStageState, Project } from "@/lib/mock/types";
import { STAGE_DESCRIPTIONS, mockClaimsFor, mockThumbnailVariants, mockPublishTime } from "@/lib/mock/production";
import { Check, RefreshCw, Clock3, ShieldCheck, ShieldAlert, ShieldQuestion, CalendarClock, XCircle } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

const CLAIM_META = {
  verified: { label: "Verified", icon: ShieldCheck, classes: "border-success/30 bg-success/10 text-success" },
  flagged: { label: "Flagged", icon: ShieldAlert, classes: "border-warning/30 bg-warning/10 text-warning" },
  unverified: { label: "Unverified", icon: ShieldQuestion, classes: "border-border bg-muted/30 text-muted-foreground" },
} as const;

export function StageDetailSheet({
  project, stage, onOpenChange,
}: { project: Project | null; stage: PipelineStageState | null; onOpenChange: (open: boolean) => void }) {
  const open = !!project && !!stage;
  const channel = project ? channelById(project.channelId) : undefined;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="scrollbar-thin w-full overflow-y-auto sm:max-w-lg">
        {project && stage && (
          <div className="space-y-6 pb-4">
            <SheetHeader>
              <div className="flex items-center gap-2">
                <StageBadge stage={stage.stage} status={stage.status} />
                {channel && <span className="text-[10px] uppercase tracking-wider text-muted-foreground">{channel.name}</span>}
              </div>
              <SheetTitle className="text-left text-lg leading-snug">{stageLabel(stage.stage)}</SheetTitle>
              <SheetDescription className="text-left">{project.title}</SheetDescription>
            </SheetHeader>

            <p className="text-sm leading-relaxed text-foreground/85">{STAGE_DESCRIPTIONS[stage.stage]}</p>

            {stage.status === "running" && (
              <div className="rounded-xl border border-brand/20 bg-brand/5 p-4">
                <div className="mb-2 flex items-center justify-between text-xs font-medium">
                  <span className="flex items-center gap-1.5 text-brand"><Clock3 className="size-3.5" /> In progress</span>
                  <span className="tabular-nums text-brand">{stage.progress ?? 0}%</span>
                </div>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted/40">
                  <div className="h-full rounded-full bg-brand transition-all duration-300" style={{ width: `${stage.progress ?? 0}%` }} />
                </div>
              </div>
            )}

            {stage.status === "queued" && (
              <div className="rounded-xl border border-border/60 bg-surface/50 p-4 text-sm text-muted-foreground">
                Queued — waiting on the previous stage to finish before this one starts.
              </div>
            )}

            {stage.status === "failed" && (
              <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4">
                <div className="mb-1 flex items-center gap-1.5 text-xs font-semibold text-destructive">
                  <XCircle className="size-3.5" /> Generation failed
                </div>
                <p className="text-sm text-foreground/85">The last run hit an error. Retry to re-queue this stage.</p>
              </div>
            )}

            {stage.stage === "thumbnail" && (
              <div className="space-y-3">
                <div className="text-xs font-semibold text-muted-foreground">Preview</div>
                <div
                  className="flex aspect-video w-full items-end rounded-xl border border-border/60 p-3 text-sm font-semibold text-white shadow-soft"
                  style={{ background: `linear-gradient(135deg, ${channel?.color ?? "#7c5cff"} 0%, color-mix(in oklab, ${channel?.color ?? "#7c5cff"} 60%, black) 100%)` }}
                >
                  <span className="line-clamp-2 drop-shadow">{project.title}</span>
                </div>
                <div className="text-xs font-semibold text-muted-foreground">Variants</div>
                <div className="space-y-2">
                  {mockThumbnailVariants(project.id).map((v) => (
                    <div key={v.id} className="flex items-center justify-between rounded-lg border border-border/60 bg-surface/50 p-2.5">
                      <span className="text-sm">{v.label}</span>
                      <span className="text-xs tabular-nums text-success">{v.predictedCtr.toFixed(1)}% CTR</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {stage.stage === "fact-check" && (
              <div className="space-y-2">
                <div className="text-xs font-semibold text-muted-foreground">Claims checked</div>
                {mockClaimsFor(project.id).map((c, i) => {
                  const meta = CLAIM_META[c.status];
                  const Icon = meta.icon;
                  return (
                    <div key={i} className="rounded-lg border border-border/60 bg-surface/50 p-3">
                      <div className="flex items-start justify-between gap-2">
                        <p className="text-sm leading-snug text-foreground/90">{c.claim}</p>
                        <span className={cn("inline-flex shrink-0 items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-medium", meta.classes)}>
                          <Icon className="size-3" /> {meta.label}
                        </span>
                      </div>
                      <div className="mt-1.5 text-[10px] text-muted-foreground">Source: {c.source}</div>
                    </div>
                  );
                })}
              </div>
            )}

            {stage.stage === "publish" && (
              <div className="space-y-3">
                <div className="flex items-center justify-between rounded-lg border border-border/60 bg-surface/50 p-3">
                  <span className="flex items-center gap-1.5 text-xs text-muted-foreground"><CalendarClock className="size-3.5" /> Scheduled for</span>
                  <span className="text-sm font-medium">{mockPublishTime(project.id)}</span>
                </div>
                <div>
                  <div className="mb-2 text-xs font-semibold text-muted-foreground">Publishing to</div>
                  <SourceRow sources={channel?.platforms ?? []} />
                </div>
              </div>
            )}

            {stage.status === "done" && stage.stage !== "thumbnail" && stage.stage !== "fact-check" && stage.stage !== "publish" && (
              <Button variant="outline" className="w-full" onClick={() => toast("Opening output…", { description: stageLabel(stage.stage) })}>
                View output
              </Button>
            )}

            {stage.status === "failed" && (
              <Button
                className="w-full text-brand-foreground shadow-glow"
                style={{ background: "var(--gradient-brand)" }}
                onClick={() => toast.success("Retrying stage…", { description: stageLabel(stage.stage) })}
              >
                <RefreshCw className="mr-2 size-4" /> Retry {stageLabel(stage.stage)}
              </Button>
            )}

            {stage.status === "needs-approval" && (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => toast("Regenerating…", { description: stageLabel(stage.stage) })}
                >
                  <RefreshCw className="mr-2 size-4" /> Regenerate
                </Button>
                <Button
                  className="flex-1 bg-success text-success-foreground hover:bg-success/90"
                  onClick={() => toast.success("Approved", { description: `${stageLabel(stage.stage)} · ${project.title}` })}
                >
                  <Check className="mr-2 size-4" /> Approve
                </Button>
              </div>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
