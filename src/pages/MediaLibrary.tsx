import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { EmptyState } from "@/components/kit/empty-state";
import { Input } from "@/components/ui/input";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { AssetCard } from "@/components/media-library/asset-card";
import { AssetDetailDialog } from "@/components/media-library/asset-detail-dialog";
import { assets, type Asset, type AssetType } from "@/lib/mock/media";
import { channels } from "@/lib/mock/data";
import { Search, Images, AlertTriangle } from "lucide-react";

const TYPE_LABEL: Record<AssetType, string> = {
  image: "Image", video: "Video", voice: "Voice", music: "Music", sfx: "SFX",
};

const TYPE_ORDER: AssetType[] = ["image", "video", "voice", "music", "sfx"];

export default function MediaLibrary() {
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState<AssetType | "all">("all");
  const [channelFilter, setChannelFilter] = useState<string>("all");
  const [active, setActive] = useState<Asset | null>(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return assets.filter((a) => {
      if (typeFilter !== "all" && a.type !== typeFilter) return false;
      if (channelFilter !== "all" && a.channelId !== channelFilter) return false;
      if (q && !(a.title.toLowerCase().includes(q) || a.tags.some((t) => t.toLowerCase().includes(q)))) return false;
      return true;
    });
  }, [search, typeFilter, channelFilter]);

  const similarCount = assets.filter((a) => a.similarDetected).length;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Media Library"
        subtitle="Copyright-safe assets, generated scenes, and voice takes in one place."
        actions={
          similarCount > 0 ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-warning/30 bg-warning/10 px-2.5 py-1 text-xs font-medium text-warning">
              <AlertTriangle className="size-3.5" /> {similarCount} possible duplicates
            </span>
          ) : undefined
        }
      />

      {/* Filter bar */}
      <GlassPanel className="flex flex-col gap-3 p-4 lg:flex-row lg:items-center lg:justify-between">
        <ToggleGroup
          type="single"
          value={typeFilter}
          onValueChange={(v) => v && setTypeFilter(v as AssetType | "all")}
          className="flex-wrap justify-start gap-1 rounded-lg bg-muted p-1"
        >
          <ToggleGroupItem value="all" className="h-7 rounded-md px-2.5 text-xs">All</ToggleGroupItem>
          {TYPE_ORDER.map((t) => (
            <ToggleGroupItem key={t} value={t} className="h-7 rounded-md px-2.5 text-xs">
              {TYPE_LABEL[t]}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>

        <div className="flex flex-1 items-center gap-2 lg:justify-end">
          <Select value={channelFilter} onValueChange={setChannelFilter}>
            <SelectTrigger className="h-9 w-full max-w-[200px] rounded-lg text-xs">
              <SelectValue placeholder="All channels" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All channels</SelectItem>
              {channels.map((c) => (
                <SelectItem key={c.id} value={c.id}>{c.avatar} {c.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="relative w-full max-w-[240px]">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search assets or tags…"
              className="h-9 rounded-lg pl-8 text-xs"
            />
          </div>
        </div>
      </GlassPanel>

      {/* Grid */}
      {filtered.length === 0 ? (
        <EmptyState
          icon={<Images className="size-6" />}
          title="No assets match"
          description="Try a different type, channel, or search term."
        />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {filtered.map((a) => (
            <AssetCard key={a.id} asset={a} onClick={() => setActive(a)} />
          ))}
        </div>
      )}

      <AssetDetailDialog asset={active} onOpenChange={(open) => !open && setActive(null)} />
    </div>
  );
}
