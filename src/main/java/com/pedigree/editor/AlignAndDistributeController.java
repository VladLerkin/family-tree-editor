package com.pedigree.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Implements alignment and distribution tools on selected nodes using a PositionAccessor.
 */
public class AlignAndDistributeController {

    public enum Alignment { TOP, MIDDLE, BOTTOM, LEFT, CENTER, RIGHT }
    public enum Distribution { HORIZONTAL, VERTICAL }

    public interface PositionAccessor {
        double getX(String id);
        double getY(String id);
        double getWidth(String id);
        double getHeight(String id);
        void setX(String id, double x);
        void setY(String id, double y);
    }

    private final SelectionModel selectionModel;
    private final PositionAccessor accessor;

    public AlignAndDistributeController(SelectionModel selectionModel, PositionAccessor accessor) {
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    public void align(Alignment mode) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedIds());
        if (ids.size() <= 1) return;

        double minX = ids.stream().mapToDouble(accessor::getX).min().orElse(0);
        double minY = ids.stream().mapToDouble(accessor::getY).min().orElse(0);
        double maxX = ids.stream().mapToDouble(id -> accessor.getX(id) + accessor.getWidth(id)).max().orElse(0);
        double maxY = ids.stream().mapToDouble(id -> accessor.getY(id) + accessor.getHeight(id)).max().orElse(0);
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;

        for (String id : ids) {
            double x = accessor.getX(id);
            double y = accessor.getY(id);
            double w = accessor.getWidth(id);
            double h = accessor.getHeight(id);

            switch (mode) {
                case LEFT -> accessor.setX(id, minX);
                case RIGHT -> accessor.setX(id, maxX - w);
                case CENTER -> accessor.setX(id, centerX - w / 2.0);
                case TOP -> accessor.setY(id, minY);
                case BOTTOM -> accessor.setY(id, maxY - h);
                case MIDDLE -> accessor.setY(id, centerY - h / 2.0);
            }
        }
    }

    public void distribute(Distribution mode) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedIds());
        if (ids.size() <= 2) return;

        if (mode == Distribution.HORIZONTAL) {
            ids.sort(Comparator.comparingDouble(accessor::getX));
            double left = accessor.getX(ids.get(0));
            double right = accessor.getX(ids.get(ids.size() - 1)) + accessor.getWidth(ids.get(ids.size() - 1));
            double totalWidth = ids.stream().mapToDouble(accessor::getWidth).sum();
            double gap = (right - left - totalWidth) / (ids.size() - 1);

            double cursor = left;
            for (String id : ids) {
                accessor.setX(id, cursor);
                cursor += accessor.getWidth(id) + gap;
            }
        } else {
            ids.sort(Comparator.comparingDouble(accessor::getY));
            double top = accessor.getY(ids.get(0));
            double bottom = accessor.getY(ids.get(ids.size() - 1)) + accessor.getHeight(ids.get(ids.size() - 1));
            double totalHeight = ids.stream().mapToDouble(accessor::getHeight).sum();
            double gap = (bottom - top - totalHeight) / (ids.size() - 1);

            double cursor = top;
            for (String id : ids) {
                accessor.setY(id, cursor);
                cursor += accessor.getHeight(id) + gap;
            }
        }
    }
}
