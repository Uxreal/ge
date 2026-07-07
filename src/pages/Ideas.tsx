import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { CategoryChipFilter } from "@/components/ideas/category-chip-filter";
import { IdeaCard } from "@/components/ideas/idea-card";
import { IdeaCardSkeleton } from "@/components/ideas/idea-skeleton";
import { Lightbulb, Search, Wand2, X as XIcon } from "lucide-react";
import { ideas as ideasFixture } from "@/lib/mock/data";
import type { Category, Idea } from "@/lib/mock/types";
import { toast } from "sonner";

type SortKey = "score" | "category" | "originality" | "newest";
type OriginFilter = "all" | "original" | "trend";

const SORT_LABEL: Record<SortKey, string> = {
  score: "Composite score",
  category: "Category",
  originality: "Originality",
  newest: "Newest",
};

const PAGE_SIZE = 9;

function parseAgeMinutes(createdAt: string): number {
  const match = /(\d+)\s*(min|h)/.exec(createdAt);
  if (!match) return Number.POSITIVE_INFINITY;
  const n = Number(match[1]);
  return match[2] === "h" ? n * 60 : n;
}

function shuffled<T>(arr: T[]): T[] {
  const next = [...arr];
  for (let i = next.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [next[i], next[j]] = [next[j], next[i]];
  }
  return next;
}

export default function Ideas() {
  const [pool, setPool] = useState<Idea[]>(ideasFixture);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [isGenerating, setIsGenerating] = useState(false);

  const [search, setSearch] = useState("");
  const [categories, setCategories] = useState<Category[]>([]);
  const [origin, setOrigin] = useState<OriginFilter>("all");
  const [sortBy, setSortBy] = useState<SortKey>("score");

  const handleGenerate = () => {
    setIsGenerating(true);
    const delay = 600 + Math.random() * 300;
    setTimeout(() => {
      setPool((prev) => shuffled(prev));
      setVisibleCount((prev) => Math.min(prev + 6, ideasFixture.length));
      setIsGenerating(false);
      toast.success("Fresh batch generated", { description: "Surfaced new concepts from the idea pool." });
    }, delay);
  };

  const filteredSorted = useMemo(() => {
    const list = pool.filter((idea) => {
      if (search && !`${idea.title} ${idea.hook}`.toLowerCase().includes(search.toLowerCase())) return false;
      if (categories.length && !categories.includes(idea.category)) return false;
      if (origin === "original" && idea.trendId) return false;
      if (origin === "trend" && !idea.trendId) return false;
      return true;
    });
    return [...list].sort((a, b) => {
      switch (sortBy) {
        case "score": return b.score - a.score;
        case "category": return a.category.localeCompare(b.category) || b.score - a.score;
        case "originality": return b.originality - a.originality;
        case "newest": return parseAgeMinutes(a.createdAt) - parseAgeMinutes(b.createdAt);
        default: return 0;
      }
    });
  }, [pool, search, categories, origin, sortBy]);

  const visible = filteredSorted.slice(0, visibleCount);
  const hasFilters = search.length > 0 || categories.length > 0 || origin !== "all";
  const canRevealMore = visibleCount < ideasFixture.length;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Idea Generator"
        subtitle="Hundreds of scored, original concepts across every category."
        actions={
          <Button
            className="h-9 rounded-lg text-brand-foreground shadow-glow"
            style={{ background: "var(--gradient-brand)" }}
            disabled={isGenerating}
            onClick={handleGenerate}
          >
            <Wand2 className={`mr-2 size-4 ${isGenerating ? "animate-spin" : ""}`} />
            {isGenerating ? "Generating…" : "Generate ideas"}
          </Button>
        }
      />

      <CategoryChipFilter selected={categories} onChange={setCategories} />

      {/* Filter bar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-border/70 bg-surface/50 p-3 sm:flex-row sm:flex-wrap sm:items-center">
        <div className="relative min-w-[200px] flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search ideas by title or hook…"
            className="h-9 rounded-lg pl-8"
          />
        </div>

        <ToggleGroup
          type="single"
          value={origin}
          onValueChange={(v) => v && setOrigin(v as OriginFilter)}
          className="rounded-lg border border-border bg-surface/60 p-0.5"
        >
          <ToggleGroupItem value="all" className="h-8 rounded-md px-3 text-xs">All</ToggleGroupItem>
          <ToggleGroupItem value="original" className="h-8 rounded-md px-3 text-xs">Original</ToggleGroupItem>
          <ToggleGroupItem value="trend" className="h-8 rounded-md px-3 text-xs">Trend-inspired</ToggleGroupItem>
        </ToggleGroup>

        <Select value={sortBy} onValueChange={(v) => setSortBy(v as SortKey)}>
          <SelectTrigger className="h-9 w-[190px] rounded-lg bg-surface/60 text-xs">
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
            onClick={() => { setSearch(""); setCategories([]); setOrigin("all"); }}
          >
            <XIcon className="mr-1 size-3.5" /> Clear
          </Button>
        )}
      </div>

      <div className="text-xs text-muted-foreground">
        Showing {visible.length} of {filteredSorted.length} matching ideas
        {ideasFixture.length !== filteredSorted.length ? ` (${ideasFixture.length} total in pool)` : ""}
      </div>

      {isGenerating ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => <IdeaCardSkeleton key={i} />)}
        </div>
      ) : visible.length === 0 ? (
        <EmptyState
          icon={<Lightbulb className="size-6" />}
          title="No ideas match"
          description="Try clearing a filter, searching differently, or generating a fresh batch."
        />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {visible.map((idea) => <IdeaCard key={idea.id} idea={idea} />)}
          </div>
          {canRevealMore && (
            <div className="flex justify-center pt-2">
              <Button variant="outline" className="h-9 rounded-lg" onClick={handleGenerate} disabled={isGenerating}>
                Reveal more ideas
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
