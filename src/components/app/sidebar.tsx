import { Link, useLocation } from "react-router-dom";
import { NAV } from "./nav-config";
import { channels } from "@/lib/mock/data";
import { ChevronsUpDown, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";
import { useState } from "react";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function AppSidebar({ collapsed }: { collapsed: boolean }) {
  const { pathname } = useLocation();
  const [active, setActive] = useState(channels[0]);

  return (
    <aside
      className={cn(
        "sticky top-0 z-30 hidden h-screen shrink-0 flex-col border-r border-sidebar-border bg-sidebar transition-[width] duration-300 md:flex",
        collapsed ? "w-[68px]" : "w-64",
      )}
    >
      {/* Brand + workspace switcher */}
      <div className={cn("flex items-center gap-2 px-3 py-3", collapsed && "justify-center")}>
        <Link to="/" className="flex items-center gap-2">
          <div className="grid size-8 shrink-0 place-items-center rounded-xl text-brand-foreground shadow-glow"
            style={{ background: "var(--gradient-brand)" }}
          >
            <Sparkles className="size-4" />
          </div>
          {!collapsed && (
            <div className="min-w-0 leading-tight">
              <div className="text-sm font-semibold tracking-tight">Vira</div>
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Content OS</div>
            </div>
          )}
        </Link>
      </div>

      {!collapsed && (
        <div className="px-3 pb-3">
          <DropdownMenu>
            <DropdownMenuTrigger className="glass flex w-full items-center justify-between gap-2 rounded-xl px-2.5 py-2 text-left transition hover:border-brand/30">
              <div className="flex min-w-0 items-center gap-2">
                <div className="grid size-7 shrink-0 place-items-center rounded-lg text-sm" style={{ backgroundColor: active.color + "33" }}>
                  <span>{active.avatar}</span>
                </div>
                <div className="min-w-0">
                  <div className="truncate text-xs font-medium">{active.name}</div>
                  <div className="truncate text-[10px] text-muted-foreground">{active.handle}</div>
                </div>
              </div>
              <ChevronsUpDown className="size-3.5 shrink-0 text-muted-foreground" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-60">
              <DropdownMenuLabel>Channels</DropdownMenuLabel>
              <DropdownMenuSeparator />
              {channels.map((c) => (
                <DropdownMenuItem key={c.id} onClick={() => setActive(c)}>
                  <span className="mr-2">{c.avatar}</span>
                  <span className="flex-1 truncate">{c.name}</span>
                  <span className="text-[10px] text-muted-foreground">{c.category}</span>
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}

      <nav className="scrollbar-thin flex-1 overflow-y-auto px-2 pb-4">
        {NAV.map((group) => (
          <div key={group.label} className="mb-4">
            {!collapsed && (
              <div className="px-2 pb-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground/70">
                {group.label}
              </div>
            )}
            <ul className="space-y-0.5">
              {group.items.map((item) => {
                const isActive = pathname === item.to;
                const Icon = item.icon;
                return (
                  <li key={item.to}>
                    <Link
                      to={item.to}
                      className={cn(
                        "group relative flex items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm transition-all duration-150",
                        collapsed && "justify-center px-0",
                        isActive
                          ? "bg-sidebar-accent text-sidebar-accent-foreground"
                          : "text-muted-foreground hover:bg-sidebar-accent/60 hover:text-foreground",
                      )}
                    >
                      {isActive && (
                        <span className="absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-r bg-brand" />
                      )}
                      <Icon className={cn("size-4 shrink-0", isActive && "text-brand")} />
                      {!collapsed && <span className="truncate">{item.label}</span>}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      {!collapsed && (
        <div className="border-t border-sidebar-border p-3">
          <div className="rounded-xl border border-border/60 bg-gradient-to-br from-brand/15 to-transparent p-3">
            <div className="text-xs font-semibold">Autopilot</div>
            <div className="mt-0.5 text-[11px] text-muted-foreground">6 videos being produced right now.</div>
            <div className="mt-2 flex items-center gap-1.5 text-[10px] text-brand">
              <span className="size-1.5 animate-pulse rounded-full bg-brand" />
              Running
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
