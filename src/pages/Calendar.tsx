import { useMemo, useRef, useState } from "react";
import {
  addMonths, endOfMonth, endOfWeek, format, isBefore, isWithinInterval,
  startOfMonth, startOfWeek, subMonths,
} from "date-fns";
import { PageHeader } from "@/components/kit/page-header";
import { GlassPanel } from "@/components/kit/glass-panel";
import { StatCard } from "@/components/kit/stat-card";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar as DatePicker } from "@/components/ui/calendar";
import { MonthGrid } from "@/components/calendar/month-grid";
import { DaySheet } from "@/components/calendar/day-sheet";
import { ScheduleDialog, type NewPostInput } from "@/components/calendar/schedule-dialog";
import { StatusLegend } from "@/components/calendar/status-legend";
import { ChevronLeft, ChevronRight, CalendarDays, CalendarClock, Plus } from "lucide-react";
import { SCHEDULE_ANCHOR, scheduledPosts, type ScheduledPost } from "@/lib/mock/schedule";
import { toast } from "sonner";

export default function Calendar() {
  const [posts, setPosts] = useState<ScheduledPost[]>(scheduledPosts);
  const [displayedMonth, setDisplayedMonth] = useState(startOfMonth(SCHEDULE_ANCHOR));
  const [jumpOpen, setJumpOpen] = useState(false);

  const [selectedDay, setSelectedDay] = useState<Date | undefined>(undefined);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogDay, setDialogDay] = useState<Date | undefined>(SCHEDULE_ANCHOR);
  const newPostCounter = useRef(0);

  const stats = useMemo(() => {
    const weekStart = startOfWeek(SCHEDULE_ANCHOR);
    const weekEnd = endOfWeek(SCHEDULE_ANCHOR);
    const monthStart = startOfMonth(SCHEDULE_ANCHOR);
    const monthEnd = endOfMonth(SCHEDULE_ANCHOR);

    const postsThisWeek = posts.filter((p) => isWithinInterval(p.date, { start: weekStart, end: weekEnd })).length;
    const postsThisMonth = posts.filter((p) => isWithinInterval(p.date, { start: monthStart, end: monthEnd })).length;

    const upcoming = posts
      .filter((p) => (p.status === "scheduled" || p.status === "queued") && !isBefore(p.date, SCHEDULE_ANCHOR))
      .sort((a, b) => a.date.getTime() - b.date.getTime());
    const next = upcoming[0];

    return {
      postsThisWeek,
      postsThisMonth,
      nextLabel: next ? `${format(next.date, "MMM d")}, ${next.scheduledTimeLabel}` : "None upcoming",
    };
  }, [posts]);

  const dayPosts = useMemo(
    () => (selectedDay ? posts.filter((p) => p.date.toDateString() === selectedDay.toDateString()) : []),
    [posts, selectedDay],
  );

  const openDay = (day: Date) => {
    setSelectedDay(day);
    setSheetOpen(true);
  };

  const openScheduleDialog = (day: Date | undefined) => {
    setDialogDay(day);
    setDialogOpen(true);
  };

  const handleCreate = (input: NewPostInput) => {
    const day = dialogDay ?? SCHEDULE_ANCHOR;
    newPostCounter.current += 1;
    const newPost: ScheduledPost = {
      id: `sp-new-${newPostCounter.current}`,
      projectTitle: input.projectTitle,
      channelId: input.channelId,
      platform: input.platform,
      date: day,
      scheduledTimeLabel: input.time,
      status: "scheduled",
    };
    setPosts((prev) => [...prev, newPost]);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Content Calendar"
        subtitle="Every scheduled post, across every channel, in one view."
        actions={
          <Button
            className="h-9 rounded-lg text-brand-foreground shadow-glow"
            style={{ background: "var(--gradient-brand)" }}
            onClick={() => openScheduleDialog(SCHEDULE_ANCHOR)}
          >
            <Plus className="mr-2 size-4" /> Schedule new post
          </Button>
        }
      />

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="Posts this week" value={String(stats.postsThisWeek)} icon={<CalendarDays className="size-4" />} />
        <StatCard label="Posts this month" value={String(stats.postsThisMonth)} icon={<CalendarDays className="size-4" />} />
        <StatCard label="Next scheduled" value={stats.nextLabel} icon={<CalendarClock className="size-4" />} />
      </section>

      <GlassPanel className="p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Button variant="outline" size="icon" className="size-8 rounded-lg" onClick={() => setDisplayedMonth((m) => subMonths(m, 1))}>
              <ChevronLeft className="size-4" />
            </Button>
            <h2 className="min-w-[140px] text-sm font-semibold">{format(displayedMonth, "MMMM yyyy")}</h2>
            <Button variant="outline" size="icon" className="size-8 rounded-lg" onClick={() => setDisplayedMonth((m) => addMonths(m, 1))}>
              <ChevronRight className="size-4" />
            </Button>
            <Button variant="ghost" size="sm" className="h-8 text-xs" onClick={() => setDisplayedMonth(startOfMonth(SCHEDULE_ANCHOR))}>
              Today
            </Button>
            <Popover open={jumpOpen} onOpenChange={setJumpOpen}>
              <PopoverTrigger asChild>
                <Button variant="ghost" size="sm" className="h-8 gap-1 text-xs text-muted-foreground">
                  <CalendarDays className="size-3.5" /> Jump to date
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0">
                <DatePicker
                  mode="single"
                  defaultMonth={displayedMonth}
                  onSelect={(d) => {
                    if (d) {
                      setDisplayedMonth(startOfMonth(d));
                      setJumpOpen(false);
                    }
                  }}
                />
              </PopoverContent>
            </Popover>
          </div>
          <StatusLegend />
        </div>

        <MonthGrid month={displayedMonth} posts={posts} anchor={SCHEDULE_ANCHOR} onDayClick={openDay} />
      </GlassPanel>

      <DaySheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        day={selectedDay}
        posts={dayPosts}
        onScheduleNew={() => {
          setSheetOpen(false);
          openScheduleDialog(selectedDay);
        }}
      />

      <ScheduleDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        day={dialogDay}
        onCreate={handleCreate}
      />
    </div>
  );
}
