package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.model.Family
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AddFamilyCommand(
    private val data: ProjectRepository.ProjectData,
    private val familyToAdd: Family
) : Command {
    private var added: Boolean = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(familyToAdd, "familyToAdd")
    }

    override fun execute() {
        val exists = data.families.any { it.id == familyToAdd.id }
        if (exists) {
            throw IllegalStateException("Family with id already exists: ${familyToAdd.id}")
        }
        data.families.add(familyToAdd)
        added = true
    }

    override fun undo() {
        if (!added) return
        data.families.removeIf { it.id == familyToAdd.id }
        added = false
    }
}
