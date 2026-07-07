import { subDays, format } from "date-fns";
import { channels, ideas } from "./data";

/**
 * Analytics-specific mock fixtures. Everything here is derived/linked from the
 * shared channels + ideas fixtures in ./data (read-only imports) but the shapes
 * are local to this page — nothing here mutates or extends the shared types.
 */

// ---------------------------------------------------------------------------
// Daily timeseries (views / watch time / revenue)
// ---------------------------------------------------------------------------

export interface DailyMetricPoint {
  date: string; // ISO yyyy-MM-dd
  label: string; // "Jun 8" — short axis label
  weekday: string; // "Mon"
  views: number;
  watchTimeHours: number;
  revenue: number;
}

const ANCHOR_DATE = new Date("2026-07-07T00:00:00Z");
const TOTAL_DAYS = 90;

function buildDailyMetrics(): DailyMetricPoint[] {
  const points: DailyMetricPoint[] = [];
  for (let i = TOTAL_DAYS - 1; i >= 0; i--) {
    const date = subDays(ANCHOR_DATE, i);
    const weekday = format(date, "EEE");
    const dayIndex = TOTAL_DAYS - 1 - i; // 0 = oldest, TOTAL_DAYS-1 = most recent
    const isWeekend = weekday === "Sat" || weekday === "Sun";
    const trend = 1 + dayIndex * 0.0035; // slow overall growth across the quarter
    const weekly = 1 + Math.sin((dayIndex / 7) * Math.PI * 2) * 0.08; // gentle weekly rhythm
    const weekendBoost = isWeekend ? 1.24 : 1;
    const viralBump = dayIndex % 11 === 0 ? 1.35 : 1; // occasional breakout day
    const views = Math.round(285_000 * trend * weekly * weekendBoost * viralBump);
    const watchTimeHours = Math.round((views * 3.1) / 60);
    const revenue = Math.round(views * 0.0083 * (isWeekend ? 1.05 : 1));
    points.push({
      date: format(date, "yyyy-MM-dd"),
      label: format(date, "MMM d"),
      weekday,
      views,
      watchTimeHours,
      revenue,
    });
  }
  return points;
}

/** Full 90-day history, oldest first. */
export const dailyMetrics: DailyMetricPoint[] = buildDailyMetrics();

export const RANGE_DAYS = { "7d": 7, "30d": 30, "90d": 90 } as const;
export type RangeKey = keyof typeof RANGE_DAYS;
export const RANGE_KEYS = Object.keys(RANGE_DAYS) as RangeKey[];

export function rangeSlice(range: RangeKey): DailyMetricPoint[] {
  return dailyMetrics.slice(-RANGE_DAYS[range]);
}

export function aggregateMetrics(points: DailyMetricPoint[]) {
  return points.reduce(
    (acc, p) => ({
      views: acc.views + p.views,
      watchTimeHours: acc.watchTimeHours + p.watchTimeHours,
      revenue: acc.revenue + p.revenue,
    }),
    { views: 0, watchTimeHours: 0, revenue: 0 },
  );
}

// ---------------------------------------------------------------------------
// Per-channel KPI summaries (trailing ~90-day totals)
// ---------------------------------------------------------------------------

export interface ChannelKpi {
  channelId: string;
  views: number;
  watchTimeAvgMinutes: number;
  retentionPercent: number;
  completionRate: number;
  likes: number;
  shares: number;
  comments: number;
  followersGained: number;
  ctr: number; // %
  rpm: number; // $ per 1,000 views
  estRevenue: number; // $
}

const channelKpiInputs: Omit<ChannelKpi, "estRevenue">[] = [
  { channelId: "ch1", views: 5_150_000, watchTimeAvgMinutes: 2.8, retentionPercent: 71, completionRate: 58, likes: 412_000, shares: 68_000, comments: 24_500, followersGained: 42_000, ctr: 8.4, rpm: 3.85 },
  { channelId: "ch2", views: 2_980_000, watchTimeAvgMinutes: 2.3, retentionPercent: 76, completionRate: 63, likes: 268_000, shares: 41_000, comments: 19_800, followersGained: 21_400, ctr: 7.1, rpm: 3.35 },
  { channelId: "ch3", views: 2_240_000, watchTimeAvgMinutes: 3.4, retentionPercent: 58, completionRate: 44, likes: 134_000, shares: 38_000, comments: 15_200, followersGained: 11_600, ctr: 6.2, rpm: 6.05 },
  { channelId: "ch4", views: 1_540_000, watchTimeAvgMinutes: 3.6, retentionPercent: 74, completionRate: 61, likes: 121_000, shares: 22_000, comments: 9_600, followersGained: 9_200, ctr: 5.8, rpm: 3.6 },
  { channelId: "ch5", views: 3_720_000, watchTimeAvgMinutes: 1.9, retentionPercent: 69, completionRate: 55, likes: 298_000, shares: 62_000, comments: 21_000, followersGained: 33_500, ctr: 8.9, rpm: 3.1 },
  { channelId: "ch6", views: 2_610_000, watchTimeAvgMinutes: 2.6, retentionPercent: 66, completionRate: 52, likes: 209_000, shares: 47_000, comments: 22_400, followersGained: 24_700, ctr: 7.6, rpm: 4.9 },
  { channelId: "ch7", views: 1_680_000, watchTimeAvgMinutes: 3.1, retentionPercent: 62, completionRate: 49, likes: 118_000, shares: 19_000, comments: 8_100, followersGained: 8_700, ctr: 6.5, rpm: 4.35 },
  { channelId: "ch8", views: 2_340_000, watchTimeAvgMinutes: 3.3, retentionPercent: 79, completionRate: 65, likes: 187_000, shares: 33_000, comments: 16_800, followersGained: 17_900, ctr: 7.9, rpm: 3.75 },
];

