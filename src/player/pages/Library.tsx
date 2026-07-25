import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bookmark, Clock, Film, Play, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AppHeader } from "../components/AppHeader";
import { formatTime, prettyUrl } from "../lib/format";
import { watchUrlFor } from "../lib/routes";
import {
  clearHistory,
  clearResumePoint,
  getAllResumePoints,
  getHistory,
  getSaved,
  removeHistory,
  toggleSaved,
  type HistoryEntry,
  type ResumePoint,
  type SavedEntry,
} from "../lib/storage";

interface Row extends HistoryEntry {
  resume?: ResumePoint;
}

export default function Library() {
  const [history, setHistory] = useState<Row[]>([]);
  const [saved, setSaved] = useState<SavedEntry[]>([]);

  const refresh = useCallback(() => {
    const resume = getAllResumePoints();
    setHistory(getHistory().map((entry) => ({ ...entry, resume: resume[entry.url] })));
    setSaved(getSaved());
  }, []);

  useEffect(() => {
    document.title = "Library — Reel";
    refresh();
  }, [refresh]);

  const inProgress = history.filter((row) => row.resume);

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />

      <main className="mx-auto max-w-5xl px-4 py-6 sm:px-6">
        <h1 className="mb-1 text-2xl font-semibold tracking-tight">Your library</h1>
        <p className="mb-6 text-sm text-muted-foreground">
          Kept in this browser only — nothing is uploaded anywhere.
        </p>

        <Tabs defaultValue="continue">
          <TabsList>
            <TabsTrigger value="continue">
              Continue watching
              {inProgress.length > 0 && (
                <span className="ml-1.5 text-xs text-muted-foreground">
                  {inProgress.length}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="saved">
              Saved
              {saved.length > 0 && (
                <span className="ml-1.5 text-xs text-muted-foreground">
                  {saved.length}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="history">History</TabsTrigger>
          </TabsList>

          <TabsContent value="continue" className="mt-5">
            {inProgress.length === 0 ? (
              <Empty
                icon={<Clock className="size-7" />}
                title="Nothing in progress"
                body="Videos you stop part-way through show up here so you can pick them back up."
              />
            ) : (
              <List>
                {inProgress.map((row) => (
                  <RowItem
                    key={row.url}
                    row={row}
                    action={
                      <IconAction
                        label="Forget position"
                        onClick={() => {
                          clearResumePoint(row.url);
                          refresh();
                        }}
                      >
                        <Trash2 className="size-4" />
                      </IconAction>
                    }
                  />
                ))}
              </List>
            )}
          </TabsContent>

          <TabsContent value="saved" className="mt-5">
            {saved.length === 0 ? (
              <Empty
                icon={<Bookmark className="size-7" />}
                title="Nothing saved yet"
                body="Use Save on any title to keep it here."
              />
            ) : (
              <List>
                {saved.map((row) => (
                  <RowItem
                    key={row.url}
                    row={row}
                    action={
                      <IconAction
                        label="Remove from library"
                        onClick={() => {
                          toggleSaved(row);
                          refresh();
                        }}
                      >
                        <Trash2 className="size-4" />
                      </IconAction>
                    }
                  />
                ))}
              </List>
            )}
          </TabsContent>

          <TabsContent value="history" className="mt-5 space-y-4">
            {history.length === 0 ? (
              <Empty
                icon={<Film className="size-7" />}
                title="No history"
                body="Everything you play is listed here."
              />
            ) : (
              <>
                <div className="flex justify-end">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      clearHistory();
                      refresh();
                    }}
                  >
                    <Trash2 className="size-4" />
                    Clear history
                  </Button>
                </div>
                <List>
                  {history.map((row) => (
                    <RowItem
                      key={row.url}
                      row={row}
                      action={
                        <IconAction
                          label="Remove from history"
                          onClick={() => {
                            removeHistory(row.url);
                            refresh();
                          }}
                        >
                          <Trash2 className="size-4" />
                        </IconAction>
                      }
                    />
                  ))}
                </List>
              </>
            )}
          </TabsContent>
        </Tabs>
      </main>
    </div>
  );
}

function List({ children }: { children: React.ReactNode }) {
  return <ul className="divide-y divide-border/60 overflow-hidden rounded-xl border border-border/60">{children}</ul>;
}

function RowItem({ row, action }: { row: Row; action: React.ReactNode }) {
  const progress =
    row.resume && row.resume.duration > 0
      ? row.resume.position / row.resume.duration
      : 0;

  return (
    <li className="flex items-center gap-3 bg-card/40 p-3 transition-colors hover:bg-accent/40">
      <Link
        to={watchUrlFor({
          id: row.sourceId,
          url: row.sourceId ? undefined : row.url,
          title: row.title,
        })}
        className="flex min-w-0 flex-1 items-center gap-3 outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <span className="relative grid h-12 w-20 shrink-0 place-items-center overflow-hidden rounded-md bg-surface-2">
          {row.poster ? (
            <img src={row.poster} alt="" className="h-full w-full object-cover" />
          ) : (
            <Play className="size-4 text-muted-foreground" />
          )}
          {progress > 0 && (
            <span className="absolute inset-x-0 bottom-0 h-1 bg-black/50">
              <span
                className="block h-full bg-brand"
                style={{ width: `${Math.min(100, progress * 100)}%` }}
              />
            </span>
          )}
        </span>

        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">{row.title}</span>
          <span className="block truncate text-xs text-muted-foreground">
            {row.resume
              ? `${formatTime(row.resume.position)} of ${formatTime(row.resume.duration)}`
              : prettyUrl(row.url, 72)}
          </span>
        </span>
      </Link>
      {action}
    </li>
  );
}

function IconAction({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <Button variant="ghost" size="icon" aria-label={label} title={label} onClick={onClick}>
      {children}
    </Button>
  );
}

function Empty({
  icon,
  title,
  body,
}: {
  icon: React.ReactNode;
  title: string;
  body: string;
}) {
  return (
    <div className="grid place-items-center rounded-xl border border-dashed border-border py-16 text-center">
      <div className="mb-3 text-muted-foreground">{icon}</div>
      <p className="text-sm font-medium">{title}</p>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">{body}</p>
    </div>
  );
}
