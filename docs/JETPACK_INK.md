# Optional Jetpack Ink

## Research summary

- **androidx.ink 1.0.0** stable / 1.1.0-alpha
- Modules: authoring-compose (`InProgressStrokes`), brush-compose (`StockBrushes`), geometry, rendering, storage
- Best for low-latency stylus mesh strokes
- ProAnimator keeps **ImageBitmap Flipbook** as source of truth for export/MP4/.pan

## Enable

In `app/build.gradle.kts`:

```kotlin
val ink = "1.0.0"
implementation("androidx.ink:ink-nativeloader:$ink")
implementation("androidx.ink:ink-strokes:$ink")
implementation("androidx.ink:ink-brush:$ink")
implementation("androidx.ink:ink-brush-compose:$ink")
implementation("androidx.ink:ink-authoring-compose:$ink")
implementation("androidx.ink:ink-rendering:$ink")
implementation("androidx.ink:ink-geometry-compose:$ink")
```

Then:

```kotlin
InkFeatureFlags.enabled = true
```

`InkBridge.isAvailable` uses reflection so the project **builds without** these deps.

## Integration path

1. Overlay `InkComposeOverlay` / `InProgressStrokes` on the canvas when `canUse`
2. `onStrokesFinished` → `InkRasterizer.rasterize` / `applyInkStrokeToFlipbook`
3. Keep eraser / onion / export on bitmap path

## Code already in repo

| File | Role |
|------|------|
| `InkBridge` / `InkFeatureFlags` | Optional detection |
| `InkRasterizer` | Pressure segment raster onto ImageBitmap |
| `InkComposeOverlay` | Host slot for InProgressStrokes |
| `applyInkStrokeToFlipbook` | Finish → active layer |

Until deps are present, **BrushEngine + pressure** is the production path.
