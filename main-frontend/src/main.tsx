import { createRoot } from "react-dom/client";
import { StrictMode } from "react";
import "shared-frontend/style.css";
import { App } from "./App";
import "./style.css";
import "./mobile.css";

const rootElement = document.getElementById("app");

if (rootElement) {
  createRoot(rootElement).render(
    <StrictMode>
      <App />
    </StrictMode>
  );
}