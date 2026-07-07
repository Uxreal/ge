import { useMemo, useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { StatCard } from "@/components/kit/stat-card";
import { ScoreBadge } from "@/components/kit/score-badge";
import { Button } from "@/components/ui/button";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PostingHeatmap } from "@/components/analytics/posting-heatmap";
import { ChannelComparisonTable } from "@/components/analytics/channel-comparison-table";
import {
  Eye, Clock, Target, CheckCircle2, UserPlus, DollarSign, Download,
} from "lucide-react";
import {
  Area, AreaChart, Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { channels, channelById } from "@/lib/mock/data";
import {
  dailyMetricsFor, previousDailyMetricsFor, aggregateMetrics, channelKpis, channelKpiById,
  topHooks, RANGE_DAYS, RANGE_KEYS, type RangeKey,
} from "@/lib/mock/analytics";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

function formatCompact(n: number) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

function deltaPercent(current: number, previous: number): number | undefined {
  if (!previous) return undefined;
  return Math.round(((current - previous) / previous) * 100);
}

export default function Analytics() {
  const [channelId, setChannelId] = useState<string | undefined>(undefined);
  const [range, setRange] = useState<RangeKey>("30d");
  const [metric, setMetric] = useState<"views" | "revenue">("views");

  const points = useMemo(() => dailyMetricsFor(channelId, range), [channelId, range]);
  const prevPoints = useMemo(() => previousDailyMetricsFor(channelId, range), [channelId, range]);
  const agg = useMemo(() => aggregateMetrics(points), [points]);
  const prevAgg = useMemo(() => aggregateMetrics(prevPoints), [prevPoints]);

  const channelStats = useMemo(() => {
    if (channelId) {
      const k = channelKpiById(channelId);
      if (!k) return { retentionPercent: 0, completionRate: 0, followersGained: 0 };
      return {
        retentionPercent: k.retentionPercent,
        completionRate: k.completionRate,
        followersGained: Math.round((k.followersGained * RANGE_DAYS[range]) / 90),
      };
    }
    const totalViews = channelKpis.reduce((s, k) => s + k.views, 0);
    const retentionPercent = Math.round(
      channelKpis.reduce((s, k) => s + k.retentionPercent * k.views, 0) / totalViews,
    );
    const completionRate = Math.round(
      channelKpis.reduce((s, k) => s + k.completionRate * k.views, 0) / totalViews,
    );
    const followersGained = Math.round(
      (channelKpis.reduce((s, k) => s + k.followersGained, 0) * RANGE_DAYS[range]) / 90,
    );
    return { retentionPercent, completionRate, followersGained };
  }, [channelId, range]);

  const filteredHooks = useMemo(
    () => (channelId ? topHooks.filter((h) => h.channelId === channelId) : topHooks),
    [channelId],
  );

  const rankedHooks = useMemo(
    () => [...filteredHooks].sort((a, b) => b.retentionPercent - a.retentionPercent),
    [filteredHooks],
  );

  const hookChartData = useMemo(
    () => rankedHooks.slice(0, 8).map((h) => ({
      name: h.hook.length > 26 ? `${h.hook.slice(0, 26)}…` : h.hook,
      hook: h.hook,
      retentionPercent: h.retentionPercent,
    })),
    [rankedHooks],
  );

  const tickInterval = Math.max(0, Math.floor(points.length / 6) - 1);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics"
        subtitle="Views, retention, RPM, and the hooks and thumbnails driving them."
        actions={
          <Button
            variant="outline"
            className="h-9 rounded-lg"
            onClick={() => toast.success("Report exported", { description: "Analytics summary saved as PDF." })}
          >
            <Download className="mr-2 size-4" /> Export report
          </Button>
        }
      />

      {/* Filter row */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Select value={channelId ?? "all"} onValueChange={(v) => setChannelId(v === "all" ? undefined : v)}>
          <SelectTrigger className="h-9 w-full rounded-lg sm:w-60">
            <SelectValue placeholder="All channels" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All channels</SelectItem>
            {channels.map((c) => (
              <SelectItem key={c.id} value={c.id}>{c.avatar} {c.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex shrink-0 gap-1.5">
          {RANGE_KEYS.map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRange(r)}
              className={cn(
                "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors duration-150",
                range === r
                  ? "border-brand/40 bg-brand/10 text-brand"
                  : "border-border bg-surface/60 text-muted-foreground hover:border-brand/25 hover:text-foreground",
              )}
            >
              {r}
            </button>
          ))}
        </div>
      </div>

      {/* KPI header */}
      <section className="grid grid-cols-2 gap-4 lg:grid-cols-3 xl:grid-cols-6">
        <StatCard
          label="Views" value={formatCompact(agg.views)} delta={deltaPercent(agg.views, prevAgg.views)}
          icon={<Eye className="size-4" />} spark={points.slice(-14).map((p) => p.views)}
        />
        <StatCard
          label="Watch time" value={`${formatCompact(agg.watchTimeHours)}h`}
          delta={deltaPercent(agg.watchTimeHours, prevAgg.watchTimeHours)}
          icon={<Clock className="size-4" />} spark={points.slice(-14).map((p) => p.watchTimeHours)}
        />
        <StatCard label="Avg retention" value={`${channelStats.retentionPercent}%`} icon={<Target className="size-4" />} />
        <StatCard label="Completion rate" value={`${channelStats.completionRate}%`} icon={<CheckCircle2 className="size-4" />} />
        <StatCard label="Followers gained" value={`+${formatCompact(channelStats.followersGained)}`} icon={<UserPlus className="size-4" />} />
        <StatCard
          label="Est. revenue" value={`$${formatCompact(agg.revenue)}`} delta={deltaPercent(agg.revenue, prevAgg.revenue)}
          icon={<DollarSign className="size-4" />} spark={points.slice(-14).map((p) => p.revenue)} accent="success"
        />
      </section>

      <div className="grid gap-4 lg:grid-cols-3">
        {/* Views / revenue over time */}
        <GlassPanel className="lg:col-span-2 p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Performance over time</h2>
              <p className="text-xs text-muted-foreground">
                {channelId ? channelById(channelId)?.name : "All channels"} · trailing {RANGE_DAYS[range]} days.
              </p>
            </div>
            <Tabs value={metric} onValueChange={(v) => setMetric(v as "views" | "revenue")}>
              <TabsList className="h-8">
                <TabsTrigger value="views" className="text-xs">Views</TabsTrigger>
                <TabsTrigger value="revenue" className="text-xs">Revenue</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={points}>
                <defs>
                  <linearGradient id="metricFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={metric === "views" ? "var(--brand)" : "var(--success)"} stopOpacity={0.45} />
                    <stop offset="100%" stopColor={metric === "views" ? "var(--brand)" : "var(--success)"} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis
                  dataKey="label" interval={tickInterval}
                  tick={{ fontSize: 10, fill: "var(--muted-foreground)" }} axisLine={false} tickLine={false}
                />
                <YAxis hide />
                <Tooltip
                  contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 12, fontSize: 12 }}
                  labelStyle={{ color: "var(--muted-foreground)" }}
                  formatter={(v: number) => [metric === "views" ? formatCompact(v) : `$${formatCompact(v)}`, metric === "views" ? "Views" : "Revenue"]}
                />
                <Area
                  type="monotone" dataKey={metric}
                  stroke={metric === "views" ? "var(--brand)" : "var(--success)"}
                  strokeWidth={2} fill="url(#metricFill)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </GlassPanel>

        {/* Top hooks by retention */}
        <GlassPanel className="p-5">
          <div className="mb-4">
            <h2 className="text-sm font-semibold">Top hooks by retention</h2>
            <p className="text-xs text-muted-foreground">Highest-retaining openers right now.</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={hookChartData} layout="vertical" margin={{ left: 8, right: 12 }}>
                <XAxis type="number" domain={[0, 100]} hide />
                <YAxis
                  type="category" dataKey="name" width={132}
                  tick={{ fontSize: 10, fill: "var(--muted-foreground)" }} axisLine={false} tickLine={false}
                />
                <Tooltip
                  cursor={{ fill: "var(--surface)" }}
                  contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 12, fontSize: 12 }}
                  labelStyle={{ color: "var(--muted-foreground)" }}
                  formatter={(v: number) => [`${v}%`, "Retention"]}
                  labelFormatter={(_l, payload) => payload?.[0]?.payload?.hook ?? ""}
                />
                <Bar dataKey="retentionPercent" fill="var(--brand)" radius={[0, 4, 4, 0]} barSize={12} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </GlassPanel>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Best posting times */}
        <GlassPanel className="p-5">
          <div className="mb-4">
            <h2 className="text-sm font-semibold">Best posting times</h2>
            <p className="text-xs text-muted-foreground">Engagement score by day and hour, across all channels.</p>
          </div>
          <PostingHeatmap />
        </GlassPanel>

        {/* Ranked top hooks list */}
        <GlassPanel className="p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Top hooks</h2>
              <p className="text-xs text-muted-foreground">Ranked by audience retention.</p>
            </div>
          </div>
          <ul className="space-y-1">
            {rankedHooks.map((h, i) => {
              const ch = channelById(h.channelId);
              return (
                <li key={h.id} className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-xl border border-transparent p-2.5 transition-all hover:border-border hover:bg-surface/50">
                  <span className="w-4 text-center text-xs font-medium text-muted-foreground tabular-nums">{i + 1}</span>
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium">{h.hook}</div>
                    <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground">
                      <span>{ch?.avatar}</span>
                      <span className="truncate">{ch?.name}</span>
                      <span>· {formatCompact(h.views)} views</span>
                    </div>
                  </div>
                  <ScoreBadge score={h.retentionPercent} label="retention" />
                </li>
              );
            })}
          </ul>
        </GlassPanel>
      </div>

      {/* Channel comparison */}
      <GlassPanel className="p-5">
        <div className="mb-4">
          <h2 className="text-sm font-semibold">Channel comparison</h2>
          <p className="text-xs text-muted-foreground">Click a column header to sort.</p>
        </div>
        <ChannelComparisonTable />
      </GlassPanel>
    </div>
  );
}
