# Phase 3 — Flipbook + ImageBitmap Integration

## What was delivered

### FlipbookBitmapEngine
- Every Flipbook frame owns a real `ImageBitmap`
- Drawing and **true eraser** (`BlendMode.Clear`) work per-frame
- Onion Skin reads previous/next frame bitmaps (red = past, green = future)
- Frame navigation (add, dup, delete, prev/next)
- Undo/Redo per-frame bitmap snapshots
- Playback syncs timeline → flipbook frames
- Checkerboard background for transparency visibility
- `CompositingStrategy.Offscreen` for correct Clear blend

### Unified model
Phase 2 Flipbook + Phase 3 ImageBitmap are now one engine.

## How to test

1. Draw on Frame 1
2. Press **+F** or **▶|** to create/go to Frame 2
3. Draw something else
4. Turn **Onion ON** → see previous frame in red tint
5. Use **Eraser** on any frame → true transparent erase (checkerboard shows)
6. Press Play → frames advance automatically

## Status

| Feature                              | Status |
|--------------------------------------|--------|
| ImageBitmap per Flipbook frame       | ✅     |
| True eraser per frame                | ✅     |
| Onion Skin with bitmaps              | ✅     |
| Frame add/dup/delete                 | ✅     |
| Playback sync                        | ✅     |
| PNG Sequence export (ready to wire)  | ✅     |
| MP4 export                           | Next   |
| Full Save/Load                       | Pending|
| .pan format                          | Pending|

## Next

**MP4 export via MediaCodec** using the frame bitmaps from `getAllBitmaps()`.
