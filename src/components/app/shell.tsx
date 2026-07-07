import { useEffect, useState, type ReactNode } from "react";
import { AppSidebar } from "./sidebar";
import { Topbar } from "./topbar";
import { CommandPalette, useCommandPalette } from "./command-palette";
import { Toaster } from "@/components/ui/sonner";

export function AppShell({ children }: { children: ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  const { open, setOpen } = useCommandPalette();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.key === "b" || e.key === "B") && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        setCollapsed((c) => !c);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div className="relative flex min-h-screen w-full bg-background text-foreground">
      {/* Ambient glow backdrop */}
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -left-40 top-0 h-[520px] w-[520px] rounded-full blur-[140px] opacity-30" style={{ background: "var(--brand)" }} />
        <div className="absolute right-[-160px] top-[30%] h-[420px] w-[420px] rounded-full blur-[140px] opacity-20" style={{ background: "var(--brand-glow)" }} />
      </div>

      <AppSidebar collapsed={collapsed} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenPalette={() => setOpen(true)} onToggleSidebar={() => setCollapsed((c) => !c)} />
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-7xl animate-fade-in">{children}</div>
        </main>
      </div>

      <CommandPalette open={open} onOpenChange={setOpen} />
      <Toaster />
    </div>
  );
}
