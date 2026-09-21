# Phase 1 — Progress

## Completed

### Multi-module structure
- `:app`
- `:domain`
- `:core:engine`
- `:core:brushes`
- `:core:timeline`

### Domain Models
- Project, Track, Content, Layer, Keyframe, Stroke, StrokePoint

### Canvas Engine (major upgrade)
- Strokes are now **persisted per layer**
- Active layer selection
- Add / Remove / Clear / Toggle visibility of layers
- Current brush, color and size state
- Real-time stroke capture

### Brush System
- 4 default brushes with fixed IDs
- Technical Pen, Soft Airbrush, Round Brush, Sketch Pencil

### UI (Workspace)
- Top bar
- **Brush selector** (horizontal scroll)
- Main canvas with multi-layer rendering
- **Layer panel** on the right (add, select, visibility, clear)
- Timeline placeholder

## Current limitations (expected in Phase 1)
- Pressure is still fixed at 1.0 (need MotionEvent for real stylus pressure)
- No ImageBitmap yet (strokes are paths — good for now, will migrate later)
- No undo stack implemented yet (structure is ready)
- No project save/load yet

## Next steps inside Phase 1
1. Real stylus pressure via MotionEvent / PointerInput
2. Simple undo (last stroke)
3. Color picker
4. Project save as JSON (temporary format)
5. Better stroke smoothing / stabilization

---

**Status:** Drawing experience is now usable. Layers work. Brushes selectable.
