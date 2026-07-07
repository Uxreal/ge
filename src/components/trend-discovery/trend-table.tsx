import type { Trend } from "@/lib/mock/types";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ScoreBadge } from "@/components/kit/score-badge";
import { SourceRow } from "@/components/kit/source-badge";
import { Button } from "@/components/ui/button";
import { Eye, ArrowRight } from "lucide-react";
import { toast } from "sonner";

export function TrendTable({ trends, onView }: { trends: Trend[]; onView: (t: Trend) => void }) {
  return (
    <div className="scrollbar-thin overflow-x-auto rounded-xl border border-border/70 bg-surface/40">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead>Topic</TableHead>
            <TableHead>Category</TableHead>
            <TableHead>Virality</TableHead>
            <TableHead>Growth</TableHead>
            <TableHead>Competition</TableHead>
            <TableHead>Evergreen</TableHead>
            <TableHead>Lifespan</TableHead>
            <TableHead>Sources</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {trends.map((t) => (
            <TableRow key={t.id} className="cursor-pointer" onClick={() => onView(t)}>
              <TableCell className="max-w-[280px]">
                <div className="truncate text-sm font-medium">{t.topic}</div>
                <div className="text-[10px] text-muted-foreground">{t.ageHours}h ago · {t.volume}</div>
              </TableCell>
              <TableCell className="whitespace-nowrap text-xs text-muted-foreground">{t.category}</TableCell>
              <TableCell><ScoreBadge score={t.viralityScore} /></TableCell>
              <TableCell className="whitespace-nowrap tabular-nums text-xs text-success">▲ {t.growthVelocity}%/d</TableCell>
              <TableCell className="whitespace-nowrap text-xs capitalize text-muted-foreground">{t.competition}</TableCell>
              <TableCell className="tabular-nums text-xs">{t.evergreenScore}</TableCell>
              <TableCell className="whitespace-nowrap text-xs text-muted-foreground">{t.estimatedLifespan}</TableCell>
              <TableCell><SourceRow sources={t.sources} /></TableCell>
              <TableCell className="text-right">
                <div className="flex justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                  <Button
                    size="sm" variant="ghost" className="h-7 px-2"
                    onClick={() => toast.success("Idea drafted", { description: t.topic })}
                  >
                    <ArrowRight className="size-3.5" />
                  </Button>
                  <Button size="sm" variant="ghost" className="h-7 px-2" onClick={() => onView(t)}>
                    <Eye className="size-3.5" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
