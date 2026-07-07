import { Position, type Edge } from "@xyflow/react";
import type { PipelineStageState, Stage } from "@/lib/mock/types";
import type { StageFlowNode } from "./stage-node";

const NODE_WIDTH = 196;
const GAP_X = 50;
const GAP_Y = 84;
const NODE_HEIGHT = 106;
const SPACING_X = NODE_WIDTH + GAP_X;
const SPACING_Y = NODE_HEIGHT + GAP_Y;
const COLS = 5; // wraps the 14-stage chain into a neat 3-row snake grid

function rowOf(i: number) {
  return Math.floor(i / COLS);
}
function colOf(i: number) {
  const row = rowOf(i);
  const inRow = i % COLS;
  return row % 2 === 0 ? inRow : COLS - 1 - inRow;
}

export function buildStageFlow(stages: PipelineStageState[], currentStage: Stage) {
  // For each stage, figure out which side its incoming/outgoing connector sits on,
  // based on how the snake grid bends between it and its neighbors.
  const sourcePos: (Position | undefined)[] = new Array(stages.length).fill(undefined);
  const targetPos: (Position | undefined)[] = new Array(stages.length).fill(undefined);

  for (let i = 0; i < stages.length - 1; i++) {
    const rowA = rowOf(i);
    const rowB = rowOf(i + 1);
    if (rowA === rowB) {
      const leftToRight = rowA % 2 === 0;
      sourcePos[i] = leftToRight ? Position.Right : Position.Left;
      targetPos[i + 1] = leftToRight ? Position.Left : Position.Right;
    } else {
      sourcePos[i] = Position.Bottom;
      targetPos[i + 1] = Position.Top;
    }
  }

  const nodes: StageFlowNode[] = stages.map((s, i) => ({
    id: s.stage,
    type: "stage",
    position: { x: colOf(i) * SPACING_X, y: rowOf(i) * SPACING_Y },
    data: {
      stage: s.stage,
      status: s.status,
      progress: s.progress,
      isCurrent: s.stage === currentStage,
      sourceHandlePos: sourcePos[i],
      targetHandlePos: targetPos[i],
    },
    draggable: true,
  }));

  const edges: Edge[] = stages.slice(1).map((s, i) => {
    const prev = stages[i];
    const color =
      prev.status === "done" ? "var(--color-success)" :
      prev.status === "failed" ? "var(--color-destructive)" :
      prev.status === "running" ? "var(--brand)" : "var(--color-border)";
    return {
      id: `${prev.stage}-${s.stage}`,
      source: prev.stage,
      target: s.stage,
      sourceHandle: "source",
      targetHandle: "target",
      type: "smoothstep",
      animated: prev.status === "running",
      style: { stroke: color, strokeWidth: 2 },
    };
  });

  return { nodes, edges };
}
