# Phase 5 — Zoom/Pan, Pressure, Ink optional, Lottie import

## Research → implementation

### 1. Zoom / Pan
- `detectTransformGestures` / multi-touch when 2+ pointers
- Centroid-aware scale (Compose docs pattern)
- `CanvasViewport.screenToCanvas` maps strokes correctly under zoom
- Single finger = draw; two fingers = zoom/pan

### 2. Real stylus pressure
- `PointerInputChange.pressure` (Compose 1.3+)
- `PointerType.Stylus` vs Touch normalization
- Fed into `StrokePoint.pressure` → BrushPreset size/opacity curves

### 3. Jetpack Ink (optional)
- Module `core/ink` + `InkFeatureFlags` reflection gate
- Docs: `docs/JETPACK_INK.md`
- No hard dependency required to build

### 4. Lottie import
- `com.airbnb.android:lottie` rasterize via `LottieDrawable` per frame
- Output: List<ImageBitmap> → `FlipbookBitmapEngine.loadFrames`
- Inspired by AnimaX workflow (AE → mobile) without C++ engine

## Dependencies to add in app module

```kotlin
implementation("com.airbnb.android:lottie:6.6.2")
// optional ink — see JETPACK_INK.md
```
