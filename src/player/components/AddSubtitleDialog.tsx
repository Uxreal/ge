import { useRef, useState } from "react";
import { FileUp, Link2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  externalSubtitleFromFile,
  externalSubtitleFromUrl,
  type ExternalSubtitle,
} from "../engine/subtitles";

interface AddSubtitleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdd: (subtitle: ExternalSubtitle) => void;
}

export function AddSubtitleDialog({
  open,
  onOpenChange,
  onAdd,
}: AddSubtitleDialogProps) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  const finish = (subtitle: ExternalSubtitle) => {
    onAdd(subtitle);
    setUrl("");
    setError(null);
    onOpenChange(false);
  };

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      finish(await externalSubtitleFromFile(file));
    } catch (err) {
      setError((err as Error).message || "Could not read that file.");
    } finally {
      setBusy(false);
    }
  };

  const handleUrl = async () => {
    if (!url.trim()) return;
    setBusy(true);
    setError(null);
    try {
      finish(await externalSubtitleFromUrl(url.trim()));
    } catch (err) {
      setError(
        (err as Error).message ||
          "Could not fetch that subtitle. The server may block cross-origin requests.",
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Add subtitles</DialogTitle>
          <DialogDescription>
            SubRip (.srt) and WebVTT (.vtt) are both accepted — SubRip is
            converted automatically.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label>From your device</Label>
            <input
              ref={fileInput}
              type="file"
              accept=".srt,.vtt,text/vtt,application/x-subrip"
              className="sr-only"
              onChange={(event) => void handleFile(event.target.files?.[0])}
            />
            <Button
              variant="outline"
              className="w-full justify-start"
              disabled={busy}
              onClick={() => fileInput.current?.click()}
            >
              <FileUp className="size-4" />
              Choose a subtitle file…
            </Button>
          </div>

          <div className="flex items-center gap-3 text-xs uppercase tracking-wide text-muted-foreground">
            <span className="h-px flex-1 bg-border" />
            or
            <span className="h-px flex-1 bg-border" />
          </div>

          <div className="space-y-2">
            <Label htmlFor="subtitle-url">From a URL</Label>
            <div className="flex gap-2">
              <Input
                id="subtitle-url"
                placeholder="https://example.com/subtitles.srt"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") void handleUrl();
                }}
              />
              <Button onClick={() => void handleUrl()} disabled={busy || !url.trim()}>
                {busy ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Link2 className="size-4" />
                )}
                Add
              </Button>
            </div>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
