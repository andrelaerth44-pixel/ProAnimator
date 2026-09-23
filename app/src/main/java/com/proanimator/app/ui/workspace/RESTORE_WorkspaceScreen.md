# Emergency restore

WorkspaceScreen was briefly overwritten. Full fixed file with Frame Viewer is in the conversation artifacts.

**Already on main (intact):**
- ActiveFramePool.kt
- FrameClipboard.kt
- FrameViewerSheet.kt
- FlipbookBitmapEngine (append/insert/delete + pool touch)
- docs/PENCIL2D_INSPIRED.md

**To restore WorkspaceScreen:**
1. Get file from commit `98d1ff03` (pre-break)
2. Add FrameClipboard import, showFrames, Frames button, FrameViewerSheet early return

Or pull the patched copy from the agent session artifacts path if available.
