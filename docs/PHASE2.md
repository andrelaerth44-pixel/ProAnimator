# Phase 2 — Current Status

## Completed

### Classic Onion Skin Colors
- Previous frames → **Red / Orange** tones
- Next frames → **Green / Cyan** tones
- Opacity falloff by distance

### Expanded Keyframe Properties
- `OPACITY`
- `POSITION_X` / `POSITION_Y`
- `SCALE`
- `ROTATION`
- All with full interpolation (Linear, EaseIn, EaseOut, EaseInOut, Hold)

### Perform Mode Foundation
- Recording state (`isRecording`)
- Start / Stop recording
- `recordProperty()` API ready to capture live values while playing

### Flipbook + Timeline
- Flipbook fully functional
- Frame strip + controls
- Strokes go directly into current Flipbook frame

## How to test

**Onion Skin**
1. Draw on frame 1
2. Add frame and draw something else
3. Turn Onion ON → previous = red, next = green

**Keyframes**
1. Switch to Keyframe mode
2. Use +Op / Op0 / +Sc / +Rot at different frames
3. Play and watch values change

**Perform**
1. Switch to Perform mode
2. Press Rec → it starts playing + recording
3. (Next step: actually capture drag gestures into keyframes)

## Next remaining pieces
- Make Perform mode capture real touch movement into Position keyframes
- Visual keyframes on the timeline tracks
- Apply Scale / Rotation / Position visually on the canvas content
- Better content-to-track binding

---

**Phase 2 is now very solid.**
