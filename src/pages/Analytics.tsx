import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { BarChart3 } from "lucide-react";

export default function Analytics() {
  return (
    <div className="space-y-6">
      <PageHeader title="Analytics" subtitle="Views, retention, RPM, and the hooks and thumbnails driving them." />
      <EmptyState icon={<BarChart3 className="size-6" />} description="The full analytics dashboard is coming together next." />
    </div>
  );
}
