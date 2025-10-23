package com.family.tree.ui

import javafx.scene.control.Button
import javafx.scene.control.ToolBar

class ToolbarFactory(private val onZoomIn: Runnable?, private val onZoomOut: Runnable?) {

    fun create(): ToolBar {
        val bZoomIn = button("Zoom +", onZoomIn)
        val bZoomOut = button("Zoom -", onZoomOut)
        return ToolBar(bZoomIn, bZoomOut)
    }

    private fun button(text: String, action: Runnable?): Button {
        val b = Button(text)
        b.setOnAction { action?.run() }
        return b
    }
}
