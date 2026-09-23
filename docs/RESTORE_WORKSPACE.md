# Restore WorkspaceScreen

Full file is large for some push paths. Local artifact: valid 33KB with Frame Viewer.

```bash
# From a good history point (before stub):
git checkout 98d1ff03 -- app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt
```

Then apply Frame Viewer wire if missing (import FrameClipboard, showFrames, Frames button, FrameViewerSheet early return).

Or ask the agent to push the full file after reconnect (content ready at session artifacts).
