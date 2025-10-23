package com.family.tree.editor

interface Command {
    fun execute()
    fun undo()
    fun getName(): String = this::class.java.simpleName
}
