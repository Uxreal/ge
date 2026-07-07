import { useMemo, useState } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { channels } from "@/lib/mock/data";
import type { Channel } from "@/lib/mock/types";
import { channelKpiById, type ChannelKpi } from "@/lib/mock/analytics";

type SortKey = "channel" | "views" | "rpm" | "revenue" | "followers";

function formatCompact(n: number) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

export function ChannelComparisonTable() {
  const [sortKey, setSortKey] = useState<SortKey>("revenue");
  const [dir, setDir] = useState<"asc" | "desc">("desc");

  const rows = useMemo(() => {
    const withKpi = channels.map((c) => ({ channel: c, kpi: channelKpiById(c.id) }))
      .filter((r): r is { channel: Channel; kpi: ChannelKpi } => !!r.kpi);

    const sorted = [...withKpi].sort((a, b) => {
      let cmp = 0;
      if (sortKey === "channel") cmp = a.channel.name.localeCompare(b.channel.name);
      if (sortKey === "views") cmp = a.kpi.views - b.kpi.views;
      if (sortKey === "rpm") cmp = a.kpi.rpm - b.kpi.rpm;
      if (sortKey === "revenue") cmp = a.kpi.estRevenue - b.kpi.estRevenue;
      if (sortKey === "followers") cmp = a.kpi.followersGained - b.kpi.followersGained;
      return dir === "asc" ? cmp : -cmp;
    });
    return sorted;
  }, [sortKey, dir]);

  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setDir("desc");
    }
  };

  const headerButton = (key: SortKey, label: string, align: "left" | "right" = "left") => (
    <button
      type="button"
      onClick={() => toggleSort(key)}
      className={cn(
        "inline-flex items-center gap-1 text-[11px] font-medium uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground",
        align === "right" && "flex-row-reverse",
      )}
    >
      {label}
      {sortKey === key ? (
        dir === "asc" ? <ArrowUp className="size-3" /> : <ArrowDown className="size-3" />
      ) : (
        <ArrowUpDown className="size-3 opacity-40" />
      )}
    </button>
  );

  return (
    <Table>
      <TableHeader>
        <TableRow className="border-border/60 hover:bg-transparent">
          <TableHead className="pl-0">{headerButton("channel", "Channel")}</TableHead>
          <TableHead className="text-right">{headerButton("views", "Views", "right")}</TableHead>
          <TableHead className="text-right">{headerButton("rpm", "RPM", "right")}</TableHead>
          <TableHead className="text-right">{headerButton("revenue", "Revenue", "right")}</TableHead>
          <TableHead className="pr-0 text-right">{headerButton("followers", "Followers +", "right")}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map(({ channel: c, kpi }) => (
          <TableRow key={c.id} className="border-border/60">
            <TableCell className="pl-0">
              <div className="flex items-center gap-2">
                <div className="grid size-7 shrink-0 place-items-center rounded-lg text-sm" style={{ backgroundColor: c.color + "33" }}>
                  <span>{c.avatar}</span>
                </div>
                <div className="min-w-0">
                  <div className="truncate text-sm font-medium">{c.name}</div>
                  <div className="truncate text-[10px] text-muted-foreground">{c.handle}</div>
                </div>
              </div>
            </TableCell>
            <TableCell className="text-right text-sm tabular-nums">{formatCompact(kpi.views)}</TableCell>
            <TableCell className="text-right text-sm tabular-nums">${kpi.rpm.toFixed(2)}</TableCell>
            <TableCell className="text-right text-sm font-medium tabular-nums text-success">${formatCompact(kpi.estRevenue)}</TableCell>
            <TableCell className="pr-0 text-right text-sm tabular-nums">+{formatCompact(kpi.followersGained)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
