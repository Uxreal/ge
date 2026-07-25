import type { ReactNode } from "react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

interface ControlButtonProps {
  label: string;
  children: ReactNode;
  onClick?: () => void;
  active?: boolean;
  disabled?: boolean;
  className?: string;
}

/**
 * Every button in the control bar: same hit area, same hover treatment, and a
 * tooltip that doubles as the accessible name.
 */
export function ControlButton({
  label,
  children,
  onClick,
  active,
  disabled,
  className,
}: ControlButtonProps) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          type="button"
          aria-label={label}
          aria-pressed={active}
          disabled={disabled}
          onClick={onClick}
          className={cn(
            "inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-white/90 transition-colors",
            "hover:bg-white/15 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70",
            "disabled:pointer-events-none disabled:opacity-40",
            active && "text-brand",
            "[&_svg]:size-5 [&_svg]:shrink-0",
            className,
          )}
        >
          {children}
        </button>
      </TooltipTrigger>
      <TooltipContent side="top" className="text-xs">
        {label}
      </TooltipContent>
    </Tooltip>
  );
}
