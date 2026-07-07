import { useState } from "react";
import { PageHeader } from "@/components/kit/page-header";
import { Button } from "@/components/ui/button";
import { ChannelCard } from "@/components/channels/channel-card";
import { ChannelDetailSheet } from "@/components/channels/channel-detail-sheet";
import { ConnectChannelDialog } from "@/components/channels/connect-channel-dialog";
import { TeamSection } from "@/components/channels/team-section";
import { channels, channelById } from "@/lib/mock/data";
import { Plus } from "lucide-react";

export default function Channels() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [connectOpen, setConnectOpen] = useState(false);

  const selectedChannel = channelById(selectedId ?? undefined) ?? null;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Channels"
        subtitle="Manage every connected account, brand voice, and workspace."
        actions={
          <Button
            className="h-9 rounded-lg text-brand-foreground shadow-glow"
            style={{ background: "var(--gradient-brand)" }}
            onClick={() => setConnectOpen(true)}
          >
            <Plus className="mr-2 size-4" /> Connect new channel
          </Button>
        }
      />

      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Connected channels</h2>
            <p className="text-xs text-muted-foreground">{channels.length} active workspaces · click a card for details.</p>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {channels.map((c) => (
            <ChannelCard key={c.id} channel={c} onSelect={() => setSelectedId(c.id)} />
          ))}
        </div>
      </section>

      <TeamSection />

      <ChannelDetailSheet channel={selectedChannel} onOpenChange={(open) => !open && setSelectedId(null)} />
      <ConnectChannelDialog open={connectOpen} onOpenChange={setConnectOpen} />
    </div>
  );
}
