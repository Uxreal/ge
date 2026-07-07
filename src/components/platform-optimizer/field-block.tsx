import type { ReactNode } from "react";
import { CopyButton } from "./copy-button";

export function FieldBlock({
  label, hint, getCopyText, children,
}: { label: string; hint?: string; getCopyText: () => string; children: ReactNode }) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-muted-foreground">{label}</label>
        <CopyButton getText={getCopyText} />
      </div>
      {children}
      {hint && <p className="text-[11px] text-muted-foreground/80">{hint}</p>}
    </div>
  );
}
