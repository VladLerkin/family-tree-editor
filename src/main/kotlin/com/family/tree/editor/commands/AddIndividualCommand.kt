package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.model.Individual
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AddIndividualCommand(
    private val data: ProjectRepository.ProjectData,
    private val individualToAdd: Individual
) : Command {
    private var added: Boolean = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(individualToAdd, "individualToAdd")
    }

    override fun execute() {
        val exists = data.individuals.any { it.id == individualToAdd.id }
        if (exists) {
            throw IllegalStateException("Individual with id already exists: ${individualToAdd.id}")
        }
        data.individuals.add(individualToAdd)
        added = true
    }

    override fun undo() {
        if (!added) return
        data.individuals.removeIf { it.id == individualToAdd.id }
        added = false
    }
}
