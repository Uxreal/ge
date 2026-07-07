import { useState } from "react";
import { GlassPanel } from "@/components/kit/glass-panel";
import { Switch } from "@/components/ui/switch";
import { plugins as initialPlugins } from "@/lib/mock/settings";
import { toast } from "sonner";

export function PluginsTab() {
  const [plugins, setPlugins] = useState(initialPlugins);

  const toggle = (id: string) => {
    setPlugins((list) =>
      list.map((p) => {
        if (p.id !== id) return p;
        const next = !p.enabled;
        toast.success(next ? "Plugin enabled" : "Plugin disabled", { description: p.name });
        return { ...p, enabled: next };
      }),
    );
  };

  return (
    <GlassPanel className="p-5">
      <div className="mb-4">
        <h2 className="text-sm font-semibold">Plugins & integrations</h2>
        <p className="text-xs text-muted-foreground">{plugins.filter((p) => p.enabled).length} of {plugins.length} enabled.</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {plugins.map((p) => (
          <div
            key={p.id}
            className="flex items-start gap-3 rounded-xl border border-border/70 bg-surface/60 p-3.5 transition hover:border-brand/30"
          >
            <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-brand/10 text-[11px] font-semibold text-brand ring-1 ring-brand/20">
              {p.shortLabel}
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between gap-2">
                <span className="truncate text-sm font-medium">{p.name}</span>
                <Switch checked={p.enabled} onCheckedChange={() => toggle(p.id)} />
              </div>
              <p className="mt-0.5 text-xs leading-relaxed text-muted-foreground">{p.description}</p>
              <span className="mt-2 inline-block rounded-md border border-border bg-surface/70 px-1.5 py-0.5 text-[10px] uppercase tracking-wider text-muted-foreground">
                {p.category}
              </span>
            </div>
          </div>
        ))}
      </div>
    </GlassPanel>
  );
}
