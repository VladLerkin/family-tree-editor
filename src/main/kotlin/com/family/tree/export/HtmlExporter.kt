package com.family.tree.export

import com.family.tree.layout.LayoutResult
import com.family.tree.render.NodeMetrics
import com.family.tree.storage.ProjectRepository
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class HtmlExporter {

    @Throws(IOException::class)
    fun exportToFile(
        data: ProjectRepository.ProjectData?,
        layout: LayoutResult?,
        metrics: NodeMetrics,
        path: Path
    ) {
        val svgExporter = SvgExporter()
        val svg = svgExporter.exportToString(data, layout, metrics)
        val html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>family.tree Export</title>
                  <style>
                    body { margin: 0; font-family: system-ui, sans-serif; }
                    header { padding: 12px 16px; background: #f6f8fa; border-bottom: 1px solid #e5e7eb; }
                    main { padding: 12px; overflow: auto; }
                    .container { margin: 0 auto; }
                  </style>
                </head>
                <body>
                  <header><h1>family.tree Export</h1></header>
                  <main><div class="container">
                    %s
                  </div></main>
                </body>
                </html>
                """.trimIndent().format(svg)
        Files.write(path, html.toByteArray(StandardCharsets.UTF_8))
    }
}
