import { defineConfig } from "vite";
export default defineConfig({
  server: {
    host: "127.0.0.1",
    proxy: {
      "/api": {
        target: process.env.QUIZ_API_PROXY || "http://127.0.0.1:8080",
        changeOrigin: true,
      },
    },
  },
});
