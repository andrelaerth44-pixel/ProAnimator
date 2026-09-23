package com.proanimator.core.timeline

/**
 * LRU tracker for "hot" frame indices around the playhead.
 *
 * Inspired by Pencil2D's ActiveFramePool concept (memory budget for recent keyframes),
 * reimplemented from scratch in Kotlin — no GPL code copied.
 *
 * Use: call [touch] on scrub/seek; query [isHot] / [hotIndices] for cache policy.
 * Does not own bitmaps — Flipbook remains source of truth; this only ranks access.
 */
class ActiveFramePool(
    /** Max frames kept "hot" (default ~15 like Pencil2D min frame count). */
    var capacity: Int = 24
) {
    private val order = ArrayDeque<Int>()
    private val set = HashSet<Int>()

    fun touch(index: Int) {
        if (index < 0) return
        if (set.contains(index)) {
            order.remove(index)
            order.addLast(index)
            return
        }
        order.addLast(index)
        set.add(index)
        while (order.size > capacity.coerceAtLeast(4)) {
            val evicted = order.removeFirst()
            set.remove(evicted)
        }
    }

    /** Touch a window around [center] (onion + playback neighbors). */
    fun touchWindow(center: Int, radius: Int = 3, maxIndexExclusive: Int) {
        if (maxIndexExclusive <= 0) return
        val r = radius.coerceAtLeast(0)
        for (i in (center - r).coerceAtLeast(0) until (center + r + 1).coerceAtMost(maxIndexExclusive)) {
            touch(i)
        }
    }

    fun isHot(index: Int): Boolean = set.contains(index)

    fun hotIndices(): Set<Int> = set.toSet()

    fun clear() {
        order.clear()
        set.clear()
    }

    fun size(): Int = set.size
}
