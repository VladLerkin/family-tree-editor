package com.family.tree.services

import com.family.tree.editor.Command
import com.family.tree.editor.CommandStack
import com.family.tree.model.ProjectMetadata
import java.time.Instant

class UndoRedoService {
    private val commandStack = CommandStack()
    private var projectMetadata: ProjectMetadata? = null

    fun bindProjectMetadata(meta: ProjectMetadata) { this.projectMetadata = meta }

    fun execute(command: Command) { commandStack.execute(command); touch() }
    fun undo() { commandStack.undo(); touch() }
    fun redo() { commandStack.redo(); touch() }
    fun canUndo(): Boolean = commandStack.canUndo()
    fun canRedo(): Boolean = commandStack.canRedo()
    fun getUndoCount(): Int = commandStack.getUndoSize()
    fun getRedoCount(): Int = commandStack.getRedoSize()

    private fun touch() {
        projectMetadata?.modifiedAt = Instant.now()
    }
}
