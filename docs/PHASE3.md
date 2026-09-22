# Phase 3 — COMPLETE (Export + Save/Load)

## Delivered

### ImageBitmap + Eraser
- Per-frame ImageBitmap
- True eraser (BlendMode.Clear + Offscreen)
- Onion Skin (red/green)

### Export
- PNG Sequence
- MP4 via MediaCodec + MediaMuxer (H.264)

### Save/Load (.pan format)
```
MAGIC "PAN1"
version, width, height, fps, currentFrame
frameCount
for each frame: PNG length + PNG bytes (lossless)
meta JSON (timeline mode, onion, etc.)
```
- GZIP compressed
- Stored in app private `files/projects/`
- **Save** / **Load** buttons in UI
- Project list overlay

### FlipbookBitmapEngine.loadFrames()
Restores full project from .pan into the engine.

## How to test Save/Load

1. Draw on several frames
2. Press **Save** → creates `.pan`
3. Press **Load** → list of projects appears
4. Tap a project → frames restore with drawings intact

## Status

| Feature                    | Status |
|----------------------------|--------|
| ImageBitmap per frame      | ✅     |
| True eraser                | ✅     |
| Onion Skin                 | ✅     |
| PNG export                 | ✅     |
| MP4 export                 | ✅     |
| **Save/Load (.pan)**       | ✅     |
| Keyframe persistence       | Partial (meta ready) |
| Bezier handles UX          | Future |

**Phase 3 core is done.**
