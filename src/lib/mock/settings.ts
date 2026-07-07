/**
 * Mock fixtures for the Settings page: available plugins/integrations and
 * custom prompt templates. Fully local/mocked — no network calls.
 */

export type PluginCategory =
  | "Publishing" | "Analytics" | "AI Generation" | "SEO" | "Storage" | "Voice" | "Video" | "Automation";

export interface Plugin {
  id: string;
  name: string;
  description: string;
  category: PluginCategory;
  enabled: boolean;
  shortLabel: string; // short icon-friendly label, e.g. "YT", "11L"
}

export const plugins: Plugin[] = [
  { id: "pl1", name: "YouTube Publisher", description: "Push finished videos, thumbnails, and metadata straight to YouTube Studio.", category: "Publishing", enabled: true, shortLabel: "YT" },
  { id: "pl2", name: "TikTok Publisher", description: "Schedule and publish shorts directly to connected TikTok accounts.", category: "Publishing", enabled: true, shortLabel: "TT" },
  { id: "pl3", name: "Instagram Publisher", description: "Auto-post Reels and carousels with synced captions and hashtags.", category: "Publishing", enabled: true, shortLabel: "IG" },
  { id: "pl4", name: "ElevenLabs Voice", description: "AI voice cloning and narration for the voiceover pipeline stage.", category: "Voice", enabled: true, shortLabel: "11L" },
  { id: "pl5", name: "Runway Video Gen", description: "Generate and upscale b-roll clips from text prompts.", category: "Video", enabled: false, shortLabel: "RW" },
  { id: "pl6", name: "Google Analytics 4", description: "Pull cross-platform view and audience data into Analytics dashboards.", category: "Analytics", enabled: true, shortLabel: "GA4" },
  { id: "pl7", name: "TubeBuddy SEO", description: "Keyword research and tag suggestions for the SEO pipeline stage.", category: "SEO", enabled: true, shortLabel: "TB" },
  { id: "pl8", name: "Zapier Automation", description: "Trigger external workflows when a project reaches a given stage.", category: "Automation", enabled: false, shortLabel: "ZP" },
  { id: "pl9", name: "Notion Sync", description: "Mirror scripts and approvals into a shared Notion workspace.", category: "Storage", enabled: false, shortLabel: "NO" },
  { id: "pl10", name: "Google Drive Storage", description: "Archive raw footage, assets, and exports to Drive automatically.", category: "Storage", enabled: true, shortLabel: "GD" },
  { id: "pl11", name: "GPT Scriptwriter", description: "LLM-assisted script drafts, hook variations, and fact-check prompts.", category: "AI Generation", enabled: true, shortLabel: "AI" },
  { id: "pl12", name: "Midjourney Thumbnails", description: "Generate thumbnail concept art from a short creative brief.", category: "AI Generation", enabled: false, shortLabel: "MJ" },
];

export type TemplateCategory = "Script" | "Fact-check" | "SEO" | "Thumbnail" | "Engagement" | "Ideation";

export interface PromptTemplate {
  id: string;
  name: string;
  description: string;
  category: TemplateCategory;
  template: string;
}

export const promptTemplates: PromptTemplate[] = [
  {
    id: "pt1",
    name: "Viral Hook Generator",
    description: "Produces 5 competing hook options for a given topic and audience.",
    category: "Script",
    template: "Write 5 distinct hooks (max 12 words each) for a short-form video about {{topic}} aimed at {{audience}}. Prioritize curiosity gaps and pattern interrupts. Avoid clickbait that the video can't pay off.",
  },
  {
    id: "pt2",
    name: "Fact-check Claim Extractor",
    description: "Pulls every checkable claim out of a script draft with confidence notes.",
    category: "Fact-check",
    template: "Read the script below and list every factual claim as a bullet, followed by a confidence rating (high/medium/low) and a suggested source to verify it against.\n\nScript:\n{{script}}",
  },
  {
    id: "pt3",
    name: "SEO Title & Description",
    description: "Generates a keyword-optimized title and description pair.",
    category: "SEO",
    template: "Given this video summary: {{summary}}, write 3 title options under 60 characters optimized for search + a 2-sentence description with primary keyword '{{keyword}}' in the first sentence.",
  },
  {
    id: "pt4",
    name: "Thumbnail Concept Brief",
    description: "Turns a script into a concrete visual brief for thumbnail generation.",
    category: "Thumbnail",
    template: "Based on this hook: '{{hook}}', describe a single thumbnail concept: subject, facial expression, background, on-image text (max 4 words), and color mood. Keep it legible at small size.",
  },
  {
    id: "pt5",
    name: "Short-form Script Rewriter",
    description: "Condenses a long-form script into a punchy under-60-second cut.",
    category: "Script",
    template: "Rewrite the following script as a sub-60-second short. Keep the strongest hook and payoff, cut all setup that isn't essential, and end on a line that invites a comment.\n\nScript:\n{{script}}",
  },
  {
    id: "pt6",
    name: "Comment Reply Assistant",
    description: "Drafts on-brand replies to top comments to boost engagement.",
    category: "Engagement",
    template: "Here are top comments on our video '{{title}}': {{comments}}. Draft a short, on-brand reply to each that encourages further discussion without sounding scripted.",
  },
  {
    id: "pt7",
    name: "Trend Angle Brainstorm",
    description: "Generates unique creative angles for a trending topic.",
    category: "Ideation",
    template: "Trend: {{trend}}. Category: {{category}}. Generate 5 distinct video angles our channel hasn't covered yet, each with a one-line reason it fits our brand voice.",
  },
  {
    id: "pt8",
    name: "Video Chapters & Timestamps",
    description: "Builds SEO-friendly chapter markers from a script or transcript.",
    category: "SEO",
    template: "From this transcript, produce YouTube chapter timestamps (format 0:00 Label) marking every major topic shift. Keep labels under 6 words.\n\nTranscript:\n{{transcript}}",
  },
];

export interface CredentialEntry {
  id: string;
  name: string;
  masked: string;
  lastRotated: string;
}

export const credentials: CredentialEntry[] = [
  { id: "cr1", name: "OpenAI API Key", masked: "sk-••••••••••••8f2a", lastRotated: "12 days ago" },
  { id: "cr2", name: "ElevenLabs API Key", masked: "el-••••••••••••91c4", lastRotated: "5 days ago" },
  { id: "cr3", name: "YouTube OAuth", masked: "oauth-••••••••••••b7d1", lastRotated: "30 days ago" },
  { id: "cr4", name: "Runway API Key", masked: "rw-••••••••••••e02f", lastRotated: "3 days ago" },
  { id: "cr5", name: "Google Analytics OAuth", masked: "oauth-••••••••••••4a9e", lastRotated: "18 days ago" },
];
