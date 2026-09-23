# Optional Jetpack Ink

## Enable (app/build.gradle.kts)

```kotlin
val ink = "1.1.0-alpha08" // or latest from developer.android.com/jetpack/androidx/releases/ink
implementation("androidx.ink:ink-nativeloader:$ink")
implementation("androidx.ink:ink-strokes:$ink")
implementation("androidx.ink:ink-brush:$ink")
implementation("androidx.ink:ink-brush-compose:$ink")
implementation("androidx.ink:ink-authoring-compose:$ink")
implementation("androidx.ink:ink-rendering:$ink")
implementation("androidx.ink:ink-geometry-compose:$ink")
```

```kotlin
InkFeatureFlags.enabled = true
```

## Compose API

```kotlin
InProgressStrokes(
    defaultBrush = brush,
    pointerEventToWorldTransform = inverseViewportMatrix,
    onStrokesFinished = { strokes ->
        // rasterize → applyInkStrokeToFlipbook / InkRasterizer
    }
)
```

See: https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-draw-stroke

## Already in repo

| File | Role |
|------|------|
| InkBridge / InkFeatureFlags | Optional detection |
| InkRasterizer | Pressure raster |
| InkComposeOverlay | Host slot |
| applyInkStrokeToFlipbook | Finish → layer |

Without deps, BrushEngine + pressure is the production path.
