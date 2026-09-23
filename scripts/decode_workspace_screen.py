#!/usr/bin/env python3
"""Decode docs/ws_parts/z0..z2.b64 (zlib+base64) -> WorkspaceScreen.kt"""
import base64, zlib
from pathlib import Path
root = Path(__file__).resolve().parents[1]
parts_dir = root / "docs" / "ws_parts"
b64 = "".join((parts_dir / f"z{i}.b64").read_text().strip() for i in range(3))
data = zlib.decompress(base64.b64decode(b64))
out = root / "app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt"
out.write_text(data.decode())
print(f"Wrote {out} ({len(data)} bytes)")
assert b"FrameViewerSheet" in data
print("OK: Frame Viewer present")
