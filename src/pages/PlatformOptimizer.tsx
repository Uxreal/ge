import { useEffect, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { EmptyState } from "@/components/kit/empty-state";
import { SourceBadge } from "@/components/kit/source-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { TagList } from "@/components/platform-optimizer/tag-list";
import { FieldBlock } from "@/components/platform-optimizer/field-block";
import { CopyButton } from "@/components/platform-optimizer/copy-button";
import { ComparisonSummary } from "@/components/platform-optimizer/comparison-summary";
import { Wand2, RefreshCw, Image as ImageIcon } from "lucide-react";
import { channelById } from "@/lib/mock/data";
import {
  platformOptimizerProjects, platformFixtureByProjectId,
  type YoutubeMeta, type TiktokMeta, type InstagramMeta,
} from "@/lib/mock/platform";
import { toast } from "sonner";

export default function PlatformOptimizer() {
  const [selectedId, setSelectedId] = useState(platformOptimizerProjects[0]?.id);
  const fixture = platformFixtureByProjectId(selectedId);

  const [youtube, setYoutube] = useState<YoutubeMeta | undefined>(fixture?.youtube);
  const [tiktok, setTiktok] = useState<TiktokMeta | undefined>(fixture?.tiktok);
  const [instagram, setInstagram] = useState<InstagramMeta | undefined>(fixture?.instagram);

  useEffect(() => {
    const next = platformFixtureByProjectId(selectedId);
    setYoutube(next?.youtube);
    setTiktok(next?.tiktok);
    setInstagram(next?.instagram);
  }, [selectedId]);

  if (!fixture || !youtube || !tiktok || !instagram) {
    return (
      <div className="space-y-6">
        <PageHeader title="Platform Optimizer" subtitle="Distinct titles, captions, hashtags, and covers tuned per platform — never a copy-paste post." />
        <EmptyState icon={<Wand2 className="size-6" />} description="No projects with generated platform metadata yet." />
      </div>
    );
  }

  const regenerate = (platform: "YouTube" | "TikTok" | "Instagram") => {
    const original = platformFixtureByProjectId(selectedId);
    if (!original) return;
    if (platform === "YouTube") setYoutube(original.youtube);
    if (platform === "TikTok") setTiktok(original.tiktok);
    if (platform === "Instagram") setInstagram(original.instagram);
    toast.success(`Regenerated for ${platform}`, { description: "New title, copy, and tags generated for this platform only." });
  };

  const comparisonRows = [
    { platform: "youtube" as const, primaryLabel: "Title", primaryLength: youtube.title.length, tagLabel: "Tags", tagCount: youtube.tags.length },
    { platform: "tiktok" as const, primaryLabel: "Caption", primaryLength: tiktok.caption.length, tagLabel: "Hashtags", tagCount: tiktok.hashtags.length },
    { platform: "instagram" as const, primaryLabel: "Caption", primaryLength: instagram.caption.length, tagLabel: "Hashtags", tagCount: instagram.hashtags.length },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Platform Optimizer"
        subtitle="Distinct titles, captions, hashtags, and covers tuned per platform — never a copy-paste post."
      />

      {/* Project picker */}
      <GlassPanel className="p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold">Select a project</h2>
          <span className="text-xs text-muted-foreground">{platformOptimizerProjects.length} with generated metadata</span>
        </div>
        <div className="scrollbar-thin -mx-1 flex snap-x gap-2 overflow-x-auto pb-1">
          {platformOptimizerProjects.map((p) => {
            const ch = channelById(p.channelId);
            const active = p.id === selectedId;
            return (
              <button
                key={p.id}
                onClick={() => setSelectedId(p.id)}
                className={`flex min-w-[220px] snap-start items-center gap-2 rounded-xl border px-3 py-2.5 text-left transition-all ${
                  active ? "border-brand/50 bg-brand/10 shadow-glow" : "border-border/70 bg-surface/60 hover:border-brand/40"
                }`}
              >
                <div
                  className="grid size-8 shrink-0 place-items-center rounded-lg text-sm"
                  style={{ backgroundColor: (ch?.color ?? "#7c5cff") + "33" }}
                >
                  <span>{ch?.avatar}</span>
                </div>
                <div className="min-w-0">
                  <div className="truncate text-xs text-muted-foreground">{ch?.name}</div>
                  <div className="truncate text-sm font-medium">{p.title}</div>
                </div>
              </button>
            );
          })}
        </div>
      </GlassPanel>

      <ComparisonSummary rows={comparisonRows} />

      <GlassPanel className="p-5">
        <Tabs defaultValue="youtube" key={selectedId} className="w-full">
          <TabsList className="h-10 gap-1 bg-surface/60 p-1">
            <TabsTrigger value="youtube" className="gap-1.5 px-3">
              <SourceBadge source="youtube" compact /> YouTube
            </TabsTrigger>
            <TabsTrigger value="tiktok" className="gap-1.5 px-3">
              <SourceBadge source="tiktok" compact /> TikTok
            </TabsTrigger>
            <TabsTrigger value="instagram" className="gap-1.5 px-3">
              <SourceBadge source="instagram" compact /> Instagram
            </TabsTrigger>
          </TabsList>

          {/* YouTube */}
          <TabsContent value="youtube" className="mt-5 space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground">Long-form metadata optimized for search and click-through.</p>
              <Button size="sm" variant="outline" className="h-8 gap-1.5" onClick={() => regenerate("YouTube")}>
                <RefreshCw className="size-3.5" /> Regenerate for this platform
              </Button>
            </div>
            <FieldBlock label="Title" getCopyText={() => youtube.title}>
              <Input value={youtube.title} onChange={(e) => setYoutube({ ...youtube, title: e.target.value })} className="rounded-lg" />
            </FieldBlock>
            <FieldBlock label="Description" getCopyText={() => youtube.description}>
              <Textarea value={youtube.description} onChange={(e) => setYoutube({ ...youtube, description: e.target.value })} className="min-h-[140px] rounded-lg" />
            </FieldBlock>
            <FieldBlock
              label="Tags"
              hint="Comma-separated — edit freely."
              getCopyText={() => youtube.tags.join(", ")}
            >
              <Textarea
                value={youtube.tags.join(", ")}
                onChange={(e) => setYoutube({ ...youtube, tags: e.target.value.split(",").map((t) => t.trim()).filter(Boolean) })}
                className="min-h-[60px] rounded-lg"
              />
              <TagList tags={youtube.tags} />
            </FieldBlock>
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldBlock label="Primary keyword" getCopyText={() => youtube.primaryKeyword}>
                <Input value={youtube.primaryKeyword} onChange={(e) => setYoutube({ ...youtube, primaryKeyword: e.target.value })} className="rounded-lg" />
              </FieldBlock>
              <FieldBlock label="Thumbnail concept" getCopyText={() => youtube.thumbnailConcept}>
                <div className="flex items-start gap-2 rounded-lg border border-border bg-surface/60 p-2.5 text-xs text-foreground/80">
                  <ImageIcon className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
                  {youtube.thumbnailConcept}
                </div>
              </FieldBlock>
            </div>
          </TabsContent>

          {/* TikTok */}
          <TabsContent value="tiktok" className="mt-5 space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground">Short, hook-first copy tuned for the For You page.</p>
              <Button size="sm" variant="outline" className="h-8 gap-1.5" onClick={() => regenerate("TikTok")}>
                <RefreshCw className="size-3.5" /> Regenerate for this platform
              </Button>
            </div>
            <FieldBlock label="Caption" getCopyText={() => tiktok.caption}>
              <Textarea value={tiktok.caption} onChange={(e) => setTiktok({ ...tiktok, caption: e.target.value })} className="min-h-[80px] rounded-lg" />
            </FieldBlock>
            <FieldBlock
              label="Hashtags"
              hint="Comma-separated — edit freely."
              getCopyText={() => tiktok.hashtags.join(" ")}
            >
              <Textarea
                value={tiktok.hashtags.join(", ")}
                onChange={(e) => setTiktok({ ...tiktok, hashtags: e.target.value.split(",").map((t) => t.trim()).filter(Boolean) })}
                className="min-h-[50px] rounded-lg"
              />
              <TagList tags={tiktok.hashtags} />
            </FieldBlock>
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldBlock label="Hook-optimization note" getCopyText={() => tiktok.hookNote}>
                <div className="rounded-lg border border-border bg-surface/60 p-2.5 text-xs text-foreground/80">{tiktok.hookNote}</div>
              </FieldBlock>
              <FieldBlock label="Trending audio suggestion" getCopyText={() => tiktok.trendingAudio}>
                <Input value={tiktok.trendingAudio} onChange={(e) => setTiktok({ ...tiktok, trendingAudio: e.target.value })} className="rounded-lg" />
              </FieldBlock>
            </div>
          </TabsContent>

          {/* Instagram */}
          <TabsContent value="instagram" className="mt-5 space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground">Reels-native copy tuned for saves and shares.</p>
              <Button size="sm" variant="outline" className="h-8 gap-1.5" onClick={() => regenerate("Instagram")}>
                <RefreshCw className="size-3.5" /> Regenerate for this platform
              </Button>
            </div>
            <FieldBlock label="Caption" getCopyText={() => instagram.caption}>
              <Textarea value={instagram.caption} onChange={(e) => setInstagram({ ...instagram, caption: e.target.value })} className="min-h-[100px] rounded-lg" />
            </FieldBlock>
            <FieldBlock
              label="Hashtags"
              hint="Comma-separated — edit freely."
              getCopyText={() => instagram.hashtags.join(" ")}
            >
              <Textarea
                value={instagram.hashtags.join(", ")}
                onChange={(e) => setInstagram({ ...instagram, hashtags: e.target.value.split(",").map((t) => t.trim()).filter(Boolean) })}
                className="min-h-[50px] rounded-lg"
              />
              <TagList tags={instagram.hashtags} />
            </FieldBlock>
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldBlock label="Cover image concept" getCopyText={() => instagram.coverConcept}>
                <div className="flex items-start gap-2 rounded-lg border border-border bg-surface/60 p-2.5 text-xs text-foreground/80">
                  <ImageIcon className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
                  {instagram.coverConcept}
                </div>
              </FieldBlock>
              <FieldBlock label="Audience-optimization note" getCopyText={() => instagram.audienceNote}>
                <div className="rounded-lg border border-border bg-surface/60 p-2.5 text-xs text-foreground/80">{instagram.audienceNote}</div>
              </FieldBlock>
            </div>
          </TabsContent>
        </Tabs>
      </GlassPanel>

      <div className="flex justify-end">
        <CopyButton
          label="Copy all three (for review)"
          getText={() =>
            [
              `YOUTUBE\nTitle: ${youtube.title}\nDescription: ${youtube.description}\nTags: ${youtube.tags.join(", ")}\nKeyword: ${youtube.primaryKeyword}`,
              `TIKTOK\nCaption: ${tiktok.caption}\nHashtags: ${tiktok.hashtags.join(" ")}\nAudio: ${tiktok.trendingAudio}`,
              `INSTAGRAM\nCaption: ${instagram.caption}\nHashtags: ${instagram.hashtags.join(" ")}`,
            ].join("\n\n")
          }
        />
      </div>
    </div>
  );
}
