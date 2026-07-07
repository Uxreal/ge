import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Lightbulb } from "lucide-react";

export default function Ideas() {
  return (
    <div className="space-y-6">
      <PageHeader title="Idea Generator" subtitle="Hundreds of scored, original concepts across every category." />
      <EmptyState icon={<Lightbulb className="size-6" />} description="The scored idea feed and category explorer are coming together next." />
    </div>
  );
}
