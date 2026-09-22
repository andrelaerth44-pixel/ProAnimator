# Phase 4 — Brushes + Bezier UI + Keyframe Tracks

## Latest commit features

### BezierEditor
- Visual unit-square editor with draggable P1/P2 handles
- Live cubic curve preview
- Presets: Ease, In, Out, InOut, Linear
- Values feed into Keyframe(bx1,by1,bx2,by2)

### KeyframeTrackStrip
- Property tabs: POS_X, POS_Y, SCALE, ROTATION, OPACITY
- Diamond markers on track (color by easing type)
- +KF adds keyframe at current frame with selected easing
- Click keyframe to load Bezier handles into editor

### Easing picker bar
- LINEAR | EASE_IN | EASE_OUT | EASE_IN_OUT | HOLD | BEZIER

### Perform + playback
- REC still records drag position
- Play evaluates all tracks including Bezier interpolation

## How to use Bezier keyframes

1. Open **Bezier** panel
2. Drag red (P1) / green (P2) handles or tap presets
3. Select property (e.g. POS_X)
4. Move to desired frame, press **+KF**
5. Play — motion uses cubic Bezier easing

## Status

| Feature | Status |
|---------|--------|
| Brush presets + smoothing | ✅ |
| Cubic Bezier math | ✅ |
| Bezier handle UI | ✅ |
| Keyframe track strip | ✅ |
| Perform multi-property | ✅ |
| Save keyframes into .pan | Partial (meta ready) |
