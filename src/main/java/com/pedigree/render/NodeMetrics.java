package com.pedigree.render;

/**
 * Provides default node metrics; can be replaced with text-aware sizing.
 */
public class NodeMetrics {
    private final double defaultWidth;
    private final double defaultHeight;

    public NodeMetrics() {
        this(120.0, 60.0);
    }

    public NodeMetrics(double defaultWidth, double defaultHeight) {
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
    }

    public double getWidth(String nodeId) {
        return defaultWidth;
    }

    public double getHeight(String nodeId) {
        return defaultHeight;
    }
}
