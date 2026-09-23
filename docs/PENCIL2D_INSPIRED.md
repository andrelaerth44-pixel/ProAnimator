# Pencil2D-inspired features (original Kotlin)

No GPL code was copied from [pencil2d/pencil](https://github.com/pencil2d/pencil).
Ideas reimplemented under ProAnimator’s own license model.

## ActiveFramePool

`core/timeline/ActiveFramePool.kt`

- LRU ranking of frame indices (capacity default 24)
- `touch` / `touchWindow` on scrub (onion radius)
- Ready for future disk unload of cold frames

## FrameClipboard + Frame Viewer

- Multi-select copy/paste of LayerStacks
- Modal UI: `FrameViewerSheet` (FlipaClip-style, not permanent chrome)
- `appendFrameWithStack` / `insertFrameAt` / `deleteFrames` on Flipbook

## Wiring

```kotlin
var showFrames by remember { mutableStateOf(false) }
val clipboard = remember { FrameClipboard() }
// Toolbar: ToolButton("Frames") { showFrames = true }
if (showFrames) {
    FrameViewerSheet(flipbook, clipboard, onClose = { showFrames = false }, onStatus = { statusMessage = it })
}
```
