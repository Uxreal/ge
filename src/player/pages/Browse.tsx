import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { AlertCircle, Loader2, SearchX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { CatalogError, searchCatalog, type CatalogItem } from "../catalog/archive";
import { COLLECTIONS, collectionById } from "../catalog/collections";
import { AppHeader } from "../components/AppHeader";
import { MovieCard, MovieCardSkeleton } from "../components/MovieCard";
import { browseUrlFor } from "../lib/routes";

const PAGE_SIZE = 24;

export default function Browse() {
  const { collectionId = "features" } = useParams();
  const [searchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const tab = collectionById(collectionId);

  const [items, setItems] = useState<CatalogItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Guards against a slow first page landing after a newer request resolved.
  const requestId = useRef(0);

  const load = useCallback(
    async (targetPage: number, append: boolean) => {
      const id = ++requestId.current;
      if (append) setLoadingMore(true);
      else {
        setLoading(true);
        setError(null);
      }

      try {
        const result = await searchCatalog({
          collection: tab.collection,
          query,
          page: targetPage,
          rows: PAGE_SIZE,
          // Relevance ordering only makes sense once there is a search term.
          sort: query.trim() ? "downloads desc" : "downloads desc",
        });
        if (id !== requestId.current) return;

        setItems((current) => (append ? [...current, ...result.items] : result.items));
        setTotal(result.total);
        setPage(targetPage);
      } catch (err) {
        if (id !== requestId.current) return;
        if ((err as Error).name === "AbortError") return;
        setError(
          err instanceof CatalogError
            ? err.message
            : "Something went wrong loading the catalog.",
        );
        if (!append) setItems([]);
      } finally {
        if (id === requestId.current) {
          setLoading(false);
          setLoadingMore(false);
        }
      }
    },
    [tab.collection, query],
  );

  useEffect(() => {
    void load(1, false);
  }, [load]);

  const hasMore = items.length < total;

  return (
    <div className="min-h-screen bg-background">
      <AppHeader collectionId={collectionId} />

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
        <div className="mb-6 space-y-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              {query ? `Results for “${query}”` : tab.label}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {query
                ? `Searching ${tab.collection ? tab.label.toLowerCase() : "everything"} on archive.org.`
                : tab.blurb}
            </p>
          </div>

          <nav className="flex flex-wrap gap-1.5" aria-label="Collections">
            {COLLECTIONS.map((collection) => (
              <Link
                key={collection.id}
                to={browseUrlFor(collection.id, query)}
                className={cn(
                  "rounded-full px-3 py-1.5 text-sm font-medium transition-colors",
                  collection.id === tab.id
                    ? "bg-foreground text-background"
                    : "bg-surface-2 text-muted-foreground hover:bg-accent hover:text-foreground",
                )}
              >
                {collection.label}
              </Link>
            ))}
          </nav>
        </div>

        {error && (
          <div className="mb-6 flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/10 p-4">
            <AlertCircle className="mt-0.5 size-5 shrink-0 text-destructive" />
            <div className="flex-1 space-y-2">
              <p className="text-sm">{error}</p>
              <Button size="sm" variant="outline" onClick={() => void load(1, false)}>
                Retry
              </Button>
            </div>
          </div>
        )}

        {loading ? (
          <Grid>
            {Array.from({ length: 12 }, (_, i) => (
              <MovieCardSkeleton key={i} />
            ))}
          </Grid>
        ) : items.length === 0 && !error ? (
          <div className="grid place-items-center rounded-xl border border-dashed border-border py-20 text-center">
            <SearchX className="mb-3 size-8 text-muted-foreground" />
            <p className="text-sm font-medium">Nothing found here</p>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
              {query
                ? "Try a different search term, or switch to the “Everything” collection."
                : "This collection returned no items."}
            </p>
          </div>
        ) : (
          <>
            <Grid>
              {items.map((item) => (
                <MovieCard key={item.identifier} item={item} />
              ))}
            </Grid>

            {hasMore && (
              <div className="mt-8 flex justify-center">
                <Button
                  variant="outline"
                  disabled={loadingMore}
                  onClick={() => void load(page + 1, true)}
                >
                  {loadingMore && <Loader2 className="size-4 animate-spin" />}
                  Load more
                </Button>
              </div>
            )}

            {items.length > 0 && (
              <p className="mt-4 text-center text-xs text-muted-foreground">
                Showing {items.length} of {total.toLocaleString()} titles from
                archive.org
              </p>
            )}
          </>
        )}
      </main>
    </div>
  );
}

function Grid({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
      {children}
    </div>
  );
}
