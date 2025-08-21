package com.pedigree.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.UnaryOperator;

public class DirtyObservableList<E> extends ArrayList<E> {

    public DirtyObservableList() {
        super();
    }

    public DirtyObservableList(Collection<? extends E> c) {
        super(c);
    }

    private void dirty() {
        DirtyFlag.setModified();
    }

    @Override
    public boolean add(E e) {
        boolean r = super.add(e);
        if (r) dirty();
        return r;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);
        dirty();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean r = super.addAll(c);
        if (r) dirty();
        return r;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean r = super.addAll(index, c);
        if (r) dirty();
        return r;
    }

    @Override
    public E remove(int index) {
        E e = super.remove(index);
        dirty();
        return e;
    }

    @Override
    public boolean remove(Object o) {
        boolean r = super.remove(o);
        if (r) dirty();
        return r;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean r = super.removeAll(c);
        if (r) dirty();
        return r;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean r = super.retainAll(c);
        if (r) dirty();
        return r;
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            super.clear();
            dirty();
        } else {
            super.clear();
        }
    }

    @Override
    public E set(int index, E element) {
        E prev = super.set(index, element);
        // consider any set as modification
        dirty();
        return prev;
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        super.replaceAll(operator);
        dirty();
    }
}
