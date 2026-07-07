import { projects, channels } from "./data";

export type AssetType = "image" | "video" | "voice" | "music" | "sfx";
export type SourceLabel = "AI-generated" | "Stock" | "Voice clone" | "Uploaded";

export interface Asset {
  id: string;
  title: string;
  type: AssetType;
  projectTitle?: string;
  channelId?: string;
  meta: string; // duration ("0:42") or dimensions ("1080×1920")
  sourceLabel: SourceLabel;
  createdAt: string;
  tags: string[];
  chart: 1 | 2 | 3 | 4 | 5; // maps to a --chart-N token for the placeholder gradient
  similarDetected?: boolean;
  waveform?: number[]; // 0-100 bar heights, only for voice/music/sfx
}

function projectTitle(id: string) {
  return projects.find((p) => p.id === id)?.title;
}

function wave(seed: number, n: number) {
  const out: number[] = [];
  let x = seed;
  for (let i = 0; i < n; i++) {
    x = (x * 9301 + 49297) % 233280;
    out.push(18 + Math.round((x / 233280) * 82));
  }
  return out;
}

export const assets: Asset[] = [
  { id: "as1", title: "Webb telescope hero shot", type: "image", projectTitle: projectTitle("p1"), channelId: "ch1", meta: "1920×1080", sourceLabel: "AI-generated", createdAt: "4 min ago", tags: ["space", "hero", "16:9"], chart: 1 },
  { id: "as2", title: "Galaxy field b-roll", type: "video", projectTitle: projectTitle("p1"), channelId: "ch1", meta: "0:18", sourceLabel: "Stock", createdAt: "12 min ago", tags: ["space", "b-roll"], chart: 1 },
  { id: "as3", title: "Narration take — Webb cold open", type: "voice", projectTitle: projectTitle("p1"), channelId: "ch1", meta: "0:34", sourceLabel: "Voice clone", createdAt: "9 min ago", tags: ["narration", "cold-open"], chart: 2, waveform: wave(7, 28) },
  { id: "as4", title: "Ambient cosmic pad", type: "music", channelId: "ch1", meta: "1:20", sourceLabel: "AI-generated", createdAt: "1 h ago", tags: ["ambient", "score"], chart: 5, waveform: wave(3, 20) },
  { id: "as5", title: "Whoosh transition", type: "sfx", meta: "0:02", sourceLabel: "Stock", createdAt: "2 h ago", tags: ["transition", "whoosh"], chart: 3, waveform: wave(11, 14) },

  { id: "as6", title: "Countdown timer overlay", type: "video", projectTitle: projectTitle("p2"), channelId: "ch2", meta: "0:05", sourceLabel: "AI-generated", createdAt: "16 min ago", tags: ["overlay", "ui"], chart: 4 },
  { id: "as7", title: "5-second rule thumbnail draft", type: "image", projectTitle: projectTitle("p2"), channelId: "ch2", meta: "1280×720", sourceLabel: "AI-generated", createdAt: "18 min ago", tags: ["thumbnail", "draft"], chart: 4, similarDetected: true },
  { id: "as8", title: "Narration take — procrastination hook", type: "voice", projectTitle: projectTitle("p2"), channelId: "ch2", meta: "0:41", sourceLabel: "Voice clone", createdAt: "22 min ago", tags: ["narration", "hook"], chart: 2, waveform: wave(19, 26) },
  { id: "as9", title: "Focus-mode brainwave loop", type: "music", channelId: "ch2", meta: "2:04", sourceLabel: "AI-generated", createdAt: "3 h ago", tags: ["focus", "loop"], chart: 5, waveform: wave(23, 22) },

  { id: "as10", title: "Fed building exterior", type: "image", projectTitle: projectTitle("p3"), channelId: "ch3", meta: "1600×900", sourceLabel: "Stock", createdAt: "40 min ago", tags: ["finance", "b-roll"], chart: 4 },
  { id: "as11", title: "Stock ticker overlay", type: "video", projectTitle: projectTitle("p3"), channelId: "ch3", meta: "0:12", sourceLabel: "AI-generated", createdAt: "44 min ago", tags: ["overlay", "finance"], chart: 4, similarDetected: true },
  { id: "as12", title: "Cash register ding", type: "sfx", meta: "0:01", sourceLabel: "Stock", createdAt: "5 h ago", tags: ["sfx", "money"], chart: 3, waveform: wave(31, 12) },

  { id: "as13", title: "Antikythera mechanism close-up", type: "image", projectTitle: projectTitle("p4"), channelId: "ch4", meta: "1080×1350", sourceLabel: "Stock", createdAt: "1 h ago", tags: ["history", "artifact"], chart: 1 },
  { id: "as14", title: "Narration take — Rome's algorithm", type: "voice", projectTitle: projectTitle("p4"), channelId: "ch4", meta: "0:58", sourceLabel: "Voice clone", createdAt: "2 h ago", tags: ["narration"], chart: 2, waveform: wave(41, 30) },
  { id: "as15", title: "Parchment texture overlay", type: "image", channelId: "ch4", meta: "2048×2048", sourceLabel: "Uploaded", createdAt: "6 h ago", tags: ["texture", "overlay"], chart: 1 },

  { id: "as16", title: "Sleep-cycle animation", type: "video", projectTitle: projectTitle("p5"), channelId: "ch5", meta: "0:24", sourceLabel: "AI-generated", createdAt: "30 min ago", tags: ["animation", "facts"], chart: 4 },
  { id: "as17", title: "Brain scan visualization", type: "image", projectTitle: projectTitle("p5"), channelId: "ch5", meta: "1920×1080", sourceLabel: "AI-generated", createdAt: "36 min ago", tags: ["science", "visualization"], chart: 1 },
  { id: "as18", title: "Night ambience loop", type: "music", channelId: "ch5", meta: "3:12", sourceLabel: "Stock", createdAt: "4 h ago", tags: ["ambient", "night"], chart: 5, waveform: wave(53, 24) },

  { id: "as19", title: "GPU render farm b-roll", type: "video", projectTitle: projectTitle("p7"), channelId: "ch6", meta: "0:15", sourceLabel: "Stock", createdAt: "8 min ago", tags: ["ai", "tech"], chart: 4 },
  { id: "as20", title: "Narration take — open-source model", type: "voice", projectTitle: projectTitle("p7"), channelId: "ch6", meta: "0:47", sourceLabel: "Voice clone", createdAt: "9 min ago", tags: ["narration"], chart: 2, waveform: wave(61, 27) },
  { id: "as21", title: "Benchmark bar chart animation", type: "video", projectTitle: projectTitle("p7"), channelId: "ch6", meta: "0:09", sourceLabel: "AI-generated", createdAt: "11 min ago", tags: ["chart", "overlay"], chart: 4, similarDetected: true },

  { id: "as22", title: "Dim alley establishing shot", type: "image", projectTitle: projectTitle("p8"), channelId: "ch8", meta: "1920×1080", sourceLabel: "Stock", createdAt: "24 min ago", tags: ["mystery", "b-roll"], chart: 3 },
  { id: "as23", title: "Tense strings sting", type: "music", channelId: "ch8", meta: "0:22", sourceLabel: "AI-generated", createdAt: "27 min ago", tags: ["sting", "tension"], chart: 5, waveform: wave(67, 16) },
  { id: "as24", title: "Case file paper rustle", type: "sfx", meta: "0:03", sourceLabel: "Stock", createdAt: "1 h ago", tags: ["sfx", "foley"], chart: 3, waveform: wave(71, 10) },

  { id: "as25", title: "Gearhead garage b-roll pack", type: "video", channelId: "ch7", meta: "0:31", sourceLabel: "Uploaded", createdAt: "1 day ago", tags: ["cars", "b-roll"], chart: 4 },
  { id: "as26", title: "Engine rev sfx", type: "sfx", channelId: "ch7", meta: "0:04", sourceLabel: "Stock", createdAt: "1 day ago", tags: ["sfx", "engine"], chart: 3, waveform: wave(79, 13) },
  { id: "as27", title: "Generic upbeat corporate loop", type: "music", meta: "1:48", sourceLabel: "Stock", createdAt: "2 days ago", tags: ["corporate", "loop"], chart: 5, waveform: wave(83, 21) },
  { id: "as28", title: "Portrait lower-third template", type: "image", meta: "1080×1920", sourceLabel: "Uploaded", createdAt: "3 days ago", tags: ["template", "lower-third"], chart: 2 },
];

export function assetTypeCount(type: AssetType) {
  return assets.filter((a) => a.type === type).length;
}

export const channelOptions = channels.map((c) => ({ id: c.id, name: c.name }));
