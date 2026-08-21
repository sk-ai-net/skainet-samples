// Makes wasmJsBrowserDevelopmentRun's dev server actually set the COOP/COEP headers Compose's
// Wasm/Skiko multi-threaded renderer needs for cross-origin isolation (SharedArrayBuffer). The
// generated devServer config doesn't set these on its own — index.html/app.html carry the
// coi-serviceworker shim for the production static build (GitHub Pages can't set response
// headers), but that shim triggers an extra reload on first load; setting the headers natively
// here avoids that during local dev.
config.devServer = config.devServer || {};
config.devServer.headers = {
    ...(config.devServer.headers || {}),
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
};
