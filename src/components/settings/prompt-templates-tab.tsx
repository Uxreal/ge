import { useState } from "react";
import { GlassPanel } from "@/components/kit/glass-panel";
import { Button } from "@/components/ui/button";
import { TemplateDialog } from "./template-dialog";
import { promptTemplates as initialTemplates, type PromptTemplate } from "@/lib/mock/settings";
import { Plus, Pencil, Trash2, FileText } from "lucide-react";
import { toast } from "sonner";

let nextId = 1000;

export function PromptTemplatesTab() {
  const [templates, setTemplates] = useState<PromptTemplate[]>(initialTemplates);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<PromptTemplate | undefined>(undefined);

  const openCreate = () => { setEditing(undefined); setDialogOpen(true); };
  const openEdit = (t: PromptTemplate) => { setEditing(t); setDialogOpen(true); };

  const handleSave = (t: Omit<PromptTemplate, "id"> & { id?: string }) => {
    if (t.id) {
      setTemplates((list) => list.map((x) => (x.id === t.id ? { ...x, ...t, id: t.id! } : x)));
      toast.success("Template saved", { description: t.name });
    } else {
      const created: PromptTemplate = { ...t, id: `pt-custom-${nextId++}` };
      setTemplates((list) => [created, ...list]);
      toast.success("Template created", { description: t.name });
    }
  };

  const handleDelete = (t: PromptTemplate) => {
    setTemplates((list) => list.filter((x) => x.id !== t.id));
    toast("Template deleted", { description: t.name });
  };

  return (
    <GlassPanel className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold">Custom prompt templates</h2>
          <p className="text-xs text-muted-foreground">{templates.length} reusable templates for scripts, SEO, and more.</p>
        </div>
        <Button size="sm" className="h-8 rounded-lg text-brand-foreground shadow-glow" style={{ background: "var(--gradient-brand)" }} onClick={openCreate}>
          <Plus className="mr-1.5 size-3.5" /> New template
        </Button>
      </div>

      {templates.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          No templates yet — create your first one.
        </p>
      ) : (
        <ul className="space-y-2">
          {templates.map((t) => (
            <li key={t.id} className="rounded-xl border border-border/70 bg-surface/60 p-3.5 transition hover:border-brand/30">
              <div className="flex items-start gap-3">
                <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-brand/10 text-brand ring-1 ring-brand/20">
                  <FileText className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium">{t.name}</span>
                    <span className="rounded-md border border-border bg-surface/70 px-1.5 py-0.5 text-[10px] uppercase tracking-wider text-muted-foreground">
                      {t.category}
                    </span>
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{t.description}</p>
                </div>
                <div className="flex shrink-0 gap-1">
                  <Button size="sm" variant="ghost" className="h-8" onClick={() => openEdit(t)}>
                    <Pencil className="size-3.5" />
                  </Button>
                  <Button size="sm" variant="ghost" className="h-8 text-destructive hover:text-destructive" onClick={() => handleDelete(t)}>
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              </div>
              <pre className="scrollbar-thin mt-2 max-h-24 overflow-y-auto overflow-x-hidden whitespace-pre-wrap break-words rounded-lg border border-border/60 bg-surface/40 p-2 font-mono text-[11px] leading-relaxed text-foreground/75">
                {t.template}
              </pre>
            </li>
          ))}
        </ul>
      )}

      <TemplateDialog open={dialogOpen} onOpenChange={setDialogOpen} initial={editing} onSave={handleSave} />
    </GlassPanel>
  );
}
