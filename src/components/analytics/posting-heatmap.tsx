import { Fragment } from "react";
import { cn } from "@/lib/utils";
import { POSTING_DAYS, POSTING_HOURS, postingScore } from "@/lib/mock/analytics";

function heatClass(score: number) {
  if (score >= 85) return "bg-brand/80 text-brand-foreground";
  if (score >= 70) return "bg-brand/55 text-foreground";
  if (score >= 55) return "bg-brand/35 text-foreground";
  if (score >= 40) return "bg-brand/20 text-foreground";
  if (score >= 25) return "bg-brand/10 text-muted-foreground";
  return "bg-surface/70 text-muted-foreground";
}

export function PostingHeatmap() {
  return (
    <div>
      <div
        className="grid gap-1"
        style={{ gridTemplateColumns: `44px repeat(${POSTING_DAYS.length}, minmax(0, 1fr))` }}
      >
        <div />
        {POSTING_DAYS.map((day) => (
          <div key={`head-${day}`} className="text-center text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
            {day}
          </div>
        ))}
        {POSTING_HOURS.map((hour) => (
          <Fragment key={hour}>
            <div className="flex items-center text-[10px] text-muted-foreground">{hour}</div>
            {POSTING_DAYS.map((day) => {
              const score = postingScore(day, hour);
              return (
                <div
                  key={`${day}-${hour}`}
                  title={`${day} ${hour} — score ${score}`}
                  className={cn(
                    "flex aspect-square items-center justify-center rounded-md text-[10px] font-medium tabular-nums transition-colors duration-150",
                    heatClass(score),
                  )}
                >
                  {score}
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-end gap-1.5 text-[10px] text-muted-foreground">
        <span>Lower</span>
        {[10, 25, 40, 55, 70, 85].map((s) => (
          <span key={s} className={cn("size-3 rounded-sm", heatClass(s))} />
        ))}
        <span>Higher</span>
      </div>
    </div>
  );
}
