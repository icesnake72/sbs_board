import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// 백엔드 CORS 미설정(BE-01) + Refresh 쿠키 SameSite=Strict 를 우회하기 위해
// 개발 서버에서 동일 오리진처럼 보이도록 프록시한다. → docs/04-프론트엔드/API-클라이언트-가이드.md
const BACKEND = 'http://localhost:8090';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(process.cwd(), 'src') },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
      '/images': { target: BACKEND, changeOrigin: true },
      // OAuth 경로는 changeOrigin 을 끈다. true 면 Host 가 localhost:8090 으로 바뀌어
      // 운영(nginx: Host $http_host)과 백엔드가 보는 오리진이 달라진다.
      '/oauth2': { target: BACKEND, changeOrigin: false },
      '/login/oauth2': { target: BACKEND, changeOrigin: false },
    },
  },
});
