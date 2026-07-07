import { Copy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";

export function CopyButton({ getText, label = "Copy" }: { getText: () => string; label?: string }) {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(getText());
      toast.success("Copied to clipboard");
    } catch {
      toast.error("Couldn't copy", { description: "Select the text manually instead." });
    }
  };

  return (
    <Button variant="ghost" size="sm" className="h-7 gap-1.5 px-2 text-xs text-muted-foreground hover:text-foreground" onClick={handleCopy}>
      <Copy className="size-3.5" /> {label}
    </Button>
  );
}
