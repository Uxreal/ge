import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { ShieldCheck } from "lucide-react";

export default function FactCheck() {
  return (
    <div className="space-y-6">
      <PageHeader title="Fact Verification" subtitle="Claim-by-claim confidence scoring before a single frame renders." />
      <EmptyState icon={<ShieldCheck className="size-6" />} description="Source cross-referencing and confidence scoring are coming together next." />
    </div>
  );
}
