import type { StreamKind } from "../engine/types";

export interface CollectionTab {
  /** Route param + Archive collection id. "all" searches every movie item. */
  id: string;
  label: string;
  blurb: string;
  /** undefined = no collection filter. */
  collection?: string;
}

/**
 * Curated entry points into archive.org's moving-image library. Everything
 * here is free to stream: public-domain features, out-of-copyright genre film,
 * and openly licensed uploads.
 *
 * Collections come and go on the Archive, so the browse page treats an empty
 * result as "nothing here" rather than an error.
 */
export const COLLECTIONS: CollectionTab[] = [
  {
    id: "features",
    label: "Feature films",
    blurb: "Full-length public-domain features.",
    collection: "feature_films",
  },
  {
    id: "noir",
    label: "Film noir",
    blurb: "Post-war crime and detective pictures.",
    collection: "film_noir",
  },
  {
    id: "scifi",
    label: "Sci-fi & horror",
    blurb: "Creature features, B-movies and serials.",
    collection: "SciFi_Horror",
  },
  {
    id: "animation",
    label: "Animation",
    blurb: "Classic theatrical cartoons and shorts.",
    collection: "animationandcartoons",
  },
  {
    id: "shorts",
    label: "Short films",
    blurb: "Under an hour, from comedy to experimental.",
    collection: "short_films",
  },
  {
    id: "prelinger",
    label: "Prelinger",
    blurb: "Ephemeral, industrial and educational film.",
    collection: "prelinger",
  },
  {
    id: "open",
    label: "Open licence",
    blurb: "Community uploads under open licences.",
    collection: "opensource_movies",
  },
  {
    id: "all",
    label: "Everything",
    blurb: "Every moving-image item on archive.org.",
  },
];

export function collectionById(id: string): CollectionTab {
  return COLLECTIONS.find((c) => c.id === id) ?? COLLECTIONS[0];
}

export interface DemoStream {
  title: string;
  url: string;
  kind: StreamKind;
  note: string;
}

/**
 * Reference streams published by their vendors for exactly this purpose —
 * handy for checking that HLS, DASH and progressive playback all work in the
 * current browser.
 */
export const DEMO_STREAMS: DemoStream[] = [
  {
    title: "Apple HLS reference (fMP4)",
    url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
    kind: "hls",
    note: "Multi-bitrate ladder with alternate audio and subtitle tracks.",
  },
  {
    title: "Mux HLS test stream",
    url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    kind: "hls",
    note: "The stream hls.js itself is tested against.",
  },
  {
    title: "DASH-IF Big Buck Bunny",
    url: "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd",
    kind: "dash",
    note: "MPEG-DASH ladder up to 4K, 30fps.",
  },
  {
    title: "Envivio DASH sample",
    url: "https://dash.akamaized.net/envivio/EnvivioDash3/manifest.mpd",
    kind: "dash",
    note: "Multi-period DASH with several audio renditions.",
  },
  {
    title: "Big Buck Bunny (MP4)",
    url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    kind: "progressive",
    note: "Plain progressive download — no adaptive ladder.",
  },
];
