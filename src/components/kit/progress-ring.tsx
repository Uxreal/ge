export function ProgressRing({
  value, size = 40, stroke = 4, className,
}: { value: number; size?: number; stroke?: number; className?: string }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const offset = c - (value / 100) * c;
  return (
    <svg width={size} height={size} className={className}>
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="var(--color-border)" strokeWidth={stroke} />
      <circle
        cx={size/2} cy={size/2} r={r} fill="none"
        stroke="var(--brand)" strokeWidth={stroke} strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={offset}
        transform={`rotate(-90 ${size/2} ${size/2})`}
        style={{ transition: "stroke-dashoffset 500ms ease" }}
      />
      <text x="50%" y="52%" dominantBaseline="middle" textAnchor="middle"
        style={{ fontSize: size * 0.28, fontWeight: 600, fill: "var(--foreground)" }}
      >
        {value}
      </text>
    </svg>
  );
}
