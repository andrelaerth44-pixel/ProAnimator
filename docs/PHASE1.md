# Phase 1 — Progress

## Completed in this commit

### Multi-module structure
- `:app`
- `:domain`
- `:core:engine`
- `:core:brushes`
- `:core:timeline`

### Domain Models
- `Project`
- `Track`
- `Content` (Drawing, Flipbook, Group)
- `Layer` + BlendMode
- `Keyframe` + Easing
- `Stroke` + `StrokePoint` (with pressure & tilt support)

### Core Engine
- `CanvasEngine`
  - Layer management
  - Stroke capture (start / add / end)
  - Basic Undo/Redo command pattern ready
  - StateFlow for reactive UI

### Brush System
- `Brush` data class
- Categories (Sketch, Ink, Paint, Airbrush...)
- Default brushes (Technical Pen, Soft Airbrush, Round)

### Timeline skeleton
- `TimelineEngine`
- Modes: COMPOSE / KEYFRAME / PERFORM
- Play / Pause / Seek

### App UI
- Dark theme professional
- Basic drawing canvas with finger/stylus
- Top bar + Timeline placeholder

## Next inside Phase 1

1. Real pressure from MotionEvent (Stylus)
2. Persist strokes into layer bitmaps (Skia or Compose ImageBitmap)
3. Multiple layers working visually
4. Simple project save (JSON for now)
5. Brush selection UI

---

**Status:** Foundation is solid and ready to evolve.
