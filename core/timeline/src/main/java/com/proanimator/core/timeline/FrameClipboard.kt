package com.proanimator.core.timeline

/**
 * In-memory clipboard for multi-frame copy/paste (Frame Viewer / FlipaClip-style).
 * Holds deep-copied LayerStacks — independent of source after copy.
 */
class FrameClipboard {

    private var stacks: List<LayerStack> = emptyList()

    val isEmpty: Boolean get() = stacks.isEmpty()
    val size: Int get() = stacks.size

    fun copyFrom(flipbook: FlipbookBitmapEngine, indices: Collection<Int>) {
        val frames = flipbook.frames.value
        val ordered = indices.filter { it in frames.indices }.sorted()
        stacks = ordered.map { i ->
            val stack = LayerStack(flipbook.width, flipbook.height, initialLayers = 0)
            stack.duplicateFrom(frames[i].layers)
            stack
        }
    }

    fun pasteAppend(flipbook: FlipbookBitmapEngine) {
        if (stacks.isEmpty()) return
        stacks.forEach { src ->
            val stack = LayerStack(flipbook.width, flipbook.height, initialLayers = 0)
            stack.duplicateFrom(src)
            flipbook.appendFrameWithStack(stack)
        }
    }

    fun pasteAt(flipbook: FlipbookBitmapEngine, insertIndex: Int) {
        if (stacks.isEmpty()) return
        var at = insertIndex.coerceIn(0, flipbook.frames.value.size)
        stacks.forEach { src ->
            val stack = LayerStack(flipbook.width, flipbook.height, initialLayers = 0)
            stack.duplicateFrom(src)
            flipbook.insertFrameAt(at, stack)
            at++
        }
    }

    fun clear() {
        stacks = emptyList()
    }
}
