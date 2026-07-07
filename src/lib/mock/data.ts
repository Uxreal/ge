import type {
  Channel, Trend, Idea, Project, Approval, ActivityEntry, WeekPoint, Stage, StageStatus,
} from "./types";

export const channels: Channel[] = [
  { id: "ch1", name: "Cosmos Unfolded", handle: "@cosmosunfolded", category: "Space", platforms: ["youtube", "tiktok", "instagram"], subscribers: 1_240_000, weeklyRevenue: 4820, color: "#7c5cff", avatar: "🪐" },
  { id: "ch2", name: "Mind Hacks Daily", handle: "@mindhacks", category: "Psychology", platforms: ["youtube", "instagram"], subscribers: 682_400, weeklyRevenue: 2340, color: "#22c1a8", avatar: "🧠" },
  { id: "ch3", name: "Money Minute", handle: "@moneyminute", category: "Finance", platforms: ["youtube", "tiktok", "x"], subscribers: 415_900, weeklyRevenue: 3120, color: "#f5b342", avatar: "💸" },
  { id: "ch4", name: "History Unlocked", handle: "@historyunlocked", category: "History", platforms: ["youtube", "tiktok"], subscribers: 298_100, weeklyRevenue: 1280, color: "#e26a5a", avatar: "🏺" },
  { id: "ch5", name: "Weird Facts Lab", handle: "@weirdfactslab", category: "Facts", platforms: ["tiktok", "instagram"], subscribers: 856_700, weeklyRevenue: 2680, color: "#d7509b", avatar: "🧪" },
  { id: "ch6", name: "Byte Sized AI", handle: "@bytesizedai", category: "AI", platforms: ["youtube", "tiktok", "x"], subscribers: 512_300, weeklyRevenue: 2910, color: "#4f8cff", avatar: "🤖" },
  { id: "ch7", name: "Gearhead Garage", handle: "@gearheadgarage", category: "Cars", platforms: ["youtube", "instagram"], subscribers: 388_200, weeklyRevenue: 1740, color: "#ff7a3d", avatar: "🏎️" },
  { id: "ch8", name: "Late Night Mysteries", handle: "@latenightmysteries", category: "Mystery", platforms: ["youtube", "tiktok"], subscribers: 623_900, weeklyRevenue: 2050, color: "#6a5acd", avatar: "🕵️" },
];

