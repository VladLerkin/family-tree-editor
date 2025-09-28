package com.pedigree.render;

import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

/**
 * Node metrics that size person rectangles based on their name split into two lines:
 * first name on the first line, last name on the second line, both centered.
 * Width is the max of line widths plus horizontal padding; height accounts for two lines.
 *
 * Measurements are approximate and independent of zoom; TreeRenderer applies zoom.
 */
public class TextAwareNodeMetrics extends NodeMetrics {
    // Base font size (layout units, at zoom=1)
    private double baseFontSize = 12.0;
    // Visual scale factor for person boxes/text relative to connector lines
    private static final double SCALE = 2.0;
    // Approximate average glyph width factor (em to px)
    private static final double AVG_CHAR_FACTOR = 0.6; // ~0.6 * font size per character
    // Padding around text (layout units)
    private static final double H_PAD = 12.0;
    private static final double V_PAD = 10.0;
    private static final double LINE_SPACING = 1.2; // line height multiplier

    private ProjectRepository.ProjectData data;

    public void setData(ProjectRepository.ProjectData data) {
        this.data = data;
    }

    public void setBaseFontSize(double baseFontSize) {
        if (baseFontSize > 6.0) this.baseFontSize = baseFontSize;
    }

    @Override
    public double getWidth(String nodeId) {
        if (data != null && nodeId != null && isIndividual(nodeId)) {
            NameParts np = nameParts(nodeId);
            double font = baseFontSize * SCALE;
            double w1 = approxTextWidth(np.first, font);
            double w2 = approxTextWidth(np.last, font);
            double w = Math.max(w1, w2) + (H_PAD * SCALE) * 2.0;
            // Minimum width fallback
            return Math.max(w, 80.0 * SCALE);
        }
        return super.getWidth(nodeId);
    }

    @Override
    public double getHeight(String nodeId) {
        if (data != null && nodeId != null && isIndividual(nodeId)) {
            double lineH = baseFontSize * LINE_SPACING * SCALE;
            double h = (V_PAD * SCALE) * 2.0 + lineH * 2.0;
            return Math.max(h, 40.0 * SCALE);
        }
        return super.getHeight(nodeId);
    }

    private boolean isIndividual(String nodeId) {
        for (Individual i : data.individuals) {
            if (nodeId.equals(i.getId())) return true;
        }
        return false;
    }

    private NameParts nameParts(String nodeId) {
        for (Individual i : data.individuals) {
            if (nodeId.equals(i.getId())) {
                String first = i.getFirstName() != null ? i.getFirstName() : "";
                String last = i.getLastName() != null ? i.getLastName() : "";
                return new NameParts(first, last);
            }
        }
        return new NameParts("", "");
    }

    public static double approxTextWidth(String s, double fontSize) {
        if (s == null || s.isEmpty()) return 0.0;
        // Rough estimate; good enough for sizing and centering without platform font metrics
        return s.length() * fontSize * AVG_CHAR_FACTOR;
    }

    public static double horizontalPadding() { return H_PAD; }
    public static double verticalPadding() { return V_PAD; }
    public static double lineSpacing() { return LINE_SPACING; }

    private record NameParts(String first, String last) {}
}
