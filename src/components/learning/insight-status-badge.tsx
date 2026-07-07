import type { InsightStatus } from "@/lib/mock/learning";
import { cn } from "@/lib/utils";

const STATUS_LABEL: Record<InsightStatus, string> = {
  proposed: "Proposed",
  applied: "Applied",
  dismissed: "Dismissed",
};

const STATUS_STYLES: Record<InsightStatus, string> = {
  proposed: "border-warning/40 bg-warning/10 text-warning",
  applied: "border-success/30 bg-success/10 text-success",
  dismissed: "border-border bg-muted/30 text-muted-foreground",
};

export function InsightStatusBadge({ status, className }: { status: InsightStatus; className?: string }) {
  return (
    <span className={cn(
      "inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[10px] font-medium",
      STATUS_STYLES[status], className,
    )}>
      <span className="size-1.5 rounded-full bg-current" />
      {STATUS_LABEL[status]}
    </span>
  );
}
