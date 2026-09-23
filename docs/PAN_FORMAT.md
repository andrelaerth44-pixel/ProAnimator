# ProAnimator `.pan` format

## Container (binary, GZIP)

```
MAGIC          4 bytes  "PAN1"
version        int32    1
width          int32
height         int32
fps            float32
currentFrame   int32
frameCount     int32
for each frame:
  pngLength     int32
  pngBytes      pngLength bytes   (lossless ARGB)
metaLength     int32
metaJson       metaLength bytes  (UTF-8 JSON)
```

## Meta JSON (schema 2)

```json
{
  "schema": 2,
  "brushId": "pen",
  "onionEnabled": true,
  "timelineMode": "COMPOSE",
  "tracks": {
    "POS_X": [
      {
        "frame": 0,
        "value": 0.0,
        "easing": "BEZIER",
        "bx1": 0.42,
        "by1": 0.0,
        "bx2": 0.58,
        "by2": 1.0
      }
    ],
    "POS_Y": [],
    "SCALE": [],
    "ROTATION": [],
    "OPACITY": []
  }
}
```

## What Save/Load restores

| Data | Restored |
|------|----------|
| Flipbook frame bitmaps | ✅ PNG lossless |
| Current frame index | ✅ |
| FPS | ✅ |
| All keyframe tracks + Bezier handles | ✅ |
| Active brush id | ✅ |
| Onion on/off | ✅ |
| Timeline mode | ✅ |

## Location

`context.filesDir/projects/*.pan`
