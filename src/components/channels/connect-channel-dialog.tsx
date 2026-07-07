import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { Platform } from "@/lib/mock/types";
import { Youtube, Instagram, Music2, Loader2, type LucideIcon } from "lucide-react";
import { toast } from "sonner";

const PLATFORM_OPTIONS: { platform: Platform; label: string; icon: LucideIcon }[] = [
  { platform: "youtube", label: "YouTube", icon: Youtube },
  { platform: "tiktok", label: "TikTok", icon: Music2 },
  { platform: "instagram", label: "Instagram", icon: Instagram },
];

export function ConnectChannelDialog({
  open, onOpenChange,
}: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const [connecting, setConnecting] = useState<Platform | null>(null);

  const handleConnect = (platform: Platform, label: string) => {
    if (connecting) return;
    setConnecting(platform);
    setTimeout(() => {
      toast.success("Connected", { description: `${label} account linked (mock OAuth flow).` });
      setConnecting(null);
      onOpenChange(false);
    }, 900);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) setConnecting(null);
        onOpenChange(next);
      }}
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Connect new channel</DialogTitle>
          <DialogDescription>Pick a platform to start a mock OAuth connection. Nothing is actually authorized.</DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          {PLATFORM_OPTIONS.map(({ platform, label, icon: Icon }) => {
            const isConnecting = connecting === platform;
            return (
              <button
                key={platform}
                disabled={!!connecting}
                onClick={() => handleConnect(platform, label)}
                className="flex w-full items-center gap-3 rounded-lg border border-border bg-surface/60 px-3 py-2.5 text-left text-sm transition-all duration-150 hover:border-brand/40 hover:bg-surface/80 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-brand/10 text-brand ring-1 ring-brand/20">
                  {isConnecting ? <Loader2 className="size-4 animate-spin" /> : <Icon className="size-4" />}
                </span>
                <span className="flex-1 font-medium">{label}</span>
                <span className="text-xs text-muted-foreground">{isConnecting ? "Connecting…" : "Connect"}</span>
              </button>
            );
          })}
        </div>
      </DialogContent>
    </Dialog>
  );
}
