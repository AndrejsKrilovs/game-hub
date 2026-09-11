import { defineConfig } from "vite";
import { createGameConfig } from "shared-frontend";

export default defineConfig(
  createGameConfig({ base: "/chess/", title: "Шахматы" })
);