package com.family.tree.core

import kotlinx.serialization.Serializable

/**
 * Simple viewport state persisted alongside project data for restoring
 * canvas scale and pan between sessions and across platforms.
 */
@Serializable
data class Viewport(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)