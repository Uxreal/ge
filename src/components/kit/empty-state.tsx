import type { ReactNode } from "react";
import { Sparkles } from "lucide-react";
import { GlassPanel } from "./glass-panel";

interface Props {
  icon?: ReactNode;
  title?: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({
  icon = <Sparkles className="size-6" />,
  title = "Coming together…",
  description = "This surface is scaffolded and ready. We'll bring it to life next.",
  action,
}: Props) {
  return (
    <GlassPanel className="flex flex-col items-center justify-center gap-4 px-8 py-20 text-center">
      <div className="grid size-14 place-items-center rounded-2xl bg-brand/10 text-brand ring-1 ring-brand/20">
        {icon}
      </div>
      <div className="max-w-md space-y-1.5">
        <h3 className="text-lg font-semibold tracking-tight">{title}</h3>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      {action}
    </GlassPanel>
  );
}
