import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Radio } from "lucide-react";

export default function Channels() {
  return (
    <div className="space-y-6">
      <PageHeader title="Channels" subtitle="Manage every connected account, brand voice, and workspace." />
      <EmptyState icon={<Radio className="size-6" />} description="Multi-account management and brand voice profiles are coming together next." />
    </div>
  );
}
