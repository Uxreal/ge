import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate, useSearchParams } from "react-router-dom";
import { Bookmark, Link2, Moon, PlayCircle, Search, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useTheme } from "@/lib/theme";
import { cn } from "@/lib/utils";
import { browseUrlFor } from "../lib/routes";
import { OpenUrlDialog } from "./OpenUrlDialog";

const NAV = [
  { to: "/browse/features", label: "Browse" },
  { to: "/library", label: "Library" },
];

export function AppHeader({ collectionId }: { collectionId?: string }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { theme, toggle } = useTheme();
  const [openUrl, setOpenUrl] = useState(false);
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => setQuery(searchParams.get("q") ?? ""), [searchParams]);

  // "/" focuses search, the way every media library does it.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const typing =
        target?.isContentEditable ||
        ["INPUT", "TEXTAREA", "SELECT"].includes(target?.tagName ?? "");
      if (event.key === "/" && !typing) {
        event.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  return (
    <header className="sticky top-0 z-40 border-b border-border/60 bg-background/85 backdrop-blur-xl">
      <div className="mx-auto flex h-14 max-w-7xl items-center gap-3 px-4 sm:px-6">
        <Link to="/" className="flex shrink-0 items-center gap-2">
          <span className="grid size-7 place-items-center rounded-lg bg-[image:var(--gradient-brand)] text-white shadow-[var(--shadow-glow)]">
            <PlayCircle className="size-4" />
          </span>
          <span className="hidden text-sm font-semibold tracking-tight sm:block">
            Reel
          </span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <form
          className="relative ml-auto min-w-0 flex-1 sm:max-w-sm"
          onSubmit={(event) => {
            event.preventDefault();
            navigate(browseUrlFor(collectionId ?? "all", query));
          }}
        >
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={searchRef}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search free movies…"
            aria-label="Search the archive"
            className="h-9 pl-8"
          />
        </form>

        <Button
          variant="outline"
          size="sm"
          className="shrink-0"
          onClick={() => setOpenUrl(true)}
        >
          <Link2 className="size-4" />
          <span className="hidden sm:inline">Open URL</span>
        </Button>

        <Button
          variant="ghost"
          size="icon"
          className="hidden shrink-0 md:inline-flex"
          aria-label="Saved"
          asChild
        >
          <Link to="/library">
            <Bookmark className="size-4" />
          </Link>
        </Button>

        <Button
          variant="ghost"
          size="icon"
          className="shrink-0"
          aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
          onClick={toggle}
        >
          {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
        </Button>
      </div>

      <OpenUrlDialog open={openUrl} onOpenChange={setOpenUrl} />
    </header>
  );
}
