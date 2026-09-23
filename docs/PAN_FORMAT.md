# ProAnimator `.pan` format

## PAN2 (current)

```
MAGIC          4 bytes  "PAN2"
version        int32    2
width          int32
height         int32
fps            float32
currentFrame   int32
frameCount     int32
for each frame:
  layerCount     int32
  activeLayer    int32
  for each layer:
    name         UTF
    visible      boolean
    opacity      float32
    pngLength    int32
    pngBytes     (layer bitmap)
metaLength     int32
metaJson       UTF-8 JSON (schema 3: tracks, brush, onion, mode)
```

Container is **GZIP** compressed.

## PAN1 (legacy, still loadable)

Composite PNG per frame + meta schema 2.

## What is restored

| Data | PAN2 |
|------|------|
| Per-layer bitmaps | ✅ |
| Layer name / visible / opacity | ✅ |
| Active layer index | ✅ |
| Keyframes + Bezier | ✅ |
| Brush, onion, timeline mode | ✅ |

## Export

- Internal: `filesDir/projects/*.pan`
- SAF: **CreateDocument** → user-chosen location
