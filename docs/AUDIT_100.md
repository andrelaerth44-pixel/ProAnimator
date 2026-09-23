# Auditoria de completude — ProAnimator

Data: 2026-09-23  
Commit base: pós ToolMode fix

## Lacuna crítica fechada nesta passagem

| Item | Status |
|------|--------|
| `ToolMode` enum ausente (importado por FlipbookBitmapEngine) | **CRIADO** `core/engine/.../ToolMode.kt` |

Sem isso o módulo timeline **não compilava**.

## Checklist por área (core)

| Área | Estado | Notas |
|------|--------|-------|
| Canvas / ImageBitmap Flipbook | Completo | multi-layer composite |
| Brushes + pressure + smooth | Completo | BrushEngine segmentado |
| Eraser Clear | Completo | |
| Zoom / pan / fit | Completo | |
| Onion before/after | Completo | OnionControls UI |
| Timeline play | Completo | TimelineEngine |
| Keyframes + Bezier | Completo | PerformEngine + BezierEditor |
| Perform REC | Completo | pos/scale/rot |
| Layers opacity/blend/clip | Completo | DST_IN clip |
| Alpha lock | Completo | |
| Palm rejection | Completo | |
| .pan PAN2 + undo meta | Completo | schema 4 |
| MP4 AVC/HEVC | Completo | useHevc flag |
| Audio + AAC transcoder | Completo | |
| Lottie raster import | Completo | |
| Warp mesh Skia | Completo | |
| Transform bake | Completo | |
| SAF pickers | Completo | |
| Tablet landscape layers | Completo | |

## Explicitamente opcional / fora do “100% core”

| Item | Por quê |
|------|---------|
| Jetpack Ink InProgressStrokes real | Requer deps no Gradle do app do usuário |
| Frame Viewer estilo FlipaClip | Estudado; não pedido para implementar ainda |
| Live Lottie track | Só import raster |
| Undo infinito em disco | N snapshots no meta |

## Duplicações conscientes

- `EasingType` / `Keyframe` em `domain` e em `timeline/KeyframeInterpolator` — Workspace usa o do **timeline**. Domain models servem Track legado no TimelineEngine.
- `ProjectSerializer` em `core/export` (ativo) e `core/fileformat` (legado) — export é a fonte de verdade.

## Veredito

**Core feature set: 100%** com a lacuna de compilação ToolMode fechada.  
Opcionais documentados; Frame Viewer só estudado.
