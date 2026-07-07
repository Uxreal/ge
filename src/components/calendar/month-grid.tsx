import { eachDayOfInterval, endOfMonth, endOfWeek, format, isSameDay, isSameMonth, startOfMonth, startOfWeek } from "date-fns";
import { cn } from "@/lib/utils";
import type { ScheduledPost } from "@/lib/mock/schedule";
import { PostChip } from "./post-chip";

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MAX_VISIBLE = 3;

export function MonthGrid({
  month, posts, anchor, onDayClick,
}: {
  month: Date;
  posts: ScheduledPost[];
  anchor: Date;
  onDayClick: (day: Date) => void;
}) {
  const gridStart = startOfWeek(startOfMonth(month));
  const gridEnd = endOfWeek(endOfMonth(month));
  const days = eachDayOfInterval({ start: gridStart, end: gridEnd });

  return (
    <div>
      <div className="grid grid-cols-7 gap-px overflow-hidden rounded-t-xl border border-border/70 bg-border/70 text-center text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
        {WEEKDAY_LABELS.map((d) => (
          <div key={d} className="bg-surface/80 py-2">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-px overflow-hidden rounded-b-xl border-x border-b border-border/70 bg-border/70">
        {days.map((day) => {
          const dayPosts = posts.filter((p) => isSameDay(p.date, day));
          const visible = dayPosts.slice(0, MAX_VISIBLE);
          const overflow = dayPosts.length - visible.length;
          const inMonth = isSameMonth(day, month);
          const isAnchor = isSameDay(day, anchor);
          return (
            <button
              key={day.toISOString()}
              onClick={() => onDayClick(day)}
              className={cn(
                "flex min-h-[104px] flex-col items-stretch gap-1 bg-surface/60 p-1.5 text-left transition-colors hover:bg-surface/90",
                !inMonth && "opacity-40",
              )}
            >
              <div className="flex items-center justify-between px-0.5">
                <span
                  className={cn(
                    "text-xs tabular-nums text-muted-foreground",
                    isAnchor && "grid size-5 place-items-center rounded-full bg-brand font-semibold text-brand-foreground",
                  )}
                >
                  {format(day, "d")}
                </span>
                {dayPosts.length > 0 && (
                  <span className="text-[10px] text-muted-foreground">{dayPosts.length}</span>
                )}
              </div>
              <div className="flex flex-1 flex-col gap-1">
                {visible.map((p) => <PostChip key={p.id} post={p} />)}
                {overflow > 0 && (
                  <span className="px-1 text-[10px] font-medium text-brand">+{overflow} more</span>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
