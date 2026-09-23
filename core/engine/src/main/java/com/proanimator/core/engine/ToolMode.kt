package com.proanimator.core.engine

/**
 * Active tool on the drawing surface.
 * DRAW uses the selected BrushPreset; ERASE forces eraser brush.
 */
enum class ToolMode {
    DRAW,
    ERASE,
    TRANSFORM,
    WARP
}
