import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { StatCard } from "@/components/kit/stat-card";
import { SourceBadge } from "@/components/kit/source-badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Progress } from "@/components/ui/progress";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { QueueStatusBadge } from "@/components/publishing/status-badge";
import { PreviewDialog } from "@/components/publishing/preview-dialog";
import { CheckCircle2, Clock, AlertTriangle, RotateCcw, Eye, X, Send } from "lucide-react";
import { channelById } from "@/lib/mock/data";
import { queueItems as queueItemsFixture, type QueueItem } from "@/lib/mock/publishing";
import { toast } from "sonner";

export default function Publishing() {
  const [items, setItems] = useState<QueueItem[]>(queueItemsFixture);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [previewItem, setPreviewItem] = useState<QueueItem | undefined>(undefined);
  const [previewOpen, setPreviewOpen] = useState(false);

  const stats = useMemo(() => ({
    publishedToday: items.filter((i) => i.status === "published").length,
    inQueue: items.filter((i) => i.status === "queued" || i.status === "uploading").length,
    failed: items.filter((i) => i.status === "failed" || i.status === "needs-review").length,
  }), [items]);

  const allSelected = items.length > 0 && selected.size === items.length;
  const someSelected = selected.size > 0 && !allSelected;

  const toggleAll = (checked: boolean) => {
    setSelected(checked ? new Set(items.map((i) => i.id)) : new Set());
  };

  const toggleOne = (id: string, checked: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id); else next.delete(id);
      return next;
    });
  };

  const handleRetry = (item: QueueItem) => {
    setItems((prev) => prev.map((i) => (i.id === item.id ? { ...i, status: "queued", error: undefined, scheduledOrPublishedLabel: "Re-queued for retry" } : i)));
    toast.success("Retrying upload", { description: item.projectTitle });
  };

  const handleCancel = (item: QueueItem) => {
    setItems((prev) => prev.filter((i) => i.id !== item.id));
    setSelected((prev) => {
      const next = new Set(prev);
      next.delete(item.id);
      return next;
    });
    toast("Removed from queue", { description: item.projectTitle });
  };

  const handlePreview = (item: QueueItem) => {
    setPreviewItem(item);
    setPreviewOpen(true);
  };

  const handleBulkPublish = () => {
    const ids = new Set(selected);
    const affected = items.filter((i) => ids.has(i.id) && i.status !== "published").length;
    setItems((prev) => prev.map((i) => (ids.has(i.id) && i.status !== "published" ? { ...i, status: "uploading", progress: 6, scheduledOrPublishedLabel: "Uploading now" } : i)));
    toast.success(`Publishing ${affected} item${affected === 1 ? "" : "s"} now`);
    setSelected(new Set());
  };

  const handleBulkRetry = () => {
    const ids = new Set(selected);
    const failedIds = items.filter((i) => ids.has(i.id) && i.status === "failed").map((i) => i.id);
    setItems((prev) => prev.map((i) => (failedIds.includes(i.id) ? { ...i, status: "queued", error: undefined, scheduledOrPublishedLabel: "Re-queued for retry" } : i)));
    const skipped = ids.size - failedIds.length;
    toast.success(`Retrying ${failedIds.length} item${failedIds.length === 1 ? "" : "s"}`, {
      description: skipped > 0 ? `${skipped} selected item${skipped === 1 ? "" : "s"} weren't failed and were skipped.` : undefined,
    });
    setSelected(new Set());
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Publishing Center" subtitle="Queue, preview, and cross-post with platform-specific metadata." />

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="Published today" value={String(stats.publishedToday)} icon={<CheckCircle2 className="size-4" />} accent="success" />
        <StatCard label="In queue" value={String(stats.inQueue)} icon={<Clock className="size-4" />} />
        <StatCard label="Needs attention" value={String(stats.failed)} icon={<AlertTriangle className="size-4" />} accent="destructive" />
      </section>

      <GlassPanel className="p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">Publish queue</h2>
            <p className="text-xs text-muted-foreground">{items.length} items across every channel and platform.</p>
          </div>
          {selected.size > 0 && (
            <div className="flex items-center gap-2 rounded-lg border border-brand/30 bg-brand/10 px-3 py-1.5 text-xs">
              <span className="font-medium text-brand">{selected.size} selected</span>
              <Button size="sm" variant="outline" className="h-7 gap-1 text-xs" onClick={handleBulkPublish}>
                <Send className="size-3.5" /> Bulk publish now
              </Button>
              <Button size="sm" variant="outline" className="h-7 gap-1 text-xs" onClick={handleBulkRetry}>
                <RotateCcw className="size-3.5" /> Bulk retry
              </Button>
            </div>
          )}
        </div>

        <div className="scrollbar-thin overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-10">
                  <Checkbox
                    checked={allSelected ? true : someSelected ? "indeterminate" : false}
                    onCheckedChange={(c) => toggleAll(c === true)}
                  />
                </TableHead>
                <TableHead>Project</TableHead>
                <TableHead>Platform</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Progress / timing</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => {
                const ch = channelById(item.channelId);
                return (
                  <TableRow key={item.id}>
                    <TableCell>
                      <Checkbox
                        checked={selected.has(item.id)}
                        onCheckedChange={(c) => toggleOne(item.id, c === true)}
                      />
                    </TableCell>
                    <TableCell className="max-w-[280px]">
                      <div className="flex items-center gap-2">
                        <span
                          className="grid size-7 shrink-0 place-items-center rounded-lg text-sm"
                          style={{ backgroundColor: (ch?.color ?? "#7c5cff") + "33" }}
                        >
                          {ch?.avatar}
                        </span>
                        <div className="min-w-0">
                          <div className="truncate text-sm font-medium">{item.projectTitle}</div>
                          <div className="truncate text-[11px] text-muted-foreground">{ch?.name}</div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell><SourceBadge source={item.platform} /></TableCell>
                    <TableCell><QueueStatusBadge status={item.status} /></TableCell>
                    <TableCell className="min-w-[160px]">
                      {item.status === "uploading" ? (
                        <div className="flex items-center gap-2">
                          <Progress value={item.progress ?? 0} className="h-1.5 w-24" />
                          <span className="text-[11px] tabular-nums text-muted-foreground">{item.progress ?? 0}%</span>
                        </div>
                      ) : (
                        <div className="space-y-0.5">
                          <div className="text-xs text-muted-foreground">{item.scheduledOrPublishedLabel}</div>
                          {item.status === "failed" && item.error && (
                            <div className="text-[11px] text-destructive">{item.error}</div>
                          )}
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button size="sm" variant="ghost" className="h-8 gap-1 text-xs" onClick={() => handlePreview(item)}>
                          <Eye className="size-3.5" /> Preview
                        </Button>
                        {item.status === "failed" && (
                          <Button size="sm" variant="ghost" className="h-8 gap-1 text-xs" onClick={() => handleRetry(item)}>
                            <RotateCcw className="size-3.5" /> Retry
                          </Button>
                        )}
                        {item.status === "queued" && (
                          <Button size="sm" variant="ghost" className="h-8 gap-1 text-xs text-muted-foreground" onClick={() => handleCancel(item)}>
                            <X className="size-3.5" /> Cancel
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      </GlassPanel>

      <PreviewDialog item={previewItem} open={previewOpen} onOpenChange={setPreviewOpen} />
    </div>
  );
}
