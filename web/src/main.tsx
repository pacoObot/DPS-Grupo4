import React from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import { SalaPublica } from "./components/SalaPublica";
import "./style.css";

const sala = window.location.pathname.match(/^\/sala\/([A-Za-z0-9_-]{12})\/?$/);
createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    {sala ? <SalaPublica codigo={sala[1]} /> : <App />}
  </React.StrictMode>,
);
