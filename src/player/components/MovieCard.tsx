import { useState } from "react";
import { Link } from "react-router-dom";
import { Film, Play } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CatalogItem } from "../catalog/archive";
import { formatCount, formatTime } from "../lib/format";
import { watchUrlFor } from "../lib/routes";

interface MovieCardProps {
  item: CatalogItem;
  /** 0–1 watch progress, drawn as a bar across the poster. */
  progress?: number;
}

export function MovieCard({ item, progress }: MovieCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  return (
    <Link
      to={watchUrlFor({ id: item.identifier, title: item.title })}
      className="group flex flex-col gap-2 rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="relative aspect-[2/3] overflow-hidden rounded-xl border border-border/60 bg-surface-2">
        {imageFailed ? (
          <div className="grid h-full place-items-center text-muted-foreground">
            <Film className="size-8" />
          </div>
        ) : (
          <img
            src={item.poster}
            alt=""
            loading="lazy"
            onError={() => setImageFailed(true)}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        )}

        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
        <div className="absolute inset-0 grid place-items-center opacity-0 transition-opacity group-hover:opacity-100">
          <span className="grid size-12 place-items-center rounded-full bg-black/60 ring-1 ring-white/25 backdrop-blur-sm">
            <Play className="size-5 translate-x-0.5 fill-white text-white" />
          </span>
        </div>

        {item.runtime !== undefined && (
          <span className="absolute bottom-2 right-2 rounded bg-black/70 px-1.5 py-0.5 font-mono text-[10px] text-white tabular-nums">
            {formatTime(item.runtime)}
          </span>
        )}

        {progress !== undefined && progress > 0 && (
          <div className="absolute inset-x-0 bottom-0 h-1 bg-black/50">
            <div
              className="h-full bg-brand"
              style={{ width: `${Math.min(100, progress * 100)}%` }}
            />
          </div>
        )}
      </div>

      <div className="min-w-0 space-y-0.5">
        <h3
          className={cn(
            "line-clamp-2 text-sm font-medium leading-snug transition-colors",
            "group-hover:text-brand",
          )}
          title={item.title}
        >
          {item.title}
        </h3>
        <p className="truncate text-xs text-muted-foreground">
          {[item.year, item.downloads ? `${formatCount(item.downloads)} views` : null]
            .filter(Boolean)
            .join(" · ")}
        </p>
      </div>
    </Link>
  );
}

export function MovieCardSkeleton() {
  return (
    <div className="flex flex-col gap-2">
      <div className="aspect-[2/3] animate-pulse rounded-xl bg-surface-2" />
      <div className="h-3.5 w-4/5 animate-pulse rounded bg-surface-2" />
      <div className="h-3 w-1/3 animate-pulse rounded bg-surface-2" />
    </div>
  );
}
