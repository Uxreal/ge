import type { Insight } from "@/lib/mock/learning";
import { ScoreBadge } from "@/components/kit/score-badge";
import { InsightStatusBadge } from "./insight-status-badge";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";

interface Props {
  insight: Insight;
  autoApply: boolean;
  onToggleAutoApply: (id: string, next: boolean) => void;
}

export function InsightCard({ insight, autoApply, onToggleAutoApply }: Props) {
  const disabled = insight.status === "dismissed";
  return (
    <div className="flex h-full flex-col gap-3 rounded-xl border border-border/70 bg-surface/60 p-4 transition hover:border-brand/40 hover:shadow-soft">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground">{insight.category}</span>
        <InsightStatusBadge status={insight.status} />
      </div>
      <div className="space-y-2">
        <p className="text-sm font-medium leading-snug">{insight.headline}</p>
        <ScoreBadge score={insight.confidence} label="confidence" />
      </div>
      <p className="text-xs leading-relaxed text-muted-foreground">{insight.recommendation}</p>
      <div className="mt-auto flex items-center justify-between gap-3 border-t border-border/60 pt-3">
        <Label htmlFor={`autoapply-${insight.id}`} className={disabled ? "text-xs text-muted-foreground/60" : "text-xs text-muted-foreground"}>
          Auto-apply to future content
        </Label>
        <Switch
          id={`autoapply-${insight.id}`}
          checked={autoApply}
          disabled={disabled}
          onCheckedChange={(checked) => onToggleAutoApply(insight.id, checked)}
        />
      </div>
    </div>
  );
}
