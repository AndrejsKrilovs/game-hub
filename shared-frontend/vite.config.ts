import { defineConfig } from 'vite';
import { resolve } from 'path';
import { fileURLToPath } from 'url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  build: {
    emptyOutDir: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'SharedFrontend',
      fileName: () => 'index.min.js',
      formats: ['es'],
    },
    rollupOptions: {
      output: { assetFileNames: 'index.min.[ext]' }
    },
    minify: true,
    sourcemap: false,
    cssCodeSplit: false
  }
});