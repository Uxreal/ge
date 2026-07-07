import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { PromptTemplate, TemplateCategory } from "@/lib/mock/settings";

const CATEGORIES: TemplateCategory[] = ["Script", "Fact-check", "SEO", "Thumbnail", "Engagement", "Ideation"];

const BLANK = { name: "", description: "", category: "Script" as TemplateCategory, template: "" };

export function TemplateDialog({
  open, onOpenChange, initial, onSave,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initial?: PromptTemplate;
  onSave: (t: Omit<PromptTemplate, "id"> & { id?: string }) => void;
}) {
  const [form, setForm] = useState<typeof BLANK>(BLANK);

  useEffect(() => {
    if (open) {
      setForm(initial ? { name: initial.name, description: initial.description, category: initial.category, template: initial.template } : BLANK);
    }
  }, [open, initial]);

  const handleSave = () => {
    if (!form.name.trim() || !form.template.trim()) return;
    onSave({ id: initial?.id, ...form });
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="scrollbar-thin max-h-[85vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{initial ? "Edit prompt template" : "New prompt template"}</DialogTitle>
          <DialogDescription>Reusable prompt scaffolding used across the content pipeline.</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label className="text-xs">Name</Label>
              <Input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} className="h-9 rounded-lg" />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Category</Label>
              <Select value={form.category} onValueChange={(v) => setForm((f) => ({ ...f, category: v as TemplateCategory }))}>
                <SelectTrigger className="h-9 rounded-lg"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-1.5">
            <Label className="text-xs">Description</Label>
            <Input value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} className="h-9 rounded-lg" />
          </div>
          <div className="space-y-1.5">
            <Label className="text-xs">Template body</Label>
            <Textarea
              value={form.template}
              onChange={(e) => setForm((f) => ({ ...f, template: e.target.value }))}
              className="min-h-[140px] resize-none font-mono text-xs"
              placeholder="Use {{placeholders}} for variables…"
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSave}>{initial ? "Save changes" : "Create template"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
