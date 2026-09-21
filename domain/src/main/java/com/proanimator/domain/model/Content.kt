package com.proanimator.domain.model

import java.util.UUID

sealed class Content {
    abstract val id: String
    abstract val name: String
    abstract val startFrame: Int
    abstract val durationFrames: Int

    data class Drawing(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Drawing",
        override val startFrame: Int = 0,
        override val durationFrames: Int = 1,
        val layerIds: List<String> = emptyList()
    ) : Content()

    data class Flipbook(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Flipbook",
        override val startFrame: Int = 0,
        override val durationFrames: Int = 24,
        val frameIds: List<String> = emptyList(),
        val onionSkinEnabled: Boolean = true
    ) : Content()

    data class Group(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Group",
        override val startFrame: Int = 0,
        override val durationFrames: Int = 1,
        val childIds: List<String> = emptyList()
    ) : Content()
}