export const channelKpis: ChannelKpi[] = channelKpiInputs.map((k) => ({
  ...k,
  estRevenue: Math.round((k.views / 1000) * k.rpm),
}));

export function channelKpiById(id?: string) {
  if (!id) return undefined;
  return channelKpis.find((k) => k.channelId === id);
}

const totalChannelViews = channelKpis.reduce((s, k) => s + k.views, 0);

function shareForChannel(channelId?: string): number {
  if (!channelId) return 1;
  const kpi = channelKpiById(channelId);
  return kpi ? kpi.views / totalChannelViews : 1 / channels.length;
}

function scalePoints(points: DailyMetricPoint[], share: number): DailyMetricPoint[] {
  if (share === 1) return points;
  return points.map((p) => ({
    ...p,
    views: Math.round(p.views * share),
    watchTimeHours: Math.round(p.watchTimeHours * share),
    revenue: Math.round(p.revenue * share),
  }));
}

/** Scales the aggregate daily timeseries down to one channel's rough share of total views. */
export function dailyMetricsFor(channelId: string | undefined, range: RangeKey): DailyMetricPoint[] {
  return scalePoints(rangeSlice(range), shareForChannel(channelId));
}

/** The period immediately preceding the current range, for computing week-over-week deltas.
 *  Returns an empty array when there isn't enough history behind the range (e.g. 90d). */
export function previousRangeSlice(range: RangeKey): DailyMetricPoint[] {
  const days = RANGE_DAYS[range];
  const end = dailyMetrics.length - days;
  const start = end - days;
  if (start < 0) return [];
  return dailyMetrics.slice(start, end);
}

export function previousDailyMetricsFor(channelId: string | undefined, range: RangeKey): DailyMetricPoint[] {
  return scalePoints(previousRangeSlice(range), shareForChannel(channelId));
}

// ---------------------------------------------------------------------------
// Top performing hooks
// ---------------------------------------------------------------------------

export interface TopHook {
  id: string;
  hook: string;
  projectTitle: string;
  channelId: string;
  retentionPercent: number;
  views: number;
}

const HOOK_SOURCE: Array<{ ideaId: string; retentionPercent: number; views: number }> = [
  { ideaId: "i1", retentionPercent: 91, views: 3_120_000 },
  { ideaId: "i6", retentionPercent: 88, views: 2_540_000 },
  { ideaId: "i5", retentionPercent: 86, views: 2_180_000 },
  { ideaId: "i10", retentionPercent: 84, views: 1_860_000 },
  { ideaId: "i2", retentionPercent: 82, views: 1_640_000 },
  { ideaId: "i4", retentionPercent: 77, views: 980_000 },
  { ideaId: "i3", retentionPercent: 69, views: 720_000 },
  { ideaId: "i9", retentionPercent: 65, views: 540_000 },
];

export const topHooks: TopHook[] = HOOK_SOURCE.map(({ ideaId, retentionPercent, views }) => {
  const idea = ideas.find((i) => i.id === ideaId)!;
  return {
    id: `hook-${idea.id}`,
    hook: idea.hook,
    projectTitle: idea.title,
    channelId: idea.channelId!,
    retentionPercent,
    views,
  };
});

// ---------------------------------------------------------------------------
// Best posting times (day x hour-bucket heatmap)
// ---------------------------------------------------------------------------

export const POSTING_DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"] as const;
export type PostingDay = (typeof POSTING_DAYS)[number];

export const POSTING_HOURS = ["6 AM", "9 AM", "12 PM", "3 PM", "6 PM", "9 PM"] as const;
export type PostingHour = (typeof POSTING_HOURS)[number];

export interface PostingSlot {
  day: PostingDay;
  hour: PostingHour;
  score: number; // 0-100
}

// Rows = days (Mon..Sun), columns = hour buckets (6 AM..9 PM)
const POSTING_SCORE_MATRIX: number[][] = [
  [28, 46, 52, 58, 74, 68], // Mon
  [30, 48, 54, 60, 78, 70], // Tue
  [32, 50, 56, 62, 82, 74], // Wed
  [34, 52, 58, 66, 88, 80], // Thu
  [36, 54, 62, 68, 92, 90], // Fri
  [40, 62, 78, 82, 86, 84], // Sat
  [38, 58, 80, 84, 90, 86], // Sun
];

export const bestPostingTimes: PostingSlot[] = POSTING_DAYS.flatMap((day, dayIdx) =>
  POSTING_HOURS.map((hour, hourIdx) => ({ day, hour, score: POSTING_SCORE_MATRIX[dayIdx][hourIdx] })),
);

export function postingScore(day: PostingDay, hour: PostingHour): number {
  return bestPostingTimes.find((s) => s.day === day && s.hour === hour)?.score ?? 0;
}
