import { format } from "date-fns";
import { Plus } from "lucide-react";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { SourceBadge } from "@/components/kit/source-badge";
import { channelById } from "@/lib/mock/data";
import type { ScheduledPost } from "@/lib/mock/schedule";
import { STATUS_BADGE, STATUS_LABEL } from "./status";

export function DaySheet({
  open, onOpenChange, day, posts, onScheduleNew,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  day: Date | undefined;
  posts: ScheduledPost[];
  onScheduleNew: () => void;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col overflow-y-auto sm:max-w-md">
        <SheetHeader>
          <SheetTitle>{day ? format(day, "EEEE, MMMM d") : "Day"}</SheetTitle>
          <SheetDescription>
            {posts.length} post{posts.length === 1 ? "" : "s"} scheduled
          </SheetDescription>
        </SheetHeader>

        <div className="mt-4 flex-1 space-y-3">
          {posts.length === 0 && (
            <p className="text-sm text-muted-foreground">Nothing scheduled yet for this day.</p>
          )}
          {posts.map((p) => {
            const ch = channelById(p.channelId);
            return (
              <div key={p.id} className="rounded-xl border border-border/70 bg-surface/60 p-3">
                <div className="flex items-center justify-between gap-2">
                  <SourceBadge source={p.platform} />
                  <span className={`rounded-full border px-2 py-0.5 text-[10px] font-medium ${STATUS_BADGE[p.status]}`}>
                    {STATUS_LABEL[p.status]}
                  </span>
                </div>
                <div className="mt-2 text-sm font-medium leading-snug">{p.projectTitle}</div>
                <div className="mt-1.5 flex items-center gap-1.5 text-xs text-muted-foreground">
                  <span
                    className="grid size-5 place-items-center rounded-full text-[11px]"
                    style={{ backgroundColor: (ch?.color ?? "#7c5cff") + "33" }}
                  >
                    {ch?.avatar}
                  </span>
                  {ch?.name} · {p.scheduledTimeLabel}
                </div>
              </div>
            );
          })}
        </div>

        <Button className="mt-4 w-full gap-1.5" onClick={onScheduleNew}>
          <Plus className="size-4" /> Schedule new post
        </Button>
      </SheetContent>
    </Sheet>
  );
}
