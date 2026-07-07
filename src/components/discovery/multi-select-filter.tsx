import { useState } from "react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface MultiSelectFilterProps {
  label: string;
  icon?: React.ReactNode;
  options: string[];
  selected: string[];
  onChange: (next: string[]) => void;
  formatOption?: (opt: string) => string;
  className?: string;
}

/** Reusable popover checkbox multi-select used to filter fixture arrays client-side. */
export function MultiSelectFilter({
  label, icon, options, selected, onChange, formatOption, className,
}: MultiSelectFilterProps) {
  const [open, setOpen] = useState(false);

  const toggle = (opt: string) => {
    onChange(selected.includes(opt) ? selected.filter((o) => o !== opt) : [...selected, opt]);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            "h-9 gap-1.5 rounded-lg border-border bg-surface/60 text-xs font-medium hover:bg-surface/80",
            selected.length > 0 && "border-brand/40 text-brand",
            className,
          )}
        >
          {icon}
          {label}
          {selected.length > 0 && (
            <span className="ml-0.5 rounded-full bg-brand/15 px-1.5 py-0.5 text-[10px] tabular-nums text-brand">
              {selected.length}
            </span>
          )}
          <ChevronDown className="size-3.5 opacity-60" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-56 p-2">
        <div className="mb-1 flex items-center justify-between px-1">
          <span className="text-xs font-semibold text-muted-foreground">{label}</span>
          {selected.length > 0 && (
            <button
              type="button"
              className="text-[10px] text-brand hover:underline"
              onClick={() => onChange([])}
            >
              Clear
            </button>
          )}
        </div>
        <div className="scrollbar-thin max-h-64 space-y-0.5 overflow-y-auto">
          {options.map((opt) => (
            <label
              key={opt}
              className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-surface/70"
            >
              <Checkbox checked={selected.includes(opt)} onCheckedChange={() => toggle(opt)} />
              <span className="truncate">{formatOption ? formatOption(opt) : opt}</span>
            </label>
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}
