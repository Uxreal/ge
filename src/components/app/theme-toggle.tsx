import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/lib/theme";
import { Button } from "@/components/ui/button";

export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  return (
    <Button
      variant="ghost" size="icon"
      onClick={toggle}
      aria-label="Toggle theme"
      className="relative size-9 rounded-lg text-muted-foreground hover:text-foreground"
    >
      <Sun className={`size-4 transition-all duration-300 ${theme === "dark" ? "-rotate-90 scale-0 opacity-0" : "rotate-0 scale-100 opacity-100"}`} />
      <Moon className={`absolute size-4 transition-all duration-300 ${theme === "dark" ? "rotate-0 scale-100 opacity-100" : "rotate-90 scale-0 opacity-0"}`} />
    </Button>
  );
}
