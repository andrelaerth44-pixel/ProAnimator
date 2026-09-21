# Phase 1 — Progress

## Latest completed

### Eraser Tool
- Tool mode: Draw / Eraser
- Visual distinction for eraser strokes
- Size control works for eraser too

### Stroke Smoothing
- Light moving-average smoothing applied on stroke end
- Point filtering by distance to reduce noise

### Tool Mode System
- Clean switch between Draw and Eraser
- UI adapts (hides color/brushes when in Eraser mode)

### UI Polish
- Better top bar with tool buttons
- Clearer visual feedback

## Full Phase 1 feature list so far

- Multi-layer support (add, select, visibility, clear)
- Multiple brushes
- Color picker (9 colors)
- Size slider
- Undo / Redo (50 steps)
- Eraser tool
- Basic stroke smoothing
- Real-time stroke preview

## Still missing / next

1. Real stylus pressure (MotionEvent)
2. Proper eraser (destination-out with ImageBitmap)
3. Project Save / Load
4. Better performance with many strokes

---

**Status:** Drawing + Erasing experience is now solid for prototype.
