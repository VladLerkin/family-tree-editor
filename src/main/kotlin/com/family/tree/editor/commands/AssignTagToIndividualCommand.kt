package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AssignTagToIndividualCommand(
    private val data: ProjectRepository.ProjectData,
    private val individualId: String,
    private val tag: com.family.tree.model.Tag
) : Command {
    private var addedToIndividual = false
    private var addedToCatalog = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(individualId, "individualId")
        Objects.requireNonNull(tag, "tag")
    }

    override fun execute() {
        val i = data.individuals.firstOrNull { it.id == individualId }
            ?: throw IllegalArgumentException("No individual: $individualId")

        if (i.tags.none { it.id == tag.id }) {
            i.tags.add(tag)
            addedToIndividual = true
        }
        if (data.tags.none { it.id == tag.id }) {
            data.tags.add(tag)
            addedToCatalog = true
        }
    }

    override fun undo() {
        if (addedToIndividual) {
            data.individuals.firstOrNull { it.id == individualId }?.tags?.removeIf { it.id == tag.id }
        }
        if (addedToCatalog) {
            data.tags.removeIf { it.id == tag.id }
        }
    }
}
