package com.pedigree.editor;

/**
 * Manages zoom and pan state for the canvas.
 * Framework-agnostic; UI toolkits can bind to these getters/setters.
 */
public class ZoomAndPanController {
    private double zoom = 1.0;
    private double minZoom = 0.1;
    private double maxZoom = 8.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double zoomStep = 1.1;

    public double getZoom() { return zoom; }
    public double getMinZoom() { return minZoom; }
    public double getMaxZoom() { return maxZoom; }
    public double getPanX() { return panX; }
    public double getPanY() { return panY; }

    public void setZoom(double level) {
        if (Double.isNaN(level) || Double.isInfinite(level)) return;
        zoom = clamp(level, minZoom, maxZoom);
    }

    public void setZoomBounds(double min, double max) {
        if (min <= 0 || max <= 0 || min > max) throw new IllegalArgumentException("Invalid zoom bounds");
        this.minZoom = min;
        this.maxZoom = max;
        setZoom(zoom);
    }

    public void setZoomStep(double stepFactor) {
        if (stepFactor <= 1.0) throw new IllegalArgumentException("Zoom step must be > 1.0");
        this.zoomStep = stepFactor;
    }

    public void zoomIn() {
        setZoom(zoom * zoomStep);
    }

    public void zoomOut() {
        setZoom(zoom / zoomStep);
    }

    public void panBy(double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) return;
        this.panX += dx;
        this.panY += dy;
    }

    public void resetView() {
        this.zoom = 1.0;
        this.panX = 0.0;
        this.panY = 0.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
