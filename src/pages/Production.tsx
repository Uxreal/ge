import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Workflow } from "lucide-react";

export default function Production() {
  return (
    <div className="space-y-6">
      <PageHeader title="Production" subtitle="The full pipeline — scenes, assets, video, voice, captions, music, thumbnail — as one visual workflow." />
      <EmptyState icon={<Workflow className="size-6" />} description="The drag-and-drop workflow builder and per-stage approvals are coming together next." />
    </div>
  );
}
