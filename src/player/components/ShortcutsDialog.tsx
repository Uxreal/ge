import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { SHORTCUTS, type Shortcut } from "../hooks/use-shortcuts";

const GROUPS: Shortcut["group"][] = ["Playback", "Navigation", "Audio", "Display"];

export function ShortcutsDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Keyboard shortcuts</DialogTitle>
          <DialogDescription>
            Available whenever the player has focus and you are not typing.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-5 sm:grid-cols-2">
          {GROUPS.map((group) => {
            const items = SHORTCUTS.filter((s) => s.group === group);
            if (items.length === 0) return null;
            return (
              <div key={group} className="space-y-2">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {group}
                </h3>
                <dl className="space-y-1.5">
                  {items.map((shortcut) => (
                    <div
                      key={shortcut.keys}
                      className="flex items-baseline justify-between gap-3 text-sm"
                    >
                      <dt className="text-muted-foreground">{shortcut.description}</dt>
                      <dd className="shrink-0 rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-[11px]">
                        {shortcut.keys}
                      </dd>
                    </div>
                  ))}
                </dl>
              </div>
            );
          })}
        </div>
      </DialogContent>
    </Dialog>
  );
}
