import type { ScheduledPostStatus } from "@/lib/mock/schedule";

// Same color convention as StageBadge: queued = muted, scheduled = brand, published = success, failed = destructive.
export const STATUS_DOT: Record<ScheduledPostStatus, string> = {
  queued: "bg-muted-foreground/50",
  scheduled: "bg-brand",
  published: "bg-success",
  failed: "bg-destructive",
};

export const STATUS_BADGE: Record<ScheduledPostStatus, string> = {
  queued: "border-border bg-muted/30 text-muted-foreground",
  scheduled: "border-brand/40 bg-brand/10 text-brand",
  published: "border-success/30 bg-success/10 text-success",
  failed: "border-destructive/40 bg-destructive/10 text-destructive",
};

export const STATUS_LABEL: Record<ScheduledPostStatus, string> = {
  queued: "Queued",
  scheduled: "Scheduled",
  published: "Published",
  failed: "Failed",
};

export const ALL_STATUSES: ScheduledPostStatus[] = ["queued", "scheduled", "published", "failed"];
