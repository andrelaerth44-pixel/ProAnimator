# Phase 3 — MP4 Export LIVE

## What was delivered

### Mp4Encoder
- H.264 (AVC) via MediaCodec
- MediaMuxer → MP4 container
- Automatic color-format detection (Planar preferred to avoid green stripes)
- Bitmap → YUV420 conversion (BT.601)
- Even dimension enforcement
- Progress callbacks
- EOS handling

### Export UI
- **MP4** button → encodes all Flipbook frames to `.mp4`
- **PNG** button → PNG sequence
- **Frame** button → single PNG
- Live progress bar during export

### Output
```
Android/data/com.proanimator.app/files/exports/
  proanimator_<timestamp>.mp4
  png_sequence_<timestamp>/
  frame_<timestamp>.png
```

## How to test MP4

1. Create 3–10 frames and draw on them
2. Press **MP4**
3. Wait for progress bar
4. File appears in app exports folder

## Full Phase 3 status

| Feature                         | Status |
|---------------------------------|--------|
| ImageBitmap per Flipbook frame  | ✅     |
| True eraser (BlendMode.Clear)   | ✅     |
| Onion Skin with bitmaps         | ✅     |
| PNG Sequence export             | ✅     |
| **MP4 export (MediaCodec)**     | ✅     |
| Save/Load completo              | Next   |
| .pan binary format              | Later  |

## AnimaX note

[lynx-family/animax](https://github.com/lynx-family/animax) is a Lottie + Alpha Video *player* engine (C++). Useful as future reference for Lottie import / off-main-thread render, not a drop-in for our drawing Flipbook.

---

**Phase 3 core export is done.**
