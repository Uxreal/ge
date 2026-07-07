import { useState } from "react";
import { GlassPanel } from "@/components/kit/glass-panel";
import { Button } from "@/components/ui/button";
import { InviteMemberDialog } from "./invite-member-dialog";
import { teamMembers, type TeamRole } from "@/lib/mock/channels-extra";
import { UserPlus } from "lucide-react";
import { cn } from "@/lib/utils";

const ROLE_STYLES: Record<TeamRole, string> = {
  Owner: "border-brand/30 bg-brand/10 text-brand",
  Admin: "border-success/30 bg-success/10 text-success",
  Editor: "border-warning/30 bg-warning/10 text-warning",
  Reviewer: "border-border bg-surface/70 text-foreground/80",
  Viewer: "border-border bg-muted/30 text-muted-foreground",
};

export function TeamSection({
  title = "Team workspace", subtitle = "Everyone with access to this workspace.",
}: { title?: string; subtitle?: string }) {
  const [inviteOpen, setInviteOpen] = useState(false);

  return (
    <GlassPanel className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold">{title}</h2>
          <p className="text-xs text-muted-foreground">{subtitle}</p>
        </div>
        <Button size="sm" variant="outline" className="h-8 rounded-lg" onClick={() => setInviteOpen(true)}>
          <UserPlus className="mr-1.5 size-3.5" /> Invite member
        </Button>
      </div>
      <ul className="divide-y divide-border/60">
        {teamMembers.map((m) => (
          <li key={m.id} className="flex items-center gap-3 py-2.5">
            <div className="grid size-8 shrink-0 place-items-center rounded-full bg-brand/10 text-xs font-semibold text-brand ring-1 ring-brand/20">
              {m.initials}
            </div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-medium">{m.name}</div>
              <div className="truncate text-[11px] text-muted-foreground">{m.email}</div>
            </div>
            <span className={cn("rounded-full border px-2 py-0.5 text-[10px] font-medium", ROLE_STYLES[m.role])}>
              {m.role}
            </span>
          </li>
        ))}
      </ul>
      <InviteMemberDialog open={inviteOpen} onOpenChange={setInviteOpen} />
    </GlassPanel>
  );
}
