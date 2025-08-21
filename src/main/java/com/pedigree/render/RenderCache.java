package com.pedigree.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple cache for render-related metrics (e.g., text widths).
 */
public class RenderCache {
    private final Map<String, Double> textWidthCache = new ConcurrentHashMap<>();

    public double getTextWidth(String text) {
        if (text == null) return 0.0;
        return textWidthCache.computeIfAbsent(text, s -> s.length() * 7.0); // naive width estimate
    }

    public void clear() {
        textWidthCache.clear();
    }
}
