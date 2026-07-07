import { ideas, projects } from "./data";

export type ScriptDurationLabel = "15s" | "30s" | "45s" | "60s" | "90s";

export interface ScriptVariant {
  durationLabel: ScriptDurationLabel;
  hook: string;
  body: string; // multi-paragraph, separated by blank lines
  wordCount: number;
  retentionTechniques: string[];
  cta: string;
}

export interface AltHook {
  text: string;
  score: number; // 0-100 mocked A/B test score
}

export interface Script {
  id: string;
  ideaOrProjectTitle: string;
  projectId?: string;
  ideaId?: string;
  channelId?: string;
  variants: ScriptVariant[];
  altHooks: AltHook[];
  originalityScore: number; // 0-100
  pacingScore: number; // 0-100
}

export const scripts: Script[] = [
  {
    id: "sc1",
    ideaOrProjectTitle: projects[0].title, // "Webb telescope just broke physics"
    projectId: "p1",
    ideaId: "i1",
    channelId: "ch1",
    originalityScore: 74,
    pacingScore: 88,
    altHooks: [
      { text: "This shouldn't exist. But it does.", score: 88 },
      { text: "The universe just got 2 billion years more confusing.", score: 81 },
      { text: "NASA didn't expect this. Nobody did.", score: 74 },
      { text: "Every textbook about the early universe just got a footnote.", score: 69 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Scientists thought this was impossible. Then Webb saw it.",
        body: "The James Webb Space Telescope just spotted a fully-formed galaxy that's older than the universe is supposed to allow it to be. Not younger — older. Astronomers are calling it impossible, and they're not being dramatic.",
        wordCount: 40,
        retentionTechniques: ["curiosity gap", "pattern interrupt"],
        cta: "Follow for the explanation — it gets weirder.",
      },
      {
        durationLabel: "30s",
        hook: "Scientists thought this was impossible. Then Webb saw it.",
        body: "The James Webb Space Telescope just found a galaxy that shouldn't exist yet. Based on how old the universe is, this galaxy looks fully formed way too early — like finding a finished house before the construction crew even arrived.\n\nAstronomers ran the numbers three times, assuming it was an error. It wasn't.",
        wordCount: 68,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "specificity"],
        cta: "Comment 'source' and I'll drop the paper link.",
      },
      {
        durationLabel: "45s",
        hook: "Scientists thought this was impossible. Then Webb saw it.",
        body: "The James Webb Space Telescope just found a galaxy that, by every model we currently trust, shouldn't exist yet. The universe is about 13.8 billion years old — and this galaxy looks fully mature far earlier than any theory allows for stars, dust, and structure to form.\n\nAstronomers double- and triple-checked the redshift data because the first read looked like an instrument error. It wasn't. The measurements held.\n\nIf this survives peer review, it doesn't just tweak one number — it means our timeline for how galaxies form in the early universe needs a rewrite.",
        wordCount: 108,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "specificity", "open loop"],
        cta: "Comment 'source' and I'll drop the paper link below.",
      },
      {
        durationLabel: "60s",
        hook: "Scientists thought this was impossible. Then Webb saw it.",
        body: "The James Webb Space Telescope just found a galaxy that, by every model astronomers currently trust, shouldn't be there. The universe is roughly 13.8 billion years old, and this galaxy already looks mature — structured, dusty, full of older stars — at a point in cosmic history when things were supposed to still be forming.\n\nThe team behind the discovery ran the redshift measurements three separate times, assuming the first result was an instrument glitch. It wasn't. The data held up every time they checked it.\n\nHere's why that matters: our whole model of the early universe assumes galaxies need time to build up mass and structure. If they can form this fast, this early, a lot of textbook cosmology needs a second look — and this isn't the only object like it that's shown up in Webb's data.",
        wordCount: 152,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "specificity", "open loop"],
        cta: "Subscribe — the follow-up on what this means for the Big Bang timeline drops Thursday.",
      },
      {
        durationLabel: "90s",
        hook: "Scientists thought this was impossible. Then Webb saw it.",
        body: "The James Webb Space Telescope just found a galaxy that, according to every cosmological model astronomers currently trust, shouldn't be there — at least not yet. The universe is about 13.8 billion years old, and this galaxy already looks mature: structured, dusty, full of aging stars, sitting at a point in cosmic history when things were supposed to still be assembling themselves.\n\nThe research team ran the redshift measurements three separate times, fully expecting to find an instrument error. They didn't find one. Every pass came back with the same impossible number.\n\nPart of why we're even seeing this now is that Webb's infrared instruments can look further back in time than anything before it — which means the early universe is turning out to be a lot busier, and a lot more mature, than the standard model predicted. If more galaxies like this one show up, it's not a footnote. It's a rewrite of how fast the universe learned to build things.\n\nEither way, the paper is under peer review right now, and every astronomer in this space is watching it closely.",
        wordCount: 216,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "specificity", "open loop", "callback"],
        cta: "Hit subscribe and turn on notifications — I'll cover the peer review verdict the moment it lands.",
      },
    ],
  },
  {
    id: "sc2",
    ideaOrProjectTitle: projects[1].title, // "The 5-second procrastination hack"
    projectId: "p2",
    ideaId: "i2",
    channelId: "ch2",
    originalityScore: 55,
    pacingScore: 91,
    altHooks: [
      { text: "Count to 5. That's the whole hack.", score: 85 },
      { text: "This works because of one weird brain glitch.", score: 79 },
      { text: "You have 5 seconds to act before your brain talks you out of it.", score: 76 },
      { text: "Procrastination isn't laziness. It's a timing bug.", score: 70 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Your brain has a 5-second window. Miss it and you're stuck.",
        body: "The moment you think of something you should do, you have about 5 seconds before your brain builds an excuse. Count backward from 5, then move before your body can talk you out of it. That's it. That's the whole hack.",
        wordCount: 42,
        retentionTechniques: ["pattern interrupt", "curiosity gap"],
        cta: "Save this — you'll need it in about 20 minutes.",
      },
      {
        durationLabel: "30s",
        hook: "Your brain has a 5-second window. Miss it and you're stuck.",
        body: "The second you think 'I should do that,' a timer starts. You've got about 5 seconds before your brain quietly builds an excuse to stay put — and once that excuse forms, you're done for.\n\nThe fix is almost embarrassingly simple: count backward, 5-4-3-2-1, and physically move on 1. No thinking. No negotiating. Just movement.",
        wordCount: 70,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity"],
        cta: "Try it right now on one thing you've been putting off.",
      },
      {
        durationLabel: "45s",
        hook: "Your brain has a 5-second window. Miss it and you're stuck.",
        body: "The second you think 'I should do that,' a timer starts. You have roughly 5 seconds before your brain quietly builds an excuse to stay exactly where you are — and once that excuse forms, the moment is gone.\n\nThe fix is almost embarrassingly simple: count backward, 5-4-3-2-1, and physically move the instant you hit 1. No thinking. No negotiating with yourself. Just movement.\n\nThe reason it works is that counting interrupts the part of your brain that talks you out of things before it gets a chance to build its case.",
        wordCount: 105,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop"],
        cta: "Try it right now on one thing you've been putting off.",
      },
      {
        durationLabel: "60s",
        hook: "Your brain has a 5-second window. Miss it and you're stuck.",
        body: "The second you think 'I should do that,' a timer starts. You have roughly 5 seconds before your brain quietly builds an excuse to stay exactly where you are — and once that excuse forms, the moment is gone, and you'll probably talk yourself out of it for the rest of the day.\n\nThe fix is almost embarrassingly simple: count backward, 5-4-3-2-1, and physically move the instant you hit 1. No thinking. No negotiating with yourself. Just movement, on the count.\n\nThe reason it works isn't magic — counting gives your brain a task simple enough that it can't simultaneously build a case for staying put. By the time the countdown ends, the window for hesitation has already closed, and you're already moving.",
        wordCount: 148,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop"],
        cta: "Try it on one thing right now, then comment how far you got.",
      },
      {
        durationLabel: "90s",
        hook: "Your brain has a 5-second window. Miss it and you're stuck.",
        body: "The second you think 'I should do that,' a timer starts. You have roughly 5 seconds before your brain quietly builds an excuse to stay exactly where you are — and once that excuse forms, the moment is gone, and you'll probably talk yourself out of it for the rest of the day.\n\nThe fix is almost embarrassingly simple: count backward, 5-4-3-2-1, and physically move the instant you hit 1. No thinking. No negotiating with yourself. Just movement, on the count.\n\nThe reason it works isn't magic. Counting gives your brain a task simple enough that it can't simultaneously build a case for staying put. By the time the countdown ends, the window for hesitation has already closed, and you're already moving — which matters, because motivation almost never shows up before action. It shows up after.\n\nUse it on the small stuff first: getting out of bed, opening the laptop, making the call. Once the counting becomes a reflex, you can point it at the bigger things you've been avoiding for weeks.",
        wordCount: 208,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop", "callback"],
        cta: "Subscribe for the follow-up on stacking this with your morning routine.",
      },
    ],
  },
  {
    id: "sc3",
    ideaOrProjectTitle: projects[2].title, // "The Fed pivot nobody sees coming"
    projectId: "p3",
    ideaId: "i3",
    channelId: "ch3",
    originalityScore: 60,
    pacingScore: 70,
    altHooks: [
      { text: "Everyone's watching the wrong number.", score: 77 },
      { text: "The Fed already told us. We just didn't listen.", score: 73 },
      { text: "This chart called the last three rate cuts perfectly.", score: 71 },
      { text: "Wall Street is bracing for the wrong outcome.", score: 64 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "This chart predicts every rate change since 2008.",
        body: "There's one chart that's called every major Fed decision since the 2008 crash — and right now it's pointing somewhere almost nobody on financial news is talking about.",
        wordCount: 30,
        retentionTechniques: ["curiosity gap", "open loop"],
        cta: "Follow before the next Fed meeting — this updates live.",
      },
      {
        durationLabel: "30s",
        hook: "This chart predicts every rate change since 2008.",
        body: "There's one chart that has correctly called every major Fed rate decision since the 2008 crash. It's not the unemployment number everyone quotes, and it's not inflation either.\n\nRight now, that chart is pointing somewhere almost nobody on financial news is talking about — and if it's right again, the next move surprises most of Wall Street.",
        wordCount: 64,
        retentionTechniques: ["curiosity gap", "open loop", "contrast"],
        cta: "Comment 'chart' and I'll break down exactly what it's showing.",
      },
      {
        durationLabel: "45s",
        hook: "This chart predicts every rate change since 2008.",
        body: "There's one chart that has correctly called every major Fed rate decision since the 2008 financial crash. It isn't the unemployment number everyone quotes on the news, and it isn't the headline inflation print either — it's a slower-moving indicator most retail investors never look at.\n\nRight now, that chart is pointing somewhere almost nobody in financial media is talking about.\n\nIf history holds, the next move catches most of Wall Street off guard — and the traders who've been quietly positioning around this indicator for months are the ones who won't be surprised.",
        wordCount: 100,
        retentionTechniques: ["curiosity gap", "open loop", "contrast", "specificity"],
        cta: "Comment 'chart' and I'll break down exactly what it's showing.",
      },
      {
        durationLabel: "60s",
        hook: "This chart predicts every rate change since 2008.",
        body: "There's one chart that has correctly called every major Fed rate decision since the 2008 financial crash. It isn't the unemployment number everyone quotes on the news, and it isn't the headline inflation print either — it's a slower-moving indicator most retail investors never look at.\n\nRight now, that chart is pointing somewhere almost nobody in financial media is talking about, which is exactly what happened right before the last two surprise moves.\n\nIf history holds, the next decision catches most of Wall Street off guard — and the traders who've quietly positioned around this indicator for months won't be the ones scrambling when the announcement hits.\n\nI'm not telling you to trade on this. I'm telling you to watch it, because the headlines are about to lag behind the chart again.",
        wordCount: 140,
        retentionTechniques: ["curiosity gap", "open loop", "contrast", "specificity"],
        cta: "Subscribe — I'll post the chart update the morning of the next Fed meeting.",
      },
      {
        durationLabel: "90s",
        hook: "This chart predicts every rate change since 2008.",
        body: "There's one chart that has correctly called every major Fed rate decision since the 2008 financial crash. It isn't the unemployment number everyone quotes on the news, and it isn't the headline inflation print either — it's a slower-moving indicator most retail investors never bother pulling up.\n\nRight now, that chart is pointing somewhere almost nobody in financial media is talking about, which is exactly what happened in the run-up to the last two surprise moves. Both times, the headlines caught up about six weeks late.\n\nIf history holds, the next decision catches most of Wall Street off guard — and the traders who've quietly positioned around this indicator for months won't be the ones scrambling when the announcement hits. That gap between what the chart shows and what the headlines say is where the actual opportunity lives.\n\nI'm not telling you to trade on this alone. I'm telling you to watch it alongside everything else, because right now the chart is about three weeks ahead of the conversation everyone else is having.",
        wordCount: 198,
        retentionTechniques: ["curiosity gap", "open loop", "contrast", "specificity", "callback"],
        cta: "Subscribe and turn on notifications — the chart update posts the morning of the next Fed meeting.",
      },
    ],
  },
  {
    id: "sc4",
    ideaOrProjectTitle: projects[3].title, // "Rome's algorithm: history's first AI"
    projectId: "p4",
    ideaId: "i4",
    channelId: "ch4",
    originalityScore: 82,
    pacingScore: 75,
    altHooks: [
      { text: "This 2,000-year-old machine 'thinks'.", score: 80 },
      { text: "Rome had a computer. Nobody believes it.", score: 78 },
      { text: "Archaeologists found a computer built before Christ.", score: 72 },
      { text: "This bronze device does math a laptop would respect.", score: 66 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "One device. One idea. The blueprint of every AI.",
        body: "Two thousand years ago, someone in ancient Greece built a bronze device that could predict eclipses years in advance using nothing but gears. Historians call it the Antikythera mechanism. It's the ancestor of every calculating machine you've ever used.",
        wordCount: 40,
        retentionTechniques: ["pattern interrupt", "curiosity gap"],
        cta: "Follow — part 2 shows how it was actually decoded.",
      },
      {
        durationLabel: "30s",
        hook: "One device. One idea. The blueprint of every AI.",
        body: "Around 150 BC, someone in ancient Greece built a bronze device with over 30 interlocking gears that could predict solar eclipses years in advance. No electricity. No writing on a single chip. Just brass and geometry.\n\nIt's called the Antikythera mechanism, and it's the closest thing the ancient world had to a computer.",
        wordCount: 60,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity"],
        cta: "Comment 'gears' and I'll show you how the mechanism worked.",
      },
      {
        durationLabel: "45s",
        hook: "One device. One idea. The blueprint of every AI.",
        body: "Around 150 BC, someone in ancient Greece built a bronze device with more than 30 interlocking gears that could predict solar eclipses years ahead of time. No electricity, no writing on a chip — just brass, geometry, and an idea nobody expected from that era.\n\nIt's called the Antikythera mechanism, pulled from a shipwreck in 1901, and for decades nobody understood what it actually did.\n\nModern researchers used X-ray imaging to finally map every gear — and what they found looks less like an ancient curiosity and more like the first mechanical computer humans ever built.",
        wordCount: 100,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop"],
        cta: "Comment 'gears' and I'll show you exactly how the mechanism worked.",
      },
      {
        durationLabel: "60s",
        hook: "One device. One idea. The blueprint of every AI.",
        body: "Around 150 BC, someone in ancient Greece built a bronze device with more than 30 interlocking gears that could predict solar eclipses years ahead of time. No electricity, no writing on a chip — just brass, geometry, and an idea nobody expected to see from that era.\n\nIt's called the Antikythera mechanism, pulled corroded and unrecognizable from a shipwreck in 1901. For most of the 20th century, nobody could agree on what it actually did.\n\nThen modern researchers used X-ray imaging to map every gear tooth hidden inside the corrosion — and the mechanism they reconstructed doesn't just track eclipses. It runs multiple astronomical cycles at once, automatically, the same way a modern calculating machine chains operations together.\n\nWe like to think of computation as a modern invention. This thing says otherwise.",
        wordCount: 148,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop", "contrast"],
        cta: "Subscribe — next episode reconstructs the mechanism gear by gear.",
      },
      {
        durationLabel: "90s",
        hook: "One device. One idea. The blueprint of every AI.",
        body: "Around 150 BC, someone in ancient Greece built a bronze device with more than 30 interlocking gears that could predict solar eclipses years ahead of time. No electricity, no writing on a chip — just brass, geometry, and an idea nobody expected to see from that era.\n\nIt's called the Antikythera mechanism, pulled corroded and unrecognizable from a Roman-era shipwreck in 1901. For most of the 20th century, historians couldn't agree on what it actually did — some assumed it was decorative.\n\nThen modern researchers used X-ray imaging to map every gear tooth hidden inside the corrosion. What they reconstructed doesn't just track a single eclipse cycle — it runs several overlapping astronomical calculations at once, automatically, chaining mechanical steps together the same way a modern calculating machine chains logical operations.\n\nThat's the part that keeps historians of technology up at night: we tend to treat computation as something invented in the last century. This device suggests the idea — turning a physical process into a predictable, repeatable calculation — is over two thousand years older than that.",
        wordCount: 210,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "specificity", "open loop", "contrast", "callback"],
        cta: "Subscribe and turn on notifications — next episode reconstructs the mechanism gear by gear.",
      },
    ],
  },
  {
    id: "sc5",
    ideaOrProjectTitle: projects[4].title, // "The weirdest thing your brain does asleep"
    projectId: "p5",
    ideaId: "i5",
    channelId: "ch5",
    originalityScore: 58,
    pacingScore: 93,
    altHooks: [
      { text: "You have no idea what your brain does at 3am.", score: 83 },
      { text: "This happens in your skull every single night.", score: 78 },
      { text: "Your brain is louder asleep than awake. Here's proof.", score: 75 },
      { text: "Nobody tells you this happens while you sleep.", score: 68 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Wait — your brain is doing THIS every night?",
        body: "While you sleep, your brain shrinks its own cells by up to 60% to flush out waste and toxins that build up while you're awake. It's basically running a nightly cleaning cycle — and you have zero control over it.",
        wordCount: 41,
        retentionTechniques: ["curiosity gap", "specificity"],
        cta: "Follow for the part about what happens if you skip it.",
      },
      {
        durationLabel: "30s",
        hook: "Wait — your brain is doing THIS every night?",
        body: "While you're asleep, your brain cells shrink by as much as 60% to make room for fluid that flushes out waste and toxins built up during the day. Scientists call it the glymphatic system, and it basically turns your skull into a nightly car wash.\n\nMiss enough sleep, and that cleanup never fully finishes.",
        wordCount: 62,
        retentionTechniques: ["curiosity gap", "specificity", "pattern interrupt"],
        cta: "Comment 'sleep' and I'll show you what happens when it's skipped.",
      },
      {
        durationLabel: "45s",
        hook: "Wait — your brain is doing THIS every night?",
        body: "While you're asleep, your brain cells actually shrink by as much as 60% to make room for fluid that flushes out waste and toxins that built up while you were awake. Scientists call it the glymphatic system — it basically turns your skull into a nightly car wash.\n\nThis only really kicks into gear during deep sleep, which is why a restless night doesn't just leave you tired.\n\nMiss enough of it, and that cleanup cycle never fully finishes — and some of what doesn't get cleared has been linked to long-term brain health.",
        wordCount: 100,
        retentionTechniques: ["curiosity gap", "specificity", "pattern interrupt", "open loop"],
        cta: "Comment 'sleep' and I'll show you what happens when it's skipped.",
      },
      {
        durationLabel: "60s",
        hook: "Wait — your brain is doing THIS every night?",
        body: "While you're asleep, your brain cells actually shrink by as much as 60% to make room for fluid that flushes out waste and toxins that built up while you were awake. Scientists call it the glymphatic system — it basically turns your skull into a nightly car wash.\n\nThis process only really kicks into high gear during deep sleep, which is why a restless night doesn't just leave you groggy — it leaves the cleanup unfinished.\n\nAnd this isn't a minor detail. Some of what doesn't get cleared out on those nights has been linked to long-term brain health, which is one reason researchers are now so focused on deep sleep specifically, not just total hours in bed.\n\nSo the next time you say you'll 'catch up' on sleep this weekend — your brain's cleaning crew doesn't really work that way.",
        wordCount: 148,
        retentionTechniques: ["curiosity gap", "specificity", "pattern interrupt", "open loop"],
        cta: "Subscribe — tomorrow's video breaks down how to actually protect deep sleep.",
      },
      {
        durationLabel: "90s",
        hook: "Wait — your brain is doing THIS every night?",
        body: "While you're asleep, your brain cells actually shrink by as much as 60% to make room for fluid that flushes out waste and toxins that built up while you were awake. Scientists call it the glymphatic system — it basically turns your skull into a nightly car wash, running while you're completely unconscious.\n\nThis process only really kicks into high gear during deep sleep, which is why a restless night doesn't just leave you groggy — it leaves the cleanup unfinished, night after night, quietly stacking up.\n\nAnd this isn't a minor detail. Some of what doesn't get cleared out on those nights has been linked to long-term brain health, which is exactly why researchers have shifted focus from 'how many hours did you sleep' to 'how much of that was actually deep sleep.'\n\nSo the next time you say you'll catch up on sleep this weekend, know that your brain's cleaning crew doesn't really work in bulk like that — it wants consistency, not a big Saturday nap making up for five short nights.",
        wordCount: 205,
        retentionTechniques: ["curiosity gap", "specificity", "pattern interrupt", "open loop", "callback"],
        cta: "Subscribe and turn on notifications for tomorrow's deep-sleep protocol video.",
      },
    ],
  },
  {
    id: "sc6",
    ideaOrProjectTitle: projects[5].title, // "Why time slows when you're scared"
    projectId: "p6",
    channelId: "ch2",
    originalityScore: 66,
    pacingScore: 84,
    altHooks: [
      { text: "Slow motion isn't a movie effect. Your brain does it too.", score: 80 },
      { text: "Fear doesn't slow time down. It speeds your brain up.", score: 77 },
      { text: "That car-crash slow-motion feeling is real. Here's why.", score: 72 },
      { text: "Your brain has a panic button that rewrites your clock.", score: 65 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Your brain has a slow-motion button. Fear presses it.",
        body: "In a real emergency, time doesn't actually slow down — your brain just starts recording more detail per second, so when you replay the memory later, it feels like it lasted longer than it did.",
        wordCount: 37,
        retentionTechniques: ["pattern interrupt", "curiosity gap"],
        cta: "Follow for the story that proves it.",
      },
      {
        durationLabel: "30s",
        hook: "Your brain has a slow-motion button. Fear presses it.",
        body: "That 'everything went slow-motion' feeling people describe during accidents isn't time actually slowing down. Your amygdala floods your brain with adrenaline, and your memory-forming regions start recording far more detail per second than normal.\n\nWhen you replay that memory later, more detail feels like more time — so the moment seems stretched out, even though the clock never changed.",
        wordCount: 65,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "contrast"],
        cta: "Comment if you've ever felt this during a close call.",
      },
      {
        durationLabel: "45s",
        hook: "Your brain has a slow-motion button. Fear presses it.",
        body: "That 'everything went slow-motion' feeling people describe during accidents isn't time actually slowing down. Your amygdala floods your system with adrenaline, and the parts of your brain responsible for forming memories start recording far more detail per second than normal.\n\nWhen you replay that memory later, more detail reads as more time — so the moment feels stretched out, even though the actual clock never changed.\n\nResearchers tested this with free-fall experiments and digital displays flashing too fast to normally read. People in genuine fear still couldn't read faster. They just remembered the fall as feeling longer.",
        wordCount: 105,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "contrast", "specificity"],
        cta: "Comment if you've ever felt this during a close call.",
      },
      {
        durationLabel: "60s",
        hook: "Your brain has a slow-motion button. Fear presses it.",
        body: "That 'everything went slow-motion' feeling people describe during accidents isn't time actually slowing down. Your amygdala floods your system with adrenaline, and the parts of your brain responsible for forming memories start recording far more detail per second than normal.\n\nWhen you replay that memory later, more detail reads as more time — so the moment feels stretched out, even though the actual clock never changed for anyone watching from outside.\n\nResearchers actually tested this directly, using free-fall drops and digital displays flashing numbers too fast to normally read. People in genuine fear still couldn't read faster in the moment — their real-time perception wasn't sped up at all.\n\nWhat changed was memory. Afterward, they remembered the fall as feeling noticeably longer than it actually was.",
        wordCount: 148,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "contrast", "specificity"],
        cta: "Subscribe — next video covers how athletes train this effect on purpose.",
      },
      {
        durationLabel: "90s",
        hook: "Your brain has a slow-motion button. Fear presses it.",
        body: "That 'everything went slow-motion' feeling people describe during accidents isn't time actually slowing down. Your amygdala floods your system with adrenaline, and the parts of your brain responsible for forming memories start recording far more detail per second than normal.\n\nWhen you replay that memory later, more detail reads as more time — so the moment feels stretched out, even though the actual clock never changed for anyone watching from outside.\n\nResearchers tested this directly using free-fall drops and digital displays flashing numbers too fast to normally read. People in genuine fear still couldn't read faster in the moment — their real-time perception wasn't actually sped up at all.\n\nWhat changed was memory. Afterward, participants consistently remembered the fall as feeling noticeably longer than it really was, purely because their brain had laid down so much extra detail to work with.\n\nSo the slow-motion car crash you remember? It happened at normal speed. Your brain just took better notes than usual, and better notes feel like more time when you look back on them.",
        wordCount: 210,
        retentionTechniques: ["pattern interrupt", "curiosity gap", "contrast", "specificity", "callback"],
        cta: "Subscribe and turn on notifications for the athlete-training follow-up.",
      },
    ],
  },
  {
    id: "sc7",
    ideaOrProjectTitle: projects[6].title, // "A free model just humiliated a $10B AI lab"
    projectId: "p7",
    ideaId: "i6",
    channelId: "ch6",
    originalityScore: 66,
    pacingScore: 80,
    altHooks: [
      { text: "The billion-dollar labs didn't see this coming.", score: 82 },
      { text: "This model shouldn't be this good.", score: 78 },
      { text: "Free just beat a $10 billion research budget.", score: 74 },
      { text: "Nobody expected the open-source model to win this.", score: 67 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "It cost $0 to train. It's beating GPT-4.",
        body: "A free, open-source model just matched or beat GPT-4 on every major benchmark — reasoning, coding, math, all of it. No subscription, no API key, no billion-dollar lab behind it. Just a team that published their weights for anyone to download.",
        wordCount: 44,
        retentionTechniques: ["contrast", "curiosity gap"],
        cta: "Follow for the benchmark breakdown.",
      },
      {
        durationLabel: "30s",
        hook: "It cost $0 to train. It's beating GPT-4.",
        body: "A free, open-source model just matched or beat GPT-4 across every major benchmark — reasoning, coding, math, all of it. No subscription. No API key. No ten-figure research budget behind it.\n\nA relatively small team published the full weights for anyone to download and run themselves, which is exactly why the big labs are suddenly a lot quieter this week.",
        wordCount: 66,
        retentionTechniques: ["contrast", "curiosity gap", "specificity"],
        cta: "Comment 'link' and I'll drop where to download it.",
      },
      {
        durationLabel: "45s",
        hook: "It cost $0 to train. It's beating GPT-4.",
        body: "A free, open-source model just matched or beat GPT-4 across every major benchmark — reasoning, coding, math, all of it. No subscription. No API key. No ten-figure research budget behind it.\n\nA relatively small team published the full model weights for anyone to download and run on their own hardware, which is exactly why the billion-dollar labs are suddenly a lot quieter this week.\n\nThe benchmarks aren't cherry-picked either — independent testers reran them and got the same results. That part is what's actually rattling people.",
        wordCount: 100,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "social proof"],
        cta: "Comment 'link' and I'll drop where to download it.",
      },
      {
        durationLabel: "60s",
        hook: "It cost $0 to train. It's beating GPT-4.",
        body: "A free, open-source model just matched or beat GPT-4 across every major benchmark — reasoning, coding, math, all of it. No subscription. No API key. No ten-figure research budget behind it.\n\nA relatively small team published the full model weights for anyone to download and run on their own hardware, which is exactly why the billion-dollar labs are suddenly a lot quieter this week.\n\nThe benchmarks aren't cherry-picked either — independent testers outside the team reran every one of them and got matching results. That's the part actually rattling people inside the big labs.\n\nWhat it means practically: the moat that justified charging for access to frontier-level intelligence just got a lot smaller, almost overnight.",
        wordCount: 148,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "social proof"],
        cta: "Subscribe — I'm running my own tests on it in the next video.",
      },
      {
        durationLabel: "90s",
        hook: "It cost $0 to train. It's beating GPT-4.",
        body: "A free, open-source model just matched or beat GPT-4 across every major benchmark — reasoning, coding, math, all of it. No subscription. No API key. No ten-figure research budget sitting behind it.\n\nA relatively small team published the full model weights for anyone to download and run on their own hardware, which is exactly why the billion-dollar labs have gone noticeably quiet this week.\n\nThe benchmarks aren't cherry-picked either — independent testers outside the original team reran every single one and got matching results. That's the detail that's actually rattling people inside the big labs, more than the scores themselves.\n\nWhat this means practically is bigger than one leaderboard: the pricing moat that justified charging for access to frontier-level intelligence just got a lot smaller, almost overnight. If a small team with no billion-dollar backing can hit this bar, the next model to do it might not even come from a company at all.",
        wordCount: 210,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "social proof", "open loop"],
        cta: "Subscribe and turn on notifications — full hands-on testing video drops this week.",
      },
    ],
  },
  {
    id: "sc8",
    ideaOrProjectTitle: projects[7].title, // "The disappearance that still doesn't add up"
    projectId: "p8",
    ideaId: "i10",
    channelId: "ch8",
    originalityScore: 64,
    pacingScore: 82,
    altHooks: [
      { text: "This case broke every rule of investigation.", score: 81 },
      { text: "Nobody has ever found the missing piece.", score: 76 },
      { text: "The last sighting doesn't match any of the evidence.", score: 71 },
      { text: "Investigators closed this case without an answer.", score: 65 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Investigators still can't explain what happened next.",
        body: "A man vanished from a locked cabin with no signs of forced entry, no footprints in fresh snow leading away, and a half-eaten dinner still on the table. Decades later, investigators still don't agree on what happened.",
        wordCount: 39,
        retentionTechniques: ["cliffhanger", "curiosity gap"],
        cta: "Follow for part 2 — the detail that changed everything.",
      },
      {
        durationLabel: "30s",
        hook: "Investigators still can't explain what happened next.",
        body: "A man vanished from a locked cabin with no signs of forced entry, no footprints in fresh snow leading away from the door, and a half-eaten dinner still sitting on the table.\n\nThe case sat cold for years — until a park ranger found something 40 miles away that didn't match a single detail of the original investigation.",
        wordCount: 62,
        retentionTechniques: ["cliffhanger", "curiosity gap", "open loop"],
        cta: "Comment 'part 2' if you want the follow-up.",
      },
      {
        durationLabel: "45s",
        hook: "Investigators still can't explain what happened next.",
        body: "A man vanished from a locked cabin with no signs of forced entry, no footprints in the fresh snow leading away from the door, and a half-eaten dinner still sitting on the table exactly where he'd left it.\n\nThe case sat cold for years — until a park ranger found something 40 miles away that didn't match a single detail from the original investigation.\n\nThat discovery should have closed the case. Instead, it opened up three new questions investigators still can't answer.",
        wordCount: 98,
        retentionTechniques: ["cliffhanger", "curiosity gap", "open loop", "specificity"],
        cta: "Comment 'part 2' if you want the follow-up.",
      },
      {
        durationLabel: "60s",
        hook: "Investigators still can't explain what happened next.",
        body: "A man vanished from a locked cabin with no signs of forced entry, no footprints in the fresh snow leading away from the door, and a half-eaten dinner still sitting on the table exactly where he'd left it.\n\nThe case sat cold for years — until a park ranger found something 40 miles away that didn't match a single detail from the original investigation.\n\nThat discovery should have closed the case. Instead, it opened up three new questions investigators still can't answer, including one that contradicts the official timeline entirely.\n\nMost true-crime channels stop at the discovery. We're going further, because the part that never made headlines is the part that actually explains what the family believes happened.",
        wordCount: 145,
        retentionTechniques: ["cliffhanger", "curiosity gap", "open loop", "specificity"],
        cta: "Subscribe — the family's theory drops in part 2.",
      },
      {
        durationLabel: "90s",
        hook: "Investigators still can't explain what happened next.",
        body: "A man vanished from a locked cabin with no signs of forced entry, no footprints in the fresh snow leading away from the door, and a half-eaten dinner still sitting on the table exactly where he'd left it.\n\nThe case sat cold for years — until a park ranger found something 40 miles away that didn't match a single detail from the original investigation.\n\nThat discovery should have closed the case. Instead, it opened up three new questions investigators still can't answer, including one that directly contradicts the official timeline released to the press at the time.\n\nMost true-crime coverage stops right at the discovery, treats it as the ending, and moves on. We're going further, because the detail that never made headlines is the one the family has held onto for decades — and it's the only theory that actually accounts for everything found at that second site.\n\nBy the end of this, you'll understand why the case was never officially closed.",
        wordCount: 205,
        retentionTechniques: ["cliffhanger", "curiosity gap", "open loop", "specificity", "callback"],
        cta: "Subscribe and turn on notifications — the family's theory drops in part 2.",
      },
    ],
  },
  {
    id: "sc9",
    ideaOrProjectTitle: ideas.find(i => i.id === "i7")!.title, // "The engine swap every studio is hiding from you"
    ideaId: "i7",
    channelId: undefined,
    originalityScore: 73,
    pacingScore: 68,
    altHooks: [
      { text: "Every studio is copying this one move.", score: 74 },
      { text: "This engine change explains everything.", score: 70 },
      { text: "Your favorite studio switched engines and didn't tell you.", score: 64 },
      { text: "Game development just changed and nobody announced it.", score: 58 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "Your favorite game might not be made the way you think.",
        body: "Half the biggest studios in gaming have quietly switched to the same engine over the last two years, and it's not the one you'd guess. It's changing how fast games ship — and how they look.",
        wordCount: 36,
        retentionTechniques: ["curiosity gap", "pattern interrupt"],
        cta: "Follow to find out which engine it is.",
      },
      {
        durationLabel: "30s",
        hook: "Your favorite game might not be made the way you think.",
        body: "Over the last two years, a surprising number of major studios have quietly switched to the same game engine — and it's not the one most players would guess.\n\nIt's not just a technical swap. It's changing how fast games ship, how they look at launch, and which small studios can suddenly compete with giants.",
        wordCount: 63,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "open loop"],
        cta: "Comment your guess before I reveal it.",
      },
      {
        durationLabel: "45s",
        hook: "Your favorite game might not be made the way you think.",
        body: "Over the last two years, a surprising number of major studios have quietly switched to the same game engine — and it's not the one most players would guess.\n\nIt's not just a technical swap. It's changing how fast games ship, how they look at launch, and which small studios can suddenly compete with giants that used to have a huge tooling advantage.\n\nA few of the switches only became public because of a stray credits screen or a job listing — the studios themselves haven't announced it.",
        wordCount: 98,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "open loop", "specificity"],
        cta: "Comment your guess before I reveal which engine it is.",
      },
      {
        durationLabel: "60s",
        hook: "Your favorite game might not be made the way you think.",
        body: "Over the last two years, a surprising number of major studios have quietly switched to the same game engine — and it's not the one most players would guess.\n\nIt's not just a technical swap. It's changing how fast games ship, how they look at launch, and which small studios can suddenly compete with giants that used to have a massive tooling advantage.\n\nA few of the switches only became public because of a stray credits screen or a job listing — the studios themselves haven't announced anything officially.\n\nWhy the secrecy? Because admitting you switched engines invites comparisons to your last game, and nobody wants to explain a mid-development pivot to their own fanbase.",
        wordCount: 145,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "open loop", "specificity"],
        cta: "Subscribe — I'm naming the studios in part 2.",
      },
      {
        durationLabel: "90s",
        hook: "Your favorite game might not be made the way you think.",
        body: "Over the last two years, a surprising number of major studios have quietly switched to the same game engine — and it's not the one most players would guess.\n\nIt's not just a technical swap. It's changing how fast games ship, how they look at launch, and which small studios can suddenly compete with giants that used to have a massive tooling advantage over everyone else.\n\nA few of the switches only became public because of a stray credits screen or a job listing — the studios themselves haven't made any kind of official announcement.\n\nWhy the secrecy? Admitting you switched engines invites direct comparisons to your last game and raises uncomfortable questions about a mid-development pivot. It also tips off competitors to exactly which tools are working right now.\n\nEither way, the shift is already reshaping which studios can realistically compete on visuals without a AAA-sized budget behind them.",
        wordCount: 205,
        retentionTechniques: ["curiosity gap", "pattern interrupt", "open loop", "specificity", "contrast"],
        cta: "Subscribe and turn on notifications — I'm naming the studios in part 2.",
      },
    ],
  },
  {
    id: "sc10",
    ideaOrProjectTitle: ideas.find(i => i.id === "i8")!.title, // "The $9 candle that's secretly a $2 candle"
    ideaId: "i8",
    channelId: undefined,
    originalityScore: 69,
    pacingScore: 77,
    altHooks: [
      { text: "You've been tricked by a candle.", score: 76 },
      { text: "Luxury brands do this on purpose.", score: 73 },
      { text: "This candle costs $2 to make. It sells for $58.", score: 70 },
      { text: "Retailers count on you never doing this math.", score: 63 },
    ],
    variants: [
      {
        durationLabel: "15s",
        hook: "This is the oldest trick in retail — and it works on you every time.",
        body: "A candle that costs about $2 to actually make gets sold for $58 once it's in the right jar, with the right font, sitting on the right shelf. That's not quality. That's pricing psychology — and it works on almost everyone.",
        wordCount: 41,
        retentionTechniques: ["contrast", "curiosity gap"],
        cta: "Follow for more pricing tricks brands don't explain.",
      },
      {
        durationLabel: "30s",
        hook: "This is the oldest trick in retail — and it works on you every time.",
        body: "A candle that costs roughly $2 in wax, wick, and scent oil to actually manufacture gets sold for $58 once it's poured into the right jar, wrapped in the right font, and placed on the right shelf.\n\nThat markup isn't about quality. It's a pricing trick called 'anchoring' — and it works on almost everyone, including people who think they're too smart to fall for it.",
        wordCount: 69,
        retentionTechniques: ["contrast", "curiosity gap", "specificity"],
        cta: "Comment 'anchor' and I'll explain the trick in one sentence.",
      },
      {
        durationLabel: "45s",
        hook: "This is the oldest trick in retail — and it works on you every time.",
        body: "A candle that costs roughly $2 in wax, wick, and scent oil to actually manufacture gets sold for $58 once it's poured into the right jar, wrapped in the right font, and placed on the right shelf next to a $92 diffuser.\n\nThat markup isn't about quality. It's a pricing trick called 'anchoring' — putting a much pricier item nearby so the $58 candle suddenly looks like the reasonable choice.\n\nRetailers know most people don't compare raw material costs. They compare items on the same shelf, and that's the comparison that's been engineered.",
        wordCount: 100,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "pattern interrupt"],
        cta: "Comment 'anchor' and I'll explain the trick in one sentence.",
      },
      {
        durationLabel: "60s",
        hook: "This is the oldest trick in retail — and it works on you every time.",
        body: "A candle that costs roughly $2 in wax, wick, and scent oil to actually manufacture gets sold for $58 once it's poured into the right jar, wrapped in the right font, and placed on the right shelf next to a $92 diffuser.\n\nThat markup isn't really about quality. It's a pricing trick called 'anchoring' — putting a much pricier item nearby so the $58 candle suddenly looks like the reasonable, almost frugal choice in comparison.\n\nRetailers know most shoppers don't compare raw material costs. They compare items sitting next to each other on the same shelf, and that's exactly the comparison that's been engineered in advance.\n\nOnce you notice one anchor price in a store, you'll start seeing them everywhere — candles, skincare, even restaurant menus.",
        wordCount: 148,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "pattern interrupt"],
        cta: "Subscribe — next video breaks down the menu version of this trick.",
      },
      {
        durationLabel: "90s",
        hook: "This is the oldest trick in retail — and it works on you every time.",
        body: "A candle that costs roughly $2 in wax, wick, and scent oil to actually manufacture gets sold for $58 once it's poured into the right jar, wrapped in the right font, and placed on the right shelf next to a $92 diffuser.\n\nThat markup isn't really about quality. It's a pricing trick called 'anchoring' — putting a much pricier item nearby so the $58 candle suddenly looks like the reasonable, almost frugal choice in comparison, even though nothing about the candle itself changed.\n\nRetailers know most shoppers don't compare raw material costs. They compare items sitting next to each other on the same shelf, and that comparison is exactly what's been engineered in advance, down to the shelf height and lighting.\n\nOnce you notice one anchor price in a store, you'll start seeing them everywhere — candles, skincare sets, even restaurant menus, where the $46 steak exists mostly to make the $28 one look sensible.\n\nNone of this means the product is bad. It just means the price you're comparing it to was never really about that product at all.",
        wordCount: 215,
        retentionTechniques: ["contrast", "curiosity gap", "specificity", "pattern interrupt", "callback"],
        cta: "Subscribe and turn on notifications for the restaurant-menu breakdown.",
      },
    ],
  },
];

export function scriptById(id?: string) {
  if (!id) return undefined;
  return scripts.find((s) => s.id === id);
}

export const DURATION_ORDER: ScriptDurationLabel[] = ["15s", "30s", "45s", "60s", "90s"];
