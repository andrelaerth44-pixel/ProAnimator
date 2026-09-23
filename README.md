# ProAnimator

**2D animation + painting for Android** — inspired by Procreate Dreams workflows.

Repo: https://github.com/andrelaerth44-pixel/ProAnimator

---

## What works today (v0.5.0-alpha)

| Area | Features |
|------|----------|
| **Canvas** | ImageBitmap per Flipbook frame, true eraser (`BlendMode.Clear`), checkerboard |
| **Brushes** | Pen, Pencil, Marker, Soft, Ink, Eraser + pressure curves + EMA smooth |
| **Stylus** | Real `PointerInputChange.pressure` |
| **Zoom/Pan** | Pinch zoom (centroid), two-finger pan, **1:1** reset |
| **Timeline** | Flipbook frames, onion skin (red/green), play |
| **Keyframes** | POS_X/Y, SCALE, ROTATION, OPACITY + cubic **Bezier** editor |
| **Perform** | REC drag → record position tracks |
| **Export** | PNG sequence, **MP4** (MediaCodec H.264) |
| **Project** | **`.pan`** Save/Load (frames + keyframes + brush + onion) |
| **Lottie** | Import JSON → raster frames into Flipbook |
| **Jetpack Ink** | Optional bridge (reflection); enable via deps + flag |

---

## Modules

```
app/
core/engine      # viewport, pressure stroke, bitmap layers
core/brushes     # BrushPreset, BrushEngine, smoother
core/timeline    # Flipbook, keyframes, Perform, Bezier math
core/export      # MP4, PNG, ProjectSerializer (.pan)
core/lottie      # Lottie → ImageBitmap frames
core/ink         # Optional Jetpack Ink gate
domain/
```

---

## Build

```bash
git clone https://github.com/andrelaerth44-pixel/ProAnimator.git
cd ProAnimator
# Open in Android Studio (Ladybug+) or:
./gradlew :app:assembleDebug
```

**Lottie test:** put a Bodymovin JSON in `app/src/main/assets/demo.json` → tap **Lottie**.

**Optional Ink:** see `docs/JETPACK_INK.md`.

---

## Docs

- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/PAN_FORMAT.md`
- `docs/PHASE3.md` … `docs/PHASE5.md`
- `docs/JETPACK_INK.md`

---

## Stack

Kotlin · Jetpack Compose · MediaCodec · Lottie Android · (optional androidx.ink)

Min SDK 26 · Target 35

---

Built with deep engineering. Inspired by Procreate Dreams. Made for Android.
