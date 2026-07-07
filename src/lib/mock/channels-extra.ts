import type { Platform } from "./types";

/**
 * Additional per-channel detail not modeled in the shared fixtures:
 * brand-voice profiles, per-platform connection detail, and a lightweight
 * performance snapshot used by the Channels detail view.
 */

export interface BrandVoiceProfile {
  description: string;
  formalCasual: number; // 0 = formal, 100 = casual
  seriousPlayful: number; // 0 = serious, 100 = playful
  conciseDetailed: number; // 0 = concise, 100 = detailed
}

export interface PlatformDetail {
  handle: string;
  followers: number;
}

export interface ChannelPerformance {
  viewsThisWeek: number;
  ctr: number; // %
  avgRetention: number; // %
  rpm: number; // $ per 1000 views
  viewsSpark: number[];
}

export interface ChannelExtra {
  brandVoice: BrandVoiceProfile;
  platformDetails: Partial<Record<Platform, PlatformDetail>>;
  performance: ChannelPerformance;
}

export const channelExtras: Record<string, ChannelExtra> = {
  ch1: {
    brandVoice: {
      description: "Awe-driven and precise — we treat the audience like they're smart. No hype words, let the discovery speak for itself, close every video on a question that reframes what they thought they knew.",
      formalCasual: 35,
      seriousPlayful: 30,
      conciseDetailed: 70,
    },
    platformDetails: {
      youtube: { handle: "@cosmosunfolded", followers: 1_240_000 },
      tiktok: { handle: "@cosmosunfolded", followers: 410_000 },
      instagram: { handle: "@cosmos.unfolded", followers: 268_000 },
    },
    performance: { viewsThisWeek: 3_180_000, ctr: 8.1, avgRetention: 64, rpm: 6.20, viewsSpark: [2.4, 2.6, 2.9, 3.0, 3.1, 3.2, 3.18] },
  },
  ch2: {
    brandVoice: {
      description: "Warm, direct, a little wry — like a smart friend explaining a hack over coffee. Short sentences, second-person framing ('you'), always land on one clear action the viewer can do today.",
      formalCasual: 62,
      seriousPlayful: 55,
      conciseDetailed: 40,
    },
    platformDetails: {
      youtube: { handle: "@mindhacksdaily", followers: 682_400 },
      instagram: { handle: "@mindhacks.daily", followers: 221_000 },
    },
    performance: { viewsThisWeek: 1_640_000, ctr: 9.4, avgRetention: 71, rpm: 4.10, viewsSpark: [1.2, 1.3, 1.35, 1.4, 1.5, 1.6, 1.64] },
  },
  ch3: {
    brandVoice: {
      description: "Confident and contrarian — we say the thing the headlines are dancing around. Data-forward, chart-first, but never condescending to viewers who aren't finance people.",
      formalCasual: 45,
      seriousPlayful: 20,
      conciseDetailed: 65,
    },
    platformDetails: {
      youtube: { handle: "@moneyminute", followers: 415_900 },
      tiktok: { handle: "@moneyminute", followers: 198_000 },
      x: { handle: "@moneyminute", followers: 84_500 },
    },
    performance: { viewsThisWeek: 980_000, ctr: 7.6, avgRetention: 58, rpm: 9.80, viewsSpark: [0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 0.98] },
  },
  ch4: {
    brandVoice: {
      description: "Curious storyteller with a documentary cadence. We build a mystery, drop context slowly, and reward patience with a payoff line the viewer will want to repeat to a friend.",
      formalCasual: 40,
      seriousPlayful: 35,
      conciseDetailed: 75,
    },
    platformDetails: {
      youtube: { handle: "@historyunlocked", followers: 298_100 },
      tiktok: { handle: "@historyunlocked", followers: 112_000 },
    },
    performance: { viewsThisWeek: 610_000, ctr: 6.9, avgRetention: 69, rpm: 3.40, viewsSpark: [0.42, 0.46, 0.5, 0.54, 0.58, 0.6, 0.61] },
  },
  ch5: {
    brandVoice: {
      description: "High-energy and playful — fast cuts, punchy captions, every line is a hook. We front-load the weirdest part of the fact in the first second and never bury the lede.",
      formalCasual: 78,
      seriousPlayful: 82,
      conciseDetailed: 25,
    },
    platformDetails: {
      tiktok: { handle: "@weirdfactslab", followers: 612_000 },
      instagram: { handle: "@weirdfacts.lab", followers: 244_700 },
    },
    performance: { viewsThisWeek: 2_210_000, ctr: 10.2, avgRetention: 76, rpm: 2.60, viewsSpark: [1.8, 1.9, 2.0, 2.1, 2.15, 2.2, 2.21] },
  },
  ch6: {
    brandVoice: {
      description: "Sharp, technical-but-accessible, a bit cheeky about hype cycles. We explain the 'so what' before the 'how' and always ground claims in a benchmark or demo.",
      formalCasual: 50,
      seriousPlayful: 48,
      conciseDetailed: 60,
    },
    platformDetails: {
      youtube: { handle: "@bytesizedai", followers: 512_300 },
      tiktok: { handle: "@bytesizedai", followers: 176_000 },
      x: { handle: "@bytesizedai", followers: 96_400 },
    },
    performance: { viewsThisWeek: 1_420_000, ctr: 8.8, avgRetention: 62, rpm: 5.70, viewsSpark: [1.0, 1.05, 1.15, 1.25, 1.32, 1.38, 1.42] },
  },
  ch7: {
    brandVoice: {
      description: "Enthusiast-to-enthusiast tone — we geek out about the engineering, use the correct technical terms, and treat the audience as fellow gearheads, not tourists.",
      formalCasual: 48,
      seriousPlayful: 40,
      conciseDetailed: 68,
    },
    platformDetails: {
      youtube: { handle: "@gearheadgarage", followers: 388_200 },
      instagram: { handle: "@gearhead.garage", followers: 151_000 },
    },
    performance: { viewsThisWeek: 540_000, ctr: 7.1, avgRetention: 60, rpm: 4.60, viewsSpark: [0.38, 0.41, 0.45, 0.48, 0.5, 0.52, 0.54] },
  },
  ch8: {
    brandVoice: {
      description: "Hushed, cinematic, deliberate pacing — we let silence do work. Every episode ends on an unresolved thread that pulls viewers into the next one.",
      formalCasual: 38,
      seriousPlayful: 18,
      conciseDetailed: 72,
    },
    platformDetails: {
      youtube: { handle: "@latenightmysteries", followers: 623_900 },
      tiktok: { handle: "@latenightmysteries", followers: 205_000 },
    },
    performance: { viewsThisWeek: 1_380_000, ctr: 9.0, avgRetention: 74, rpm: 3.90, viewsSpark: [1.0, 1.05, 1.12, 1.2, 1.28, 1.34, 1.38] },
  },
};

export function channelExtraById(id?: string): ChannelExtra | undefined {
  if (!id) return undefined;
  return channelExtras[id];
}

export type TeamRole = "Owner" | "Admin" | "Editor" | "Reviewer" | "Viewer";

export interface TeamMember {
  id: string;
  name: string;
  email: string;
  role: TeamRole;
  initials: string;
}

export const teamMembers: TeamMember[] = [
  { id: "tm1", name: "Morgan Reyes", email: "morgan@vira.ai", role: "Owner", initials: "MR" },
  { id: "tm2", name: "Priya Anand", email: "priya@vira.ai", role: "Admin", initials: "PA" },
  { id: "tm3", name: "Diego Fuentes", email: "diego@vira.ai", role: "Editor", initials: "DF" },
  { id: "tm4", name: "Sasha Klein", email: "sasha@vira.ai", role: "Reviewer", initials: "SK" },
  { id: "tm5", name: "Noah Bennett", email: "noah@vira.ai", role: "Viewer", initials: "NB" },
];
