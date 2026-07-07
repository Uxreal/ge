import type { Category } from "@/lib/mock/types";
import {
  Lightbulb, Cpu, Bot, Gamepad2, Briefcase, Landmark, Car, ScrollText,
  Eye, Rocket, PawPrint, FlaskConical, CheckCircle2, Wand2, Brain,
  Clapperboard, Flame, Dumbbell, ChefHat, Hammer, GraduationCap,
  BookOpen, Gem, Laugh, Smile, type LucideIcon,
} from "lucide-react";

export const CATEGORY_ICON: Record<Category, LucideIcon> = {
  Facts: Lightbulb,
  Technology: Cpu,
  AI: Bot,
  Gaming: Gamepad2,
  Business: Briefcase,
  Finance: Landmark,
  Cars: Car,
  History: ScrollText,
  Mystery: Eye,
  Space: Rocket,
  Animals: PawPrint,
  Science: FlaskConical,
  Productivity: CheckCircle2,
  "Life Hacks": Wand2,
  Psychology: Brain,
  "Movies & TV": Clapperboard,
  Motivation: Flame,
  Fitness: Dumbbell,
  Cooking: ChefHat,
  DIY: Hammer,
  Educational: GraduationCap,
  Storytelling: BookOpen,
  Luxury: Gem,
  Humor: Laugh,
  Memes: Smile,
};

export function CategoryIcon({ category, className }: { category: Category; className?: string }) {
  const Icon = CATEGORY_ICON[category] ?? Lightbulb;
  return <Icon className={className} />;
}

/** Full Category union as a runtime array, derived from the exhaustive CATEGORY_ICON map. */
export const ALL_CATEGORIES = Object.keys(CATEGORY_ICON) as Category[];
