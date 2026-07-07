import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { TeamRole } from "@/lib/mock/channels-extra";
import { toast } from "sonner";

const ROLES: TeamRole[] = ["Admin", "Editor", "Reviewer", "Viewer"];

export function InviteMemberDialog({
  open, onOpenChange,
}: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<TeamRole>("Editor");

  const reset = () => { setEmail(""); setRole("Editor"); };

  const handleSubmit = () => {
    if (!email.trim()) {
      toast.error("Enter an email address");
      return;
    }
    toast.success("Invite sent", { description: `${email} invited as ${role}.` });
    reset();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={(next) => { if (!next) reset(); onOpenChange(next); }}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Invite team member</DialogTitle>
          <DialogDescription>Mocked invite — no email is actually sent.</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label className="text-xs">Email</Label>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="teammate@company.com"
              className="h-9 rounded-lg"
            />
          </div>
          <div className="space-y-1.5">
            <Label className="text-xs">Role</Label>
            <Select value={role} onValueChange={(v) => setRole(v as TeamRole)}>
              <SelectTrigger className="h-9 rounded-lg"><SelectValue /></SelectTrigger>
              <SelectContent>
                {ROLES.map((r) => <SelectItem key={r} value={r}>{r}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSubmit}>Send invite</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
