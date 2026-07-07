import { Link } from "react-router-dom";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { StatCard } from "@/components/kit/stat-card";
import { ScoreBadge } from "@/components/kit/score-badge";
import { SourceRow } from "@/components/kit/source-badge";
import { StageBadge, stageLabel } from "@/components/kit/stage-badge";
import { ProgressRing } from "@/components/kit/progress-ring";
import { Button } from "@/components/ui/button";
import {
  Radio, Film, ClipboardCheck, Flame, DollarSign, Sparkles,
  ArrowRight, ArrowUpRight, Check, Eye, CircleDot, Bot, User, Server,
} from "lucide-react";
import {
  channels, trends, projects, approvals, activity, weekPerformance,
  dashboardStats, channelById,
} from "@/lib/mock/data";
import { Line, LineChart, ResponsiveContainer, Area, AreaChart, XAxis, YAxis, Tooltip } from "recharts";
import { toast } from "sonner";

function formatCompact(n: number) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

export default function Dashboard() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Command Center"
        subtitle="Every channel, every stage, every opportunity — one operator, one pane of glass."
        actions={
          <>
            <Button variant="outline" className="h-9 rounded-lg">
              <Eye className="mr-2 size-4" /> Review queue
            </Button>
            <Button
              className="h-9 rounded-lg text-brand-foreground shadow-glow"
              style={{ background: "var(--gradient-brand)" }}
              onClick={() => toast.success("Generation queued", { description: "New video pipeline started." })}
            >
              <Sparkles className="mr-2 size-4" /> Generate video
            </Button>
          </>
        }
      />

      {/* Stat row */}
      <section className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <StatCard label="Active channels" value={String(dashboardStats.activeChannels)} delta={0} icon={<Radio className="size-4" />} spark={[3,3,4,4,5,5,5]} />
        <StatCard label="In production" value={String(dashboardStats.videosInProduction)} delta={18} icon={<Film className="size-4" />} spark={[2,3,3,4,4,5,6]} />
        <StatCard label="Pending approvals" value={String(dashboardStats.pendingApprovals)} delta={-12} icon={<ClipboardCheck className="size-4" />} spark={[6,5,5,4,4,3,4]} />
        <StatCard label="Viral opportunities" value={String(dashboardStats.trendingOpportunities)} delta={34} icon={<Flame className="size-4" />} spark={[1,2,3,3,4,4,4]} />
        <StatCard label="Est. weekly revenue" value={`$${formatCompact(dashboardStats.weeklyRevenue)}`} delta={22} icon={<DollarSign className="size-4" />} spark={weekPerformance.map(w => w.revenue)} accent="success" />
      </section>

      <div className="grid gap-4 lg:grid-cols-3">
        {/* Viral opportunities */}
        <GlassPanel className="lg:col-span-2 p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Viral opportunities</h2>
              <p className="text-xs text-muted-foreground">Trends compounding across sources in the last 24h.</p>
            </div>
            <Link to="/trends" className="text-xs text-brand hover:underline">See all →</Link>
          </div>
          <ul className="space-y-2">
            {trends.slice(0, 5).map((t) => (
              <li key={t.id} className="group grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 rounded-xl border border-transparent p-3 transition-all hover:border-border hover:bg-surface/50">
                <div className="min-w-0 space-y-1.5">
                  <div className="flex items-center gap-2">
                    <ScoreBadge score={t.viralityScore} />
                    <span className="text-[10px] uppercase tracking-wider text-muted-foreground">{t.category}</span>
                    <span className="text-[10px] text-muted-foreground">· {t.ageHours}h ago · {t.volume}</span>
                  </div>
                  <div className="truncate text-sm font-medium">{t.topic}</div>
                  <SourceRow sources={t.sources} />
                </div>
                <div className="flex items-center gap-3">
                  <div className="h-8 w-20">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={t.growth.map((v,i)=>({i,v}))}>
                        <Line type="monotone" dataKey="v" stroke="var(--brand)" strokeWidth={1.75} dot={false} />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                  <Button size="sm" variant="ghost" className="opacity-70 transition group-hover:opacity-100" onClick={() => toast.success("Turned into video draft")}>
                    Turn into video <ArrowRight className="ml-1 size-3.5" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </GlassPanel>

        {/* This week's performance */}
        <GlassPanel className="p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">This week</h2>
              <p className="text-xs text-muted-foreground">Views across every channel.</p>
            </div>
            <span className="text-xs tabular-nums text-success">▲ 24%</span>
          </div>
          <div className="h-40">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={weekPerformance}>
                <defs>
                  <linearGradient id="viewsFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--brand)" stopOpacity={0.5} />
                    <stop offset="100%" stopColor="var(--brand)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" tick={{ fontSize: 10, fill: "var(--muted-foreground)" }} axisLine={false} tickLine={false} />
                <YAxis hide />
                <Tooltip
                  contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 12, fontSize: 12 }}
                  labelStyle={{ color: "var(--muted-foreground)" }}
                  formatter={(v: number) => [formatCompact(v), "Views"]}
                />
                <Area type="monotone" dataKey="views" stroke="var(--brand)" strokeWidth={2} fill="url(#viewsFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-3 grid grid-cols-3 gap-3 text-center">
            <div className="rounded-lg border border-border/60 p-2">
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Views</div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums">4.9M</div>
            </div>
            <div className="rounded-lg border border-border/60 p-2">
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground">RPM</div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums">$2.94</div>
            </div>
            <div className="rounded-lg border border-border/60 p-2">
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground">CTR</div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums">7.2%</div>
            </div>
          </div>
        </GlassPanel>
      </div>

      {/* Pipeline at a glance */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Pipeline at a glance</h2>
            <p className="text-xs text-muted-foreground">Every video in flight, by stage.</p>
          </div>
          <Link to="/production" className="text-xs text-brand hover:underline">Open Production →</Link>
        </div>
        <div className="scrollbar-thin -mx-1 flex snap-x gap-3 overflow-x-auto pb-2">
          {projects.map((p) => {
            const ch = channelById(p.channelId);
            const stage = p.stages.find(s => s.stage === p.currentStage);
            const done = p.stages.filter(s => s.status === "done").length;
            const pct = Math.round((done / p.stages.length) * 100);
            return (
              <div key={p.id} className="min-w-[260px] snap-start rounded-xl border border-border/70 bg-surface/60 p-4 transition hover:border-brand/40">
                <div className="flex items-center gap-2">
                  <div className="grid size-8 shrink-0 place-items-center rounded-lg text-sm" style={{ backgroundColor: (ch?.color ?? "#7c5cff") + "33" }}>
                    <span>{ch?.avatar}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-xs text-muted-foreground">{ch?.name}</div>
                    <div className="truncate text-sm font-medium">{p.title}</div>
                  </div>
                  <ProgressRing value={pct} size={36} />
                </div>
                <div className="mt-3 flex items-center gap-1.5">
                  <StageBadge stage={p.currentStage} status={stage?.status ?? "running"} />
                  <span className="ml-auto text-[10px] text-muted-foreground">{p.updatedAt}</span>
                </div>
                <div className="mt-3 flex gap-0.5">
                  {p.stages.map((s, i) => (
                    <div
                      key={i}
                      title={stageLabel(s.stage)}
                      className={`h-1 flex-1 rounded-full ${
                        s.status === "done" ? "bg-success" :
                        s.status === "running" ? "bg-brand animate-pulse" :
                        s.status === "needs-approval" ? "bg-warning" :
                        s.status === "failed" ? "bg-destructive" : "bg-muted"
                      }`}
                    />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </GlassPanel>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Approvals */}
        <GlassPanel className="p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Pending approvals</h2>
              <p className="text-xs text-muted-foreground">Human-in-the-loop gate.</p>
            </div>
            <span className="rounded-full border border-warning/30 bg-warning/10 px-2 py-0.5 text-[10px] font-medium text-warning">
              {approvals.length} waiting
            </span>
          </div>
          <ul className="divide-y divide-border/60">
            {approvals.map((a) => {
              const ch = channelById(a.channelId);
              return (
                <li key={a.id} className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 py-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-muted-foreground">{ch?.name}</span>
                      <span className="rounded-md border border-border bg-surface/60 px-1.5 py-0.5 text-[10px] uppercase tracking-wider text-muted-foreground">
                        {a.type.replace("-", " ")}
                      </span>
                    </div>
                    <div className="mt-0.5 truncate text-sm font-medium">{a.projectTitle}</div>
                    <div className="text-[10px] text-muted-foreground">{a.createdAt}</div>
                  </div>
                  <div className="flex gap-1.5">
                    <Button size="sm" variant="ghost" className="h-8" onClick={() => toast("Opening review…")}>
                      Review
                    </Button>
                    <Button size="sm" className="h-8 gap-1 bg-success text-success-foreground hover:bg-success/90"
                      onClick={() => toast.success("Approved", { description: a.projectTitle })}>
                      <Check className="size-3.5" /> Approve
                    </Button>
                  </div>
                </li>
              );
            })}
          </ul>
        </GlassPanel>

        {/* Activity */}
        <GlassPanel className="p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Recent activity</h2>
              <p className="text-xs text-muted-foreground">System, AI, and you.</p>
            </div>
            <button className="text-xs text-brand hover:underline">Filter</button>
          </div>
          <ul className="relative space-y-0">
            <span className="absolute left-[15px] top-2 bottom-2 w-px bg-border" />
            {activity.map((e) => {
              const Icon = e.actor === "ai" ? Bot : e.actor === "you" ? User : Server;
              const color = e.actor === "ai" ? "text-brand bg-brand/15 ring-brand/25"
                : e.actor === "you" ? "text-success bg-success/15 ring-success/25"
                : "text-muted-foreground bg-muted/40 ring-border";
              return (
                <li key={e.id} className="relative flex gap-3 py-2.5">
                  <div className={`relative z-10 grid size-8 shrink-0 place-items-center rounded-full ring-1 ${color}`}>
                    <Icon className="size-3.5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm">{e.message}</div>
                    <div className="text-[10px] text-muted-foreground">{e.time}</div>
                  </div>
                </li>
              );
            })}
          </ul>
        </GlassPanel>
      </div>

      {/* Channel roster */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Channels</h2>
            <p className="text-xs text-muted-foreground">{channels.length} active workspaces.</p>
          </div>
          <Link to="/channels" className="text-xs text-brand hover:underline">Manage →</Link>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          {channels.map((c) => (
            <div key={c.id} className="group rounded-xl border border-border/70 bg-surface/60 p-3 transition hover:border-brand/40 hover:shadow-soft">
              <div className="flex items-center gap-2">
                <div className="grid size-9 shrink-0 place-items-center rounded-xl text-base" style={{ backgroundColor: c.color + "33" }}>
                  <span>{c.avatar}</span>
                </div>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">{c.name}</div>
                  <div className="truncate text-[10px] text-muted-foreground">{c.handle}</div>
                </div>
                <ArrowUpRight className="size-3.5 shrink-0 text-muted-foreground opacity-0 transition group-hover:opacity-100" />
              </div>
              <div className="mt-3 flex items-center justify-between text-[11px]">
                <span className="flex items-center gap-1 text-muted-foreground">
                  <CircleDot className="size-3" /> {formatCompact(c.subscribers)}
                </span>
                <span className="tabular-nums text-success">${formatCompact(c.weeklyRevenue)}/wk</span>
              </div>
            </div>
          ))}
        </div>
      </GlassPanel>
    </div>
  );
}
