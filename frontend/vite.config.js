import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        // ⚠️ Windows 下 localhost 可能被 Node 优先解析为 IPv6 的 ::1，而后端 Tomcat 默认只绑 IPv4
        //     强制写死 127.0.0.1 可以避免 "connect ECONNREFUSED ::1:8080"
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.error('[vite proxy error]', req.url, '->', err.message)
          })
        }
      },
      '/uploads': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})