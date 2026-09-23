package com.proanimator.core.engine

import androidx.compose.ui.graphics.ImageBitmap

/**
 * @deprecated Use [WarpEngine] — real Skia drawBitmapMesh path.
 */
@Deprecated("Use WarpEngine")
object WarpStub {
    fun isImplemented(): Boolean = true
    fun apply(source: ImageBitmap, ignored: Any? = null): ImageBitmap = source
}
