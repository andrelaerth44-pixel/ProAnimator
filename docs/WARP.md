# Mesh Warp / Liquify

## Engine

`com.proanimator.core.engine.WarpEngine`

Uses Android **`Canvas.drawBitmapMesh`** — Skia tessellates the mesh on GPU/CPU path; not a naive per-pixel software loop.

## Grid

Default **32×32** cells → 33×33 vertices. Each vertex is an (x,y) in bitmap space.

## Operations

| Op | Method | Behavior |
|----|--------|----------|
| Push / liquify | `push(cx,cy,dx,dy,radius,strength)` | Gaussian weight moves verts |
| Pinch / bloat | `pinchBloat(cx,cy,amount,radius)` | Radial scale of verts |
| Apply | `apply(ImageBitmap)` | drawBitmapMesh → new bitmap |
| Reset | `resetMesh()` | Restores regular grid |

## Flipbook API

```kotlin
flipbook.setWarpMode(true)
flipbook.warpPush(x, y, dx, dy, radius = 100f)
flipbook.applyWarp()   // bake
flipbook.cancelWarp()
```

## Transform bake

```kotlin
flipbook.bakeTransform(tx, ty, scale, rotationDeg)
```

Uses `Matrix` + `Canvas.drawBitmap` (FILTER_BITMAP).
