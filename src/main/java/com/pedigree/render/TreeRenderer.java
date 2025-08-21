package com.pedigree.render;

import com.pedigree.layout.LayoutResult;
import com.pedigree.model.Family;
import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import com.pedigree.model.Relationship;
import com.pedigree.storage.ProjectRepository;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
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
                g.setFillColor(255, 245, 230, 1.0);
                g.setStrokeColor(120, 90, 40, 1.0);
                Family fam = familiesById.get(id);
                if (fam != null) {
                    Individual a = fam.getHusbandId() != null ? individualsById.get(fam.getHusbandId()) : null;
                    Individual b = fam.getWifeId() != null ? individualsById.get(fam.getWifeId()) : null;
                    String nameA = a != null
                            ? ((a.getFirstName() != null ? a.getFirstName() : "") + " " + (a.getLastName() != null ? a.getLastName() : "")).trim()
                            : "";
                    String nameB = b != null
                            ? ((b.getFirstName() != null ? b.getFirstName() : "") + " " + (b.getLastName() != null ? b.getLastName() : "")).trim()
                            : "";
                    if (!nameA.isEmpty() && !nameB.isEmpty()) {
                        label = nameA + " — " + nameB;
                    } else if (!nameA.isEmpty()) {
                        label = nameA;
                    } else if (!nameB.isEmpty()) {
                        label = nameB;
                    } else {
                        label = "Family";
                    }
                }
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

        // Draw edges with simple orthogonal routing
        g.setStrokeColor(60, 60, 60, 1.0);
        g.setLineWidth(1.0);
        for (Relationship rel : data.relationships) {
            Point2D p1 = layout.getPosition(rel.getFromId());
            Point2D p2 = layout.getPosition(rel.getToId());
            if (p1 == null || p2 == null) continue;

            double w1 = metrics.getWidth(rel.getFromId()) * zoom;
            double h1 = metrics.getHeight(rel.getFromId()) * zoom;
            double w2 = metrics.getWidth(rel.getToId()) * zoom;
            double h2 = metrics.getHeight(rel.getToId()) * zoom;

            double x1 = p1.getX() * zoom + panX + w1 / 2.0;
            double y1 = p1.getY() * zoom + panY + h1 / 2.0;
            double x2 = p2.getX() * zoom + panX + w2 / 2.0;
            double y2 = p2.getY() * zoom + panY + h2 / 2.0;

            double midX = (x1 + x2) / 2.0;
            g.drawLine(x1, y1, midX, y1);
            g.drawLine(midX, y1, midX, y2);
            g.drawLine(midX, y2, x2, y2);
        }
    }
}
