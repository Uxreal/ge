import type { QueueItemStatus } from "@/lib/mock/publishing";
import { cn } from "@/lib/utils";

// Same color convention used across the app: queued = muted, in-flight = brand,
// published/done = success, failed = destructive, needs-review = warning.
const STYLES: Record<QueueItemStatus, string> = {
  queued: "border-border bg-muted/30 text-muted-foreground",
  uploading: "border-brand/40 bg-brand/10 text-brand",
  published: "border-success/30 bg-success/10 text-success",
  failed: "border-destructive/40 bg-destructive/10 text-destructive",
  "needs-review": "border-warning/40 bg-warning/10 text-warning",
};

const LABELS: Record<QueueItemStatus, string> = {
  queued: "Queued",
  uploading: "Uploading",
  published: "Published",
  failed: "Failed",
  "needs-review": "Needs review",
};

export function QueueStatusBadge({ status, className }: { status: QueueItemStatus; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[10px] font-medium", STYLES[status], className)}>
      <span className={cn("size-1.5 rounded-full bg-current", status === "uploading" && "animate-pulse")} />
      {LABELS[status]}
    </span>
  );
}
