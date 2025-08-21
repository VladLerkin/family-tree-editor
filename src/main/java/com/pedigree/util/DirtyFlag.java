package com.pedigree.util;

public final class DirtyFlag {
    private static volatile boolean modified = false;

    private DirtyFlag() { }

    public static boolean isModified() {
        return modified;
    }

    public static void setModified() {
        modified = true;
    }

    public static void clear() {
        modified = false;
    }
}
