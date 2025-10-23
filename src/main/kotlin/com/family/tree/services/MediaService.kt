package com.family.tree.services

import com.family.tree.model.Family
import com.family.tree.model.Individual
import com.family.tree.model.MediaAttachment
import com.family.tree.storage.ProjectRepository
import java.util.*

class MediaService(private val data: ProjectRepository.ProjectData) {
    init { Objects.requireNonNull(data, "data") }

    fun attachToIndividual(individualId: String, media: MediaAttachment) {
        val individual: Individual = findIndividual(individualId).orElseThrow()
        val exists = individual.media.stream().anyMatch { m -> m.id == media.id }
        if (!exists) {
            individual.media.add(media)
        }
    }

    fun attachToFamily(familyId: String, media: MediaAttachment) {
        val family: Family = findFamily(familyId).orElseThrow()
        val exists = family.media.stream().anyMatch { m -> m.id == media.id }
        if (!exists) {
            family.media.add(media)
        }
    }

    fun detachFromIndividual(individualId: String, mediaId: String) {
        findIndividual(individualId).ifPresent { i -> i.media.removeIf { m -> m.id == mediaId } }
    }

    fun detachFromFamily(familyId: String, mediaId: String) {
        findFamily(familyId).ifPresent { f -> f.media.removeIf { m -> m.id == mediaId } }
    }

    fun getThumbnail(mediaId: String): ByteArray {
        throw UnsupportedOperationException("Thumbnail generation not yet implemented")
    }

    private fun findIndividual(id: String) = data.individuals.stream().filter { it.id == id }.findFirst()
    private fun findFamily(id: String) = data.families.stream().filter { it.id == id }.findFirst()
}
