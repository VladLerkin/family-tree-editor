package com.family.tree.core.search

import com.family.tree.core.ProjectData
import com.family.tree.core.model.Family
import com.family.tree.core.model.Individual

class QuickSearchService(private val data: ProjectData) {
    private val prefixToIds: MutableMap<String, MutableSet<String>> = mutableMapOf()

    init {
        rebuildIndex()
    }

    fun rebuildIndex() {
        prefixToIds.clear()
        for (individual in data.individuals) {
            indexIndividual(individual)
        }
    }

    fun findIndividualsByName(query: String?): List<Individual> {
        if (query.isNullOrBlank()) return emptyList()
        val q = normalize(query)

        val resultIds: Set<String> = prefixToIds.getOrDefault(q, emptySet())

        val prefixMatches = data.individuals.filter { resultIds.contains(it.id.value) }
        val substringMatches = data.individuals
            .filter { !resultIds.contains(it.id.value) }
            .filter { 
                containsNormalized(it.firstName, q) || 
                containsNormalized(it.lastName, q) ||
                it.tags.any { tag -> containsNormalized(tag.name, q) }
            }

        val combined = ArrayList<Individual>(prefixMatches.size + substringMatches.size)
        combined.addAll(prefixMatches)
        combined.addAll(substringMatches)

        combined.sortWith(
            compareBy<Individual> { it.lastName?.lowercase() }
                .thenBy { it.firstName?.lowercase() }
        )
        return combined
    }

    fun findFamiliesBySpouseName(query: String?): List<Family> {
        if (query.isNullOrBlank()) return emptyList()
        val q = normalize(query)

        val matchingIndividuals = findIndividualsByName(q)
        val matchingIds = matchingIndividuals.map { it.id }.toSet()

        return data.families.filter { family ->
            matchingIds.contains(family.husbandId) || 
            matchingIds.contains(family.wifeId) ||
            family.tags.any { tag -> containsNormalized(tag.name, q) }
        }
    }

    private fun indexIndividual(individual: Individual) {
        for (token in tokensFor(individual)) {
            for (i in 1..token.length) {
                val prefix = token.substring(0, i)
                prefixToIds.computeIfAbsent(prefix) { mutableSetOf() }.add(individual.id.value)
            }
        }
    }

    private fun tokensFor(i: Individual): List<String> {
        val tokens: MutableList<String> = mutableListOf()
        if (i.firstName != null) tokens.add(normalize(i.firstName!!))
        if (i.lastName != null) tokens.add(normalize(i.lastName!!))
        return tokens
    }

    private fun containsNormalized(value: String?, needle: String): Boolean {
        if (value == null) return false
        return normalize(value).contains(needle)
    }

    private fun normalize(s: String): String = s.lowercase().trim()
}
