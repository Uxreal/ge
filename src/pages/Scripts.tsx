import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { PenLine } from "lucide-react";

export default function Scripts() {
  return (
    <div className="space-y-6">
      <PageHeader title="Script Writer" subtitle="Hook-first, retention-optimized scripts across every video length." />
      <EmptyState icon={<PenLine className="size-6" />} description="Multi-length script variations and hook A/B testing are coming together next." />
    </div>
  );
}