export const trends: Trend[] = [
  { id: "t1", topic: "James Webb finds unexpected galaxy older than universe estimates", category: "Space", viralityScore: 94, sources: ["youtube", "news", "reddit", "google-trends"], growth: [10,14,22,31,44,62,88,94], volume: "2.4M searches", ageHours: 6, growthVelocity: 38, competition: "medium", audienceOverlap: 72, estimatedLifespan: "4-6 days left", revenuePotential: "$$$", evergreenScore: 61, seasonalRelevance: "Trending now", whyItsWorking: "Strong curiosity-gap hook + counters expectations of 'settled science', high shareability among space/science audiences." },
  { id: "t2", topic: "The 5-second rule your brain uses to break procrastination", category: "Psychology", viralityScore: 87, sources: ["tiktok", "instagram", "reddit"], growth: [20,25,28,40,55,68,80,87], volume: "1.1M views/hr", ageHours: 14, growthVelocity: 22, competition: "high", audienceOverlap: 58, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 88, seasonalRelevance: "Year-round", whyItsWorking: "Actionable life-hack framing, fast-cut editing on TikTok originals, immediate personal payoff drives saves/shares." },
  { id: "t3", topic: "Why the Fed's next move will surprise everyone", category: "Finance", viralityScore: 78, sources: ["x", "youtube", "news"], growth: [30,32,35,42,55,64,72,78], volume: "820K impressions", ageHours: 3, growthVelocity: 45, competition: "high", audienceOverlap: 40, estimatedLifespan: "1-2 days left", revenuePotential: "$$$$", evergreenScore: 22, seasonalRelevance: "Seasonal — Fed meeting week", whyItsWorking: "Timely news-jacking, contrarian framing against consensus, high RPM finance audience." },
  { id: "t4", topic: "The forgotten Roman invention that predicted modern AI", category: "History", viralityScore: 71, sources: ["youtube", "reddit", "rss"], growth: [15,18,22,28,38,52,64,71], volume: "410K searches", ageHours: 22, growthVelocity: 14, competition: "low", audienceOverlap: 35, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 91, seasonalRelevance: "Year-round", whyItsWorking: "Novelty bridge between ancient history and AI zeitgeist; low competition niche with strong watch-to-the-end curiosity." },
  { id: "t5", topic: "The weirdest thing your brain does while you sleep", category: "Facts", viralityScore: 82, sources: ["tiktok", "instagram", "reddit"], growth: [12,18,26,38,50,62,74,82], volume: "1.6M views/hr", ageHours: 9, growthVelocity: 30, competition: "high", audienceOverlap: 66, estimatedLifespan: "5-7 days left", revenuePotential: "$$", evergreenScore: 74, seasonalRelevance: "Year-round", whyItsWorking: "Universal relatable topic (everyone sleeps), strong emotional 'wait, what?' hook in first 2 seconds." },
  { id: "t6", topic: "This open-source model just beat GPT-4 on every benchmark", category: "AI", viralityScore: 91, sources: ["x", "reddit", "news", "google-trends"], growth: [25,34,48,60,72,84,89,91], volume: "3.1M impressions", ageHours: 5, growthVelocity: 52, competition: "high", audienceOverlap: 48, estimatedLifespan: "2-3 days left", revenuePotential: "$$$", evergreenScore: 30, seasonalRelevance: "Trending now", whyItsWorking: "Underdog-beats-giant narrative, tech audience loves benchmarks, easy to visualize with comparison charts." },
  { id: "t7", topic: "Why every game studio is quietly switching to this engine", category: "Gaming", viralityScore: 68, sources: ["youtube", "reddit", "x"], growth: [10,14,20,28,38,50,60,68], volume: "540K searches", ageHours: 30, growthVelocity: 12, competition: "medium", audienceOverlap: 44, estimatedLifespan: "1-2 weeks left", revenuePotential: "$$", evergreenScore: 55, seasonalRelevance: "Year-round", whyItsWorking: "Insider-knowledge framing appeals to aspiring developers; strong niche community engagement." },
  { id: "t8", topic: "The pricing psychology trick every luxury brand uses", category: "Business", viralityScore: 76, sources: ["instagram", "tiktok", "x"], growth: [18,22,30,40,50,62,70,76], volume: "690K views/hr", ageHours: 11, growthVelocity: 24, competition: "medium", audienceOverlap: 52, estimatedLifespan: "Evergreen", revenuePotential: "$$$", evergreenScore: 82, seasonalRelevance: "Year-round", whyItsWorking: "Reveals 'secret' industry tactics, satisfying pattern-recognition payoff, easily illustrated with visual examples." },
  { id: "t9", topic: "This $200K hypercar has an engine flaw nobody talks about", category: "Cars", viralityScore: 73, sources: ["youtube", "instagram"], growth: [14,18,24,32,42,54,64,73], volume: "480K views/hr", ageHours: 16, growthVelocity: 19, competition: "medium", audienceOverlap: 38, estimatedLifespan: "1 week left", revenuePotential: "$$", evergreenScore: 48, seasonalRelevance: "Year-round", whyItsWorking: "Contrarian take on aspirational object, gearhead audience loves technical 'gotcha' reveals." },
  { id: "t10", topic: "The unsolved disappearance that still haunts investigators", category: "Mystery", viralityScore: 85, sources: ["youtube", "reddit", "tiktok"], growth: [22,28,36,46,58,70,80,85], volume: "1.3M views/hr", ageHours: 8, growthVelocity: 28, competition: "medium", audienceOverlap: 41, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 86, seasonalRelevance: "Year-round", whyItsWorking: "True-crime curiosity gap, cliffhanger structure keeps completion rate high, strong comment-bait ending." },
  { id: "t11", topic: "Octopuses can taste with their arms and it changes everything", category: "Animals", viralityScore: 79, sources: ["tiktok", "instagram", "reddit"], growth: [16,22,30,40,52,64,74,79], volume: "980K views/hr", ageHours: 12, growthVelocity: 26, competition: "low", audienceOverlap: 33, estimatedLifespan: "Evergreen", revenuePotential: "$", evergreenScore: 89, seasonalRelevance: "Year-round", whyItsWorking: "Bite-sized 'mind blown' animal fact, highly visual, low competition sub-niche with strong shareability." },
  { id: "t12", topic: "Physicists may have just found evidence for a fifth force", category: "Science", viralityScore: 84, sources: ["news", "reddit", "x", "google-trends"], growth: [20,26,34,44,56,68,78,84], volume: "1.2M searches", ageHours: 10, growthVelocity: 33, competition: "medium", audienceOverlap: 55, estimatedLifespan: "3-4 days left", revenuePotential: "$$", evergreenScore: 44, seasonalRelevance: "Trending now", whyItsWorking: "Breaking-science framing, taps into 'rewrite the textbooks' fascination, strong cross-post potential from Space audience." },
  { id: "t13", topic: "The 2-minute morning routine that rewires focus", category: "Productivity", viralityScore: 81, sources: ["instagram", "tiktok"], growth: [19,25,33,42,54,66,76,81], volume: "1.0M views/hr", ageHours: 13, growthVelocity: 27, competition: "high", audienceOverlap: 60, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 85, seasonalRelevance: "Seasonal — New Year spike", whyItsWorking: "Low-effort high-reward promise, satisfies self-improvement algorithm favorites, strong save rate." },
  { id: "t14", topic: "This $8 tool replaces five things in your kitchen drawer", category: "Life Hacks", viralityScore: 70, sources: ["tiktok", "instagram"], growth: [14,18,24,32,42,54,64,70], volume: "620K views/hr", ageHours: 18, growthVelocity: 17, competition: "high", audienceOverlap: 62, estimatedLifespan: "1 week left", revenuePotential: "$", evergreenScore: 68, seasonalRelevance: "Year-round", whyItsWorking: "Product-demo satisfaction loop, low price point removes purchase friction, strong affiliate potential." },
  { id: "t15", topic: "A24's next film is quietly rewriting horror rules", category: "Movies & TV", viralityScore: 77, sources: ["x", "reddit", "youtube"], growth: [16,22,30,40,52,64,72,77], volume: "710K impressions", ageHours: 20, growthVelocity: 20, competition: "medium", audienceOverlap: 30, estimatedLifespan: "2 weeks left", revenuePotential: "$$", evergreenScore: 40, seasonalRelevance: "Seasonal — release window", whyItsWorking: "Fan-community insider framing, trailer-breakdown style content performs consistently for film audiences." },
  { id: "t16", topic: "The one-rep trick that fixed everyone's deadlift form", category: "Fitness", viralityScore: 74, sources: ["instagram", "tiktok", "youtube"], growth: [15,20,27,36,46,58,68,74], volume: "560K views/hr", ageHours: 15, growthVelocity: 18, competition: "high", audienceOverlap: 47, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 80, seasonalRelevance: "Year-round", whyItsWorking: "Direct before/after visual proof, satisfies form-check niche, high save-for-later rate." },
  { id: "t17", topic: "The one ingredient swap chefs never mention", category: "Cooking", viralityScore: 69, sources: ["tiktok", "instagram"], growth: [13,17,23,31,40,52,62,69], volume: "500K views/hr", ageHours: 19, growthVelocity: 15, competition: "high", audienceOverlap: 50, estimatedLifespan: "1 week left", revenuePotential: "$", evergreenScore: 70, seasonalRelevance: "Year-round", whyItsWorking: "Insider-secret framing, simple visual payoff, easy duet/remix potential drives distribution." },
  { id: "t18", topic: "Why the pyramids couldn't be built today for any price", category: "Educational", viralityScore: 80, sources: ["youtube", "reddit", "rss"], growth: [17,23,31,41,53,65,75,80], volume: "890K searches", ageHours: 17, growthVelocity: 21, competition: "low", audienceOverlap: 36, estimatedLifespan: "Evergreen", revenuePotential: "$$", evergreenScore: 90, seasonalRelevance: "Year-round", whyItsWorking: "Contrarian modern-vs-ancient framing, satisfies engineering curiosity, strong long-form retention." },
  { id: "t19", topic: "A stranger's note ended up changing a small town forever", category: "Storytelling", viralityScore: 72, sources: ["tiktok", "reddit"], growth: [14,19,26,34,44,56,66,72], volume: "430K views/hr", ageHours: 21, growthVelocity: 16, competition: "medium", audienceOverlap: 28, estimatedLifespan: "5 days left", revenuePotential: "$", evergreenScore: 65, seasonalRelevance: "Year-round", whyItsWorking: "Emotional narrative arc with a twist ending, strong completion rate from suspense pacing." },
  { id: "t20", topic: "Inside the $40M apartment nobody is allowed to see", category: "Luxury", viralityScore: 75, sources: ["instagram", "youtube"], growth: [15,20,28,37,48,60,69,75], volume: "600K views/hr", ageHours: 14, growthVelocity: 19, competition: "medium", audienceOverlap: 32, estimatedLifespan: "1 week left", revenuePotential: "$$$", evergreenScore: 58, seasonalRelevance: "Year-round", whyItsWorking: "Aspirational voyeurism, exclusivity framing ('nobody is allowed'), strong visual production value expectation." },
];

