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
        g.setLineWidth(1.5 * zoom);
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
                    String first = ind.getFirstName() != null ? ind.getFirstName() : "";
                    String last = ind.getLastName() != null ? ind.getLastName() : "";

                    // Draw background box first
                    g.fillRect(x, y, w, h);
                    g.drawRect(x, y, w, h);

                    // Text layout: two lines, centered
                    double fontSize = 12.0 * 2.0 * Math.max(zoom, 0.1);
                    g.setFontSize(fontSize);
                    double lineH = fontSize * com.pedigree.render.TextAwareNodeMetrics.lineSpacing();
                    double padV = com.pedigree.render.TextAwareNodeMetrics.verticalPadding() * zoom * 2.0;
                    double cx = x + w / 2.0;
                    double y1 = y + padV + lineH; // baseline of first line
                    double y2 = y + padV + lineH * 2.0; // baseline of second line

                    double w1 = com.pedigree.render.TextAwareNodeMetrics.approxTextWidth(first, fontSize);
                    double w2 = com.pedigree.render.TextAwareNodeMetrics.approxTextWidth(last, fontSize);

                    // Ensure label text is black
                    g.setFillColor(0, 0, 0, 1.0);
                    // First line centered
                    g.drawText(first, cx - w1 / 2.0, y1);
                    // Second line centered
                    g.drawText(last, cx - w2 / 2.0, y2);

                    // Draw selection highlight frame if selected (for individuals too)
                    java.util.Set<String> sel = com.pedigree.render.RenderHighlightState.getSelectedIds();
                    if (sel != null && sel.contains(id)) {
                        double baseLW = 1.5 * zoom;
                        g.setLineWidth(3.0 * zoom);
                        g.setStrokeColor(0, 180, 80, 1.0);
                        double pad = 2.0 * zoom;
                        g.drawRect(x - pad, y - pad, w + pad * 2.0, h + pad * 2.0);
                        g.setLineWidth(baseLW);
                    }

                    // Skip the generic label drawing below since we already drew text
                    continue;
                }
            } else if (familyIds.contains(id)) {
                // Do not visualize family nodes on the canvas per requirement
                continue;
            } else {
                g.setFillColor(235, 235, 235, 1.0);
                g.setStrokeColor(60, 60, 60, 1.0);
                g.fillRect(x, y, w, h);
                g.drawRect(x, y, w, h);

                // Ensure label text is black
                g.setFillColor(0, 0, 0, 1.0);
                g.drawText(label, x + 8 * zoom, y + 20 * zoom);
                continue;
            }

            // Default fallback (should rarely reach here)
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
            g.drawText(label, x + 8 * zoom, y + 20 * zoom);
        }

        // Draw family-aligned edges (spouse bar + child stem), like in the reference screenshot
        g.setStrokeColor(60, 60, 60, 1.0);
        g.setLineWidth(1.0 * zoom);

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
                        // Single child: route the stem toward the child's center so it looks correct even if child is offset.
                        double cx = childAnchors.get(0)[0];
                        double ct = childAnchors.get(0)[1];
                        double midY = (barY + childBarY) / 2.0;
                        // vertical from spouses' bar midpoint to mid level
                        g.drawLine(barMidX, barY, barMidX, midY);
                        // horizontal at mid level toward the child center
                        g.drawLine(Math.min(barMidX, cx), midY, Math.max(barMidX, cx), midY);
                        // vertical from child center down to children bar level
                        g.drawLine(cx, midY, cx, childBarY);
                        // vertical down to the child
                        g.drawLine(cx, childBarY, cx, ct);
                    } else {
                        // Multiple children: route stem to the center of the children range, then span the children bar.
                        double childMidX = (minChildX + maxChildX) / 2.0;
                        double midY = (barY + childBarY) / 2.0;
                        // vertical from spouses' bar midpoint to mid level
                        g.drawLine(barMidX, barY, barMidX, midY);
                        // horizontal at mid level toward the children range center
                        g.drawLine(Math.min(barMidX, childMidX), midY, Math.max(barMidX, childMidX), midY);
                        // vertical from children range center down to the children bar level
                        g.drawLine(childMidX, midY, childMidX, childBarY);
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
