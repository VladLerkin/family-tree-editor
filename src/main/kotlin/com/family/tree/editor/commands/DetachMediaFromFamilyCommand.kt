package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.storage.ProjectRepository
import java.util.Objects

class DetachMediaFromFamilyCommand(
    private val data: ProjectRepository.ProjectData,
    private val familyId: String,
    private val mediaId: String
) : Command {

    init {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(familyId, "familyId")
        Objects.requireNonNull(mediaId, "mediaId")
    }

    override fun execute() {
        val f = data.families.firstOrNull { it.id == familyId }
            ?: throw IllegalArgumentException("No family: $familyId")
        f.media.removeIf { it.id == mediaId }
    }

    override fun undo() {
        // Not restoring the media payload here; would need snapshot
    }
}
