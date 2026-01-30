package com.family.tree.core.utils

import kotlin.random.Random

/**
 * Generates a random UUID (version 4) string.
 */
fun uuid4(): String {
    val chars = "0123456789abcdef"
    return buildString(36) {
        for (i in 0 until 36) {
            if (i == 8 || i == 13 || i == 18 || i == 23) append('-')
            else append(chars[Random.nextInt(chars.length)])
        }
    }
}
