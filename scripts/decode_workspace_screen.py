#!/usr/bin/env python3
"""Decode docs/WorkspaceScreen.kt.zlib.b64 -> WorkspaceScreen.kt"""
import base64, zlib, pathlib
root = pathlib.Path(__file__).resolve().parents[1]
b64 = (root / "docs/WorkspaceScreen.kt.zlib.b64").read_text().strip()
out = root / "app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt"
out.write_text(zlib.decompress(base64.b64decode(b64)).decode())
print("Wrote", out, "bytes", out.stat().st_size)
