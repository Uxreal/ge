/**
 * Learning Loop-specific mock fixtures. channelId links back to the shared
 * channels fixture in ./data (read-only) but every shape here is local to
 * this page.
 */

export type InsightCategory =
  | "Hooks" | "Thumbnails" | "Editing style" | "Posting times" | "Video length"
  | "Pacing" | "Topics" | "CTAs" | "Audience preferences";

export const INSIGHT_CATEGORIES: InsightCategory[] = [
  "Hooks", "Thumbnails", "Editing style", "Posting times", "Video length",
  "Pacing", "Topics", "CTAs", "Audience preferences",
];

export type InsightStatus = "proposed" | "applied" | "dismissed";

export interface Insight {
  id: string;
  category: InsightCategory;
  headline: string;
  confidence: number; // 0-100
  recommendation: string;
  status: InsightStatus;
}

export const insights: Insight[] = [
  {
    id: "in1", category: "Hooks",
    headline: "Questions in the first 2 seconds retain 18% better",
    confidence: 92,
    recommendation: "Auto-generate a question-based hook variant for every new script.",
    status: "applied",
  },
  {
    id: "in2", category: "Hooks",
    headline: "Hooks under 6 words outperform longer hooks by 24% in avg view duration",
    confidence: 81,
    recommendation: "Cap generated hook options at 6 words during idea generation.",
    status: "proposed",
  },
  {
    id: "in3", category: "Thumbnails",
    headline: "Faces with visible eye contact lift CTR by 14%",
    confidence: 88,
    recommendation: "Prioritize eye-contact framing in the thumbnail generation preset.",
    status: "applied",
  },
  {
    id: "in4", category: "Thumbnails",
    headline: "A single bold word overlay outperforms multi-word text by 11%",
    confidence: 74,
    recommendation: "Default thumbnail text overlay to 1-3 words maximum.",
    status: "proposed",
  },
  {
    id: "in5", category: "Editing style",
    headline: "Jump cuts every 2-3 seconds keep retention above 70% through the first 30 seconds",
    confidence: 85,
    recommendation: "Set default cut cadence to 2.5s for the first 30 seconds of every edit.",
    status: "applied",
  },
  {
    id: "in6", category: "Posting times",
    headline: "Uploads between 6-8pm local time outperform morning uploads by 21% in first-hour views",
    confidence: 90,
    recommendation: "Shift the default publish scheduling window to 6-8pm.",
    status: "applied",
  },
  {
    id: "in7", category: "Posting times",
    headline: "Weekend uploads see a 15% lower completion rate on finance content",
    confidence: 66,
    recommendation: "Avoid scheduling Money Minute uploads on weekends.",
    status: "proposed",
  },
  {
    id: "in8", category: "Video length",
    headline: "Videos between 52-68 seconds have the highest completion rate across all channels",
    confidence: 79,
    recommendation: "Bias script generation toward a 55-65 second target runtime.",
    status: "proposed",
  },
  {
    id: "in9", category: "Pacing",
    headline: "Scripts with a twist or reveal in the final 15% retain 12% more viewers to the end",
    confidence: 83,
    recommendation: "Require a late-script reveal beat in the fact-check stage checklist.",
    status: "applied",
  },
  {
    id: "in10", category: "Topics",
    headline: "Space and AI content cross-pollinate audiences with 30%+ overlap",
    confidence: 70,
    recommendation: "Suggest cross-channel remix ideas between Cosmos Unfolded and Byte Sized AI.",
    status: "proposed",
  },
  {
    id: "in11", category: "Topics",
    headline: "Contrarian framing (“X nobody talks about”) outperforms neutral framing by 19% CTR",
    confidence: 87,
    recommendation: "Bias idea angle generation toward contrarian framing by default.",
    status: "applied",
  },
  {
    id: "in12", category: "CTAs",
    headline: "Asking a direct question as the outro CTA increases comment rate by 26%",
    confidence: 76,
    recommendation: "Default outro CTA copy to a direct-question format.",
    status: "proposed",
  },
  {
    id: "in13", category: "Audience preferences",
    headline: "True-crime and mystery audiences over-index on late-night watch sessions (9pm-1am)",
    confidence: 68,
    recommendation: "Schedule Late Night Mysteries uploads for evening release only.",
    status: "dismissed",
  },
];

export type AdjustmentStatus = "pending" | "approved" | "dismissed";

export interface PendingAdjustment {
  id: string;
  title: string;
  rationale: string;
  confidence: number; // 0-100
  channelId?: string; // undefined = applies globally, across all channels
  status: AdjustmentStatus;
}

export const pendingAdjustments: PendingAdjustment[] = [
  {
    id: "adj1",
    title: "Shift posting time for Money Minute to 7pm ET",
    rationale: "Finance audience engagement peaks 6-8pm ET; the current 11am slot underperforms first-hour views by 21%.",
    confidence: 90,
    channelId: "ch3",
    status: "pending",
  },
  {
    id: "adj2",
    title: "Increase pacing on History Unlocked scripts by 15%",
    rationale: "Average watch time drops sharply after 45s on recent uploads; faster scene cuts correlate with 12% better retention.",
    confidence: 78,
    channelId: "ch4",
    status: "pending",
  },
  {
    id: "adj3",
    title: "Auto-generate contrarian-framed hook variants for every new idea",
    rationale: "Contrarian framing has produced measurably higher CTR across 6 of 8 channels over the last 30 days.",
    confidence: 84,
    status: "pending",
  },
  {
    id: "adj4",
    title: "Cap thumbnail text overlay to 3 words for Weird Facts Lab",
    rationale: "Short-overlay thumbnails on this channel are outperforming longer captions by 17% CTR.",
    confidence: 71,
    channelId: "ch5",
    status: "pending",
  },
  {
    id: "adj5",
    title: "Reduce upload frequency on Late Night Mysteries weekends",
    rationale: "Weekend uploads on this channel show a 15% completion-rate dip versus weekday uploads.",
    confidence: 66,
    channelId: "ch8",
    status: "pending",
  },
];
