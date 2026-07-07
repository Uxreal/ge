import { cn } from "@/lib/utils";
import type { HTMLAttributes } from "react";

interface Props extends HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "strong" | "flat";
}

export function GlassPanel({ className, variant = "default", ...rest }: Props) {
  return (
    <div
      className={cn(
        "rounded-2xl",
        variant === "default" && "glass",
        variant === "strong" && "glass-strong",
        variant === "flat" && "bg-card border border-border",
        className,
      )}
      {...rest}
    />
  );
}
