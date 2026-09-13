import type { UserConfig } from "vite";
import react from "@vitejs/plugin-react";

export interface GameConfigOptions {
  base: string;
  title: string;
}

export const createGameConfig = ({ base, title }: GameConfigOptions): UserConfig => {
  const htmlContent = `<!DOCTYPE html>
<html lang="ru">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>${title}</title>
    </head>
    <body>
        <script type="module" src="/src/main.tsx"></script>
    </body>
</html>`;

  return {
    base,
    plugins: [
      react(),
      {
        name: 'virtual-html-plugin',
        configureServer(server: any) {
          server.middlewares.use((req: any, res: any, next: any) => {
            if (req.url === '/' || req.url === '/index.html') {
              res.setHeader('Content-Type', 'text/html');
              return res.end(htmlContent);
            }
            next();
          });
        },
        resolveId(id: string) {
          if (id === 'index.html' || id === '/index.html') {
            return 'index.html';
          }
        },
        load(id: string) {
          if (id === 'index.html') {
            return htmlContent;
          }
        },
      },
    ],
    resolve: {
      dedupe: ['react', 'react-dom'],
    },
    build: {
      emptyOutDir: true,
      rollupOptions: {
        input: 'index.html',
      },
    },
  };
};