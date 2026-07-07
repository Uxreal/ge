import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { TrendingUp } from "lucide-react";

export default function TrendDiscovery() {
  return (
    <div className="space-y-6">
      <PageHeader title="Trend Discovery" subtitle="Signals compounding across YouTube, TikTok, Reddit, X, News, and Google Trends." />
      <EmptyState icon={<TrendingUp className="size-6" />} description="Trend ingestion, virality scoring, and category filters coming together next." />
    </div>
  );
}
