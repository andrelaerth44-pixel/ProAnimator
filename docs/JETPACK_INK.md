# Optional Jetpack Ink

## Research summary

- **androidx.ink 1.0.0** stable (Dec 2025) / 1.1.0-alpha available
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

1. Overlay `InProgressStrokes` on the canvas when `canUse`
2. `onStrokesFinished` → rasterize stroke mesh to current Flipbook frame bitmap
3. Keep eraser / onion / export on bitmap path

Until enabled, **BrushEngine + pressure** is the production path.
