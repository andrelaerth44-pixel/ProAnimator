# ProAnimator — Progress Status

**Overall: 100% core** (ToolMode gap closed + audit)

| Area | % |
|------|---|
| Canvas + Flipbook | 100% |
| Brushes + pressure | 100% |
| Eraser | 100% |
| Zoom / pan / fit | 100% |
| Onion skin | 100% |
| Timeline | 100% |
| Keyframes + Bezier | 100% |
| Perform | 100% |
| Layers | 100% |
| Alpha lock | 100% |
| Palm rejection | 100% |
| .pan + undo meta | 100% |
| MP4 (AVC/HEVC) + audio | 100% |
| Lottie import | 100% |
| Jetpack Ink path | 100% (overlay + raster; deps optional) |
| Tablet UI | 100% |
| SAF pickers | 100% |
| Clip / mask | 100% |
| Transform tool | 100% |
| Audio track | 100% |
| Warp / liquify | 100% |
| Eternal undo in file | 100% |

## Audit

See `docs/AUDIT_100.md`.

## FlipaClip Frame Viewer

See `docs/FLIPACLIP_FRAME_VIEWER.md` — **implemented** as modal `FrameViewerSheet` (grid + multi-select + Copy/Paste/Dup/Del/+F). Wired via Frames button in WorkspaceScreen toolbar. Backed by `FrameClipboard` + `ActiveFramePool` (Pencil2D-inspired, original Kotlin).

## Optional later

- androidx.ink classpath + real InProgressStrokes
- Infinite disk undo (file-backed beyond current UndoArchive)
- Live Lottie track (playback-synced, not only import raster)
