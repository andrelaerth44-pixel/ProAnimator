# Phase 2 — Progress

## Completed in this iteration

### Flipbook System
- `Flipbook` + `FlipbookFrame` models
- `FlipbookEngine` with full frame management:
  - Add frame
  - Duplicate current frame
  - Delete frame
  - Clear frame
  - Next / Previous frame
  - Auto-add frame when going past the end

### Onion Skin
- Previous frames (configurable count)
- Next frames (configurable count)
- Opacity falloff (older frames more transparent)
- Toggle ON/OFF

### UI
- Frame strip at the bottom (click to jump)
- Frame counter
- Onion Skin toggle
- Flipbook controls (add, dup, del, clear, prev, next)

### Drawing integration
- Every stroke drawn is automatically added to the **current Flipbook frame**

## How to use right now

1. Draw something on Frame 1
2. Press **+Frame** or **▶|** (next)
3. Draw on the new frame
4. Turn **Onion ON** to see previous frames as ghost
5. Navigate between frames with the strip or arrows

## Current full Phase 2 features

- Timeline (playhead, play/pause, modes)
- Keyframe system + interpolation (Linear / Ease In/Out/In-Out)
- **Flipbook + Onion Skin**

## Next

1. Better visual separation of onion skin colors (classic red/green)
2. Link Flipbook to Timeline tracks properly
3. Perform mode
4. More properties for keyframes (position, scale...)

---

**Status:** You can already do proper frame-by-frame animation with onion skin.
