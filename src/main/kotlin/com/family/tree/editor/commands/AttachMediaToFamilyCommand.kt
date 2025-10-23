package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class AttachMediaToFamilyCommand(
    private val data: ProjectRepository.ProjectData,
    private val familyId: String,
    private val media: com.family.tree.model.MediaAttachment
) : Command {
    private var attached = false

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(familyId, "familyId")
        Objects.requireNonNull(media, "media")
    }

    override fun execute() {
        val f = data.families.firstOrNull { it.id == familyId }
            ?: throw IllegalArgumentException("No family: $familyId")
        val exists = f.media.any { it.id == media.id }
        if (!exists) {
            f.media.add(media)
            attached = true
        }
    }

    override fun undo() {
        if (!attached) return
        data.families.firstOrNull { it.id == familyId }?.media?.removeIf { it.id == media.id }
        attached = false
    }
}
