import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Images } from "lucide-react";

export default function MediaLibrary() {
  return (
    <div className="space-y-6">
      <PageHeader title="Media Library" subtitle="Copyright-safe assets, generated scenes, and voice takes in one place." />
      <EmptyState icon={<Images className="size-6" />} description="Asset browsing and duplicate-content detection are coming together next." />
    </div>
  );
}