export const ideas: Idea[] = [
  { id: "i1", title: "Webb telescope just broke physics — here's what it means", hook: "Scientists thought this was impossible. Then Webb saw it.", channelId: "ch1", trendId: "t1", category: "Space", score: 92, estimatedViews: "3.2M", clickPotential: 94, retentionPrediction: 88, monetizationValue: 76, originality: 71, audienceInterest: 95, trendAlignment: 97, angle: "Frame it as 'what scientists got wrong' rather than just reporting the discovery.", hookOptions: ["Scientists thought this was impossible. Then Webb saw it.", "This shouldn't exist. But it does.", "The universe just got 2 billion years more confusing."], createdAt: "6 min ago" },
  { id: "i2", title: "The 5-second rule that rewires procrastination", hook: "Your brain has a 5-second window. Miss it and you're stuck.", channelId: "ch2", trendId: "t2", category: "Psychology", score: 88, estimatedViews: "1.8M", clickPotential: 85, retentionPrediction: 90, monetizationValue: 62, originality: 55, audienceInterest: 89, trendAlignment: 93, angle: "Pair the rule with a real-time on-screen countdown demo instead of just explaining it.", hookOptions: ["Your brain has a 5-second window. Miss it and you're stuck.", "Count to 5. That's the whole hack.", "This works because of one weird brain glitch."], createdAt: "18 min ago" },
  { id: "i3", title: "The Fed pivot nobody is talking about", hook: "This chart predicts every rate change since 2008.", channelId: "ch3", trendId: "t3", category: "Finance", score: 79, estimatedViews: "740K", clickPotential: 74, retentionPrediction: 68, monetizationValue: 91, originality: 60, audienceInterest: 72, trendAlignment: 88, angle: "Use a single recurring chart as the visual anchor across the whole video for authority.", hookOptions: ["This chart predicts every rate change since 2008.", "Everyone's watching the wrong number.", "The Fed already told us. We just didn't listen."], createdAt: "40 min ago" },
  { id: "i4", title: "Rome invented the algorithm 2,000 years before Google", hook: "One device. One idea. The blueprint of every AI.", channelId: "ch4", trendId: "t4", category: "History", score: 74, estimatedViews: "610K", clickPotential: 69, retentionPrediction: 77, monetizationValue: 48, originality: 82, audienceInterest: 65, trendAlignment: 70, angle: "Draw a direct visual parallel between the Antikythera mechanism and a modern neural net diagram.", hookOptions: ["One device. One idea. The blueprint of every AI.", "This 2,000-year-old machine 'thinks'.", "Rome had a computer. Nobody believes it."], createdAt: "1 h ago" },
  { id: "i5", title: "Your brain runs a secret app while you sleep", hook: "Wait — your brain is doing THIS every night?", channelId: "ch5", trendId: "t5", category: "Facts", score: 85, estimatedViews: "1.4M", clickPotential: 90, retentionPrediction: 84, monetizationValue: 55, originality: 58, audienceInterest: 88, trendAlignment: 91, angle: "Frame nightly brain activity as an 'app' running in the background for relatability.", hookOptions: ["Wait — your brain is doing THIS every night?", "You have no idea what your brain does at 3am.", "This happens in your skull every single night."], createdAt: "22 min ago" },
  { id: "i6", title: "A free model just humiliated a $10B AI lab", hook: "It cost $0 to train. It's beating GPT-4.", channelId: "ch6", trendId: "t6", category: "AI", score: 90, estimatedViews: "2.6M", clickPotential: 93, retentionPrediction: 80, monetizationValue: 70, originality: 66, audienceInterest: 92, trendAlignment: 95, angle: "Lead with the cost-vs-performance irony rather than technical benchmarks.", hookOptions: ["It cost $0 to train. It's beating GPT-4.", "The billion-dollar labs didn't see this coming.", "This model shouldn't be this good."], createdAt: "9 min ago" },
  { id: "i7", title: "The engine swap every studio is hiding from you", hook: "Your favorite game might not be made the way you think.", channelId: undefined, trendId: "t7", category: "Gaming", score: 68, estimatedViews: "380K", clickPotential: 64, retentionPrediction: 66, monetizationValue: 50, originality: 73, audienceInterest: 60, trendAlignment: 72, angle: "Position as an insider exposé rather than a technical explainer.", hookOptions: ["Your favorite game might not be made the way you think.", "Every studio is copying this one move.", "This engine change explains everything."], createdAt: "2 h ago" },
  { id: "i8", title: "The $9 candle that's secretly a $2 candle", hook: "This is the oldest trick in retail — and it works on you every time.", channelId: undefined, trendId: "t8", category: "Business", score: 77, estimatedViews: "520K", clickPotential: 75, retentionPrediction: 71, monetizationValue: 68, originality: 69, audienceInterest: 74, trendAlignment: 80, angle: "Use a real product price-anchoring example instead of an abstract explanation.", hookOptions: ["This is the oldest trick in retail — and it works on you every time.", "You've been tricked by a candle.", "Luxury brands do this on purpose."], createdAt: "55 min ago" },
  { id: "i9", title: "This hypercar's $200K flaw nobody mentions", hook: "For $200,000 you'd expect this NOT to happen.", channelId: "ch7", trendId: "t9", category: "Cars", score: 72, estimatedViews: "410K", clickPotential: 70, retentionPrediction: 63, monetizationValue: 58, originality: 61, audienceInterest: 66, trendAlignment: 75, angle: "Contrast the price tag against the flaw visually within the first frame.", hookOptions: ["For $200,000 you'd expect this NOT to happen.", "The flaw hiding under the hood.", "Nobody talks about this hypercar's one weakness."], createdAt: "3 h ago" },
  { id: "i10", title: "The disappearance that still doesn't add up", hook: "Investigators still can't explain what happened next.", channelId: "ch8", trendId: "t10", category: "Mystery", score: 86, estimatedViews: "1.5M", clickPotential: 88, retentionPrediction: 91, monetizationValue: 60, originality: 64, audienceInterest: 87, trendAlignment: 90, angle: "Structure as a slow-reveal with the biggest twist held for the final 5 seconds.", hookOptions: ["Investigators still can't explain what happened next.", "This case broke every rule of investigation.", "Nobody has ever found the missing piece."], createdAt: "31 min ago" },
  { id: "i11", title: "Octopuses taste with their skin and it's unsettling", hook: "This animal can taste you before it even touches you.", channelId: undefined, trendId: "t11", category: "Animals", score: 78, estimatedViews: "890K", clickPotential: 80, retentionPrediction: 82, monetizationValue: 40, originality: 75, audienceInterest: 79, trendAlignment: 76, angle: "Open on the unsettling implication before explaining the biology.", hookOptions: ["This animal can taste you before it even touches you.", "Its whole body is a tongue.", "Scientists are still figuring this one out."], createdAt: "1 h ago" },
  { id: "i12", title: "Is there a fifth force hiding in the universe?", hook: "Physics has four forces. This might be a fifth.", channelId: "ch1", trendId: "t12", category: "Science", score: 83, estimatedViews: "1.1M", clickPotential: 82, retentionPrediction: 79, monetizationValue: 58, originality: 62, audienceInterest: 85, trendAlignment: 89, angle: "Use the 'four forces you learned in school' setup to create a knowledge-gap hook.", hookOptions: ["Physics has four forces. This might be a fifth.", "Everything you learned about physics might be incomplete.", "Scientists found something that shouldn't exist."], createdAt: "14 min ago" },
  { id: "i13", title: "Fix your focus in 2 minutes flat", hook: "Do this for 2 minutes before you touch your phone.", channelId: "ch2", trendId: "t13", category: "Productivity", score: 80, estimatedViews: "980K", clickPotential: 78, retentionPrediction: 83, monetizationValue: 54, originality: 50, audienceInterest: 81, trendAlignment: 84, angle: "Demo the routine live on screen with a visible timer rather than describing it.", hookOptions: ["Do this for 2 minutes before you touch your phone.", "Your focus resets with this one trick.", "This 2-minute routine beats every productivity app."], createdAt: "47 min ago" },
  { id: "i14", title: "The $8 tool that replaced 5 kitchen gadgets", hook: "I threw away 5 tools after finding this.", channelId: undefined, trendId: "t14", category: "Life Hacks", score: 69, estimatedViews: "560K", clickPotential: 72, retentionPrediction: 60, monetizationValue: 65, originality: 45, audienceInterest: 70, trendAlignment: 73, angle: "Show the 'before' clutter drawer for contrast before revealing the single tool.", hookOptions: ["I threw away 5 tools after finding this.", "One tool. Five jobs. $8.", "This drawer used to have 5 gadgets in it."], createdAt: "2 h ago" },
  { id: "i15", title: "A24 is quietly breaking every horror rule", hook: "This studio just broke the biggest rule in horror.", channelId: undefined, trendId: "t15", category: "Movies & TV", score: 76, estimatedViews: "620K", clickPotential: 74, retentionPrediction: 70, monetizationValue: 52, originality: 68, audienceInterest: 71, trendAlignment: 77, angle: "Compare against a well-known horror trope the audience already recognizes.", hookOptions: ["This studio just broke the biggest rule in horror.", "Horror movies never do this. This one just did.", "The trailer is hiding the real twist."], createdAt: "1 h ago" },
  { id: "i16", title: "One rep fixed everyone's deadlift", hook: "Your deadlift form has one hidden flaw.", channelId: undefined, trendId: "t16", category: "Fitness", score: 73, estimatedViews: "470K", clickPotential: 71, retentionPrediction: 75, monetizationValue: 56, originality: 47, audienceInterest: 72, trendAlignment: 78, angle: "Use split-screen before/after form footage as the core visual.", hookOptions: ["Your deadlift form has one hidden flaw.", "This single cue fixes 90% of deadlifts.", "Trainers don't explain this part."], createdAt: "3 h ago" },
  { id: "i17", title: "Chefs never tell you about this one swap", hook: "Restaurants use this swap. Home cooks never do.", channelId: undefined, trendId: "t17", category: "Cooking", score: 67, estimatedViews: "430K", clickPotential: 68, retentionPrediction: 58, monetizationValue: 44, originality: 52, audienceInterest: 66, trendAlignment: 71, angle: "Do a blind side-by-side taste-test framing to build curiosity.", hookOptions: ["Restaurants use this swap. Home cooks never do.", "This swap changes the whole dish.", "One ingredient. Completely different result."], createdAt: "4 h ago" },
  { id: "i18", title: "Why the pyramids are impossible to build today", hook: "We have more technology. We still couldn't do this.", channelId: "ch4", trendId: "t18", category: "Educational", score: 81, estimatedViews: "740K", clickPotential: 76, retentionPrediction: 86, monetizationValue: 50, originality: 78, audienceInterest: 80, trendAlignment: 82, angle: "Use modern engineering cost estimates as the comparison hook.", hookOptions: ["We have more technology. We still couldn't do this.", "This would cost more today than it did in 2560 BC.", "Nobody has solved how they actually did this."], createdAt: "25 min ago" },
  { id: "i19", title: "A note in a bottle changed this town forever", hook: "One note. One stranger. A town that was never the same.", channelId: undefined, trendId: "t19", category: "Storytelling", score: 71, estimatedViews: "390K", clickPotential: 66, retentionPrediction: 74, monetizationValue: 38, originality: 80, audienceInterest: 68, trendAlignment: 69, angle: "Withhold the town's name until the final act for suspense.", hookOptions: ["One note. One stranger. A town that was never the same.", "Nobody expected a note to do this.", "This story has a twist you won't see coming."], createdAt: "2 h ago" },
  { id: "i20", title: "Inside the apartment almost nobody has seen", hook: "$40M. One floor. Almost nobody has been inside.", channelId: undefined, trendId: "t20", category: "Luxury", score: 74, estimatedViews: "480K", clickPotential: 73, retentionPrediction: 65, monetizationValue: 62, originality: 55, audienceInterest: 70, trendAlignment: 74, angle: "Open with the price tag as an on-screen stat before showing a single frame of the interior.", hookOptions: ["$40M. One floor. Almost nobody has been inside.", "This apartment isn't for sale. We got in anyway.", "Most people will never see a room like this."], createdAt: "1 h ago" },
  { id: "i21", title: "The joke format that's quietly taking over every feed", hook: "This meme format has a psychological trick baked in.", channelId: undefined, trendId: undefined, category: "Memes", score: 64, estimatedViews: "310K", clickPotential: 70, retentionPrediction: 55, monetizationValue: 30, originality: 85, audienceInterest: 62, trendAlignment: 40, angle: "Fully original concept — no trend tie-in, built from a format-analysis angle instead.", hookOptions: ["This meme format has a psychological trick baked in.", "Why this exact joke structure keeps working.", "You've seen this format 100 times. Here's why."], createdAt: "5 h ago" },
  { id: "i22", title: "The comedian trick that makes any story funnier", hook: "Comedians use this on every single story. You never noticed.", channelId: undefined, trendId: undefined, category: "Humor", score: 66, estimatedViews: "340K", clickPotential: 68, retentionPrediction: 60, monetizationValue: 35, originality: 88, audienceInterest: 64, trendAlignment: 35, angle: "Fully original — breaks down comedic timing as a teachable technique.", hookOptions: ["Comedians use this on every single story. You never noticed.", "This is why some stories are just funnier.", "The 3-beat rule every comedian uses."], createdAt: "6 h ago" },
  { id: "i23", title: "The DIY shelf that costs $12 and looks like $400", hook: "$12 in parts. Looks like a $400 shelf.", channelId: undefined, trendId: undefined, category: "DIY", score: 70, estimatedViews: "410K", clickPotential: 73, retentionPrediction: 68, monetizationValue: 48, originality: 79, audienceInterest: 69, trendAlignment: 30, angle: "Fully original — a build video with a cost-reveal twist at the end.", hookOptions: ["$12 in parts. Looks like a $400 shelf.", "Nobody will guess what this actually cost.", "I built this for less than a coffee run."], createdAt: "7 h ago" },
  { id: "i24", title: "The mindset shift that got me unstuck in a week", hook: "I was stuck for a year. This shift took a week.", channelId: undefined, trendId: undefined, category: "Motivation", score: 68, estimatedViews: "360K", clickPotential: 65, retentionPrediction: 70, monetizationValue: 42, originality: 74, audienceInterest: 67, trendAlignment: 32, angle: "Fully original — personal narrative format rather than trend-inspired.", hookOptions: ["I was stuck for a year. This shift took a week.", "One sentence changed how I see failure.", "This is the shift nobody told me about."], createdAt: "8 h ago" },
];

