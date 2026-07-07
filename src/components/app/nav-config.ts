import {
  LayoutDashboard, TrendingUp, Lightbulb, PenLine, ShieldCheck, Workflow, Images,
  Wand2, CalendarDays, Send, BarChart3, Brain, Radio, Settings,
} from "lucide-react";

export interface NavItem {
  label: string;
  to: string;
  icon: React.ComponentType<{ className?: string }>;
  shortcut?: string;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

export const NAV: NavGroup[] = [
  {
    label: "Discover",
    items: [
      { label: "Dashboard", to: "/", icon: LayoutDashboard, shortcut: "G D" },
      { label: "Trend Discovery", to: "/trends", icon: TrendingUp, shortcut: "G T" },
      { label: "Ideas", to: "/ideas", icon: Lightbulb, shortcut: "G I" },
    ],
  },
  {
    label: "Create",
    items: [
      { label: "Scripts", to: "/scripts", icon: PenLine },
      { label: "Fact Check", to: "/fact-check", icon: ShieldCheck },
      { label: "Production", to: "/production", icon: Workflow },
      { label: "Media Library", to: "/media", icon: Images },
    ],
  },
  {
    label: "Distribute",
    items: [
      { label: "Platform Optimizer", to: "/platform-optimizer", icon: Wand2 },
      { label: "Calendar", to: "/calendar", icon: CalendarDays },
      { label: "Publishing", to: "/publishing", icon: Send },
    ],
  },
  {
    label: "Improve",
    items: [
      { label: "Analytics", to: "/analytics", icon: BarChart3 },
      { label: "Learning Loop", to: "/learning", icon: Brain },
    ],
  },
  {
    label: "Manage",
    items: [
      { label: "Channels", to: "/channels", icon: Radio },
      { label: "Settings", to: "/settings", icon: Settings },
    ],
  },
];

export const ALL_NAV = NAV.flatMap((g) => g.items);
