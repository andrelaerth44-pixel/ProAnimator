# Phase 2 — Progress

## Completed

### Timeline Foundation
- Multi-track
- Playhead + Playback (Play/Pause/Loop)
- Seek by click
- FPS + Duration
- Modes: Compose / Keyframe / Perform

### Keyframe System (new)
- `Keyframe` model with EasingType
- `AnimatableProperty` with interpolation
- Supported easings:
  - LINEAR
  - EASE_IN
  - EASE_OUT
  - EASE_IN_OUT (default)
  - HOLD
- `valueAt(frame)` method that correctly interpolates between keyframes
- API to add/update/remove keyframes

### UI
- Keyframe mode shows demo opacity property
- Quick buttons to add keyframes at current frame
- Visual feedback of interpolated value

## How to test Keyframes right now

1. Switch to **Keyframe** mode
2. Go to frame 0 → press **KF 0** (sets opacity = 0)
3. Go to frame 60 → press **+KF** (sets opacity = 1)
4. Press Play → watch the opacity value change with easing

## Next steps in Phase 2

1. Attach real Content (drawings) to tracks
2. Flipbook + Onion Skin
3. Visual keyframes on the timeline tracks
4. More properties (position, scale, rotation)
5. Perform mode (record)

---

**Status:** Keyframe interpolation is working.
