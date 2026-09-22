# Phase 3 — ImageBitmap + True Eraser

## What was delivered

### LayerBitmapEngine
- Each layer owns a real `ImageBitmap` (ARGB_8888)
- Drawing happens **directly on the bitmap**
- **True eraser** using `BlendMode.Clear`
- `saveLayer` / `restore` for correct blend isolation
- `CompositingStrategy.Offscreen` on the Compose Canvas (critical for Clear to work)
- Checkerboard background so transparency is visible
- Undo / Redo via bitmap snapshots (30 steps)
- Multiple layers with visibility toggle

### Research applied
- SmartToolFactory Compose Drawing patterns
- Android official BlendMode.Clear docs
- `graphicsLayer { compositingStrategy = Offscreen }` requirement
- Native canvas `saveLayer` for blend isolation

## How to test the true eraser

1. Draw something with a bright color
2. Switch to **Eraser**
3. Erase over the drawing
4. You should see the checkerboard background through the erased area (true transparency)

## Current status

| Feature                        | Status      |
|--------------------------------|-------------|
| ImageBitmap per layer          | ✅          |
| True eraser (BlendMode.Clear)  | ✅          |
| Offscreen compositing          | ✅          |
| Undo/Redo on bitmaps           | ✅          |
| Checkerboard transparency      | ✅          |
| PNG Sequence export            | ✅ (prev)   |
| MP4 export                     | Pending     |
| .pan format                    | Pending     |
| Full Save/Load                 | Pending     |

## Next

1. Integrate bitmap layers with Flipbook frames
2. MP4 export via MediaCodec
3. Complete project Save/Load
4. .pan binary format
