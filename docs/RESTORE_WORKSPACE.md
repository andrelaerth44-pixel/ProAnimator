# Restore full WorkspaceScreen.kt

```bash
python3 scripts/decode_workspace_screen.py
```

Joins docs/ws_parts/z0.b64 + z1.b64 + z2.b64 (zlib+base64).
CI runs this automatically before assembleDebug.
