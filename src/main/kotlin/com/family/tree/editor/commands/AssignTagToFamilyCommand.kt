package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.model.Tag
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AssignTagToFamilyCommand(
    private val data: ProjectRepository.ProjectData,
    private val familyId: String,
    private val tag: com.family.tree.model.Tag
) : Command {
    private var addedToFamily = false
    private var addedToCatalog = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(familyId, "familyId")
        Objects.requireNonNull(tag, "tag")
    }

    override fun execute() {
        val f = data.families.firstOrNull { it.id == familyId }
            ?: throw IllegalArgumentException("No family: $familyId")
        if (f.tags.none { it.id == tag.id }) {
            f.tags.add(tag)
            addedToFamily = true
        }
        if (data.tags.none { it.id == tag.id }) {
            data.tags.add(tag)
            addedToCatalog = true
        }
    }

    override fun undo() {
        if (addedToFamily) {
            data.families.firstOrNull { it.id == familyId }?.tags?.removeIf { it.id == tag.id }
        }
        if (addedToCatalog) {
            data.tags.removeIf { it.id == tag.id }
        }
    }
}
