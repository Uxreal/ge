import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  Bookmark,
  BookmarkCheck,
  ExternalLink,
  Link2,
  Loader2,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import {
  archiveDetailsUrl,
  CatalogError,
  fetchItem,
  type ItemDetail,
  type PlayableFile,
} from "../catalog/archive";
import { AppHeader } from "../components/AppHeader";
import { VideoPlayer } from "../components/VideoPlayer";
import { formatBytes, formatTime, prettyUrl, titleFromUrl } from "../lib/format";
import { absoluteWatchUrl, parseWatchTarget } from "../lib/routes";
import { isSaved, recordHistory, toggleSaved } from "../lib/storage";
import type { PlayerSource } from "../hooks/use-player";

export default function Watch() {
  const [searchParams] = useSearchParams();
  const target = useMemo(() => parseWatchTarget(searchParams), [searchParams]);

  const [detail, setDetail] = useState<ItemDetail | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [selectedFile, setSelectedFile] = useState<PlayableFile | null>(null);
  /** Set when switching source files, to resume where the last one stopped. */
  const [startAt, setStartAt] = useState<number | undefined>(target.t);
  const [saved, setSaved] = useState(false);

  const playerRef = useRef<HTMLDivElement>(null);

  /* ------------------------------------------------- resolve archive item -- */

  useEffect(() => {
    if (!target.id) {
      setDetail(null);
      setSelectedFile(null);
      return;
    }

    const controller = new AbortController();
    setLoadingDetail(true);
    setDetailError(null);

    fetchItem(target.id, controller.signal)
      .then((result) => {
        setDetail(result);
        if (result.files.length === 0) {
          setDetailError(
            "This item has no files a browser can play. Its only videos are in formats (MKV, AVI, MPEG-2) that browsers cannot decode.",
          );
        }
        // fetchItem returns files best-first.
        setSelectedFile(result.files[0] ?? null);
      })
      .catch((err) => {
        if ((err as Error).name === "AbortError") return;
        setDetailError(
          err instanceof CatalogError ? err.message : "Could not load this title.",
        );
      })
      .finally(() => setLoadingDetail(false));

    return () => controller.abort();
  }, [target.id]);

  /* ------------------------------------------------------- player source -- */

  const source: PlayerSource | null = useMemo(() => {
    if (target.url) {
      return {
        url: target.url,
        title: target.title || titleFromUrl(target.url),
        startAt,
      };
    }
    if (selectedFile && detail) {
      return {
        url: selectedFile.url,
        title: detail.item.title,
        poster: detail.item.poster,
        sourceId: detail.item.identifier,
        startAt,
      };
    }
    return null;
  }, [target.url, target.title, selectedFile, detail, startAt]);

  const title = source?.title ?? target.title ?? "Loading…";

  useEffect(() => {
    if (!source) return;
    document.title = `${source.title ?? "Playing"} — Reel`;
    recordHistory({
      url: source.url,
      title: source.title ?? source.url,
      poster: source.poster,
      sourceId: source.sourceId,
    });
    setSaved(isSaved(source.url));
  }, [source]);

  const onSave = useCallback(() => {
    if (!source) return;
    const nowSaved = toggleSaved({
      url: source.url,
      title: source.title ?? source.url,
      poster: source.poster,
      sourceId: source.sourceId,
    });
    setSaved(nowSaved);
    toast(nowSaved ? "Saved to your library" : "Removed from your library");
  }, [source]);

  const copyLink = useCallback(async () => {
    if (!source) return;
    const video = playerRef.current?.querySelector("video");
    const at = video?.currentTime ?? 0;
    const link = absoluteWatchUrl({
      id: source.sourceId,
      url: source.sourceId ? undefined : source.url,
      title: source.title,
      t: at > 5 ? at : undefined,
    });

    try {
      await navigator.clipboard.writeText(link);
      toast("Link copied", {
        description: at > 5 ? `Starts at ${formatTime(at)}` : undefined,
      });
    } catch {
      // Clipboard access needs a secure context and permission; show the link
      // so it can still be copied by hand.
      toast("Copy this link", { description: link });
    }
  }, [source]);

  const changeFile = (file: PlayableFile) => {
    const video = playerRef.current?.querySelector("video");
    setStartAt(video?.currentTime && video.currentTime > 5 ? video.currentTime : undefined);
    setSelectedFile(file);
  };

  const nothingToPlay = !target.url && !target.id;

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-0 py-0 sm:px-6 sm:py-6">
        <div ref={playerRef} className="overflow-hidden sm:rounded-2xl sm:shadow-[var(--shadow-elevated)]">
          {nothingToPlay ? (
            <EmptyPlayer
              message="No stream selected."
              hint="Pick something from Browse, or paste a stream URL."
            />
          ) : loadingDetail && !source ? (
            <EmptyPlayer message="Resolving stream…" spinner />
          ) : detailError && !source ? (
            <EmptyPlayer message={detailError} icon />
          ) : (
            <VideoPlayer source={source} />
          )}
        </div>

        <div className="space-y-6 px-4 py-5 sm:px-0">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0 space-y-1">
              <h1 className="text-xl font-semibold leading-tight tracking-tight">
                {title}
              </h1>
              <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
                {detail?.item.year && <span>{detail.item.year}</span>}
                {detail?.item.creator && (
                  <>
                    <Dot />
                    <span className="truncate">{detail.item.creator}</span>
                  </>
                )}
                {detail?.item.runtime && (
                  <>
                    <Dot />
                    <span>{formatTime(detail.item.runtime)}</span>
                  </>
                )}
                {target.url && !detail && (
                  <span className="truncate font-mono text-xs">
                    {prettyUrl(target.url)}
                  </span>
                )}
              </p>
            </div>

            <div className="flex shrink-0 flex-wrap items-center gap-2">
              <Button variant="outline" size="sm" onClick={onSave} disabled={!source}>
                {saved ? (
                  <BookmarkCheck className="size-4 text-brand" />
                ) : (
                  <Bookmark className="size-4" />
                )}
                {saved ? "Saved" : "Save"}
              </Button>
              <Button variant="outline" size="sm" onClick={() => void copyLink()} disabled={!source}>
                <Link2 className="size-4" />
                Copy link
              </Button>
              {detail && (
                <Button variant="ghost" size="sm" asChild>
                  <a
                    href={archiveDetailsUrl(detail.item.identifier)}
                    target="_blank"
                    rel="noreferrer noopener"
                  >
                    <ExternalLink className="size-4" />
                    archive.org
                  </a>
                </Button>
              )}
            </div>
          </div>

          {detailError && source && (
            <div className="flex items-start gap-3 rounded-xl border border-warning/30 bg-warning/10 p-3 text-sm">
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-warning" />
              <span>{detailError}</span>
            </div>
          )}

          {/* Source picker — the Archive publishes several derivatives per item
              and the best one depends on the connection. */}
          {detail && detail.files.length > 1 && (
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm font-medium">Source file</span>
              <Select
                value={selectedFile?.name}
                onValueChange={(name) => {
                  const file = detail.files.find((f) => f.name === name);
                  if (file) changeFile(file);
                }}
              >
                <SelectTrigger className="w-full max-w-md">
                  <SelectValue placeholder="Choose a file" />
                </SelectTrigger>
                <SelectContent>
                  {detail.files.map((file) => (
                    <SelectItem key={file.name} value={file.name}>
                      <span className="flex w-full items-center gap-2">
                        <span className="flex-1 truncate">{file.label}</span>
                        <span className="text-xs text-muted-foreground">
                          {formatBytes(file.sizeBytes)}
                        </span>
                      </span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Badge variant="secondary" className="font-mono text-[10px]">
                {detail.files.length} derivatives
              </Badge>
            </div>
          )}

          {detail?.subtitles && detail.subtitles.length > 0 && (
            <p className="text-sm text-muted-foreground">
              This item ships {detail.subtitles.length} subtitle file
              {detail.subtitles.length === 1 ? "" : "s"}. Add one from the player's
              settings menu → Subtitles → Add subtitle file, using{" "}
              <span className="font-mono text-xs">{detail.subtitles[0].url}</span>.
            </p>
          )}

          {detail?.item.description && (
            <div className="max-w-3xl space-y-2">
              <h2 className="text-sm font-semibold">About</h2>
              <p className="whitespace-pre-line text-sm leading-relaxed text-muted-foreground">
                {detail.item.description}
              </p>
            </div>
          )}

          <div>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/browse/features">
                <ArrowLeft className="size-4" />
                Back to browse
              </Link>
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
}

function Dot() {
  return <span aria-hidden className="text-muted-foreground/50">·</span>;
}

function EmptyPlayer({
  message,
  hint,
  spinner,
  icon,
}: {
  message: string;
  hint?: string;
  spinner?: boolean;
  icon?: boolean;
}) {
  return (
    <div className="grid aspect-video w-full place-items-center bg-black px-6 text-center">
      <div className="max-w-md space-y-2">
        {spinner && <Loader2 className="mx-auto size-8 animate-spin text-white/70" />}
        {icon && <AlertCircle className="mx-auto size-8 text-destructive" />}
        <p className="text-sm text-white/80">{message}</p>
        {hint && <p className="text-sm text-white/50">{hint}</p>}
      </div>
    </div>
  );
}
