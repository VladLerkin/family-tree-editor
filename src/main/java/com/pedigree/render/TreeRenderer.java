package com.pedigree.render;

import com.pedigree.layout.LayoutResult;
import com.pedigree.model.Family;
import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import com.pedigree.model.Relationship;
import com.pedigree.storage.ProjectRepository;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders nodes and edges to a framework-agnostic GraphicsContext.
 */
public class TreeRenderer {

    private final NodeMetrics metrics;

    public TreeRenderer() {
        this(new NodeMetrics());
    }

    public TreeRenderer(NodeMetrics metrics) {
        this.metrics = metrics;
    }

    public void render(ProjectRepository.ProjectData data, LayoutResult layout, GraphicsContext g) {
        render(data, layout, g, 1.0, 0.0, 0.0);
    }

    public void render(ProjectRepository.ProjectData data, LayoutResult layout, GraphicsContext g,
                       double zoom, double panX, double panY) {
        if (data == null || layout == null || g == null) return;

        // Build id sets to differentiate individuals and families
        Set<String> individualIds = new HashSet<>();
        for (Individual i : data.individuals) individualIds.add(i.getId());
        Set<String> familyIds = new HashSet<>();
        for (Family f : data.families) familyIds.add(f.getId());

        // Map for quick lookup of Individuals by id (to access gender)
        Map<String, Individual> individualsById = new HashMap<>();
        for (Individual i : data.individuals) {
            individualsById.put(i.getId(), i);
        }
        // Map for quick lookup of Families by id
        Map<String, Family> familiesById = new HashMap<>();
        for (Family f : data.families) {
            familiesById.put(f.getId(), f);
        }

        // Draw nodes
        g.setLineWidth(1.5);
        for (String id : layout.getNodeIds()) {
            Point2D p = layout.getPosition(id);
            if (p == null) continue;
            double w = metrics.getWidth(id) * zoom;
            double h = metrics.getHeight(id) * zoom;
            double x = p.getX() * zoom + panX;
            double y = p.getY() * zoom + panY;
            String label = id;

            if (individualIds.contains(id)) {
                Individual ind = individualsById.get(id);
                if (ind != null && ind.getGender() == Gender.FEMALE) {
                    // Yellow for female
                    g.setFillColor(255, 255, 180, 1.0);
                    g.setStrokeColor(120, 110, 40, 1.0);
                } else if (ind != null && ind.getGender() == Gender.MALE) {
                    // Light blue for male
                    g.setFillColor(240, 240, 255, 1.0);
                    g.setStrokeColor(50, 50, 80, 1.0);
                } else {
                    // Unknown/other
                    g.setFillColor(235, 240, 245, 1.0);
                    g.setStrokeColor(70, 80, 90, 1.0);
                }
                if (ind != null) {
                    String fn = ind.getFirstName() != null ? ind.getFirstName() : "";
                    String ln = ind.getLastName() != null ? ind.getLastName() : "";
                    String combined = (fn + " " + ln).trim();
                    label = combined.isEmpty() ? id : combined;
                }
            } else if (familyIds.contains(id)) {
                // Do not visualize family nodes on the canvas per requirement
                continue;
            } else {
                g.setFillColor(235, 235, 235, 1.0);
                g.setStrokeColor(60, 60, 60, 1.0);
            }
            g.fillRect(x, y, w, h);
            g.drawRect(x, y, w, h);

            // Highlight selected nodes with a distinct colored frame
            Set<String> sel = RenderHighlightState.getSelectedIds();
            if (sel != null && sel.contains(id)) {
                double oldW = 1.5;
                g.setLineWidth(3.0);
                g.setStrokeColor(0, 180, 80, 1.0); // vivid green frame
                g.drawRect(x - 2, y - 2, w + 4, h + 4);
                g.setLineWidth(oldW);
            }

            // Ensure label text is black
            g.setFillColor(0, 0, 0, 1.0);
            g.drawText(label, x + 8, y + 20);
        }

        // Draw family-aligned edges (spouse bar + child stem), like in the reference screenshot
        g.setStrokeColor(60, 60, 60, 1.0);
        g.setLineWidth(1.0);

        // Collect relationships by family
        Map<String, List<String>> spousesByFamily = new HashMap<>(); // familyId -> spouseIds
        Map<String, List<String>> childrenByFamily = new HashMap<>(); // familyId -> childIds
        for (Relationship rel : data.relationships) {
            if (rel.getType() == Relationship.Type.SPOUSE_TO_FAMILY) {
                spousesByFamily.computeIfAbsent(rel.getToId(), k -> new ArrayList<>()).add(rel.getFromId());
            } else if (rel.getType() == Relationship.Type.FAMILY_TO_CHILD) {
                childrenByFamily.computeIfAbsent(rel.getFromId(), k -> new ArrayList<>()).add(rel.getToId());
            }
        }

        // Families involved
        Set<String> famIds = new HashSet<>();
        famIds.addAll(spousesByFamily.keySet());
        famIds.addAll(childrenByFamily.keySet());

        final double barGap = 6.0 * zoom;          // gap below spouse boxes
        final double singleBarHalf = 20.0 * zoom;  // half-width when only one spouse

        for (String famId : famIds) {
            List<String> spouses = spousesByFamily.getOrDefault(famId, java.util.List.of());
            List<String> children = childrenByFamily.getOrDefault(famId, java.util.List.of());

            double barY = Double.NEGATIVE_INFINITY;
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            List<double[]> spouseAnchors = new ArrayList<>(); // [xCenter, bottomY]

            for (String sId : spouses) {
                Point2D ps = layout.getPosition(sId);
                if (ps == null) continue;
                double w = metrics.getWidth(sId) * zoom;
                double h = metrics.getHeight(sId) * zoom;
                double xCenter = ps.getX() * zoom + panX + w / 2.0;
                double bottomY = ps.getY() * zoom + panY + h;
                spouseAnchors.add(new double[]{xCenter, bottomY});
                if (xCenter < minX) minX = xCenter;
                if (xCenter > maxX) maxX = xCenter;
                if (bottomY > barY) barY = bottomY;
            }

            // Fallback to family position if no spouse positions are known
            double barX1, barX2;
            if (spouseAnchors.isEmpty()) {
                Point2D pf = layout.getPosition(famId);
                if (pf == null) continue; // nothing to draw
                double w = metrics.getWidth(famId) * zoom;
                double xMid = pf.getX() * zoom + panX + w / 2.0;
                double y = pf.getY() * zoom + panY;
                barY = y; // use family Y as bar Y
                barX1 = xMid - singleBarHalf;
                barX2 = xMid + singleBarHalf;
            } else if (spouseAnchors.size() == 1) {
                double xMid = spouseAnchors.get(0)[0];
                barY = spouseAnchors.get(0)[1] + barGap;
                barX1 = xMid - singleBarHalf;
                barX2 = xMid + singleBarHalf;
                // vertical from the single spouse
                g.drawLine(xMid, spouseAnchors.get(0)[1], xMid, barY);
            } else {
                barY = barY + barGap;
                barX1 = minX;
                barX2 = maxX;
                // verticals from each spouse down to the bar
                for (double[] a : spouseAnchors) {
                    g.drawLine(a[0], a[1], a[0], barY);
                }
            }

            // Horizontal bar
            g.drawLine(barX1, barY, barX2, barY);
            double barMidX = (barX1 + barX2) / 2.0;

            // Children alignment: a lower bar aligned to children, connected via a short stem from the spouses' bar
            if (!children.isEmpty()) {
                double minChildX = Double.POSITIVE_INFINITY;
                double maxChildX = Double.NEGATIVE_INFINITY;
                double minTopY = Double.POSITIVE_INFINITY;
                List<double[]> childAnchors = new ArrayList<>(); // [xCenter, topY]
                for (String cId : children) {
                    Point2D pc = layout.getPosition(cId);
                    if (pc == null) continue;
                    double w = metrics.getWidth(cId) * zoom;
                    double xCenter = pc.getX() * zoom + panX + w / 2.0;
                    double topY = pc.getY() * zoom + panY;
                    childAnchors.add(new double[]{xCenter, topY});
                    if (xCenter < minChildX) minChildX = xCenter;
                    if (xCenter > maxChildX) maxChildX = xCenter;
                    if (topY < minTopY) minTopY = topY;
                }
                if (!childAnchors.isEmpty()) {
                    final double childBarGap = 8.0 * zoom; // distance above children
                    double childBarY = minTopY - childBarGap;

                    if (childAnchors.size() == 1) {
                        // Special-case: single child. Draw a short stub bar centered at the child so it remains visible.
                        double cx = childAnchors.get(0)[0];
                        double ct = childAnchors.get(0)[1];
                        // vertical from spouses' bar midpoint to children bar level
                        g.drawLine(barMidX, barY, barMidX, childBarY);
                        // horizontal from the stem to the child's x
                        g.drawLine(Math.min(barMidX, cx), childBarY, Math.max(barMidX, cx), childBarY);
                        // vertical down to the child
                        g.drawLine(cx, childBarY, cx, ct);
                    } else {
                        // vertical from spouses' bar midpoint to children bar
                        g.drawLine(barMidX, barY, barMidX, childBarY);
                        // horizontal children bar spanning children range
                        g.drawLine(minChildX, childBarY, maxChildX, childBarY);
                        // verticals from children bar down to each child top
                        for (double[] a : childAnchors) {
                            g.drawLine(a[0], childBarY, a[0], a[1]);
                        }
                    }
                }
            }
        }
    }
}
