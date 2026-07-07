import { addDays } from "date-fns";
import type { Platform } from "./types";

// Fixed anchor date for this mocked session — never derive from the real clock.
// July 7, 2026. All fixture dates are offsets from this single anchor.
export const SCHEDULE_ANCHOR = new Date(2026, 6, 7);

export type ScheduledPostStatus = "scheduled" | "queued" | "published" | "failed";

export interface ScheduledPost {
  id: string;
  projectTitle: string;
  channelId: string;
  platform: Platform;
  date: Date;
  scheduledTimeLabel: string;
  status: ScheduledPostStatus;
}

interface Seed {
  offset: number;
  projectTitle: string;
  channelId: string;
  platform: Platform;
  time: string;
  status: ScheduledPostStatus;
}

const seeds: Seed[] = [
  { offset: -5, projectTitle: "Webb telescope just broke physics", channelId: "ch1", platform: "youtube", time: "9:00 AM", status: "published" },
  { offset: -4, projectTitle: "The 5-second procrastination hack", channelId: "ch2", platform: "tiktok", time: "11:30 AM", status: "published" },
  { offset: -4, projectTitle: "The Fed pivot nobody sees coming", channelId: "ch3", platform: "instagram", time: "2:00 PM", status: "failed" },
  { offset: -3, projectTitle: "Rome's algorithm: history's first AI", channelId: "ch4", platform: "youtube", time: "9:00 AM", status: "published" },
  { offset: -2, projectTitle: "The weirdest thing your brain does asleep", channelId: "ch5", platform: "tiktok", time: "10:00 AM", status: "published" },
  { offset: -2, projectTitle: "Why time slows when you're scared", channelId: "ch2", platform: "instagram", time: "4:00 PM", status: "published" },
  { offset: -1, projectTitle: "This hypercar's $200K flaw nobody mentions", channelId: "ch7", platform: "youtube", time: "9:00 AM", status: "failed" },
  { offset: 0, projectTitle: "A free model just humiliated a $10B AI lab", channelId: "ch6", platform: "youtube", time: "9:00 AM", status: "scheduled" },
  { offset: 0, projectTitle: "The disappearance that still doesn't add up", channelId: "ch8", platform: "tiktok", time: "1:00 PM", status: "queued" },
  { offset: 0, projectTitle: "Octopuses taste with their skin and it's unsettling", channelId: "ch5", platform: "instagram", time: "5:30 PM", status: "scheduled" },
  { offset: 1, projectTitle: "The pricing psychology trick every luxury brand uses", channelId: "ch3", platform: "youtube", time: "9:00 AM", status: "scheduled" },
  { offset: 1, projectTitle: "Fix your focus in 2 minutes flat", channelId: "ch2", platform: "tiktok", time: "12:00 PM", status: "queued" },
  { offset: 2, projectTitle: "Inside the $40M apartment nobody is allowed to see", channelId: "ch1", platform: "instagram", time: "3:00 PM", status: "scheduled" },
  { offset: 3, projectTitle: "Is there a fifth force hiding in the universe?", channelId: "ch1", platform: "youtube", time: "9:00 AM", status: "scheduled" },
  { offset: 3, projectTitle: "This open-source model just beat GPT-4", channelId: "ch6", platform: "tiktok", time: "11:00 AM", status: "queued" },
  { offset: 4, projectTitle: "The unsolved disappearance that still haunts investigators", channelId: "ch8", platform: "youtube", time: "9:00 AM", status: "scheduled" },
  { offset: 5, projectTitle: "Why every game studio is quietly switching engines", channelId: "ch6", platform: "instagram", time: "6:00 PM", status: "scheduled" },
  { offset: 6, projectTitle: "The one ingredient swap chefs never mention", channelId: "ch5", platform: "tiktok", time: "10:30 AM", status: "queued" },
  { offset: 8, projectTitle: "Why the pyramids couldn't be built today", channelId: "ch4", platform: "youtube", time: "9:00 AM", status: "scheduled" },
  { offset: 10, projectTitle: "The $8 tool that replaces five kitchen gadgets", channelId: "ch5", platform: "instagram", time: "1:30 PM", status: "scheduled" },
  { offset: 13, projectTitle: "A stranger's note that changed a small town forever", channelId: "ch8", platform: "tiktok", time: "11:00 AM", status: "scheduled" },
  { offset: 17, projectTitle: "The one-rep trick that fixed everyone's deadlift form", channelId: "ch7", platform: "youtube", time: "9:00 AM", status: "scheduled" },
];

export const scheduledPosts: ScheduledPost[] = seeds.map((s, i) => ({
  id: `sp${i + 1}`,
  projectTitle: s.projectTitle,
  channelId: s.channelId,
  platform: s.platform,
  date: addDays(SCHEDULE_ANCHOR, s.offset),
  scheduledTimeLabel: s.time,
  status: s.status,
}));
