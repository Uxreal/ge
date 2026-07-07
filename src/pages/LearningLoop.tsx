import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Brain } from "lucide-react";

export default function LearningLoop() {
  return (
    <div className="space-y-6">
      <PageHeader title="Learning Loop" subtitle="What's working, distilled into the next batch of ideas — with you in the loop." />
      <EmptyState icon={<Brain className="size-6" />} description="Performance-driven insights and recommendation tuning are coming together next." />
    </div>
  );
}
