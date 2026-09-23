# ProAnimator

**2D animation + painting for Android** — Procreate Dreams–class core workflow.

Repo: https://github.com/andrelaerth44-pixel/ProAnimator

**Status: 100% core** — see `docs/STATUS.md` and `docs/AUDIT_100.md`.

---

## Features

| Area | Features |
|------|----------|
| **Canvas** | Multi-layer ImageBitmap Flipbook, eraser `BlendMode.Clear`, checkerboard |
| **Brushes** | Pen, Pencil, Marker, Soft, Ink, Eraser + pressure + smooth |
| **Stylus** | Pressure + palm rejection |
| **Zoom/Pan** | Pinch, pan, Fit, 1:1 |
| **Timeline** | Frames, onion (red/green + range), play |
| **Keyframes** | POS/SCALE/ROT/OPACITY + cubic Bezier editor |
| **Perform** | REC drag / scale / rotation |
| **Layers** | Opacity, blend modes, clip-to-below |
| **Alpha lock** | SrcIn |
| **Warp** | Skia `drawBitmapMesh` liquify |
| **Transform** | Move/scale/rotate + bake |
| **Export** | PNG sequence, MP4 H.264 / optional HEVC, AAC audio mux |
| **Project** | `.pan` PAN2 + undo snapshots in meta |
| **Lottie** | JSON → raster frames |
| **Audio** | SAF pick + playhead sync + MP3→AAC |
| **Ink** | Optional Jetpack Ink path (`docs/JETPACK_INK.md`) |

---

## Modules

```
app/                 WorkspaceScreen + UI
core/engine          Viewport, pressure, ToolMode, warp, transform
core/brushes         BrushEngine
core/timeline        Flipbook, keyframes, Perform, audio
core/export          MP4, AAC, .pan, undo archive
core/lottie          Import
core/ink             Optional Ink bridge
domain/
```

---

## Build

```bash
git clone https://github.com/andrelaerth44-pixel/ProAnimator.git
cd ProAnimator
# Android Studio Ladybug+ or:
./gradlew :app:assembleDebug
```

Min SDK 26 · Target 35 · Kotlin · Jetpack Compose

---

## Docs

- `docs/ARCHITECTURE.md` · `docs/ROADMAP.md` · `docs/PAN_FORMAT.md`
- `docs/WARP.md` · `docs/JETPACK_INK.md`
- `docs/FLIPACLIP_FRAME_VIEWER.md` — estudo Frame Viewer (não implementado)
- `docs/AUDIT_100.md`

---

Built with deep engineering. Inspired by Procreate Dreams. Made for Android.
