# Fase 7: Workspace Canvas — Planning Detail

## Goal
Node graph via Compose Canvas API yang auto-generate dari struktur project. Toggle show/hide untuk mode "full code". Pan/zoom native tanpa WebView.

## Scope
- File tree → visual node graph (directories = group nodes, files = leaf nodes)
- Compose Canvas API (`androidx.compose.ui.graphics.drawscope`)
- Pan/zoom via `detectTransformGestures`
- Tree layout algorithm (vertical, auto-spaced)
- Toggle show/hide dari toolbar
- Click node → buka file di editor

## Komponen

### Android
- `data/model/CanvasModels.kt` — GraphNode, GraphEdge, CanvasState
- `ui/canvas/CanvasViewModel.kt` — convert FileNode tree → graph nodes + layout
- `ui/canvas/WorkspaceCanvas.kt` — Compose Canvas rendering, pan/zoom, node click
- Integrasi ke `WorkspaceScreen.kt` — toggle button + panel

### Daemon (tidak perlu)
- v1 cukup client-side dari FileNode tree yang sudah ada
- Tidak perlu WS handler tambahan

## Validation
1. File tree muncul sebagai node graph
2. Pan/zoom berfungsi
3. Click node → file terbuka di editor
4. Toggle show/hide dari toolbar
