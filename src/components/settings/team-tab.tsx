import { Link } from "react-router-dom";
import { TeamSection } from "@/components/channels/team-section";
import { ArrowRight } from "lucide-react";

export function TeamTab() {
  return (
    <div className="space-y-3">
      <TeamSection title="Workspace members" subtitle="Roles and access for everyone on this Vira workspace." />
      <p className="flex items-center gap-1.5 px-1 text-xs text-muted-foreground">
        Looking for per-channel access instead?
        <Link to="/channels" className="inline-flex items-center gap-1 text-brand hover:underline">
          Manage channels <ArrowRight className="size-3" />
        </Link>
      </p>
    </div>
  );
}
