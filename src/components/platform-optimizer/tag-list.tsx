export function TagList({ tags }: { tags: string[] }) {
  if (tags.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-1.5">
      {tags.map((t, i) => (
        <span key={i} className="rounded-md border border-border bg-surface/70 px-1.5 py-0.5 text-[10px] font-medium text-foreground/80">
          {t}
        </span>
      ))}
    </div>
  );
}
