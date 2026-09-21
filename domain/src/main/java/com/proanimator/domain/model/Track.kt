package com.proanimator.domain.model

import java.util.UUID

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contents: List<Content> = emptyList(),
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val colorTag: Int? = null
)
