import { useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { ScoreBadge, scoreTone } from "@/components/kit/score-badge";
import { ProgressRing } from "@/components/kit/progress-ring";
import { Button } from "@/components/ui/button";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { channelById } from "@/lib/mock/data";
import { factCheckReports, factCheckByProjectId, CLAIM_STATUSES } from "@/lib/mock/factchecks";
import type { ClaimStatus } from "@/lib/mock/factchecks";
import {
  Check, RefreshCw, ShieldCheck, ShieldAlert, ShieldQuestion, Clock, ExternalLink,
} from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

const STATUS_META: Record<ClaimStatus, { label: string; icon: React.ComponentType<{ className?: string }>; classes: string }> = {
  verified: { label: "Verified", icon: ShieldCheck, classes: "border-success/30 bg-success/10 text-success" },
  flagged: { label: "Flagged", icon: ShieldAlert, classes: "border-warning/30 bg-warning/10 text-warning" },
  outdated: { label: "Outdated", icon: Clock, classes: "border-destructive/30 bg-destructive/10 text-destructive" },
  unverified: { label: "Unverified", icon: ShieldQuestion, classes: "border-border bg-muted/30 text-muted-foreground" },
};

function ClaimStatusBadge({ status }: { status: ClaimStatus }) {
  const meta = STATUS_META[status];
  const Icon = meta.icon;
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[10px] font-medium", meta.classes)}>
      <Icon className="size-3" /> {meta.label}
    </span>
  );
}

