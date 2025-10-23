package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class DetachMediaFromIndividualCommand(
    private val data: ProjectRepository.ProjectData,
    private val individualId: String,
    private val mediaId: String
) : Command {
    private var detached: Boolean = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(individualId, "individualId")
        Objects.requireNonNull(mediaId, "mediaId")
    }

    override fun execute() {
        val i = data.individuals.firstOrNull { it.id == individualId }
            ?: throw IllegalArgumentException("No individual: $individualId")
        val removed = i.media.removeIf { it.id == mediaId }
        detached = removed
    }

    override fun undo() {
        // Not restoring the media payload here (would require snapshot)
    }
}
