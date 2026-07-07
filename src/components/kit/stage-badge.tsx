import type { Stage, StageStatus } from "@/lib/mock/types";
import { cn } from "@/lib/utils";

const STAGE_LABEL: Record<Stage, string> = {
  trend: "Trend", idea: "Idea", script: "Script", "fact-check": "Fact-check",
  scenes: "Scenes", assets: "Assets", video: "Video", voice: "Voice",
  captions: "Captions", music: "Music", thumbnail: "Thumbnail", seo: "SEO",
  schedule: "Schedule", publish: "Publish",
};

const STATUS_STYLES: Record<StageStatus, string> = {
  queued: "border-border bg-muted/30 text-muted-foreground",
  running: "border-brand/40 bg-brand/10 text-brand",
  "needs-approval": "border-warning/40 bg-warning/10 text-warning",
  done: "border-success/30 bg-success/10 text-success",
  failed: "border-destructive/40 bg-destructive/10 text-destructive",
};

export function StageBadge({ stage, status, className }: { stage: Stage; status: StageStatus; className?: string }) {
  return (
    <span className={cn(
      "inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[10px] font-medium",
      STATUS_STYLES[status], className,
    )}>
      <span className={cn(
        "size-1.5 rounded-full",
        status === "running" ? "bg-current animate-pulse" : "bg-current",
      )} />
      {STAGE_LABEL[stage]}
    </span>
  );
}

export const stageLabel = (s: Stage) => STAGE_LABEL[s];

export const ALL_STAGES: Stage[] = [
  "trend", "idea", "script", "fact-check", "scenes", "assets", "video",
  "voice", "captions", "music", "thumbnail", "seo", "schedule", "publish",
];
