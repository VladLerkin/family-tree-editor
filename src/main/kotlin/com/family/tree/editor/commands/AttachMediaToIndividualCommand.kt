package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AttachMediaToIndividualCommand(
    private val data: ProjectRepository.ProjectData,
    private val individualId: String,
    private val media: com.family.tree.model.MediaAttachment
) : Command {
    private var attached = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(individualId, "individualId")
        Objects.requireNonNull(media, "media")
    }

    override fun execute() {
        val i = data.individuals.firstOrNull { it.id == individualId }
            ?: throw IllegalArgumentException("No individual: $individualId")
        val exists = i.media.any { it.id == media.id }
        if (!exists) {
            i.media.add(media)
            attached = true
        }
    }

    override fun undo() {
        if (!attached) return
        data.individuals.firstOrNull { it.id == individualId }?.media?.removeIf { it.id == media.id }
        attached = false
    }
}
