import { projects } from "./data";

export type ClaimStatus = "verified" | "flagged" | "outdated" | "unverified";

export interface Claim {
  id: string;
  claimText: string;
  confidenceScore: number; // 0-100
  status: ClaimStatus;
  sources: string[];
  note?: string;
}

export interface FactCheckReport {
  id: string;
  projectId: string;
  projectTitle: string;
  channelId: string;
  overallConfidence: number; // 0-100
  claims: Claim[];
}

function projectTitle(id: string) {
  return projects.find((p) => p.id === id)?.title ?? id;
}

function projectChannel(id: string) {
  return projects.find((p) => p.id === id)?.channelId ?? "";
}

export const factCheckReports: FactCheckReport[] = [
  {
    id: "fc1",
    projectId: "p1",
    projectTitle: projectTitle("p1"),
    channelId: projectChannel("p1"),
    overallConfidence: 84,
    claims: [
      {
        id: "fc1-c1",
        claimText: "JWST identified a galaxy candidate with a measured redshift placing its formation roughly 290-320 million years after the Big Bang.",
        confidenceScore: 91,
        status: "verified",
        sources: ["NASA JPL press release, 2026", "Nature Astronomy peer-reviewed preprint"],
      },
      {
        id: "fc1-c2",
        claimText: "The redshift measurement was independently checked by three separate research teams before publication.",
        confidenceScore: 84,
        status: "verified",
        sources: ["European Space Agency briefing, 2026"],
      },
      {
        id: "fc1-c3",
        claimText: "This is the first galaxy ever discovered that predates the theoretical age of the universe.",
        confidenceScore: 52,
        status: "flagged",
        sources: ["Space.com summary article, 2026"],
        note: "Overstated — the galaxy doesn't predate the universe, it formed unexpectedly early within the existing timeline. This framing is common in early aggregator coverage but should be corrected in-script.",
      },
      {
        id: "fc1-c4",
        claimText: "Similar 'impossibly early' galaxy candidates have been reported at least four times since 2023, and most were later reclassified as closer, dustier objects.",
        confidenceScore: 66,
        status: "unverified",
        sources: ["Reddit r/space megathread"],
      },
    ],
  },
  {
    id: "fc2",
    projectId: "p3",
    projectTitle: projectTitle("p3"),
    channelId: projectChannel("p3"),
    overallConfidence: 58,
    claims: [
      {
        id: "fc2-c1",
        claimText: "The Fed has cut rates by more than 0.25% in a single meeting only three times since 2008.",
        confidenceScore: 72,
        status: "verified",
        sources: ["Federal Reserve FOMC historical statements", "Bloomberg terminal data export"],
      },
      {
        id: "fc2-c2",
        claimText: "This indicator has preceded every major Fed policy reversal since the 2008 financial crisis.",
        confidenceScore: 45,
        status: "flagged",
        sources: ["Independent macro research newsletter, 2026"],
        note: "Causal claim overstates the indicator's track record — it coincided with 3 of the last 4 reversals, not all of them. Soften to 'most major reversals' in-script.",
      },
      {
        id: "fc2-c3",
        claimText: "Market pricing currently implies an 80% probability of a rate cut at the next meeting.",
        confidenceScore: 61,
        status: "outdated",
        sources: ["CME FedWatch Tool snapshot, taken 9 days ago"],
        note: "Futures-implied probability shifts daily; this figure is over a week old and should be re-pulled the morning of publish.",
      },
      {
        id: "fc2-c4",
        claimText: "Consumer inflation expectations have not moved meaningfully across the last two reporting periods.",
        confidenceScore: 55,
        status: "unverified",
        sources: ["Analyst commentary, unlinked"],
        note: "No primary source cited yet — needs a direct link to a University of Michigan or NY Fed survey release before this airs.",
      },
    ],
  },
  {
    id: "fc3",
    projectId: "p4",
    projectTitle: projectTitle("p4"),
    channelId: projectChannel("p4"),
    overallConfidence: 91,
    claims: [
      {
        id: "fc3-c1",
        claimText: "The Antikythera mechanism contains at least 30 bronze gears and dates to approximately 150-100 BC.",
        confidenceScore: 93,
        status: "verified",
        sources: ["Antikythera Mechanism Research Project, Nature", "Scientific American feature, 2021"],
      },
      {
        id: "fc3-c2",
        claimText: "The device could predict solar and lunar eclipses years in advance using the Saros cycle.",
        confidenceScore: 89,
        status: "verified",
        sources: ["Antikythera Mechanism Research Project technical papers"],
      },
      {
        id: "fc3-c3",
        claimText: "It is definitively the world's first analog computer, a title with no reasonable competitor.",
        confidenceScore: 65,
        status: "flagged",
        sources: ["Popular history blog post"],
        note: "'First computer' framing is a common simplification. Historians debate whether comparable devices existed and were lost to time, so 'earliest known' is the more defensible phrasing.",
      },
      {
        id: "fc3-c4",
        claimText: "The mechanism was recovered from a Roman-era shipwreck near the island of Antikythera in 1901.",
        confidenceScore: 96,
        status: "verified",
        sources: ["Hellenic Ministry of Culture archival record"],
      },
    ],
  },
  {
    id: "fc4",
    projectId: "p5",
    projectTitle: projectTitle("p5"),
    channelId: projectChannel("p5"),
    overallConfidence: 68,
    claims: [
      {
        id: "fc4-c1",
        claimText: "Brain cells can shrink by up to 60% during deep sleep to allow cerebrospinal fluid to flush out metabolic waste.",
        confidenceScore: 80,
        status: "verified",
        sources: ["Nedergaard Lab, University of Rochester study", "Science journal, foundational 2013 study"],
      },
      {
        id: "fc4-c2",
        claimText: "This waste-clearance process is called the glymphatic system.",
        confidenceScore: 92,
        status: "verified",
        sources: ["National Institutes of Health overview"],
      },
      {
        id: "fc4-c3",
        claimText: "Missing one night of sleep permanently reduces the brain's ability to clear waste.",
        confidenceScore: 38,
        status: "flagged",
        sources: ["Wellness blog summary"],
        note: "Overstated — evidence supports temporary impairment from acute sleep loss, not a permanent reduction. The underlying animal-study result is being generalized too broadly here.",
      },
      {
        id: "fc4-c4",
        claimText: "This mechanism has been directly observed and measured in living human brains, not just animal models.",
        confidenceScore: 54,
        status: "outdated",
        sources: ["Conference abstract, 2015"],
        note: "Human-brain evidence has since been strengthened by newer imaging studies; this source predates the more recent human MRI confirmation and should be swapped before publish.",
      },
    ],
  },
  {
    id: "fc5",
    projectId: "p7",
    projectTitle: projectTitle("p7"),
    channelId: projectChannel("p7"),
    overallConfidence: 47,
    claims: [
      {
        id: "fc5-c1",
        claimText: "The open-source model matched or exceeded GPT-4 on standard reasoning and coding benchmarks reported by the developing team.",
        confidenceScore: 70,
        status: "verified",
        sources: ["Model technical report, arXiv preprint"],
      },
      {
        id: "fc5-c2",
        claimText: "Independent third-party evaluators reproduced the reported benchmark scores within a small margin.",
        confidenceScore: 62,
        status: "verified",
        sources: ["Independent community-run eval leaderboard"],
      },
      {
        id: "fc5-c3",
        claimText: "The model was trained entirely for free with no significant compute cost.",
        confidenceScore: 28,
        status: "flagged",
        sources: ["Social media summary thread"],
        note: "Misleading — 'free' refers to the released weights being free to download, not the training cost, which industry estimates put in the millions of dollars.",
      },
      {
        id: "fc5-c4",
        claimText: "This model now outperforms every closed-source frontier model on every task.",
        confidenceScore: 24,
        status: "flagged",
        sources: ["Viral social post, unverified account"],
        note: "Overreaching — the model leads on a subset of published benchmarks but trails closed-source leaders on several others, including long-context and multimodal tasks.",
      },
      {
        id: "fc5-c5",
        claimText: "The benchmark results referenced in coverage are from the model's initial release version.",
        confidenceScore: 50,
        status: "unverified",
        sources: ["GitHub repository README"],
      },
    ],
  },
];

export function factCheckByProjectId(projectId?: string) {
  if (!projectId) return undefined;
  return factCheckReports.find((r) => r.projectId === projectId);
}

export const CLAIM_STATUSES: ClaimStatus[] = ["verified", "flagged", "outdated", "unverified"];
