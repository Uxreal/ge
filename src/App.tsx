import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@/lib/theme";
import { AppShell } from "@/components/app/shell";

import Dashboard from "@/pages/Dashboard";
import TrendDiscovery from "@/pages/TrendDiscovery";
import Ideas from "@/pages/Ideas";
import Scripts from "@/pages/Scripts";
import FactCheck from "@/pages/FactCheck";
import Production from "@/pages/Production";
import MediaLibrary from "@/pages/MediaLibrary";
import PlatformOptimizer from "@/pages/PlatformOptimizer";
import Calendar from "@/pages/Calendar";
import Publishing from "@/pages/Publishing";
import Analytics from "@/pages/Analytics";
import LearningLoop from "@/pages/LearningLoop";
import Channels from "@/pages/Channels";
import Settings from "@/pages/Settings";
import NotFound from "@/pages/NotFound";

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AppShell>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/trends" element={<TrendDiscovery />} />
            <Route path="/ideas" element={<Ideas />} />
            <Route path="/scripts" element={<Scripts />} />
            <Route path="/fact-check" element={<FactCheck />} />
            <Route path="/production" element={<Production />} />
            <Route path="/media" element={<MediaLibrary />} />
            <Route path="/platform-optimizer" element={<PlatformOptimizer />} />
            <Route path="/calendar" element={<Calendar />} />
            <Route path="/publishing" element={<Publishing />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/learning" element={<LearningLoop />} />
            <Route path="/channels" element={<Channels />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </AppShell>
      </BrowserRouter>
    </ThemeProvider>
  );
}
