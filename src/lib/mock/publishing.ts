import type { Platform } from "./types";

export type QueueItemStatus = "uploading" | "queued" | "published" | "failed" | "needs-review";

export interface QueueItem {
  id: string;
  projectTitle: string;
  channelId: string;
  platform: Platform;
  status: QueueItemStatus;
  progress?: number; // 0-100, meaningful when status === "uploading"
  scheduledOrPublishedLabel: string;
  error?: string; // present when status === "failed"
}

export const queueItems: QueueItem[] = [
  { id: "q1", projectTitle: "A free model just humiliated a $10B AI lab", channelId: "ch6", platform: "youtube", status: "uploading", progress: 74, scheduledOrPublishedLabel: "Uploading now" },
  { id: "q2", projectTitle: "The disappearance that still doesn't add up", channelId: "ch8", platform: "tiktok", status: "uploading", progress: 31, scheduledOrPublishedLabel: "Uploading now" },
  { id: "q3", projectTitle: "Octopuses taste with their skin and it's unsettling", channelId: "ch5", platform: "instagram", status: "queued", scheduledOrPublishedLabel: "Publishes today, 5:30 PM" },
  { id: "q4", projectTitle: "The pricing psychology trick every luxury brand uses", channelId: "ch3", platform: "youtube", status: "queued", scheduledOrPublishedLabel: "Publishes tomorrow, 9:00 AM" },
  { id: "q5", projectTitle: "Fix your focus in 2 minutes flat", channelId: "ch2", platform: "tiktok", status: "queued", scheduledOrPublishedLabel: "Publishes tomorrow, 12:00 PM" },
  { id: "q6", projectTitle: "Webb telescope just broke physics", channelId: "ch1", platform: "youtube", status: "published", scheduledOrPublishedLabel: "Published today, 9:04 AM" },
  { id: "q7", projectTitle: "The 5-second procrastination hack", channelId: "ch2", platform: "tiktok", status: "published", scheduledOrPublishedLabel: "Published today, 11:31 AM" },
  { id: "q8", projectTitle: "Why time slows when you're scared", channelId: "ch2", platform: "instagram", status: "published", scheduledOrPublishedLabel: "Published today, 4:02 PM" },
  { id: "q9", projectTitle: "The Fed pivot nobody sees coming", channelId: "ch3", platform: "instagram", status: "failed", scheduledOrPublishedLabel: "Failed at 2:00 PM", error: "Instagram API rejected the cover image — aspect ratio outside allowed range." },
  { id: "q10", projectTitle: "This hypercar's $200K flaw nobody mentions", channelId: "ch7", platform: "youtube", status: "failed", scheduledOrPublishedLabel: "Failed at 9:03 AM", error: "Upload timed out after 3 retries — connection to YouTube Studio API dropped." },
  { id: "q11", projectTitle: "Is there a fifth force hiding in the universe?", channelId: "ch1", platform: "youtube", status: "needs-review", scheduledOrPublishedLabel: "Held for review" },
  { id: "q12", projectTitle: "This open-source model just beat GPT-4", channelId: "ch6", platform: "tiktok", status: "needs-review", scheduledOrPublishedLabel: "Held for review" },
  { id: "q13", projectTitle: "The unsolved disappearance that still haunts investigators", channelId: "ch8", platform: "youtube", status: "queued", scheduledOrPublishedLabel: "Publishes in 4 days, 9:00 AM" },
  { id: "q14", projectTitle: "Why every game studio is quietly switching engines", channelId: "ch6", platform: "instagram", status: "queued", scheduledOrPublishedLabel: "Publishes in 5 days, 6:00 PM" },
];
