package com.pedigree.model;

import java.util.HashMap;
import java.util.Map;

public class ProjectLayout {
    public static class NodePos {
        public double x;
        public double y;
    }

    private double zoom = 1.0;
    private double viewOriginX = 0.0;
    private double viewOriginY = 0.0;
    private final Map<String, NodePos> nodePositions = new HashMap<>();

    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; com.pedigree.util.DirtyFlag.setModified(); }
    public double getViewOriginX() { return viewOriginX; }
    public void setViewOriginX(double viewOriginX) { this.viewOriginX = viewOriginX; com.pedigree.util.DirtyFlag.setModified(); }
    public double getViewOriginY() { return viewOriginY; }
    public void setViewOriginY(double viewOriginY) { this.viewOriginY = viewOriginY; com.pedigree.util.DirtyFlag.setModified(); }
    public Map<String, NodePos> getNodePositions() { return nodePositions; }
}


