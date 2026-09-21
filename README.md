# ProAnimator

**High-Performance 2D Animation & Motion Graphics Engine for Android**

ProAnimator is an ambitious open-source project aiming to deliver a professional-grade 2D animation experience on Android, inspired by the architecture and workflows of Procreate Dreams.

> Goal: Create the most powerful native 2D animation tool for Android tablets, foldables and high-end devices.

---

## Vision

- Real-time painting + compositing engine
- Multi-track timeline with **Compose / Keyframe / Perform** modes
- High-resolution canvases (up to 8K+)
- Advanced brush system with pressure, tilt, velocity and stabilization
- Onion skinning, Alpha Lock, Warp/Distort, non-destructive effects
- Custom file format `.pan` with streaming + eternal undo history
- Professional export pipeline (MP4 H.264/HEVC, transparent video, PNG sequences, GIF)
- Designed for S-Pen, stylus and high refresh rate displays

---

## Architecture Overview

```
ProAnimator/
├── app/                          # Main application (Jetpack Compose UI)
├── core/
│   ├── engine/                   # Painting + Compositing engine
│   ├── timeline/                 # Multi-track timeline + keyframes + Perform
│   ├── brushes/                  # Data-driven brush engine
│   ├── fileformat/               # .pan format (streaming + undo history)
│   └── export/                   # Export pipeline
├── rendering/                    # Skia (primary) + Vulkan path
├── domain/                       # Pure domain models
├── data/                         # Repositories & persistence
└── docs/                         # Architecture & Roadmap
```

### Core Design Principles (from deep analysis of Procreate Dreams)

1. **Real-time first** — No RAM preview. Every change must be playable instantly.
2. **Streaming resources** — Large projects open and scrub without loading everything into memory.
3. **Gesture-native timeline** — Multi-touch + stylus as primary input.
4. **Modular brush engine** — Brushes are data-driven and extensible.
5. **Non-destructive by default** — Keyframes, effects and transforms stay editable.
6. **Eternal undo** — Undo history lives inside the project file.

---

## Current Status

**Phase 0 — Foundation (this commit)**
- Project structure defined
- Architecture documentation
- Core vision and principles
- Ready for multi-module Gradle setup

---

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md)

### Quick Summary
- **Phase 1**: Canvas + Brush Engine foundation
- **Phase 2**: Timeline core (Flipbook + Keyframes)
- **Phase 3**: Perform mode + advanced animation
- **Phase 4**: `.pan` format + high performance
- **Phase 5**: Professional export + polish

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Rendering**: Skia (primary) + Vulkan experimental path
- **Async**: Kotlin Coroutines + Flow
- **DI**: Hilt
- **Min SDK**: 26 | Target: 35+

---

## How to Contribute

1. Read `docs/ARCHITECTURE.md`
2. Follow the roadmap
3. Open PRs with clear descriptions

---

**Built with deep engineering mindset.**  
Inspired by the best. Built for Android.
