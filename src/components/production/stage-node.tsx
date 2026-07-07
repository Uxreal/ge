import { Handle, Position, type Node, type NodeProps } from "@xyflow/react";
import {
  TrendingUp, Lightbulb, FileText, ShieldCheck, Clapperboard, Layers, Video, Mic,
  Captions, Music2, Image, Search, CalendarClock, UploadCloud,
} from "lucide-react";
import { StageBadge, stageLabel } from "@/components/kit/stage-badge";
import type { Stage, StageStatus } from "@/lib/mock/types";
import { cn } from "@/lib/utils";

const STAGE_ICON: Record<Stage, React.ComponentType<{ className?: string }>> = {
  trend: TrendingUp, idea: Lightbulb, script: FileText, "fact-check": ShieldCheck,
  scenes: Clapperboard, assets: Layers, video: Video, voice: Mic, captions: Captions,
  music: Music2, thumbnail: Image, seo: Search, schedule: CalendarClock, publish: UploadCloud,
};

export type StageNodeData = {
  stage: Stage;
  status: StageStatus;
  progress?: number;
  isCurrent: boolean;
  sourceHandlePos?: Position;
  targetHandlePos?: Position;
};

export type StageFlowNode = Node<StageNodeData, "stage">;

const handleStyle: React.CSSProperties = {
  background: "var(--color-border)",
  border: "none",
  width: 6,
  height: 6,
};

export function StageNode({ data }: NodeProps<StageFlowNode>) {
  const Icon = STAGE_ICON[data.stage];
  return (
    <div
      className={cn(
        "glass w-[196px] cursor-pointer rounded-xl border p-3 shadow-soft transition-all duration-200 hover:border-brand/40 hover:shadow-glow",
        data.isCurrent ? "border-brand/50 shadow-glow" : "border-border/70",
        data.status === "needs-approval" && "ring-1 ring-warning/40",
        data.status === "failed" && "ring-1 ring-destructive/40",
      )}
    >
      {data.targetHandlePos && <Handle type="target" id="target" position={data.targetHandlePos} style={handleStyle} />}
      {data.sourceHandlePos && <Handle type="source" id="source" position={data.sourceHandlePos} style={handleStyle} />}
      <div className="flex items-center gap-2">
        <div className="grid size-7 shrink-0 place-items-center rounded-lg bg-brand/10 text-brand ring-1 ring-brand/20">
          <Icon className="size-3.5" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-xs font-semibold">{stageLabel(data.stage)}</div>
        </div>
      </div>
      <div className="mt-2.5">
        <StageBadge stage={data.stage} status={data.status} />
      </div>
      {data.status === "running" && (
        <div className="mt-2.5 space-y-1">
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted/40">
            <div
              className="h-full rounded-full bg-brand transition-all duration-300"
              style={{ width: `${data.progress ?? 0}%` }}
            />
          </div>
          <div className="text-right text-[10px] tabular-nums text-muted-foreground">{data.progress ?? 0}%</div>
        </div>
      )}
    </div>
  );
}
