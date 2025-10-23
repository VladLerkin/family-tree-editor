package com.family.tree.editor

import java.util.ArrayDeque

class CommandStack {
    private val undoStack: ArrayDeque<Command> = ArrayDeque()
    private val redoStack: ArrayDeque<Command> = ArrayDeque()

    fun execute(command: Command) {
        command.execute()
        undoStack.push(command)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (!canUndo()) return
        val command = undoStack.pop()
        command.undo()
        redoStack.push(command)
    }

    fun redo() {
        if (!canRedo()) return
        val command = redoStack.pop()
        command.execute()
        undoStack.push(command)
    }

    fun getUndoSize(): Int = undoStack.size
    fun getRedoSize(): Int = redoStack.size
}
