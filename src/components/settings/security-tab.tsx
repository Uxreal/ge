import { useState } from "react";
import { GlassPanel } from "@/components/kit/glass-panel";
import { Button } from "@/components/ui/button";
import { credentials as initialCredentials } from "@/lib/mock/settings";
import { Lock, RotateCcw, Trash2, ShieldCheck } from "lucide-react";
import { toast } from "sonner";

export function SecurityTab() {
  const [creds, setCreds] = useState(initialCredentials);

  const rotate = (id: string, name: string) => {
    setCreds((list) => list.map((c) => (c.id === id ? { ...c, lastRotated: "just now" } : c)));
    toast.success("Credential rotated", { description: `${name} — a new value has been generated (mock).` });
  };

  const remove = (id: string, name: string) => {
    setCreds((list) => list.filter((c) => c.id !== id));
    toast("Credential removed", { description: `${name} was disconnected.` });
  };

  return (
    <GlassPanel className="p-5">
      <div className="mb-4">
        <h2 className="text-sm font-semibold">Credential storage</h2>
        <p className="text-xs text-muted-foreground">API keys and OAuth tokens used by connected plugins. Encrypted at rest — this is a mocked view, no real secrets are stored or displayed.</p>
      </div>
      {creds.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          No stored credentials.
        </p>
      ) : (
        <ul className="divide-y divide-border/60">
          {creds.map((c) => (
            <li key={c.id} className="flex flex-wrap items-center gap-3 py-3">
              <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-brand/10 text-brand ring-1 ring-brand/20">
                <Lock className="size-4" />
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium">{c.name}</span>
                  <span className="inline-flex items-center gap-1 rounded-full border border-success/30 bg-success/10 px-1.5 py-0.5 text-[10px] font-medium text-success">
                    <ShieldCheck className="size-3" /> Encrypted
                  </span>
                </div>
                <div className="mt-0.5 font-mono text-xs text-muted-foreground">{c.masked}</div>
                <div className="text-[10px] text-muted-foreground">Rotated {c.lastRotated} · encrypted at rest</div>
              </div>
              <div className="flex gap-1.5">
                <Button size="sm" variant="ghost" className="h-8" onClick={() => rotate(c.id, c.name)}>
                  <RotateCcw className="mr-1.5 size-3.5" /> Rotate
                </Button>
                <Button size="sm" variant="ghost" className="h-8 text-destructive hover:text-destructive" onClick={() => remove(c.id, c.name)}>
                  <Trash2 className="mr-1.5 size-3.5" /> Remove
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </GlassPanel>
  );
}
