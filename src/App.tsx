import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@/lib/theme";
import { AppShell } from "@/components/app/shell";
import Dashboard from "@/pages/Dashboard";

const TrendDiscovery = lazy(() => import("@/pages/TrendDiscovery"));
const Ideas = lazy(() => import("@/pages/Ideas"));
const Scripts = lazy(() => import("@/pages/Scripts"));
const FactCheck = lazy(() => import("@/pages/FactCheck"));
const Production = lazy(() => import("@/pages/Production"));
const MediaLibrary = lazy(() => import("@/pages/MediaLibrary"));
const PlatformOptimizer = lazy(() => import("@/pages/PlatformOptimizer"));
const Calendar = lazy(() => import("@/pages/Calendar"));
const Publishing = lazy(() => import("@/pages/Publishing"));
const Analytics = lazy(() => import("@/pages/Analytics"));
const LearningLoop = lazy(() => import("@/pages/LearningLoop"));
const Channels = lazy(() => import("@/pages/Channels"));
const Settings = lazy(() => import("@/pages/Settings"));
const NotFound = lazy(() => import("@/pages/NotFound"));

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AppShell>
          <Suspense fallback={null}>
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
          </Suspense>
        </AppShell>
      </BrowserRouter>
    </ThemeProvider>
  );
}
