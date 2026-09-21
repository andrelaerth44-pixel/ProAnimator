# ProAnimator — Architecture Document

## 1. High-Level Goals

Create a 2D animation engine for Android that rivals Procreate Dreams in:

- Real-time responsiveness
- Brush quality and feel
- Timeline power and fluidity
- Ability to handle large projects without choking
- Professional export quality

## 2. Core Modules

### 2.1 Rendering Layer (`rendering/`)

**Primary path**: Skia  
**High-performance path**: Vulkan (for large canvases and complex compositing)

Responsibilities:
- Canvas surface management
- Layer compositing
- Real-time filters and effects
- Texture streaming for high-resolution content

Key principle: **Never block the UI thread**. All heavy work happens on dedicated render threads.

### 2.2 Engine (`core/engine/`)

The heart of the painting and compositing system.

- `CanvasEngine`: Manages the active drawing surface and layers
- `Compositor`: Blends layers with blend modes, masks, opacity
- `StrokeProcessor`: Handles pressure, tilt, velocity, stabilization
- `HistoryManager`: Undo/Redo with optional infinite history

### 2.3 Timeline (`core/timeline/`)

Three operational modes (directly inspired by Dreams):

| Mode       | Purpose                                      |
|------------|----------------------------------------------|
| Compose    | Organize tracks and content                  |
| Keyframe   | Precise property animation                   |
| Perform    | Record motion/effects in real-time via touch |

Key classes:
- `Timeline`
- `Track`
- `Content` (Drawing, Flipbook, Video, Audio, Text, Group)
- `Keyframe`
- `Playhead`

### 2.4 Brush System (`core/brushes/`)

Data-driven brush engine.

- Brush definitions stored as structured data
- Support for pressure, tilt, rotation, velocity dynamics
- Stabilization and smoothing
- Brush memory (last used size/opacity per brush)

### 2.5 File Format (`.pan`)

**Design goals** (learned from Dreams `.drm`):

- Instant open even for large projects
- Streaming of layer/content data
- Undo history stored inside the file
- Efficient for future cloud sync

Proposed structure:
```
.pan (container)
├── header (version, metadata, stage size, fps)
├── project metadata
├── content/
│   ├── track_001/
│   │   ├── frames/ or keyframes/
│   │   └── assets/
│   └── ...
└── history/ (optional eternal undo)
```

### 2.6 Export Pipeline (`core/export/`)

- Frame-by-frame renderer
- Hardware-accelerated encoding (MediaCodec)
- Support for transparent video (HEVC)
- PNG sequence with alpha
- GIF

## 3. Performance Strategy

1. Tile-based thinking where possible
2. Aggressive resource streaming and caching
3. Multi-threaded stroke processing and compositing
4. Level-of-detail for timeline scrubbing on lower-end devices
5. Prefer GPU for everything that can run on GPU

## 4. Domain Model (Core)

```kotlin
data class Project(
    val id: String,
    val name: String,
    val stageWidth: Int,
    val stageHeight: Int,
    val fps: Float,
    val tracks: List<Track>
)

data class Track(
    val id: String,
    val name: String,
    val contents: List<Content>,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

sealed class Content {
    data class Drawing(...)
    data class Flipbook(...)
    data class Video(...)
    data class Audio(...)
    data class Text(...)
    data class Group(...)
}

data class Keyframe(
    val time: Long,
    val properties: Map<String, Any>,
    val easing: Easing
)
```

## 5. UI Philosophy

- Full-screen canvas when drawing (timeline can be flicked away)
- Heavy use of gestures (pinch, multi-touch selection, stylus pressure)
- Minimal chrome, maximum creative space
- Dark theme first (professional feel)

## 6. Next Immediate Steps

1. Set up Gradle multi-module project
2. Implement basic Skia canvas + stroke capture
3. Create domain models
4. Build minimal timeline UI
5. Design `.pan` format v0.1

---

**This document is living.** Update it as the engine evolves.
