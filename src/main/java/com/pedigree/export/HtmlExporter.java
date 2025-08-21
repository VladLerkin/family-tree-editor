package com.pedigree.export;

import com.pedigree.layout.LayoutResult;
import com.pedigree.render.NodeMetrics;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlExporter {

    public void exportToFile(ProjectRepository.ProjectData data, LayoutResult layout, NodeMetrics metrics, Path path) throws IOException {
        SvgExporter svgExporter = new SvgExporter();
        String svg = svgExporter.exportToString(data, layout, metrics);
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Pedigree Export</title>
                  <style>
                    body { margin: 0; font-family: system-ui, sans-serif; }
                    header { padding: 12px 16px; background: #f6f8fa; border-bottom: 1px solid #e5e7eb; }
                    main { padding: 12px; overflow: auto; }
                    .container { margin: 0 auto; }
                  </style>
                </head>
                <body>
                  <header><h1>Pedigree Export</h1></header>
                  <main><div class="container">
                    %s
                  </div></main>
                </body>
                </html>
                """.formatted(svg);
        Files.write(path, html.getBytes(StandardCharsets.UTF_8));
    }
}
