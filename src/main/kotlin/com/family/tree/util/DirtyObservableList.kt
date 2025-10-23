package com.family.tree.util

import java.util.ArrayList

/**
 * ArrayList that marks the global DirtyFlag on any mutating operation.
 * Mirrors the original Java API for seamless interop.
 */
class DirtyObservableList<E> : ArrayList<E> {

    constructor() : super()
    constructor(c: Collection<out E>) : super(c)

    private fun dirty() = DirtyFlag.setModified()

    override fun add(element: E): Boolean {
        val r = super.add(element)
        if (r) dirty()
        return r
    }

    override fun add(index: Int, element: E) {
        super.add(index, element)
        dirty()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val r = super.addAll(elements)
        if (r) dirty()
        return r
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        val r = super.addAll(index, elements)
        if (r) dirty()
        return r
    }

    override fun removeAt(index: Int): E {
        val e = super.removeAt(index)
        dirty()
        return e
    }

    override fun remove(element: E): Boolean {
        val r = super.remove(element)
        if (r) dirty()
        return r
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val r = super.removeAll(elements)
        if (r) dirty()
        return r
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val r = super.retainAll(elements)
        if (r) dirty()
        return r
    }

    override fun clear() {
        if (isNotEmpty()) {
            super.clear()
            dirty()
        } else {
            super.clear()
        }
    }

    override fun set(index: Int, element: E): E {
        val prev = super.set(index, element)
        dirty()
        return prev
    }

    override fun replaceAll(operator: java.util.function.UnaryOperator<E>) {
        super.replaceAll(operator)
        dirty()
    }
}
