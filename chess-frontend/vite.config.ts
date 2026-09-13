import { defineConfig } from 'vite';
import { createGameConfig } from 'shared-frontend/config';

export default defineConfig(
  createGameConfig({
    base: '/chess/',
    title: 'Шахматы',
  })
);