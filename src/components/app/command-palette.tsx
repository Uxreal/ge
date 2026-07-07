import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator,
} from "@/components/ui/command";
import { NAV } from "./nav-config";
import { Sparkles, Wand2, PenLine, TrendingUp, Zap } from "lucide-react";
import { toast } from "sonner";

export function CommandPalette({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const navigate = useNavigate();
  const go = (to: string) => { onOpenChange(false); navigate(to); };
  const run = (label: string) => { onOpenChange(false); toast.success(label, { description: "Mock action fired." }); };

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput placeholder="Search actions, pages, ideas…" />
      <CommandList>
        <CommandEmpty>No results.</CommandEmpty>
        <CommandGroup heading="Actions">
          <CommandItem onSelect={() => run("Generate new video")}><Sparkles className="mr-2 size-4 text-brand" />Generate new video</CommandItem>
          <CommandItem onSelect={() => run("Scan for viral trends")}><TrendingUp className="mr-2 size-4" />Scan for viral trends</CommandItem>
          <CommandItem onSelect={() => run("Draft 10 script variations")}><PenLine className="mr-2 size-4" />Draft 10 script variations</CommandItem>
          <CommandItem onSelect={() => run("Optimize scheduled posts")}><Wand2 className="mr-2 size-4" />Optimize scheduled posts</CommandItem>
          <CommandItem onSelect={() => run("Run learning loop")}><Zap className="mr-2 size-4" />Run learning loop</CommandItem>
        </CommandGroup>
        <CommandSeparator />
        {NAV.map((group) => (
          <CommandGroup key={group.label} heading={group.label}>
            {group.items.map((item) => (
              <CommandItem key={item.to} onSelect={() => go(item.to)}>
                <item.icon className="mr-2 size-4" />
                {item.label}
              </CommandItem>
            ))}
          </CommandGroup>
        ))}
      </CommandList>
    </CommandDialog>
  );
}

export function useCommandPalette() {
  const [open, setOpen] = useState(false);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.key === "k" || e.key === "K") && (e.metaKey || e.ctrlKey)) {
        e.preventDefault(); setOpen((o) => !o);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  return { open, setOpen };
}