export default function FactCheck() {
  const [selectedProjectId, setSelectedProjectId] = useState(factCheckReports[0].projectId);
  const [filter, setFilter] = useState<ClaimStatus | "all">("all");

  const report = factCheckByProjectId(selectedProjectId) ?? factCheckReports[0];
  const channel = channelById(report.channelId);
  const tone = scoreTone(report.overallConfidence);
  const toneVar = tone === "success" ? "var(--success)" : tone === "warning" ? "var(--warning)" : "var(--destructive)";

  const counts = CLAIM_STATUSES.reduce<Record<ClaimStatus, number>>((acc, s) => {
    acc[s] = report.claims.filter((c) => c.status === s).length;
    return acc;
  }, { verified: 0, flagged: 0, outdated: 0, unverified: 0 });

  const visibleClaims = filter === "all" ? report.claims : report.claims.filter((c) => c.status === filter);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Fact Verification"
        subtitle="Claim-by-claim confidence scoring before a single frame renders."
        actions={
          <Button
            className="h-9 gap-1.5 rounded-lg bg-success text-success-foreground hover:bg-success/90"
            onClick={() => toast.success("Fact-check approved", { description: report.projectTitle })}
          >
            <Check className="size-4" /> Approve fact-check
          </Button>
        }
      />

      {/* Project picker */}
      <GlassPanel className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Choose a report</h2>
            <p className="text-xs text-muted-foreground">{factCheckReports.length} projects awaiting or under verification.</p>
          </div>
        </div>
        <div className="scrollbar-thin -mx-1 flex snap-x gap-3 overflow-x-auto pb-2">
          {factCheckReports.map((r) => {
            const ch = channelById(r.channelId);
            const active = r.projectId === selectedProjectId;
            return (
              <button
                key={r.id}
                onClick={() => setSelectedProjectId(r.projectId)}
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
                    <span>{ch?.avatar ?? "🔍"}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-[10px] text-muted-foreground">{ch?.name ?? "Unassigned"}</div>
                    <div className="truncate text-sm font-medium">{r.projectTitle}</div>
                  </div>
                </div>
                <div className="mt-3 flex items-center justify-between">
                  <ScoreBadge score={r.overallConfidence} label="confidence" />
                  <span className="text-[10px] text-muted-foreground">{r.claims.length} claims</span>
                </div>
              </button>
            );
          })}
        </div>
      </GlassPanel>

      <div className="grid gap-4 lg:grid-cols-3">
        {/* Confidence gauge */}
        <GlassPanel className="flex flex-col items-center justify-center gap-4 p-5">
          <div>
            <h2 className="text-center text-sm font-semibold">Overall confidence</h2>
            <p className="text-center text-xs text-muted-foreground">{report.projectTitle}</p>
          </div>
          <div style={{ "--brand": toneVar } as React.CSSProperties}>
            <ProgressRing value={report.overallConfidence} size={132} stroke={10} />
          </div>
          {channel && (
            <div className="flex items-center gap-2 rounded-lg border border-border/60 px-3 py-2">
              <div className="grid size-6 shrink-0 place-items-center rounded-md text-xs" style={{ backgroundColor: channel.color + "33" }}>
                {channel.avatar}
              </div>
              <span className="text-xs text-muted-foreground">{channel.name}</span>
            </div>
          )}
          <div className="grid w-full grid-cols-2 gap-2 text-center">
            {CLAIM_STATUSES.map((s) => {
              const meta = STATUS_META[s];
              return (
                <div key={s} className="rounded-lg border border-border/60 p-2">
                  <div className="text-[10px] uppercase tracking-wider text-muted-foreground">{meta.label}</div>
                  <div className="mt-0.5 text-sm font-semibold tabular-nums">{counts[s]}</div>
                </div>
              );
            })}
          </div>
        </GlassPanel>

        {/* Claims list */}
        <GlassPanel className="p-5 lg:col-span-2">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold">Claims</h2>
              <p className="text-xs text-muted-foreground">Every statement checked against sources before publish.</p>
            </div>
            <ToggleGroup
              type="single"
              value={filter}
              onValueChange={(v) => v && setFilter(v as ClaimStatus | "all")}
              className="flex-wrap justify-start gap-1 rounded-lg bg-muted p-1"
            >
              <ToggleGroupItem value="all" className="h-7 rounded-md px-2.5 text-xs">All</ToggleGroupItem>
              {CLAIM_STATUSES.map((s) => (
                <ToggleGroupItem key={s} value={s} className="h-7 rounded-md px-2.5 text-xs">
                  {STATUS_META[s].label}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          </div>

          <div className="scrollbar-thin -mx-1 max-h-[520px] overflow-y-auto px-1">
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead>Claim</TableHead>
                  <TableHead className="w-24">Confidence</TableHead>
                  <TableHead className="w-28">Status</TableHead>
                  <TableHead className="w-40">Sources</TableHead>
                  <TableHead className="w-32 text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibleClaims.map((c) => (
                  <TableRow key={c.id} className="align-top">
                    <TableCell className="max-w-xs">
                      <p className="text-sm leading-snug">{c.claimText}</p>
                      {c.note && (
                        <p className="mt-1.5 rounded-md border border-warning/25 bg-warning/5 px-2 py-1 text-[11px] leading-snug text-warning">
                          {c.note}
                        </p>
                      )}
                    </TableCell>
                    <TableCell>
                      <ScoreBadge score={c.confidenceScore} />
                    </TableCell>
                    <TableCell>
                      <ClaimStatusBadge status={c.status} />
                    </TableCell>
                    <TableCell>
                      <ul className="space-y-1">
                        {c.sources.map((src, i) => (
                          <li key={i} className="flex items-start gap-1 text-[11px] text-muted-foreground">
                            <ExternalLink className="mt-0.5 size-3 shrink-0" />
                            <span className="leading-snug">{src}</span>
                          </li>
                        ))}
                      </ul>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex flex-col items-end gap-1.5">
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-7 gap-1 text-xs"
                          onClick={() => toast("Re-verifying claim…", { description: c.claimText })}
                        >
                          <RefreshCw className="size-3.5" /> Re-verify
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-7 gap-1 text-xs"
                          onClick={() => toast.success("Marked reviewed", { description: c.claimText })}
                        >
                          <Check className="size-3.5" /> Mark reviewed
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {visibleClaims.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="py-8 text-center text-sm text-muted-foreground">
                      No claims match this filter.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </GlassPanel>
      </div>
    </div>
  );
}
