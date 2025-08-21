package com.pedigree.layout;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores layout positions for nodes by ID.
 */
public class LayoutResult {
    private final Map<String, Point2D> positions = new LinkedHashMap<>();

    public void setPosition(String nodeId, double x, double y) {
        positions.put(nodeId, new Point2D.Double(x, y));
    }

    public Point2D getPosition(String nodeId) {
        return positions.get(nodeId);
    }

    public Set<String> getNodeIds() {
        return Collections.unmodifiableSet(positions.keySet());
    }
}
