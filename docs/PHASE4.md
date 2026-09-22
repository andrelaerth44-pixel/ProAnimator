# Phase 4 — Brushes + Bezier Keyframes + Perform

## Research sources applied

- **Jetpack Ink** StockBrushes (pressure, marker, opacity curves)
- **SmartToolFactory** path properties + BlendMode.Clear
- **DrawBox / ArtMaker** pressure sampling ideas
- **Compose CubicBezierEasing** + CSS cubic-bezier Newton solve
- **keyframesWithSpline** concept for smooth multi-keyframe motion

## Delivered

### BrushPreset + BrushLibrary
| Brush | Behavior |
|-------|----------|
| Pen | Pressure → size |
| Pencil | Pressure → size + opacity |
| Marker | Wide, semi-transparent |
| Soft | Soft edge, low opacity |
| Ink | High size variation with pressure |
| Eraser | BlendMode.Clear |

### StrokeSmoother
- Distance filter
- EMA smoothing tunable per brush

### BrushEngine
- Draws pressure-aware strokes onto ImageBitmap
- Compatible with Flipbook + true eraser + MP4/PNG export

### KeyframeInterpolator
- LINEAR, EASE_IN/OUT/IN_OUT, HOLD
- **BEZIER** with (x1,y1,x2,y2) control points
- Newton-Raphson cubic solve (CSS-accurate)

### PerformEngine
- Records POS_X, POS_Y, SCALE, ROTATION, OPACITY
- Live evaluate at playhead
- Drag-to-record position

## Integration notes

Wire `BrushEngine` into `FlipbookBitmapEngine.endStroke()` and
`PerformEngine.evaluate(frame)` on timeline tick.

## Next

- UI brush picker chips
- Visual Bezier handle editor
- Jetpack Ink optional path (advanced)
