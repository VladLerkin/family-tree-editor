package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class EditIndividualNameCommand(
    private val data: ProjectRepository.ProjectData,
    private val individualId: String,
    private val newFirstName: String?,
    private val newLastName: String?
) : Command {
    private var oldFirstName: String? = null
    private var oldLastName: String? = null

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(individualId, "individualId")
    }

    override fun execute() {
        val i = data.individuals.firstOrNull { it.id == individualId }
            ?: throw IllegalArgumentException("No individual: $individualId")
        oldFirstName = i.firstName
        oldLastName = i.lastName
        i.firstName = newFirstName
        i.lastName = newLastName
    }

    override fun undo() {
        val i = data.individuals.firstOrNull { it.id == individualId }
            ?: throw IllegalArgumentException("No individual: $individualId")
        i.firstName = oldFirstName
        i.lastName = oldLastName
    }
}
