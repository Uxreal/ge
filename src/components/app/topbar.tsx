import { Bell, Search, Sparkles, PanelLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "./theme-toggle";
import { toast } from "sonner";

export function Topbar({
  onOpenPalette, onToggleSidebar,
}: { onOpenPalette: () => void; onToggleSidebar: () => void }) {
  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-2 border-b border-border/60 bg-background/70 px-3 backdrop-blur-xl sm:px-4">
      <Button variant="ghost" size="icon" className="size-9" onClick={onToggleSidebar} aria-label="Toggle sidebar">
        <PanelLeft className="size-4" />
      </Button>

      <button
        onClick={onOpenPalette}
        className="glass group flex h-9 min-w-0 flex-1 items-center gap-2 rounded-lg px-3 text-left text-sm text-muted-foreground transition hover:border-brand/30 sm:max-w-md"
      >
        <Search className="size-4" />
        <span className="min-w-0 flex-1 truncate">Search anything…</span>
        <kbd className="hidden shrink-0 rounded border border-border bg-muted/40 px-1.5 py-0.5 font-mono text-[10px] sm:inline-block">⌘K</kbd>
      </button>

      <div className="ml-auto flex items-center gap-1.5">
        <Button
          onClick={() => toast.success("Generate", { description: "New video pipeline queued." })}
          className="hidden h-9 gap-2 rounded-lg text-brand-foreground shadow-glow sm:inline-flex"
          style={{ background: "var(--gradient-brand)" }}
        >
          <Sparkles className="size-4" />
          Generate
        </Button>
        <ThemeToggle />
        <Button variant="ghost" size="icon" className="relative size-9 rounded-lg" aria-label="Notifications">
          <Bell className="size-4" />
          <span className="absolute right-2 top-2 size-1.5 rounded-full bg-brand" />
        </Button>
        <div className="ml-1 grid size-9 place-items-center rounded-lg bg-gradient-to-br from-brand to-brand-glow text-xs font-semibold text-brand-foreground">
          JD
        </div>
      </div>
    </header>
  );
}
