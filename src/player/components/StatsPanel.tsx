import { useEffect, useState } from "react";
import { X } from "lucide-react";

interface StatsPanelProps {
  getStats: () => Record<string, string | number>;
  extra?: Record<string, string | number>;
  onClose: () => void;
}

/** Live decode/network readout, refreshed once a second while visible. */
export function StatsPanel({ getStats, extra, onClose }: StatsPanelProps) {
  const [stats, setStats] = useState<Record<string, string | number>>({});

  useEffect(() => {
    const update = () => setStats(getStats());
    update();
    const id = window.setInterval(update, 1000);
    return () => window.clearInterval(id);
  }, [getStats]);

  const rows = { ...stats, ...extra };

  return (
    <div className="absolute left-3 top-3 w-64 rounded-lg bg-black/80 p-3 font-mono text-[11px] text-white/85 shadow-lg backdrop-blur-sm">
      <div className="mb-2 flex items-center justify-between">
        <span className="font-sans text-xs font-medium uppercase tracking-wide text-white/60">
          Stats
        </span>
        <button
          type="button"
          aria-label="Hide stats"
          onClick={onClose}
          className="rounded p-0.5 text-white/60 hover:bg-white/10 hover:text-white"
        >
          <X className="size-3.5" />
        </button>
      </div>
      <dl className="space-y-1">
        {Object.entries(rows).map(([key, value]) => (
          <div key={key} className="flex items-baseline justify-between gap-3">
            <dt className="shrink-0 text-white/50">{key}</dt>
            <dd className="truncate text-right tabular-nums">{String(value)}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
