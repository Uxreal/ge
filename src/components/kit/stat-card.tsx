import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { GlassPanel } from "./glass-panel";
import { Line, LineChart, ResponsiveContainer } from "recharts";

interface Props {
  label: string;
  value: string;
  delta?: number;
  icon?: ReactNode;
  spark?: number[];
  accent?: string; // css var name w/o --
}

export function StatCard({ label, value, delta, icon, spark, accent = "brand" }: Props) {
  const up = (delta ?? 0) >= 0;
  const data = (spark ?? []).map((v, i) => ({ i, v }));
  return (
    <GlassPanel className="group relative overflow-hidden p-5 transition-all duration-300 hover:border-brand/30 hover:shadow-glow">
      <div className="pointer-events-none absolute inset-x-0 -top-24 h-40 opacity-0 blur-3xl transition-opacity duration-500 group-hover:opacity-40"
        style={{ background: `radial-gradient(circle, var(--${accent}) 0%, transparent 70%)` }}
      />
      <div className="relative flex items-start justify-between">
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">{label}</p>
          <p className="mt-2 text-2xl font-semibold tracking-tight tabular-nums sm:text-3xl">{value}</p>
          {delta !== undefined && (
            <p className={cn("mt-1 text-xs tabular-nums", up ? "text-success" : "text-destructive")}>
              {up ? "▲" : "▼"} {Math.abs(delta)}% vs last week
            </p>
          )}
        </div>
        {icon && (
          <div className="grid size-9 shrink-0 place-items-center rounded-xl bg-brand/10 text-brand ring-1 ring-brand/20">
            {icon}
          </div>
        )}
      </div>
      {data.length > 0 && (
        <div className="relative -mx-1 mt-4 h-10">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
              <Line type="monotone" dataKey="v" stroke={`var(--${accent})`} strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </GlassPanel>
  );
}
