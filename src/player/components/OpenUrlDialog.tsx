import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Clapperboard, Play } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DEMO_STREAMS } from "../catalog/collections";
import { detectStreamKind, streamKindLabel } from "../engine/detect";
import { watchUrlFor } from "../lib/routes";

export function OpenUrlDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const navigate = useNavigate();
  const [value, setValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = (raw: string) => {
    const trimmed = raw.trim();
    if (!trimmed) return;

    let parsed: URL;
    try {
      parsed = new URL(trimmed);
    } catch {
      setError("That does not look like a URL. Include the https:// prefix.");
      return;
    }
    if (!/^https?:$/.test(parsed.protocol)) {
      setError("Only http:// and https:// URLs can be played.");
      return;
    }
    // A page served over HTTPS cannot load media over plain HTTP.
    if (parsed.protocol === "http:" && window.location.protocol === "https:") {
      setError(
        "This page is served over HTTPS, so the browser will block a plain http:// stream. Use an https:// URL.",
      );
      return;
    }

    setError(null);
    setValue("");
    onOpenChange(false);
    navigate(watchUrlFor({ url: trimmed }));
  };

  const detected = value.trim() ? detectStreamKind(value.trim()) : null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Open a stream</DialogTitle>
          <DialogDescription>
            Paste a link to an HLS playlist (.m3u8), a DASH manifest (.mpd), or a
            video file (.mp4, .webm, .ogv).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <Label htmlFor="stream-url">Stream URL</Label>
          <div className="flex gap-2">
            <Input
              id="stream-url"
              autoFocus
              placeholder="https://example.com/master.m3u8"
              value={value}
              onChange={(event) => {
                setValue(event.target.value);
                setError(null);
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") submit(value);
              }}
            />
            <Button onClick={() => submit(value)} disabled={!value.trim()}>
              <Play className="size-4" />
              Play
            </Button>
          </div>
          {error ? (
            <p className="text-sm text-destructive">{error}</p>
          ) : detected ? (
            <p className="text-xs text-muted-foreground">
              Detected format: {streamKindLabel(detected.kind)}
              {!detected.confident &&
                " (a guess — the player will try other formats if it fails)"}
            </p>
          ) : (
            <p className="text-xs text-muted-foreground">
              The stream's server must allow cross-origin playback, otherwise the
              browser will refuse it.
            </p>
          )}
        </div>

        <div className="space-y-2">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Reference streams
          </h3>
          <ul className="grid gap-1.5">
            {DEMO_STREAMS.map((stream) => (
              <li key={stream.url}>
                <button
                  type="button"
                  onClick={() => submit(stream.url)}
                  className="flex w-full items-center gap-3 rounded-lg border border-border/60 bg-card/60 px-3 py-2 text-left transition-colors hover:border-brand/50 hover:bg-accent"
                >
                  <Clapperboard className="size-4 shrink-0 text-muted-foreground" />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">
                      {stream.title}
                    </span>
                    <span className="block truncate text-xs text-muted-foreground">
                      {stream.note}
                    </span>
                  </span>
                  <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
                    {streamKindLabel(stream.kind)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </DialogContent>
    </Dialog>
  );
}
