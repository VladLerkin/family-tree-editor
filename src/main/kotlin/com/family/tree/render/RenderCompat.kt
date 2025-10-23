package com.family.tree.render

import com.family.tree.layout.LayoutResult
import com.family.tree.storage.ProjectRepository

object RenderCompat {
    @JvmStatic
    fun render(
        renderer: TreeRenderer,
        data: ProjectRepository.ProjectData,
        layout: LayoutResult,
        g: GraphicsContext,
        zoom: Double,
        panX: Double,
        panY: Double
    ) {
        renderer.render(data, layout, g, zoom, panX, panY)
    }
}
