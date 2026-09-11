import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      "/api/auth": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/auth/, "/api/auth")
      },
      "/api/infrastructure": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
      "/api/chat": {
        target: "http://localhost:8082",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/chat/, "/api/chat")
      }
    }
  }
})
