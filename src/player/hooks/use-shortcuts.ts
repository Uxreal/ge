import { useEffect } from "react";

export interface ShortcutHandlers {
  togglePlay: () => void;
  seekBy: (delta: number) => void;
  seekToFraction: (fraction: number) => void;
  volumeBy: (delta: number) => void;
  toggleMute: () => void;
  toggleFullscreen: () => void;
  togglePip: () => void;
  toggleCaptions: () => void;
  rateBy: (direction: 1 | -1) => void;
  toggleHelp: () => void;
  onActivity: () => void;
}

export interface Shortcut {
  keys: string;
  description: string;
  group: "Playback" | "Navigation" | "Audio" | "Display";
}

export const SHORTCUTS: Shortcut[] = [
  { keys: "Space / K", description: "Play or pause", group: "Playback" },
  { keys: "J / L", description: "Back or forward 10 seconds", group: "Playback" },
  { keys: "← / →", description: "Back or forward 5 seconds", group: "Playback" },
  { keys: "Shift + ← / →", description: "Back or forward 60 seconds", group: "Playback" },
  { keys: "< / >", description: "Slow down or speed up", group: "Playback" },
  { keys: "Home / End", description: "Jump to start or end", group: "Navigation" },
  { keys: "0 – 9", description: "Jump to 0%–90% of the video", group: "Navigation" },
  { keys: "↑ / ↓", description: "Volume up or down", group: "Audio" },
  { keys: "M", description: "Mute or unmute", group: "Audio" },
  { keys: "C", description: "Toggle subtitles", group: "Display" },
  { keys: "F", description: "Toggle fullscreen", group: "Display" },
  { keys: "I", description: "Toggle picture-in-picture", group: "Display" },
  { keys: "?", description: "Show this help", group: "Display" },
];

/** Typing in a field should never scrub the video. */
function isTypingTarget(target: EventTarget | null): boolean {
  const el = target as HTMLElement | null;
  if (!el) return false;
  if (el.isContentEditable) return true;
  const tag = el.tagName;
  return tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT";
}

export function useShortcuts(handlers: ShortcutHandlers, enabled = true) {
  useEffect(() => {
    if (!enabled) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (isTypingTarget(event.target)) return;
      if (event.metaKey || event.ctrlKey || event.altKey) return;

      const { key } = event;
      let handled = true;

      switch (key) {
        case " ":
        case "k":
        case "K":
          handlers.togglePlay();
          break;
        case "j":
        case "J":
          handlers.seekBy(-10);
          break;
        case "l":
        case "L":
          handlers.seekBy(10);
          break;
        case "ArrowLeft":
          handlers.seekBy(event.shiftKey ? -60 : -5);
          break;
        case "ArrowRight":
          handlers.seekBy(event.shiftKey ? 60 : 5);
          break;
        case "ArrowUp":
          handlers.volumeBy(0.05);
          break;
        case "ArrowDown":
          handlers.volumeBy(-0.05);
          break;
        case "Home":
          handlers.seekToFraction(0);
          break;
        case "End":
          handlers.seekToFraction(1);
          break;
        case "m":
        case "M":
          handlers.toggleMute();
          break;
        case "f":
        case "F":
          handlers.toggleFullscreen();
          break;
        case "i":
        case "I":
          handlers.togglePip();
          break;
        case "c":
        case "C":
          handlers.toggleCaptions();
          break;
        case ",":
        case "<":
          handlers.rateBy(-1);
          break;
        case ".":
        case ">":
          handlers.rateBy(1);
          break;
        case "?":
          handlers.toggleHelp();
          break;
        default:
          if (/^[0-9]$/.test(key)) handlers.seekToFraction(Number(key) / 10);
          else handled = false;
      }

      if (handled) {
        event.preventDefault();
        handlers.onActivity();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [handlers, enabled]);
}
