import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { AlertTriangle, Download, Wand2 } from "lucide-react";
import { toast } from "sonner";
import type { Asset } from "@/lib/mock/media";
import { AssetVisual } from "./asset-visual";

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/60 bg-surface/50 p-2.5">
      <div className="text-[10px] uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className="mt-0.5 truncate text-sm font-semibold capitalize">{value}</div>
    </div>
  );
}

export function AssetDetailDialog({
  asset, onOpenChange,
}: { asset: Asset | null; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={!!asset} onOpenChange={onOpenChange}>
      <DialogContent className="scrollbar-thin max-h-[85vh] overflow-y-auto sm:max-w-lg">
        {asset && (
          <div className="space-y-5">
            <DialogHeader>
              <DialogTitle className="text-left">{asset.title}</DialogTitle>
              <DialogDescription className="text-left">
                {asset.projectTitle ?? "Unassigned"} · {asset.meta}
              </DialogDescription>
            </DialogHeader>

            <AssetVisual asset={asset} />

            {asset.similarDetected && (
              <div className="flex items-start gap-2 rounded-lg border border-warning/30 bg-warning/10 p-3 text-xs text-warning">
                <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
                <span>Similar asset detected elsewhere in your library — review for duplicate or overlapping usage rights before publishing.</span>
              </div>
            )}

            <div className="grid grid-cols-2 gap-3">
              <DetailRow label="Type" value={asset.type} />
              <DetailRow label="Source" value={asset.sourceLabel} />
              <DetailRow label="Created" value={asset.createdAt} />
              <DetailRow label="Size / duration" value={asset.meta} />
            </div>

            <div>
              <div className="mb-2 text-xs font-semibold text-muted-foreground">Tags</div>
              <div className="flex flex-wrap gap-1.5">
                {asset.tags.map((t) => (
                  <span key={t} className="rounded-md border border-border/60 bg-surface/50 px-2 py-0.5 text-[10px] text-muted-foreground">
                    #{t}
                  </span>
                ))}
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="outline" className="flex-1" onClick={() => toast("Downloading…", { description: asset.title })}>
                <Download className="mr-2 size-4" /> Download
              </Button>
              <Button
                className="flex-1 text-brand-foreground shadow-glow"
                style={{ background: "var(--gradient-brand)" }}
                onClick={() => toast.success("Added to project", { description: asset.title })}
              >
                <Wand2 className="mr-2 size-4" /> Use in project
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
