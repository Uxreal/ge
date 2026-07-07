import { useEffect, useState } from "react";
import { format } from "date-fns";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { channels, projects } from "@/lib/mock/data";
import type { Platform } from "@/lib/mock/types";
import { toast } from "sonner";

const PLATFORMS: { value: Platform; label: string }[] = [
  { value: "youtube", label: "YouTube" },
  { value: "tiktok", label: "TikTok" },
  { value: "instagram", label: "Instagram" },
];

export interface NewPostInput {
  projectTitle: string;
  channelId: string;
  platform: Platform;
  time: string;
}

export function ScheduleDialog({
  open, onOpenChange, day, onCreate,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  day: Date | undefined;
  onCreate: (post: NewPostInput) => void;
}) {
  const [projectTitle, setProjectTitle] = useState(projects[0]?.title ?? "");
  const [channelId, setChannelId] = useState(channels[0]?.id ?? "");
  const [platform, setPlatform] = useState<Platform>("youtube");
  const [time, setTime] = useState("9:00 AM");

  useEffect(() => {
    if (open) {
      setProjectTitle(projects[0]?.title ?? "");
      setChannelId(channels[0]?.id ?? "");
      setPlatform("youtube");
      setTime("9:00 AM");
    }
  }, [open]);

  const handleSubmit = () => {
    onCreate({ projectTitle, channelId, platform, time });
    toast.success("Post scheduled", {
      description: `${projectTitle} — ${day ? format(day, "MMM d") : "this day"} at ${time}`,
    });
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Schedule new post</DialogTitle>
          <DialogDescription>
            {day ? format(day, "EEEE, MMMM d") : "Pick a day"} — mocked scheduling, nothing is actually published.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label className="text-xs">Project</Label>
            <Select value={projectTitle} onValueChange={setProjectTitle}>
              <SelectTrigger className="h-9 rounded-lg"><SelectValue /></SelectTrigger>
              <SelectContent>
                {projects.map((p) => (
                  <SelectItem key={p.id} value={p.title}>{p.title}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="text-xs">Channel</Label>
            <Select value={channelId} onValueChange={setChannelId}>
              <SelectTrigger className="h-9 rounded-lg"><SelectValue /></SelectTrigger>
              <SelectContent>
                {channels.map((c) => (
                  <SelectItem key={c.id} value={c.id}>{c.avatar} {c.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label className="text-xs">Platform</Label>
              <Select value={platform} onValueChange={(v) => setPlatform(v as Platform)}>
                <SelectTrigger className="h-9 rounded-lg"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {PLATFORMS.map((p) => (
                    <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Time</Label>
              <Input value={time} onChange={(e) => setTime(e.target.value)} placeholder="e.g. 2:30 PM" className="h-9 rounded-lg" />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSubmit}>Schedule post</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
