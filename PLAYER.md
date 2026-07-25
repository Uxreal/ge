# Reel — web media player

A browser video player for **HLS**, **MPEG-DASH** and **progressive** streams, plus a browsable
library of free, public-domain film from the Internet Archive.

This repo builds two apps from one Vite project. The player is a separate entry point and does not
touch the Vira prototype documented in [README.md](README.md).

```bash
npm install
npm run dev       # Vira at http://localhost:5173/ — player at http://localhost:5173/player
npm run build     # builds both entries
npm run preview
```

## What it plays

| Format | Engine | Notes |
| --- | --- | --- |
| HLS (`.m3u8`) | [hls.js](https://github.com/video-dev/hls.js) via MSE | Full bitrate ladder, alternate audio, embedded subtitles |
| HLS on Safari / iOS | Native | No MSE path exists there; the platform owns ABR, so the quality menu is hidden |
| MPEG-DASH (`.mpd`) | [dash.js](https://github.com/Dash-Industry-Forum/dash.js) | Representation picker, audio tracks, embedded text tracks |
| MP4 / M4V / WebM / OGV / audio | Native `<video>` | Byte-range seeking, whatever codecs the browser ships |

Format is detected from the URL, then from content-type. When detection is only a guess, a fatal load
error silently retries with the next engine before anything is shown to the user; the error overlay
also offers a manual **Force HLS / DASH / Progressive** override.

Both streaming engines are behind dynamic `import()`, so the initial bundle is ~78 kB and a session
downloads at most one of them.

**Not playable in any browser, by design:** MKV and AVI containers (no browser demuxes them), and
anything behind DRM/Widevine. The player detects these and says so rather than failing blankly.

## Player features

- **Transport** — play/pause, ±10 s skip, scrubber with buffered-range shading and hover time preview
- **Quality** — auto (ABR) or a pinned level, with live resolution/bitrate in the stats overlay
- **Audio tracks** — switch language or descriptive-audio renditions where the stream carries them
- **Subtitles** — embedded tracks, plus external `.srt`/`.vtt` from a local file or URL (SubRip is
  converted to WebVTT in-browser)
- **Speed** 0.25×–4×, **volume** with scroll-wheel control, **picture-in-picture**, **fullscreen**
- **Resume** — playback position is remembered per URL and restored on return
- **Stats overlay** — engine, level count, resolution, bitrate, bandwidth estimate, buffer ahead,
  dropped frames
- **Deep links** — `/player/watch?url=…&t=90` plays any stream from a shareable link

### Keyboard

| Key | Action | Key | Action |
| --- | --- | --- | --- |
| `Space` `K` | Play / pause | `M` | Mute |
| `J` `L` | ∓10 s | `C` | Subtitles |
| `←` `→` | ∓5 s (`Shift` = 60 s) | `F` | Fullscreen |
| `↑` `↓` | Volume | `I` | Picture-in-picture |
| `0`–`9` | Jump to 0–90 % | `<` `>` | Speed down / up |
| `Home` `End` | Start / end | `?` | Shortcut help |

## The library

Browse and search are backed by the Internet Archive's public JSON APIs — no key, no account, CORS
enabled, byte-range friendly. Collections are curated in `src/player/catalog/collections.ts`; each
title resolves through `/metadata/<id>`, which lists every derivative the Archive holds, ranked so the
default pick is a browser-friendly H.264 MP4 rather than a 4 GB MPEG-2 original. All of it is public
domain or openly licensed.

`Open URL` plays any stream you paste, and ships the vendor-published HLS/DASH reference streams for
checking browser support.

Continue-watching, saved titles and history live in `localStorage` only — nothing is uploaded.

## Scope

The player streams URLs that a server is willing to serve to a browser. It contains no scraper,
link-extractor or DRM-circumvention code, so it will not pull video off sites that do not publish a
playable stream — most "free streaming" aggregators fall in that category. Where a source refuses,
the error overlay explains which of the three causes (bad URL, unreachable host, missing CORS headers)
applies, because none of them are fixable from the client.

## Layout

```
player.html                     # entry document, served at /player
src/player/
  App.tsx  main.tsx  player.css # router (basename /player), mount, ::cue styles
  engine/
    types.ts                    # the MediaEngine interface every backend implements
    detect.ts                   # URL/MIME sniffing + engine fallback order
    create-engine.ts            # picks and lazily imports an engine
    hls-engine.ts               # hls.js
    dash-engine.ts              # dash.js
    native-engine.ts            # plain <video>, and native HLS on Safari
    subtitles.ts                # external tracks, SubRip -> WebVTT
  hooks/
    use-player.ts               # the controller: state machine, resume, track selection
    use-shortcuts.ts  use-idle.ts  use-fullscreen.ts
  components/                   # VideoPlayer, ControlBar, SeekBar, SettingsMenu, overlays, dialogs
  catalog/                      # Internet Archive client + curated collections
  pages/                        # Browse, Watch, Library
  lib/                          # formatting, routes, localStorage
```

Adding another playback technology means implementing `MediaEngine` and adding a case to
`createEngine` — the UI branches on nothing format-specific.
