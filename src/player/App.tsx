import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Toaster } from "@/components/ui/sonner";
import { ThemeProvider } from "@/lib/theme";
import Watch from "./pages/Watch";

// Browse and Library are secondary screens — keep them out of the initial
// bundle so the player itself loads first.
const Browse = lazy(() => import("./pages/Browse"));
const Library = lazy(() => import("./pages/Library"));

export default function App() {
  return (
    <ThemeProvider>
      {/* The player is served from /player.html, so every route hangs off
          /player — see the SPA fallback in vite.config.ts. */}
      <BrowserRouter basename="/player">
        <Suspense fallback={<div className="min-h-screen bg-background" />}>
          <Routes>
            <Route path="/" element={<Navigate to="/browse/features" replace />} />
            <Route path="/browse" element={<Navigate to="/browse/features" replace />} />
            <Route path="/browse/:collectionId" element={<Browse />} />
            <Route path="/watch" element={<Watch />} />
            <Route path="/library" element={<Library />} />
            <Route path="*" element={<Navigate to="/browse/features" replace />} />
          </Routes>
        </Suspense>
        <Toaster />
      </BrowserRouter>
    </ThemeProvider>
  );
}