const seq = (states: Array<[Stage, StageStatus, number?]>) =>
  states.map(([stage, status, progress]) => ({ stage, status, progress }));

export const projects: Project[] = [
  {
    id: "p1", title: "Webb telescope just broke physics", channelId: "ch1",
    currentStage: "video", score: 92, updatedAt: "4 min ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","done"], ["video","running", 62],
      ["voice","queued"], ["captions","queued"], ["music","queued"],
      ["thumbnail","queued"], ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p2", title: "The 5-second procrastination hack", channelId: "ch2",
    currentStage: "thumbnail", score: 88, updatedAt: "18 min ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","done"], ["video","done"], ["voice","done"],
      ["captions","done"], ["music","done"], ["thumbnail","needs-approval"],
      ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p3", title: "The Fed pivot nobody sees coming", channelId: "ch3",
    currentStage: "fact-check", score: 79, updatedAt: "1 h ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","needs-approval"],
      ["scenes","queued"], ["assets","queued"], ["video","queued"], ["voice","queued"],
      ["captions","queued"], ["music","queued"], ["thumbnail","queued"],
      ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p4", title: "Rome's algorithm: history's first AI", channelId: "ch4",
    currentStage: "script", score: 74, updatedAt: "2 h ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","running", 41],
      ["fact-check","queued"], ["scenes","queued"], ["assets","queued"], ["video","queued"],
      ["voice","queued"], ["captions","queued"], ["music","queued"], ["thumbnail","queued"],
      ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p5", title: "The weirdest thing your brain does asleep", channelId: "ch5",
    currentStage: "seo", score: 85, updatedAt: "36 min ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","done"], ["video","done"], ["voice","done"],
      ["captions","done"], ["music","done"], ["thumbnail","done"],
      ["seo","running", 78], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p6", title: "Why time slows when you're scared", channelId: "ch2",
    currentStage: "publish", score: 91, updatedAt: "just now",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","done"], ["video","done"], ["voice","done"],
      ["captions","done"], ["music","done"], ["thumbnail","done"],
      ["seo","done"], ["schedule","done"], ["publish","needs-approval"],
    ]),
  },
  {
    id: "p7", title: "A free model just humiliated a $10B AI lab", channelId: "ch6",
    currentStage: "voice", score: 90, updatedAt: "9 min ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","done"], ["video","done"], ["voice","running", 55],
      ["captions","queued"], ["music","queued"], ["thumbnail","queued"],
      ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
  {
    id: "p8", title: "The disappearance that still doesn't add up", channelId: "ch8",
    currentStage: "assets", score: 86, updatedAt: "27 min ago",
    stages: seq([
      ["trend","done"], ["idea","done"], ["script","done"], ["fact-check","done"],
      ["scenes","done"], ["assets","running", 33], ["video","queued"], ["voice","queued"],
      ["captions","queued"], ["music","queued"], ["thumbnail","queued"],
      ["seo","queued"], ["schedule","queued"], ["publish","queued"],
    ]),
  },
];

