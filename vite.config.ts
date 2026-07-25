import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";
import type { IncomingMessage, ServerResponse } from "node:http";

/**
 * The repo builds two apps from one Vite project: Vira at `/` (index.html) and
 * the media player at `/player` (player.html).
 *
 * Vite's built-in SPA fallback only knows about index.html, so client-side
 * routes under `/player` (`/player/watch`, `/player/browse/noir`, …) would 404
 * on a hard refresh. This rewrites them to the player's entry document, in
 * both `vite dev` and `vite preview`.
 */
function playerSpaFallback(): Plugin {
  const middleware = (
    req: IncomingMessage,
    _res: ServerResponse,
    next: () => void,
  ) => {
    const pathname = (req.url ?? "").split("?")[0];
    if (pathname === "/player" || pathname.startsWith("/player/")) {
      // Query and hash live in the browser, so dropping them here is safe —
      // the router reads them from window.location.
      req.url = "/player.html";
    }
    next();
  };

  return {
    name: "player-spa-fallback",
    configureServer(server) {
      server.middlewares.use(middleware);
    },
    configurePreviewServer(server) {
      server.middlewares.use(middleware);
    },
  };
}

export default defineConfig({
  plugins: [react(), tailwindcss(), playerSpaFallback()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, "index.html"),
        player: path.resolve(__dirname, "player.html"),
      },
    },
  },
  server: {
    host: true,
    port: 5173,
  },
});
