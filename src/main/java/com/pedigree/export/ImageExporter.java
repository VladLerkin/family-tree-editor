package com.pedigree.export;

import com.pedigree.layout.LayoutResult;
import com.pedigree.render.NodeMetrics;
import com.pedigree.render.TreeRenderer;
import com.pedigree.storage.ProjectRepository;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ImageExporter {

    public void exportToFile(ProjectRepository.ProjectData data, LayoutResult layout, NodeMetrics metrics, Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();
        String fmt = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ? "jpg" : "png";
        exportToFile(data, layout, metrics, path, fmt);
    }

    public void exportToFile(ProjectRepository.ProjectData data, LayoutResult layout, NodeMetrics metrics, Path path, String format) throws IOException {
        if (data == null || layout == null) return;
        Bounds b = computeBounds(layout, metrics);
        double margin = 20.0;
        double width = (b.maxX - b.minX) + margin * 2.0;
        double height = (b.maxY - b.minY) + margin * 2.0;
        double panX = -b.minX + margin;
        double panY = -b.minY + margin;

        Canvas canvas = new Canvas(Math.max(1.0, width), Math.max(1.0, height));
        var jfxGc = canvas.getGraphicsContext2D();
        var gc = new com.pedigree.ui.JavaFxGraphicsContext(jfxGc);

        TreeRenderer renderer = new TreeRenderer(metrics);
        renderer.render(data, layout, gc, 1.0, panX, panY);

        var img = canvas.snapshot(null, null);
        BufferedImage bi = SwingFXUtils.fromFXImage(img, null);
        ImageIO.write(bi, format, path.toFile());
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {}
    private static Bounds computeBounds(LayoutResult layout, NodeMetrics metrics) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        Set<String> ids = layout.getNodeIds();
        for (String id : ids) {
            Point2D p = layout.getPosition(id);
            if (p == null) continue;
            double x = p.getX();
            double y = p.getY();
            double w = metrics.getWidth(id);
            double h = metrics.getHeight(id);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
        }
        if (ids.isEmpty()) {
            minX = minY = 0; maxX = maxY = 1;
        }
        return new Bounds(minX, minY, maxX, maxY);
    }
}
