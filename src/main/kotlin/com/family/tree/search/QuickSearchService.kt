package com.family.tree.search

import com.family.tree.model.Individual
import com.family.tree.storage.ProjectRepository
import java.util.*

class QuickSearchService(private val data: ProjectRepository.ProjectData) {
    private val prefixToIds: MutableMap<String, MutableSet<String>> = HashMap()

    init {
        Objects.requireNonNull(data, "data")
        rebuildIndex()
    }

    fun rebuildIndex() {
        prefixToIds.clear()
        for (individual in data.individuals) {
            indexIndividual(individual)
        }
    }

    fun addIndividual(individual: Individual) {
        data.individuals.add(individual)
        indexIndividual(individual)
    }

    fun updateIndividual(individual: Individual) {
        removeIndividual(individual.id)
        data.individuals.add(individual)
        indexIndividual(individual)
    }

    fun removeIndividual(individualId: String) {
        data.individuals.removeIf { it.id == individualId }
        for (ids in prefixToIds.values) {
            ids.remove(individualId)
        }
    }

    fun findByName(query: String?): List<Individual> {
        if (query == null || query.isBlank()) return emptyList()
        val q = normalize(query)

        val resultIds: Set<String> = prefixToIds.getOrDefault(q, emptySet())

        val prefixMatches = data.individuals.filter { resultIds.contains(it.id) }
        val substringMatches = data.individuals
            .filter { !resultIds.contains(it.id) }
            .filter { containsNormalized(it.firstName, q) || containsNormalized(it.lastName, q) }

        val combined = ArrayList<Individual>(prefixMatches.size + substringMatches.size)
        combined.addAll(prefixMatches)
        combined.addAll(substringMatches)

        combined.sortWith(
            compareBy<Individual> { it.lastName?.let { s -> s.lowercase(Locale.getDefault()) } }
                .thenBy { it.firstName?.let { s -> s.lowercase(Locale.getDefault()) } }
        )
        return combined
    }

    private fun indexIndividual(individual: Individual) {
        for (token in tokensFor(individual)) {
            for (i in 1..token.length) {
                val prefix = token.substring(0, i)
                prefixToIds.computeIfAbsent(prefix) { HashSet() }.add(individual.id)
            }
        }
    }

    private fun tokensFor(i: Individual): List<String> {
        val tokens: MutableList<String> = ArrayList()
        if (i.firstName != null) tokens.add(normalize(i.firstName!!))
        if (i.lastName != null) tokens.add(normalize(i.lastName!!))
        return tokens
    }

    private fun containsNormalized(value: String?, needle: String): Boolean {
        if (value == null) return false
        return normalize(value).contains(needle)
    }

    private fun normalize(s: String): String = s.lowercase(Locale.ROOT).trim()
}
