# Phase 2 — Timeline Foundation

## What was delivered

### TimelineEngine
- Multi-track support
- Playhead (currentFrame)
- Playback (Play / Pause / Loop)
- FPS control
- Duration in frames
- Three modes: **Compose / Keyframe / Perform**
- Seek by click on timeline
- Previous / Next frame

### UI Integration
- Full timeline panel at the bottom
- Mode switcher (Compose, Keyframe, Perform)
- Play / Pause / Step controls
- Visual playhead (red line)
- Track list
- Frame counter + FPS display

### Architecture
- Timeline is completely independent from CanvasEngine
- Ready to receive Content (Drawings, Flipbooks, etc.)
- Playback ticker using coroutines

## Next inside Phase 2

1. Connect canvas drawings to timeline tracks (as Content)
2. Flipbook mode (frame-by-frame)
3. Onion Skin
4. Basic Keyframe system (position / opacity)
5. Perform mode (record motion)

---

**Status:** Timeline foundation is live and playable.
