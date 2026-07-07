import { useState } from "react";
import { Link } from "react-router-dom";
import type { Idea } from "@/lib/mock/types";
import { channelById, trendById } from "@/lib/mock/data";
import { GlassPanel } from "@/components/kit/glass-panel";
import { ScoreBadge, scoreTone } from "@/components/kit/score-badge";
import { CategoryIcon } from "@/components/discovery/category-icons";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { Button } from "@/components/ui/button";
import { ChevronDown, FileText, RefreshCw, BookmarkPlus, Link2, Sparkles, Quote } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const DIMENSIONS: { key: keyof Idea; label: string }[] = [
  { key: "clickPotential", label: "Click potential" },
  { key: "retentionPrediction", label: "Retention prediction" },
  { key: "monetizationValue", label: "Monetization value" },
  { key: "originality", label: "Originality" },
  { key: "audienceInterest", label: "Audience interest" },
  { key: "trendAlignment", label: "Trend alignment" },
];

const TONE_BAR: Record<string, string> = {
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-destructive",
};

export function IdeaCard({ idea }: { idea: Idea }) {
  const [open, setOpen] = useState(false);
  const channel = channelById(idea.channelId);
  const trend = trendById(idea.trendId);
  const isOriginal = !idea.trendId;

  return (
    <GlassPanel
      className={cn(
        "flex flex-col gap-3 border-l-2 p-5 transition-all duration-200 hover:shadow-soft",
        isOriginal ? "border-l-success/40 hover:border-success/50" : "border-l-brand/40 hover:border-brand/40",
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-1.5 text-[10px] uppercase tracking-wider text-muted-foreground">
            <CategoryIcon category={idea.category} className="size-3" />
            {idea.category}
            <span>· {idea.createdAt}</span>
          </div>
          <h3 className="text-sm font-semibold leading-snug">{idea.title}</h3>
        </div>
        <ScoreBadge score={idea.score} />
      </div>

      <p className="text-xs italic leading-relaxed text-foreground/80">&ldquo;{idea.hook}&rdquo;</p>
      <p className="text-xs text-muted-foreground">
        <span className="font-medium text-foreground/70">Angle — </span>{idea.angle}
      </p>

      <div className="flex flex-wrap items-center gap-2">
        {channel && (
          <span className="inline-flex items-center gap-1.5 rounded-full border border-border bg-surface/60 px-2 py-1 text-[11px]">
            <span
              className="grid size-5 place-items-center rounded-full text-[11px]"
              style={{ backgroundColor: channel.color + "33" }}
            >
              {channel.avatar}
            </span>
            {channel.name}
          </span>
        )}
        {trend ? (
          <Link
            to="/trends"
            className="inline-flex max-w-[220px] items-center gap-1.5 truncate rounded-full border border-brand/25 bg-brand/5 px-2 py-1 text-[11px] text-brand transition-colors hover:bg-brand/10"
          >
            <Link2 className="size-3 shrink-0" />
            <span className="truncate">Inspired by: {trend.topic}</span>
          </Link>
        ) : (
          <span className="inline-flex items-center gap-1.5 rounded-full border border-success/30 bg-success/10 px-2 py-1 text-[11px] font-medium text-success">
            <Sparkles className="size-3" /> Original concept
          </span>
        )}
      </div>

      <Collapsible open={open} onOpenChange={setOpen}>
        <CollapsibleTrigger asChild>
          <button
            type="button"
            className="flex w-full items-center justify-between rounded-lg border border-border/60 bg-surface/40 px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-surface/70"
          >
            Score breakdown &amp; alternates
            <ChevronDown className={cn("size-3.5 transition-transform duration-200", open && "rotate-180")} />
          </button>
        </CollapsibleTrigger>
        <CollapsibleContent className="space-y-4 pt-3">
          <div className="space-y-2.5">
            {DIMENSIONS.map((d) => {
              const value = idea[d.key] as number;
              const tone = scoreTone(value);
              return (
                <div key={String(d.key)} className="space-y-1">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-muted-foreground">{d.label}</span>
                    <span className="font-medium tabular-nums">{value}</span>
                  </div>
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                    <div className={cn("h-full rounded-full", TONE_BAR[tone])} style={{ width: `${value}%` }} />
                  </div>
                </div>
              );
            })}
          </div>

          <div>
            <div className="mb-1.5 flex items-center gap-1.5 text-[11px] font-semibold text-muted-foreground">
              <Quote className="size-3" /> Alternate hooks
            </div>
            <ul className="space-y-1.5">
              {idea.hookOptions.map((h, i) => (
                <li
                  key={i}
                  className={cn(
                    "rounded-md border px-2.5 py-1.5 text-xs leading-snug",
                    h === idea.hook
                      ? "border-brand/30 bg-brand/5 text-foreground"
                      : "border-border/60 bg-surface/40 text-muted-foreground",
                  )}
                >
                  {h}
                </li>
              ))}
            </ul>
          </div>
        </CollapsibleContent>
      </Collapsible>

      <div className="mt-auto flex items-center gap-2 pt-1">
        <Button
          size="sm"
          className="h-8 flex-1"
          onClick={() => toast.success("Script draft started", { description: idea.title })}
        >
          <FileText className="mr-1.5 size-3.5" /> Write script
        </Button>
        <Button
          size="sm" variant="outline" className="h-8"
          onClick={() => toast("Regenerating idea…", { description: idea.title })}
        >
          <RefreshCw className="size-3.5" />
        </Button>
        <Button
          size="sm" variant="ghost" className="h-8"
          onClick={() => toast.success("Saved to library", { description: idea.title })}
        >
          <BookmarkPlus className="size-3.5" />
        </Button>
      </div>
    </GlassPanel>
  );
}
