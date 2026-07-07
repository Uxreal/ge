export type Platform = "youtube" | "tiktok" | "instagram" | "reddit" | "x" | "google-trends" | "news" | "rss";

export type Category =
  | "Facts" | "Technology" | "AI" | "Gaming" | "Business" | "Finance" | "Cars" | "History"
  | "Mystery" | "Space" | "Animals" | "Science" | "Productivity" | "Life Hacks" | "Psychology"
  | "Movies & TV" | "Motivation" | "Fitness" | "Cooking" | "DIY" | "Educational" | "Storytelling"
  | "Luxury" | "Humor" | "Memes";

export type Stage =
  | "trend"
  | "idea"
  | "script"
  | "fact-check"
  | "scenes"
  | "assets"
  | "video"
  | "voice"
  | "captions"
  | "music"
  | "thumbnail"
  | "seo"
  | "schedule"
  | "publish";

export type StageStatus = "queued" | "running" | "needs-approval" | "done" | "failed";

export type CompetitionLevel = "low" | "medium" | "high";

export interface Channel {
  id: string;
  name: string;
  handle: string;
  category: Category;
  platforms: Platform[];
  subscribers: number;
  weeklyRevenue: number;
  color: string; // hex/oklch accent
  avatar: string; // emoji or initials
}

export interface Trend {
  id: string;
  topic: string;
  category: Category;
  viralityScore: number; // 0-100
  sources: Platform[];
  growth: number[]; // sparkline
  volume: string; // "1.2M searches"
  ageHours: number;
  growthVelocity: number; // %/day
  competition: CompetitionLevel;
  audienceOverlap: number; // 0-100 vs existing channels
  estimatedLifespan: string; // "3-5 days left" | "Evergreen"
  revenuePotential: string; // "$$$" tier or $ estimate
  evergreenScore: number; // 0-100
  seasonalRelevance: string; // "Trending now" | "Year-round" | "Seasonal — Q4"
  whyItsWorking: string;
}

export interface Idea {
  id: string;
  title: string;
  hook: string;
  channelId?: string;
  trendId?: string;
  category: Category;
  score: number;
  estimatedViews: string;
  clickPotential: number;
  retentionPrediction: number;
  monetizationValue: number;
  originality: number;
  audienceInterest: number;
  trendAlignment: number;
  angle: string;
  hookOptions: string[];
  createdAt: string;
}

export interface PipelineStageState {
  stage: Stage;
  status: StageStatus;
  progress?: number; // 0-100
}

export interface Project {
  id: string;
  title: string;
  channelId: string;
  currentStage: Stage;
  stages: PipelineStageState[];
  thumbnail?: string;
  updatedAt: string; // relative "2h ago"
  score: number;
}

export interface Approval {
  id: string;
  projectId: string;
  projectTitle: string;
  channelId: string;
  type: "script" | "thumbnail" | "final-cut" | "seo";
  createdAt: string;
}

export interface ActivityEntry {
  id: string;
  actor: "system" | "ai" | "you";
  message: string;
  time: string; // "3 min ago"
  icon?: string;
}

export interface WeekPoint {
  day: string;
  views: number;
  revenue: number;
}
