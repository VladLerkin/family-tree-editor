package com.pedigree.editor;

public interface Command {
    void execute();
    void undo();
    default String getName() { return this.getClass().getSimpleName(); }
}



