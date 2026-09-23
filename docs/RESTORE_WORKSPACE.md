# Restore full WorkspaceScreen.kt

Verified: `z0`+`z1`+`z2` decode to 704 lines with Frame Viewer.

```bash
cd ProAnimator
python3 scripts/decode_workspace_screen.py
```

This writes `app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt`
with Frames button → `FrameViewerSheet` modal + all Phase 1–5 UI.

**Already on main (no restore needed):**
- `FrameViewerSheet.kt`
- `FrameClipboard.kt` / `ActiveFramePool.kt`
- Flipbook append/insert/delete + LRU touch
- STATUS 100% core
