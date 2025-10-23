package com.family.tree.editor.commands

import com.family.tree.editor.Command
import com.family.tree.layout.LayoutResult

class MoveNodeCommand(
    private val layout: LayoutResult,
    private val nodeId: String,
    private val oldX: Double,
    private val oldY: Double,
    private val newX: Double,
    private val newY: Double,
    private val onChanged: Runnable?
) : Command {

    override fun execute() {
        layout.setPosition(nodeId, newX, newY)
        onChanged?.run()
    }

    override fun undo() {
        layout.setPosition(nodeId, oldX, oldY)
        onChanged?.run()
    }
}