export const approvals: Approval[] = [
  { id: "a1", projectId: "p2", projectTitle: "The 5-second procrastination hack", channelId: "ch2", type: "thumbnail", createdAt: "5 min ago" },
  { id: "a2", projectId: "p3", projectTitle: "The Fed pivot nobody sees coming", channelId: "ch3", type: "script", createdAt: "22 min ago" },
  { id: "a3", projectId: "p6", projectTitle: "Why time slows when you're scared", channelId: "ch2", type: "final-cut", createdAt: "just now" },
  { id: "a4", projectId: "p5", projectTitle: "The weirdest thing your brain does asleep", channelId: "ch5", type: "seo", createdAt: "1 h ago" },
];

export const activity: ActivityEntry[] = [
  { id: "ac1", actor: "ai", message: "Generated 12 new script variations for Cosmos Unfolded", time: "2 min ago" },
  { id: "ac2", actor: "system", message: "Published 'Neurons that fire together' to TikTok and Reels", time: "14 min ago" },
  { id: "ac3", actor: "you", message: "Approved thumbnail for 'The Fed pivot nobody sees coming'", time: "38 min ago" },
  { id: "ac4", actor: "ai", message: "Detected 3 rising trends in Space category", time: "1 h ago" },
  { id: "ac5", actor: "system", message: "Fact-check flagged 1 claim in 'Rome's algorithm'", time: "1 h ago" },
  { id: "ac6", actor: "ai", message: "Optimized SEO for 6 videos scheduled this week", time: "3 h ago" },
];

export const weekPerformance: WeekPoint[] = [
  { day: "Mon", views: 420_000, revenue: 1120 },
  { day: "Tue", views: 512_000, revenue: 1340 },
  { day: "Wed", views: 468_000, revenue: 1210 },
  { day: "Thu", views: 690_000, revenue: 1740 },
  { day: "Fri", views: 812_000, revenue: 2010 },
  { day: "Sat", views: 940_000, revenue: 2380 },
  { day: "Sun", views: 1_060_000, revenue: 2620 },
];

export const dashboardStats = {
  activeChannels: channels.length,
  videosInProduction: projects.filter(p => !["publish"].includes(p.currentStage) || p.stages.at(-1)?.status !== "done").length,
  pendingApprovals: approvals.length,
  trendingOpportunities: trends.filter(t => t.viralityScore >= 75).length,
  weeklyRevenue: channels.reduce((s, c) => s + c.weeklyRevenue, 0),
};

export function channelById(id?: string) {
  if (!id) return undefined;
  return channels.find(c => c.id === id);
}

export function trendById(id?: string) {
  if (!id) return undefined;
  return trends.find(t => t.id === id);
}
