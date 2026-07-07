import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Send } from "lucide-react";

export default function Publishing() {
  return (
    <div className="space-y-6">
      <PageHeader title="Publishing Center" subtitle="Queue, preview, and cross-post with platform-specific metadata." />
      <EmptyState icon={<Send className="size-6" />} description="The publishing queue and retry/preview tools are coming together next." />
    </div>
  );
}
