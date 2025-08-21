package com.pedigree.export;

import com.pedigree.layout.LayoutResult;
import com.pedigree.render.GraphicsContext;
import com.pedigree.render.NodeMetrics;
import com.pedigree.render.TreeRenderer;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.geom.Point2D;
import java.util.Set;

public class SvgExporter {

    public void exportToFile(ProjectRepository.ProjectData data, LayoutResult layout, NodeMetrics metrics, Path path) throws IOException {
        String svg = exportToString(data, layout, metrics);
        Files.write(path, svg.getBytes(StandardCharsets.UTF_8));
    }

    public String exportToString(ProjectRepository.ProjectData data, LayoutResult layout, NodeMetrics metrics) {
        if (data == null || layout == null) return "";
        Bounds b = computeBounds(layout, metrics);
        double margin = 20.0;
        double panX = -b.minX + margin;
        double panY = -b.minY + margin;

        SvgGraphicsContext g = new SvgGraphicsContext();
        TreeRenderer renderer = new TreeRenderer(metrics);
        renderer.render(data, layout, g, 1.0, panX, panY);

        double width = (b.maxX - b.minX) + margin * 2.0;
        double height = (b.maxY - b.minY) + margin * 2.0;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"").append(width).append("\" height=\"").append(height)
          .append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");
        sb.append(g.getContent());
        sb.append("</svg>\n");
        return sb.toString();
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

    private static class SvgGraphicsContext implements GraphicsContext {
        private final StringBuilder sb = new StringBuilder();
        private String stroke = "rgb(0,0,0)";
        private String fill = "none";
        private double alphaStroke = 1.0;
        private double alphaFill = 1.0;
        private double lineWidth = 1.0;

        public String getContent() {
            return sb.toString();
        }

        @Override
        public void setStrokeColor(int r, int g, int b, double a) {
            this.stroke = "rgb(" + r + "," + g + "," + b + ")";
            this.alphaStroke = a;
        }

        @Override
        public void setFillColor(int r, int g, int b, double a) {
            this.fill = "rgb(" + r + "," + g + "," + b + ")";
            this.alphaFill = a;
        }

        @Override
        public void setLineWidth(double w) {
            this.lineWidth = w;
        }

        @Override
        public void drawLine(double x1, double y1, double x2, double y2) {
            sb.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
              .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
              .append("\" stroke=\"").append(stroke).append("\" stroke-opacity=\"").append(alphaStroke)
              .append("\" stroke-width=\"").append(lineWidth).append("\" />\n");
        }

        @Override
        public void fillRect(double x, double y, double w, double h) {
            sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w).append("\" height=\"").append(h)
              .append("\" fill=\"").append(fill).append("\" fill-opacity=\"").append(alphaFill)
              .append("\" stroke=\"none\" />\n");
        }

        @Override
        public void drawRect(double x, double y, double w, double h) {
            sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w).append("\" height=\"").append(h)
              .append("\" fill=\"none\" stroke=\"").append(stroke).append("\" stroke-opacity=\"").append(alphaStroke)
              .append("\" stroke-width=\"").append(lineWidth).append("\" />\n");
        }

        @Override
        public void drawText(String text, double x, double y) {
            sb.append("<text x=\"").append(x).append("\" y=\"").append(y)
              .append("\" fill=\"").append(stroke).append("\" fill-opacity=\"").append(alphaStroke)
              .append("\" font-family=\"Sans-Serif\" font-size=\"12\">")
              .append(escape(text)).append("</text>\n");
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
