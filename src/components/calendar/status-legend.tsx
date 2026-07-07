import { ALL_STATUSES, STATUS_DOT, STATUS_LABEL } from "./status";

export function StatusLegend() {
  return (
    <div className="flex flex-wrap items-center gap-3 text-[11px] text-muted-foreground">
      {ALL_STATUSES.map((s) => (
        <span key={s} className="flex items-center gap-1.5">
          <span className={`size-2 rounded-full ${STATUS_DOT[s]}`} />
          {STATUS_LABEL[s]}
        </span>
      ))}
    </div>
  );
}
