package com.family.tree.services

import com.family.tree.model.Family
import com.family.tree.model.Individual
import com.family.tree.model.Tag
import com.family.tree.storage.ProjectRepository
import java.util.*

class TagService(private val data: ProjectRepository.ProjectData) {
    init { Objects.requireNonNull(data, "data") }

    fun assignTagToIndividual(individualId: String, tag: Tag) {
        val individual: Individual = findIndividual(individualId).orElseThrow()
        if (individual.tags.none { it.id == tag.id }) {
            individual.tags.add(tag)
        }
        if (data.tags.none { it.id == tag.id }) {
            data.tags.add(tag)
        }
    }

    fun assignTagToFamily(familyId: String, tag: Tag) {
        val family: Family = findFamily(familyId).orElseThrow()
        if (family.tags.none { it.id == tag.id }) {
            family.tags.add(tag)
        }
        if (data.tags.none { it.id == tag.id }) {
            data.tags.add(tag)
        }
    }

    fun removeTagFromIndividual(individualId: String, tagId: String) {
        findIndividual(individualId).ifPresent { i -> i.tags.removeIf { it.id == tagId } }
    }

    fun removeTagFromFamily(familyId: String, tagId: String) {
        findFamily(familyId).ifPresent { f -> f.tags.removeIf { it.id == tagId } }
    }

    fun filterIndividualsByTagName(tagName: String): List<Individual> {
        return data.individuals.filter { i -> i.tags.any { t -> tagName.equals(t.name, ignoreCase = true) } }
    }

    fun filterFamiliesByTagName(tagName: String): List<Family> {
        return data.families.filter { f -> f.tags.any { t -> tagName.equals(t.name, ignoreCase = true) } }
    }

    fun allTags(): List<Tag> = ArrayList(data.tags)

    private fun findIndividual(id: String) = data.individuals.stream().filter { it.id == id }.findFirst()
    private fun findFamily(id: String) = data.families.stream().filter { it.id == id }.findFirst()
}
