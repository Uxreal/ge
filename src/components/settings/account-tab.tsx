import { useState } from "react";
import { GlassPanel } from "@/components/kit/glass-panel";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Save } from "lucide-react";
import { toast } from "sonner";

export function AccountTab() {
  const [name, setName] = useState("Morgan Reyes");
  const [email, setEmail] = useState("morgan@vira.ai");
  const [role, setRole] = useState("Workspace owner");

  return (
    <GlassPanel className="max-w-xl p-5">
      <div className="mb-4">
        <h2 className="text-sm font-semibold">Profile</h2>
        <p className="text-xs text-muted-foreground">Your account details for this Vira workspace.</p>
      </div>
      <div className="space-y-4">
        <div className="space-y-1.5">
          <Label className="text-xs">Full name</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} className="h-9 rounded-lg" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Email</Label>
          <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="h-9 rounded-lg" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Role</Label>
          <Input value={role} onChange={(e) => setRole(e.target.value)} className="h-9 rounded-lg" />
        </div>
        <Button
          className="text-brand-foreground shadow-glow"
          style={{ background: "var(--gradient-brand)" }}
          onClick={() => toast.success("Profile saved", { description: "Your account details were updated." })}
        >
          <Save className="mr-2 size-4" /> Save changes
        </Button>
      </div>
    </GlassPanel>
  );
}
