import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({
  plugins: [vue({ template: { transformAssetUrls: { includeAbsolute: false } } })],
  server: { port: 5173, strictPort: true, proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } } },
  build: { rollupOptions: { output: { manualChunks(id) { if(id.includes('node_modules/element-plus')) return 'element'; if(id.includes('node_modules/vue')||id.includes('node_modules/@vue')) return 'vendor' } } } }
})
