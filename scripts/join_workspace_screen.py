#!/usr/bin/env python3
"""Join docs/ws_parts/part*.b64 -> WorkspaceScreen.kt"""
import base64
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parts_dir = root / "docs" / "ws_parts"
chunks = []
for i in range(5):
    p = parts_dir / f"part{i}.b64"
    chunks.append(p.read_text().strip())
data = base64.b64decode("".join(chunks))
out = root / "app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt"
out.write_text(data.decode())
print(f"Wrote {out} ({len(data)} bytes)")
assert b"FrameViewerSheet" in data
print("OK: Frame Viewer present")
