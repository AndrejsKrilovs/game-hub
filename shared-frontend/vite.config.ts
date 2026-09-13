import { defineConfig } from 'vite';
import { resolve } from 'path';
import { fileURLToPath } from 'url';
import react from "@vitejs/plugin-react";

const __dirname = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  plugins: [react()],
  build: {
    emptyOutDir: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'SharedFrontend',
      fileName: () => 'index.min.js',
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        /^react($|\/)/,
        /^react-dom($|\/)/,
      ],
      output: { assetFileNames: 'index.min.[ext]' },
    },
    minify: true,
    sourcemap: false,
    cssCodeSplit: false,
  },
});