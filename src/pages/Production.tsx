import { useMemo, useState } from "react";
import {
  ReactFlow, Background, BackgroundVariant, Controls, MiniMap,
  useNodesState, useEdgesState, type NodeTypes,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { ProgressRing } from "@/components/kit/progress-ring";
import { StageBadge } from "@/components/kit/stage-badge";
import { ScoreBadge } from "@/components/kit/score-badge";
import { StageNode, type StageNodeData } from "@/components/production/stage-node";
import { buildStageFlow } from "@/components/production/layout";
import { StageDetailSheet } from "@/components/production/stage-detail-sheet";
import { projects, channelById } from "@/lib/mock/data";
import type { PipelineStageState, Project } from "@/lib/mock/types";
import { Workflow, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { useTheme } from "@/lib/theme";

const nodeTypes: NodeTypes = { stage: StageNode };

function PipelineCanvas({
  project, onSelectStage,
}: { project: Project; onSelectStage: (s: PipelineStageState) => void }) {
  const { theme } = useTheme();
  const initial = useMemo(() => buildStageFlow(project.stages, project.currentStage), [project]);
  const [nodes, , onNodesChange] = useNodesState(initial.nodes);
  const [edges, , onEdgesChange] = useEdgesState(initial.edges);

  return (
    <ReactFlow
      colorMode={theme}
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      nodeTypes={nodeTypes}
      onNodeClick={(_, node) => {
        const data = node.data as StageNodeData;
        const stage = project.stages.find((s) => s.stage === data.stage);
        if (stage) onSelectStage(stage);
      }}
      nodesConnectable={false}
      fitView
      fitViewOptions={{ padding: 0.25 }}
      minZoom={0.4}
      maxZoom={1.5}
    >
      <Background variant={BackgroundVariant.Dots} gap={22} size={1} color="var(--color-border)" />
      <Controls showInteractive={false} />
      <MiniMap
        pannable
        zoomable
        bgColor="var(--color-surface)"
        maskColor="color-mix(in oklab, var(--color-background) 65%, transparent)"
        nodeColor={(n) => {
          const status = (n.data as StageNodeData).status;
          return status === "done" ? "var(--color-success)" :
            status === "running" ? "var(--brand)" :
            status === "needs-approval" ? "var(--color-warning)" :
            status === "failed" ? "var(--color-destructive)" : "var(--color-border)";
        }}
      />
    </ReactFlow>
  );
}

export default function Production() {
  const [selectedId, setSelectedId] = useState(projects[0].id);
  const [activeStage, setActiveStage] = useState<PipelineStageState | null>(null);

  const project = projects.find((p) => p.id === selectedId) ?? projects[0];
  const channel = channelById(project.channelId);
  const done = project.stages.filter((s) => s.status === "done").length;
  const pct = Math.round((done / project.stages.length) * 100);
  const currentStageState = project.stages.find((s) => s.stage === project.currentStage);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Production"
        subtitle="The full pipeline — scenes, assets, video, voice, captions, music, thumbnail — as one visual workflow."
        actions={
          <button
            className="inline-flex h-9 items-center gap-2 rounded-lg border border-border bg-surface/60 px-3 text-xs text-muted-foreground transition hover:border-brand/40 hover:text-foreground"
            onClick={() => toast.success("Pipeline queued", { description: "New run started for " + project.title })}
          >
            <Sparkles className="size-3.5" /> Re-run pipeline
          </button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[300px_1fr]">
        {/* Project rail */}
        <GlassPanel className="flex max-h-[600px] flex-col p-4">
          <div className="mb-3 flex items-center justify-between px-1">
            <h2 className="text-sm font-semibold">Projects</h2>
            <span className="text-[10px] text-muted-foreground">{projects.length} in flight</span>
          </div>
          <div className="scrollbar-thin -mx-1 flex-1 space-y-2 overflow-y-auto px-1">
            {projects.map((p) => {
              const ch = channelById(p.channelId);
              const doneCount = p.stages.filter((s) => s.status === "done").length;
              const percent = Math.round((doneCount / p.stages.length) * 100);
              const active = p.id === selectedId;
              return (
                <button
                  key={p.id}
                  onClick={() => setSelectedId(p.id)}
                  className={cn(
                    "flex w-full items-center gap-3 rounded-xl border p-2.5 text-left transition-all duration-150",
                    active ? "border-brand/50 bg-brand/10 shadow-glow" : "border-border/70 bg-surface/60 hover:border-brand/40",
                  )}
                >
                  <ProgressRing value={percent} size={38} stroke={3.5} />
                  <div className="min-w-0 flex-1 text-left">
                    <div className="flex items-center gap-1.5">
                      <span className="grid size-4 shrink-0 place-items-center rounded text-[11px]">{ch?.avatar}</span>
                      <span className="truncate text-[11px] text-muted-foreground">{ch?.name}</span>
                    </div>
                    <div className="truncate text-sm font-medium">{p.title}</div>
                    <div className="mt-1 flex items-center gap-1.5">
                      <StageBadge stage={p.currentStage} status={p.stages.find((s) => s.stage === p.currentStage)?.status ?? "queued"} />
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </GlassPanel>

        <div className="space-y-4">
          {/* Overall progress header */}
          <GlassPanel className="flex flex-wrap items-center gap-4 p-4">
            <div className="grid size-10 shrink-0 place-items-center rounded-xl text-lg" style={{ backgroundColor: (channel?.color ?? "#7c5cff") + "33" }}>
              <span>{channel?.avatar}</span>
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground">{channel?.name}</span>
                <ScoreBadge score={project.score} />
              </div>
              <div className="truncate text-base font-semibold">{project.title}</div>
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-surface/50 px-3 py-2">
              <span className="text-[10px] uppercase tracking-wider text-muted-foreground">Current stage</span>
              <StageBadge stage={project.currentStage} status={currentStageState?.status ?? "queued"} />
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-surface/50 px-3 py-2">
              <span className="text-[10px] uppercase tracking-wider text-muted-foreground">Progress</span>
              <span className="text-sm font-semibold tabular-nums">{pct}%</span>
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-surface/50 px-3 py-2">
              <span className="text-[10px] uppercase tracking-wider text-muted-foreground">Updated</span>
              <span className="text-sm font-medium">{project.updatedAt}</span>
            </div>
          </GlassPanel>

          {/* Canvas */}
          <GlassPanel className="overflow-hidden p-0">
            <div className="h-[480px] w-full">
              <PipelineCanvas key={project.id} project={project} onSelectStage={setActiveStage} />
            </div>
          </GlassPanel>
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Workflow className="size-3.5" /> Drag nodes to rearrange, scroll to zoom, click a stage for details.
          </p>
        </div>
      </div>

      <StageDetailSheet project={project} stage={activeStage} onOpenChange={(open) => !open && setActiveStage(null)} />
    </div>
  );
}
