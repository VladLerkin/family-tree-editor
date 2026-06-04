package com.family.tree.desktop

import com.family.tree.core.ai.agent.GenealogyTools

fun main() {
    println("--- RUNNING CACHE VERIFICATION TEST ---")
    val key1 = GenealogyTools.getFamilySearchCacheKey(
        firstName = "Степан",
        lastName = "Ярыгин",
        birthYear = "1918",
        birthPlace = "Якутск",
        deathYear = null,
        deathPlace = null,
        gender = "M",
        exactMatch = false,
        spouseLastName = null,
        spouseFirstName = null
    )
    val key2 = GenealogyTools.getFamilySearchCacheKey(
        firstName = "Степан",
        lastName = "Ярыгин",
        birthYear = "1918",
        birthPlace = "Якутск",
        deathYear = null,
        deathPlace = null,
        gender = "M",
        exactMatch = false,
        spouseLastName = null,
        spouseFirstName = null
    )
    println("Key 1: $key1")
    println("Key 2: $key2")
    if (key1 == key2) {
        println("SUCCESS: Keys are identical for identical arguments!")
    } else {
        println("FAILURE: Keys differ!")
        throw RuntimeException("Cache keys differ for identical arguments")
    }
}
