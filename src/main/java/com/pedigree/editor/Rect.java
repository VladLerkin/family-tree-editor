package com.pedigree.editor;

/**
 * Simple immutable rectangle helper for selection ranges.
 */
public final class Rect {
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Rect(double x, double y, double width, double height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative width/height");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public boolean intersects(Rect other) {
        if (other == null) return false;
        return this.x < other.x + other.width &&
               this.x + this.width > other.x &&
               this.y < other.y + other.height &&
               this.y + this.height > other.y;
    }
}
