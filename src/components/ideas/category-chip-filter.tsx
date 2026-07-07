import type { Category } from "@/lib/mock/types";
import { ALL_CATEGORIES, CategoryIcon } from "@/components/discovery/category-icons";
import { cn } from "@/lib/utils";

interface Props {
  selected: Category[];
  onChange: (next: Category[]) => void;
}

/** Visual chip grid across the full Category union, used to filter the idea feed. */
export function CategoryChipFilter({ selected, onChange }: Props) {
  const toggle = (cat: Category) => {
    onChange(selected.includes(cat) ? selected.filter((c) => c !== cat) : [...selected, cat]);
  };

  const chipClass = (active: boolean) =>
    cn(
      "inline-flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors duration-150",
      active
        ? "border-brand/40 bg-brand/10 text-brand"
        : "border-border bg-surface/60 text-muted-foreground hover:border-brand/25 hover:text-foreground",
    );

  return (
    <div className="scrollbar-thin flex flex-wrap gap-1.5">
      <button type="button" onClick={() => onChange([])} className={chipClass(selected.length === 0)}>
        All categories
      </button>
      {ALL_CATEGORIES.map((cat) => (
        <button key={cat} type="button" onClick={() => toggle(cat)} className={chipClass(selected.includes(cat))}>
          <CategoryIcon category={cat} className="size-3.5" />
          {cat}
        </button>
      ))}
    </div>
  );
}
