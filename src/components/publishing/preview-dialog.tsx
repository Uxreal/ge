import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { SourceBadge } from "@/components/kit/source-badge";
import { Film } from "lucide-react";
import type { QueueItem } from "@/lib/mock/publishing";
import { cn } from "@/lib/utils";

const ASPECT: Record<QueueItem["platform"], string> = {
  youtube: "aspect-video",
  tiktok: "aspect-[9/16]",
  instagram: "aspect-[4/5]",
  reddit: "aspect-video",
  x: "aspect-video",
  "google-trends": "aspect-video",
  news: "aspect-video",
  rss: "aspect-video",
};

const ASPECT_LABEL: Record<QueueItem["platform"], string> = {
  youtube: "16:9 — YouTube",
  tiktok: "9:16 — TikTok",
  instagram: "4:5 — Instagram",
  reddit: "16:9",
  x: "16:9",
  "google-trends": "16:9",
  news: "16:9",
  rss: "16:9",
};

export function PreviewDialog({
  item, open, onOpenChange,
}: { item: QueueItem | undefined; open: boolean; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {item && <SourceBadge source={item.platform} />}
            Preview
          </DialogTitle>
          <DialogDescription>{item?.projectTitle}</DialogDescription>
        </DialogHeader>
        {item && (
          <div className={cn("mx-auto flex w-full max-w-[260px] flex-col items-center justify-center gap-2 rounded-xl border border-border bg-surface/70 text-center", ASPECT[item.platform])}>
            <Film className="size-8 text-muted-foreground" />
            <p className="px-4 text-xs text-muted-foreground">Preview placeholder — {ASPECT_LABEL[item.platform]}</p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
