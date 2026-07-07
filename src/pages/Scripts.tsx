import { useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { ScoreBadge, scoreTone } from "@/components/kit/score-badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { channelById } from "@/lib/mock/data";
import { scripts, scriptById, DURATION_ORDER } from "@/lib/mock/scripts";
import type { ScriptDurationLabel } from "@/lib/mock/scripts";
import { RefreshCw, ShieldCheck, Check, Megaphone, Wand2 } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

export default function Scripts() {
  const [selectedId, setSelectedId] = useState(scripts[0].id);
  const [duration, setDuration] = useState<ScriptDurationLabel>("30s");

  const selected = scriptById(selectedId) ?? scripts[0];
  const channel = channelById(selected.channelId);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Script Writer"
        subtitle="Hook-first, retention-optimized scripts across every video length."
        actions={
          <>
            <Button
              variant="outline"
              className="h-9 rounded-lg"
              onClick={() => toast("Sent to Fact Check", { description: selected.ideaOrProjectTitle })}
            >
              <ShieldCheck className="mr-2 size-4" /> Send to Fact Check
            </Button>
            <Button
              className="h-9 gap-1.5 rounded-lg bg-success text-success-foreground hover:bg-success/90"
              onClick={() => toast.success("Script approved", { description: selected.ideaOrProjectTitle })}
            >
              <Check className="size-4" /> Approve script
            </Button>
          </>
        }
      />

      {/* Project / idea picker */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Choose a script</h2>
            <p className="text-xs text-muted-foreground">{scripts.length} scripts in the writers' room.</p>
          </div>
        </div>
        <div className="scrollbar-thin -mx-1 flex snap-x gap-3 overflow-x-auto pb-2">
          {scripts.map((s) => {
            const ch = channelById(s.channelId);
            const active = s.id === selectedId;
            return (
              <button
                key={s.id}
                onClick={() => setSelectedId(s.id)}
                className={cn(
                  "min-w-[220px] shrink-0 snap-start rounded-xl border p-4 text-left transition-all duration-150",
                  active
                    ? "border-brand/60 bg-brand/10 shadow-glow"
                    : "border-border/70 bg-surface/60 hover:border-brand/40",
                )}
              >
                <div className="flex items-center gap-2">
                  <div
                    className="grid size-8 shrink-0 place-items-center rounded-lg text-sm"
                    style={{ backgroundColor: (ch?.color ?? "#7c5cff") + "33" }}
                  >
                    <span>{ch?.avatar ?? "✍️"}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-[10px] text-muted-foreground">{ch?.name ?? "Unassigned"}</div>
                    <div className="truncate text-sm font-medium">{s.ideaOrProjectTitle}</div>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-1.5">
                  <ScoreBadge score={s.originalityScore} label="original" />
                  <ScoreBadge score={s.pacingScore} label="pacing" />
                </div>
              </button>
            );
          })}
        </div>
      </GlassPanel>

      {/* Selected script — duration tabs */}
      <GlassPanel className="p-5">
        <Tabs value={duration} onValueChange={(v) => setDuration(v as ScriptDurationLabel)}>
          <TabsList>
            {DURATION_ORDER.map((d) => (
              <TabsTrigger key={d} value={d}>{d}</TabsTrigger>
            ))}
          </TabsList>

          {DURATION_ORDER.map((d) => {
            const v = selected.variants.find((vv) => vv.durationLabel === d);
            if (!v) return null;
            return (
              <TabsContent key={d} value={d} className="space-y-5 pt-4">
                {/* Hook */}
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Hook · first 1-3 seconds
                    </span>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 gap-1 text-xs"
                      onClick={() => toast("Regenerating hook…", { description: "AI is drafting 3 new openings." })}
                    >
                      <RefreshCw className="size-3.5" /> Regenerate hook
                    </Button>
                  </div>
                  <div className="rounded-xl border border-brand/25 bg-brand/5 p-5">
                    <p className="text-xl font-semibold leading-snug tracking-tight sm:text-2xl">{v.hook}</p>
                  </div>
                </div>

                {/* Body */}
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Full script · {v.wordCount} words
                    </span>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 gap-1 text-xs"
                      onClick={() => toast("Regenerating script…", { description: `New ${d} draft on the way.` })}
                    >
                      <RefreshCw className="size-3.5" /> Regenerate script
                    </Button>
                  </div>
                  <div className="scrollbar-thin max-h-[340px] overflow-y-auto rounded-xl border border-border bg-surface/50 p-4">
                    {v.body.split("\n\n").map((para, i) => (
                      <p key={i} className={cn("text-sm leading-relaxed text-foreground/90", i > 0 && "mt-3")}>
                        {para}
                      </p>
                    ))}
                  </div>
                </div>

                {/* Retention tags + CTA */}
                <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto]">
                  <div>
                    <span className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Retention techniques
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {v.retentionTechniques.map((t) => (
                        <span
                          key={t}
                          className="rounded-full border border-border bg-surface/70 px-2.5 py-0.5 text-[10px] font-medium text-muted-foreground"
                        >
                          {t}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="sm:w-72">
                    <span className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Call to action
                    </span>
                    <div className="flex items-center gap-2 rounded-lg border border-brand/30 bg-brand/5 px-3 py-2">
                      <Megaphone className="size-4 shrink-0 text-brand" />
                      <span className="text-sm font-medium">{v.cta}</span>
                    </div>
                  </div>
                </div>
              </TabsContent>
            );
          })}
        </Tabs>
      </GlassPanel>

      {/* Scores + Test hooks */}
      <div className="grid gap-4 lg:grid-cols-3">
        <GlassPanel className="p-5">
          <h2 className="mb-4 text-sm font-semibold">Script scores</h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between rounded-lg border border-border/60 p-3">
              <span className="text-xs text-muted-foreground">Originality</span>
              <ScoreBadge score={selected.originalityScore} />
            </div>
            <div className="flex items-center justify-between rounded-lg border border-border/60 p-3">
              <span className="text-xs text-muted-foreground">Pacing</span>
              <ScoreBadge score={selected.pacingScore} />
            </div>
            {channel ? (
              <div className="flex items-center gap-2 rounded-lg border border-border/60 p-3">
                <div
                  className="grid size-7 shrink-0 place-items-center rounded-lg text-xs"
                  style={{ backgroundColor: channel.color + "33" }}
                >
                  {channel.avatar}
                </div>
                <div className="min-w-0">
                  <div className="truncate text-xs font-medium">{channel.name}</div>
                  <div className="truncate text-[10px] text-muted-foreground">{channel.handle}</div>
                </div>
              </div>
            ) : (
              <div className="rounded-lg border border-dashed border-border/60 p-3 text-center text-[10px] text-muted-foreground">
                Not yet assigned to a channel
              </div>
            )}
          </div>
        </GlassPanel>

        <GlassPanel className="p-5 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Test hooks</h2>
              <p className="text-xs text-muted-foreground">Alternate openings, scored for predicted hook retention.</p>
            </div>
            <Wand2 className="size-4 text-muted-foreground" />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            {selected.altHooks.map((h, i) => {
              const tone = scoreTone(h.score);
              return (
                <div key={i} className="rounded-xl border border-border/70 bg-surface/60 p-3">
                  <p className="text-sm font-medium leading-snug">{h.text}</p>
                  <div className="mt-3 flex items-center gap-2">
                    <div className="h-1.5 flex-1 rounded-full bg-muted">
                      <div
                        className={cn(
                          "h-full rounded-full",
                          tone === "success" ? "bg-success" : tone === "warning" ? "bg-warning" : "bg-destructive",
                        )}
                        style={{ width: `${h.score}%` }}
                      />
                    </div>
                    <span className="text-xs tabular-nums text-muted-foreground">{h.score}</span>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="mt-2 h-7 w-full text-xs"
                    onClick={() => toast.success("Hook swapped", { description: h.text })}
                  >
                    Use this hook
                  </Button>
                </div>
              );
            })}
          </div>
        </GlassPanel>
      </div>
    </div>
  );
}
