import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { StatCard } from "@/components/kit/stat-card";
import { EmptyState } from "@/components/kit/empty-state";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { MultiSelectFilter } from "@/components/discovery/multi-select-filter";
import { TrendCard } from "@/components/trend-discovery/trend-card";
import { TrendTable } from "@/components/trend-discovery/trend-table";
import { TrendDetailSheet } from "@/components/trend-discovery/trend-detail-sheet";
import {
  TrendingUp, Search, LayoutGrid, List as ListIcon, Flame, Gauge, Globe2, X as XIcon, RefreshCw,
} from "lucide-react";
import { trends } from "@/lib/mock/data";
import type { Trend, Platform } from "@/lib/mock/types";
import { toast } from "sonner";

type SortKey = "virality" | "growth" | "newest" | "evergreen" | "revenue";

const SORT_LABEL: Record<SortKey, string> = {
  virality: "Virality score",
  growth: "Growth velocity",
  newest: "Newest",
  evergreen: "Evergreen score",
  revenue: "Revenue potential",
};

const PLATFORM_LABEL: Record<Platform, string> = {
  youtube: "YouTube",
  tiktok: "TikTok",
  instagram: "Instagram",
  reddit: "Reddit",
  x: "X",
  "google-trends": "Google Trends",
  news: "News",
  rss: "RSS",
};

export default function TrendDiscovery() {
  const [search, setSearch] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [platforms, setPlatforms] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<SortKey>("virality");
  const [view, setView] = useState<"cards" | "table">("cards");
  const [activeTrend, setActiveTrend] = useState<Trend | null>(null);

  const allCategories = useMemo(
    () => Array.from(new Set(trends.map((t) => t.category))).sort(),
    [],
  );
  const allPlatforms = useMemo(
    () => Array.from(new Set(trends.flatMap((t) => t.sources))).sort(),
    [],
  );

  const stats = useMemo(() => {
    const risingFast = trends.filter((t) => t.growthVelocity > 25).length;
    const avgVirality = Math.round(trends.reduce((s, t) => s + t.viralityScore, 0) / trends.length);
    const distinctSources = new Set(trends.flatMap((t) => t.sources)).size;
    return { total: trends.length, risingFast, avgVirality, distinctSources };
  }, []);

  const filtered = useMemo(() => {
    const list = trends.filter((t) => {
      if (search && !t.topic.toLowerCase().includes(search.toLowerCase())) return false;
      if (categories.length && !categories.includes(t.category)) return false;
      if (platforms.length && !t.sources.some((s) => platforms.includes(s))) return false;
      return true;
    });
    return [...list].sort((a, b) => {
      switch (sortBy) {
        case "virality": return b.viralityScore - a.viralityScore;
        case "growth": return b.growthVelocity - a.growthVelocity;
        case "newest": return a.ageHours - b.ageHours;
        case "evergreen": return b.evergreenScore - a.evergreenScore;
        case "revenue": return b.revenuePotential.length - a.revenuePotential.length;
        default: return 0;
      }
    });
  }, [search, categories, platforms, sortBy]);

  const hasFilters = search.length > 0 || categories.length > 0 || platforms.length > 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Trend Discovery"
        subtitle="Signals compounding across YouTube, TikTok, Reddit, X, News, and Google Trends."
        actions={
          <Button
            className="h-9 rounded-lg text-brand-foreground shadow-glow"
            style={{ background: "var(--gradient-brand)" }}
            onClick={() => toast.success("Scan complete", { description: "Checked 8 sources for fresh signals." })}
          >
            <RefreshCw className="mr-2 size-4" /> Scan for trends
          </Button>
        }
      />

      {/* Stat row */}
      <section className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Trends tracked" value={String(stats.total)} icon={<TrendingUp className="size-4" />} />
        <StatCard label="Rising fast" value={String(stats.risingFast)} icon={<Flame className="size-4" />} accent="success" />
        <StatCard label="Avg. virality score" value={String(stats.avgVirality)} icon={<Gauge className="size-4" />} />
        <StatCard label="Sources scanned" value={String(stats.distinctSources)} icon={<Globe2 className="size-4" />} />
      </section>

      {/* Filter bar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-border/70 bg-surface/50 p-3 sm:flex-row sm:flex-wrap sm:items-center">
        <div className="relative min-w-[200px] flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search trends by topic…"
            className="h-9 rounded-lg pl-8"
          />
        </div>

        <MultiSelectFilter label="Category" options={allCategories} selected={categories} onChange={setCategories} />
        <MultiSelectFilter
          label="Source"
          options={allPlatforms}
          selected={platforms}
          onChange={setPlatforms}
          formatOption={(p) => PLATFORM_LABEL[p as Platform] ?? p}
        />

        <Select value={sortBy} onValueChange={(v) => setSortBy(v as SortKey)}>
          <SelectTrigger className="h-9 w-[180px] rounded-lg bg-surface/60 text-xs">
            <SelectValue placeholder="Sort by" />
          </SelectTrigger>
          <SelectContent>
            {(Object.keys(SORT_LABEL) as SortKey[]).map((k) => (
              <SelectItem key={k} value={k}>{SORT_LABEL[k]}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        {hasFilters && (
          <Button
            variant="ghost" size="sm" className="h-9 text-xs text-muted-foreground"
            onClick={() => { setSearch(""); setCategories([]); setPlatforms([]); }}
          >
            <XIcon className="mr-1 size-3.5" /> Clear
          </Button>
        )}

        <ToggleGroup
          type="single"
          value={view}
          onValueChange={(v) => v && setView(v as "cards" | "table")}
          className="ml-auto rounded-lg border border-border bg-surface/60 p-0.5"
        >
          <ToggleGroupItem value="cards" className="h-8 rounded-md px-2.5" aria-label="Card view">
            <LayoutGrid className="size-3.5" />
          </ToggleGroupItem>
          <ToggleGroupItem value="table" className="h-8 rounded-md px-2.5" aria-label="Table view">
            <ListIcon className="size-3.5" />
          </ToggleGroupItem>
        </ToggleGroup>
      </div>

      <div className="text-xs text-muted-foreground">{filtered.length} of {trends.length} trends</div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Search className="size-6" />}
          title="No trends match"
          description="Try clearing a filter or searching a different topic."
        />
      ) : view === "cards" ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((t) => <TrendCard key={t.id} trend={t} onView={setActiveTrend} />)}
        </div>
      ) : (
        <TrendTable trends={filtered} onView={setActiveTrend} />
      )}

      <TrendDetailSheet trend={activeTrend} onOpenChange={(open) => !open && setActiveTrend(null)} />
    </div>
  );
}
