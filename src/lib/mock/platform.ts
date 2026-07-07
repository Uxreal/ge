import { projects } from "./data";

export interface YoutubeMeta {
  title: string;
  description: string;
  tags: string[];
  thumbnailConcept: string;
  primaryKeyword: string;
}

export interface TiktokMeta {
  caption: string;
  hashtags: string[];
  hookNote: string;
  trendingAudio: string;
}

export interface InstagramMeta {
  caption: string;
  hashtags: string[];
  coverConcept: string;
  audienceNote: string;
}

export interface PlatformOptimizerFixture {
  id: string;
  projectId: string;
  youtube: YoutubeMeta;
  tiktok: TiktokMeta;
  instagram: InstagramMeta;
}

export const platformFixtures: PlatformOptimizerFixture[] = [
  {
    id: "po1",
    projectId: "p1",
    youtube: {
      title: "The Webb Telescope Just Found Something That Shouldn't Exist",
      description:
        "A newly confirmed galaxy is billions of years older than our models say the universe allows for. In this video we break down what JWST actually saw, why cosmologists are scrambling to explain it, and what it means for the Big Bang timeline as we know it.\n\nTimestamps and sources in the pinned comment. Subscribe for weekly deep dives into the discoveries rewriting science.",
      tags: ["james webb telescope", "jwst discovery", "early universe", "cosmology explained", "space news 2026", "astrophysics", "big bang theory"],
      thumbnailConcept: "Split frame: left half a familiar Hubble deep-field image, right half the same field rendered in stark JWST infrared with a glowing red circle around the anomalous galaxy; bold white outline text reads 'IMPOSSIBLE'.",
      primaryKeyword: "james webb telescope discovery",
    },
    tiktok: {
      caption: "this galaxy is older than the universe is supposed to be 😳 scientists have no idea what to do with this",
      hashtags: ["#space", "#jwst", "#science", "#universe", "#learnontiktok", "#spacetok"],
      hookNote: "Open on the anomaly stat as on-screen text before any narration — first 1.5s must contain the number, not a wind-up.",
      trendingAudio: "Suspenseful riser — 'Unraveling' by Kessoku Sound (trending in science/edu niche)",
    },
    instagram: {
      caption: "Astronomers are calling this the discovery that breaks the timeline. Full explainer in the Reel — save this for your next argument about how old the universe actually is. 🔭✨",
      hashtags: ["#space", "#jwst", "#astronomy", "#science", "#universe", "#reelsinstagram"],
      coverConcept: "Deep-space infrared frame cropped tight and vertical, high contrast, with a small 'WATCH' badge in the corner to read well as a static cover.",
      audienceNote: "Cosmos Unfolded's IG audience skews slightly older and more caption-first than TikTok — lead with the implication, not the meme energy.",
    },
  },
  {
    id: "po2",
    projectId: "p2",
    youtube: {
      title: "The 5-Second Rule That Rewires Procrastination (Backed by Neuroscience)",
      description:
        "Your brain has a tiny window to act before your prefrontal cortex talks you out of it. In this video I break down the neuroscience behind the 5-second rule, show it live with an on-screen countdown, and give you three ways to apply it today.\n\nIf you've ever said 'I'll do it later' and didn't — this one's for you.",
      tags: ["procrastination", "5 second rule", "productivity hack", "neuroscience", "mel robbins", "habit change", "focus tips"],
      thumbnailConcept: "Extreme close-up of a hand hovering over a phone with a giant glowing countdown '5-4-3' overlay and a shocked expression reaction bubble in the corner.",
      primaryKeyword: "5 second rule procrastination",
    },
    tiktok: {
      caption: "count backwards from 5 and just GO. this is the only productivity hack that's ever actually worked on me",
      hashtags: ["#productivity", "#procrastination", "#mindset", "#5secondrule", "#studytok", "#mentalhealth"],
      hookNote: "Start mid-demo — show the countdown already happening, then rewind to explain. Don't open with 'let me tell you about'.",
      trendingAudio: "Low-fi ticking clock loop, trending sound 'Decision' (used across productivity niche)",
    },
    instagram: {
      caption: "The 5-second window your brain gives you before it talks you out of anything. Swipe-worthy carousel version coming Friday — save this Reel so you don't lose it. ⏱️",
      hashtags: ["#productivitytips", "#procrastination", "#mindsetshift", "#focus", "#reels"],
      coverConcept: "Clean minimal cover: a single bold '5' on a soft gradient background, no clutter, readable at thumbnail size in a grid feed.",
      audienceNote: "Mind Hacks Daily's IG followers respond best to save-and-reference formats — frame the caption as a tool they'll come back to.",
    },
  },
  {
    id: "po3",
    projectId: "p3",
    youtube: {
      title: "Why the Fed's Next Move Will Catch Everyone Off Guard",
      description:
        "Everyone's watching the wrong number. In this breakdown I walk through the one chart that's predicted every rate decision since 2008, why the consensus take is missing it this time, and what it means for your portfolio in the next quarter.\n\nNot financial advice — just the pattern nobody's talking about.",
      tags: ["federal reserve", "interest rates 2026", "fed meeting", "economy explained", "investing", "inflation", "market predictions"],
      thumbnailConcept: "Jerome Powell photo (news-use still) next to a red arrow chart spiking unexpectedly, with bold yellow text 'NOBODY SAW THIS COMING'.",
      primaryKeyword: "fed rate decision 2026",
    },
    tiktok: {
      caption: "the fed is about to do something nobody is pricing in. here's the chart that calls it every time",
      hashtags: ["#fed", "#economy", "#investing", "#finance", "#interestrates", "#stocktok"],
      hookNote: "Lead with the contrarian claim as bold on-screen text within the first frame — finance TikTok scrolls fast, the hook has to be the thesis, not a tease.",
      trendingAudio: "Tense trading-floor ambience cut, trending in finance/business niche",
    },
    instagram: {
      caption: "One chart has called every Fed move since 2008 — and it's flashing something nobody's talking about. Full breakdown in the Reel. 📉",
      hashtags: ["#finance", "#fed", "#economy", "#investing", "#markets"],
      coverConcept: "The recurring chart graphic rendered clean and vertical with a single highlighted data point circled, brand color overlay for consistency across Money Minute's grid.",
      audienceNote: "Money Minute's IG audience wants the takeaway up front — caption should state the implication before the hook, unlike the more curiosity-driven TikTok version.",
    },
  },
  {
    id: "po4",
    projectId: "p4",
    youtube: {
      title: "This 2,000-Year-Old Roman Machine Predicted Modern AI",
      description:
        "Long before silicon chips, the Antikythera mechanism was quietly doing something no one expected from the ancient world: modeling complex systems to predict outcomes. In this video I draw the direct line between this Roman-era device and the neural networks powering AI today.\n\nHistory nerds and AI nerds — this one's for both of you.",
      tags: ["antikythera mechanism", "ancient history", "history of ai", "roman engineering", "ancient technology", "history documentary", "artificial intelligence history"],
      thumbnailConcept: "The corroded bronze Antikythera gears on the left morphing via a glowing seam into a modern neural-network diagram on the right, same circular composition on both halves.",
      primaryKeyword: "antikythera mechanism ai",
    },
    tiktok: {
      caption: "the romans built a computer 2,000 years before computers existed and almost nobody knows this",
      hashtags: ["#history", "#ancienthistory", "#ai", "#historytok", "#didyouknow", "#rome"],
      hookNote: "Show the actual artifact first — TikTok history audiences reward 'proof it's real' framing over narration-first openings.",
      trendingAudio: "Ancient/orchestral hybrid trending cue, 'Old World' (used in history-documentary shorts)",
    },
    instagram: {
      caption: "Rome had a machine that modeled the future 2,000 years before AI existed. The full story — and why it matters now — is in the Reel. 🏺⚙️",
      hashtags: ["#history", "#ancienthistory", "#technology", "#ai", "#rome"],
      coverConcept: "Museum-lit macro shot of the bronze gear fragments against black, single soft spotlight, italic serif title card overlay for a premium-documentary feel.",
      audienceNote: "History Unlocked's IG following skews toward museum/documentary aesthetics — cover and caption should read as prestige, not clickbait.",
    },
  },
  {
    id: "po5",
    projectId: "p5",
    youtube: {
      title: "The Weirdest Thing Your Brain Does Every Night While You're Asleep",
      description:
        "Your brain isn't resting when you sleep — it's running a background process most people have never heard of. In this video I break down the specific nightly brain activity, why it evolved, and what it means for memory and mood the next day.\n\nWatch till the end for the one thing you can do tonight to improve it.",
      tags: ["sleep facts", "brain facts", "neuroscience", "sleep science", "weird facts", "how the brain works", "sleep tips"],
      thumbnailConcept: "Side profile silhouette of a sleeping head with a glowing brain outline showing visible 'activity waves', deep blue night palette, bold text 'WHILE YOU SLEEP'.",
      primaryKeyword: "what your brain does while you sleep",
    },
    tiktok: {
      caption: "your brain does THIS every single night and you had no idea. wait for the part at 0:14",
      hashtags: ["#brainfacts", "#sleep", "#didyouknow", "#weirdfacts", "#learnontiktok", "#psychology"],
      hookNote: "Use the 'wait for it' timestamp callout in the caption — this format consistently outperforms plain statements for Weird Facts Lab's audience.",
      trendingAudio: "Eerie lullaby-glitch hybrid, trending sound 'Nightshift' (facts/mystery niche)",
    },
    instagram: {
      caption: "Your brain runs a secret process every night — most people have never heard of it. Full explainer in the Reel, save it for your next 2am scroll. 🌙🧠",
      hashtags: ["#brainfacts", "#sleepscience", "#didyouknow", "#weirdfacts", "#reels"],
      coverConcept: "Moody dark-blue cover, a single closed eye lit from below with faint neural-pattern overlay, minimal text so it reads at small grid size.",
      audienceNote: "Weird Facts Lab's IG saves outperform likes — caption should explicitly invite a save, not just a watch.",
    },
  },
  {
    id: "po6",
    projectId: "p6",
    youtube: {
      title: "Why Time Feels Like It Slows Down When You're Scared",
      description:
        "That 'everything moved in slow motion' feeling during a scary moment isn't your imagination — it's a real perceptual effect, and neuroscientists finally understand why. This video breaks down the brain mechanism behind fear-based time dilation, with real case examples.\n\nSubscribe for more of the brain science behind everyday feelings.",
      tags: ["fear response", "brain science", "time perception", "psychology facts", "adrenaline", "how the brain works", "neuroscience explained"],
      thumbnailConcept: "A clock face visibly warping/melting mid-frame behind a wide-eyed startled expression, motion-blur streaks radiating outward, high-contrast red and black palette.",
      primaryKeyword: "why time slows down when scared",
    },
    tiktok: {
      caption: "time doesn't actually slow down when you're scared — your brain just does THIS instead",
      hashtags: ["#psychology", "#brainfacts", "#fear", "#neuroscience", "#mindblown", "#learnontiktok"],
      hookNote: "Open with the myth-busting contradiction ('it's not what you think') — this channel's top performers all open on a correction, not a fact.",
      trendingAudio: "Glitchy time-stretch sound effect into ambient synth, trending 'Slow-Mo' cue",
    },
    instagram: {
      caption: "That slow-motion feeling during a scary moment? Your brain is doing something specific — not what you'd guess. Full breakdown in the Reel. ⏳😨",
      hashtags: ["#psychology", "#brainfacts", "#neuroscience", "#mindblown", "#reels"],
      coverConcept: "Warped clock-face graphic in deep red on black, single bold word 'WHY?' centered, consistent with the channel's high-contrast cover style.",
      audienceNote: "This channel's IG audience cross-visits from TikTok — keep the myth-busting angle intact rather than softening it for IG.",
    },
  },
];

export function platformFixtureByProjectId(projectId?: string) {
  if (!projectId) return undefined;
  return platformFixtures.find((f) => f.projectId === projectId);
}

export const platformOptimizerProjects = projects.filter((p) =>
  platformFixtures.some((f) => f.projectId === p.id),
);
