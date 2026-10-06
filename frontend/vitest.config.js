import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Config separada de vitest.config.js (em vez de meter "test" dentro do
// vite.config.js principal) pra não arriscar interferir no build/dev normal
// do projeto. Roda com: npm test (ver script adicionado no package.json).
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.js"],
  },
});
