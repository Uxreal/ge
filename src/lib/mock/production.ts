import type { Stage } from "./types";

// Page-specific mock detail content for the Production pipeline view.
// Deterministic "random" picks are derived from the project id so the same
// project always shows the same mocked detail (no re-shuffling on re-render).

function seedFor(id: string) {
  return [...id].reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
}

export const STAGE_DESCRIPTIONS: Record<Stage, string> = {
  trend: "Source trend identified and scored for relevance to this channel's audience.",
  idea: "Concept, hook, and angle generated from the trend and validated against past hits.",
  script: "Full narration script drafted and beat-mapped to the retention curve.",
  "fact-check": "Every factual claim cross-referenced against sources before a frame renders.",
  scenes: "Script broken into a scene-by-scene shot list with visual direction per beat.",
  assets: "Stock, generated, and licensed visual assets gathered per scene.",
  video: "Scenes assembled, cut, and rendered into a first full edit.",
  voice: "AI voice narration synthesized and time-matched to the script.",
  captions: "Auto-captions generated and styled to platform conventions.",
  music: "Background score selected and mixed under narration and SFX.",
  thumbnail: "Click-optimized thumbnail variants generated and CTR-scored.",
  seo: "Title, description, and tags optimized for discovery per platform.",
  schedule: "Optimal publish time selected per platform from audience-activity data.",
  publish: "Final package pushed live across every connected platform.",
};

export interface MockClaim {
  claim: string;
  status: "verified" | "flagged" | "unverified";
  source: string;
}

const CLAIM_BANK: MockClaim[] = [
  { claim: "The headline figure matches the original primary source.", status: "verified", source: "Primary source archive" },
  { claim: "This finding exceeds what prior published estimates predicted.", status: "flagged", source: "Cross-reference preprint" },
  { claim: "No earlier public report covers this specific detail.", status: "verified", source: "News index scan" },
  { claim: "The claim as phrased slightly overstates the original data.", status: "flagged", source: "Community fact-check" },
  { claim: "An independent source has not yet corroborated this point.", status: "unverified", source: "Pending citation" },
  { claim: "Figures were recalculated and match within rounding error.", status: "verified", source: "Analyst recalculation" },
];

export function mockClaimsFor(projectId: string): MockClaim[] {
  const seed = seedFor(projectId);
  return [0, 1, 2].map((i) => CLAIM_BANK[(seed + i * 2) % CLAIM_BANK.length]);
}

export interface MockThumbnailVariant {
  id: string;
  label: string;
  predictedCtr: number;
}

export function mockThumbnailVariants(projectId: string): MockThumbnailVariant[] {
  const seed = seedFor(projectId);
  return [
    { id: "a", label: "Variant A — bold text overlay", predictedCtr: 6.1 + (seed % 4) },
    { id: "b", label: "Variant B — face + reaction", predictedCtr: 8.3 + (seed % 3) },
    { id: "c", label: "Variant C — pattern interrupt", predictedCtr: 5.4 + (seed % 5) },
  ];
}

const PUBLISH_TIME_BANK = [
  "Today, 6:30 PM ET",
  "Tomorrow, 9:15 AM ET",
  "Tomorrow, 1:00 PM ET",
  "In 2 days, 8:00 AM ET",
  "Today, 11:45 PM ET",
];

export function mockPublishTime(projectId: string) {
  const seed = seedFor(projectId);
  return PUBLISH_TIME_BANK[seed % PUBLISH_TIME_BANK.length];
}
