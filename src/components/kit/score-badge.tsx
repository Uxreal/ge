import { cn } from "@/lib/utils";

export function scoreTone(score: number) {
  if (score >= 80) return "success";
  if (score >= 60) return "warning";
  return "danger";
}

export function ScoreBadge({ score, label, className }: { score: number; label?: string; className?: string }) {
  const tone = scoreTone(score);
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium tabular-nums",
        tone === "success" && "border-success/30 bg-success/10 text-success",
        tone === "warning" && "border-warning/30 bg-warning/10 text-warning",
        tone === "danger" && "border-destructive/30 bg-destructive/10 text-destructive",
        className,
      )}
    >
      <span className="size-1.5 rounded-full bg-current" />
      {score}
      {label && <span className="text-[10px] font-normal uppercase tracking-wider opacity-70">{label}</span>}
    </span>
  );
}

export function MetricPill({
  icon, label, value, delta,
}: { icon?: React.ReactNode; label: string; value: string; delta?: number }) {
  const up = (delta ?? 0) >= 0;
  return (
    <span className="inline-flex items-center gap-2 rounded-full border border-border bg-surface/60 px-3 py-1 text-xs">
      {icon && <span className="text-muted-foreground">{icon}</span>}
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium tabular-nums text-foreground">{value}</span>
      {delta !== undefined && (
        <span className={cn("tabular-nums", up ? "text-success" : "text-destructive")}>
          {up ? "▲" : "▼"} {Math.abs(delta)}%
        </span>
      )}
    </span>
  );
}
