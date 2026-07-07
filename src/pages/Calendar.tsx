import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { CalendarDays } from "lucide-react";

export default function Calendar() {
  return (
    <div className="space-y-6">
      <PageHeader title="Content Calendar" subtitle="Every scheduled post, across every channel, in one view." />
      <EmptyState icon={<CalendarDays className="size-6" />} description="The drag-and-drop content calendar is coming together next." />
    </div>
  );
}
