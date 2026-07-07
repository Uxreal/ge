import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Wand2 } from "lucide-react";

export default function PlatformOptimizer() {
  return (
    <div className="space-y-6">
      <PageHeader title="Platform Optimizer" subtitle="Distinct titles, captions, hashtags, and covers tuned per platform — never a copy-paste post." />
      <EmptyState icon={<Wand2 className="size-6" />} description="Per-platform metadata generation is coming together next." />
    </div>
  );
}
