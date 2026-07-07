import { useState } from "react";
import type { Channel, Platform } from "@/lib/mock/types";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { StatCard } from "@/components/kit/stat-card";
import { SourceBadge } from "@/components/kit/source-badge";
import { Slider } from "@/components/ui/slider";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { channelExtraById } from "@/lib/mock/channels-extra";
import { CircleDot, DollarSign, Eye, Save } from "lucide-react";
import { toast } from "sonner";

function formatCompact(n: number) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

function VoiceSlider({
  left, right, value, onChange,
}: { left: string; right: string; value: number; onChange: (v: number) => void }) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-[11px] text-muted-foreground">
        <span>{left}</span>
        <span className="tabular-nums text-foreground/70">{value}</span>
        <span>{right}</span>
      </div>
      <Slider value={[value]} max={100} step={1} onValueChange={([v]) => onChange(v)} />
    </div>
  );
}

function ChannelDetailBody({ channel }: { channel: Channel }) {
  const extra = channelExtraById(channel.id);
  const [formalCasual, setFormalCasual] = useState(extra?.brandVoice.formalCasual ?? 50);
  const [seriousPlayful, setSeriousPlayful] = useState(extra?.brandVoice.seriousPlayful ?? 50);
  const [conciseDetailed, setConciseDetailed] = useState(extra?.brandVoice.conciseDetailed ?? 50);
  const [description, setDescription] = useState(extra?.brandVoice.description ?? "");

  const platformEntries = (Object.entries(extra?.platformDetails ?? {}) as [Platform, { handle: string; followers: number }][]);
  const perf = extra?.performance;

  return (
    <div className="space-y-6 pb-4">
      <SheetHeader>
        <div className="flex items-center gap-3">
          <div className="grid size-11 shrink-0 place-items-center rounded-xl text-xl" style={{ backgroundColor: channel.color + "33" }}>
            <span>{channel.avatar}</span>
          </div>
          <div className="min-w-0 text-left">
            <SheetTitle className="text-left leading-snug">{channel.name}</SheetTitle>
            <SheetDescription className="text-left">{channel.handle} · {channel.category}</SheetDescription>
          </div>
        </div>
      </SheetHeader>

      {/* Platforms connected */}
      <div>
        <div className="mb-2 text-xs font-semibold text-muted-foreground">Platforms connected</div>
        <div className="space-y-2">
          {platformEntries.map(([platform, detail]) => (
            <div key={platform} className="flex items-center justify-between rounded-lg border border-border/60 bg-surface/50 p-2.5">
              <SourceBadge source={platform} />
              <div className="text-right">
                <div className="text-xs font-medium">{detail.handle}</div>
                <div className="text-[10px] text-muted-foreground">{formatCompact(detail.followers)} followers</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent performance */}
      <div>
        <div className="mb-2 text-xs font-semibold text-muted-foreground">Recent performance</div>
        <div className="grid grid-cols-2 gap-3">
          <StatCard label="Subscribers" value={formatCompact(channel.subscribers)} icon={<CircleDot className="size-4" />} />
          <StatCard
            label="Weekly revenue"
            value={`$${formatCompact(channel.weeklyRevenue)}`}
            icon={<DollarSign className="size-4" />}
            accent="success"
          />
        </div>
        {perf && (
          <>
            <div className="mt-3">
              <StatCard label="Views this week" value={formatCompact(perf.viewsThisWeek)} icon={<Eye className="size-4" />} spark={perf.viewsSpark} />
            </div>
            <div className="mt-3 grid grid-cols-3 gap-3 text-center">
              <div className="rounded-lg border border-border/60 p-2">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">CTR</div>
                <div className="mt-0.5 text-sm font-semibold tabular-nums">{perf.ctr}%</div>
              </div>
              <div className="rounded-lg border border-border/60 p-2">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Retention</div>
                <div className="mt-0.5 text-sm font-semibold tabular-nums">{perf.avgRetention}%</div>
              </div>
              <div className="rounded-lg border border-border/60 p-2">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">RPM</div>
                <div className="mt-0.5 text-sm font-semibold tabular-nums">${perf.rpm.toFixed(2)}</div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Brand voice editor */}
      <div className="space-y-4 rounded-xl border border-brand/20 bg-brand/5 p-4">
        <div className="text-xs font-semibold text-brand">Brand voice</div>
        <VoiceSlider left="Formal" right="Casual" value={formalCasual} onChange={setFormalCasual} />
        <VoiceSlider left="Serious" right="Playful" value={seriousPlayful} onChange={setSeriousPlayful} />
        <VoiceSlider left="Concise" right="Detailed" value={conciseDetailed} onChange={setConciseDetailed} />
        <div className="space-y-1.5">
          <Label className="text-xs text-muted-foreground">Tone description</Label>
          <Textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="min-h-[100px] resize-none bg-surface/60 text-sm"
          />
        </div>
        <Button
          className="w-full text-brand-foreground shadow-glow"
          style={{ background: "var(--gradient-brand)" }}
          onClick={() => toast.success("Brand voice saved", { description: `Updated tone profile for ${channel.name}.` })}
        >
          <Save className="mr-2 size-4" /> Save brand voice
        </Button>
      </div>
    </div>
  );
}

export function ChannelDetailSheet({
  channel, onOpenChange,
}: { channel: Channel | null; onOpenChange: (open: boolean) => void }) {
  return (
    <Sheet open={!!channel} onOpenChange={onOpenChange}>
      <SheetContent className="scrollbar-thin w-full overflow-y-auto sm:max-w-lg">
        {channel && <ChannelDetailBody key={channel.id} channel={channel} />}
      </SheetContent>
    </Sheet>
  );
}
