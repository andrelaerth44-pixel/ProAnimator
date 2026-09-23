package com.proanimator.core.engine

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Warp / Liquify API stub — mesh deform path for future GPU implementation.
 *
 * Full 100%: grid mesh + fragment sampling (Skia/ImageFilter or OpenGL).
 * Current: identity passthrough so callers can integrate without crash.
 */
object WarpStub {

    data class Mesh(
        val cols: Int = 8,
        val rows: Int = 8,
        /** Flattened [x,y] displacements per vertex */
        val deltas: FloatArray = FloatArray((cols + 1) * (rows + 1) * 2)
    )

    fun identityMesh(cols: Int = 8, rows: Int = 8) = Mesh(cols, rows)

    /**
     * Apply warp. Stub returns [source] unchanged until GPU path lands.
     */
    fun apply(source: ImageBitmap, mesh: Mesh): ImageBitmap {
        // TODO: implement mesh sampling
        return source
    }

    fun isImplemented(): Boolean = false
}
