package org.aksw.twig.structs;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class AVLTree<T extends Comparable<T>> implements Collection<T> {

    private AVLNode root;

    private int size;

    private int height;

    private Class<T> clazz;

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (this.root == null) {
            return false;
        }

        if (!this.clazz.isInstance(o)) {
            return false;
        }

        T val = this.clazz.cast(o);
        Triple<AVLNode, AVLNode, Integer> triple = this.root.traverse(val, true);
        return triple.getMiddle().val.equals(val);
    }

    @Override
    public Iterator<T> iterator() {
        return null; // TODO
    }

    @Override
    public Object[] toArray() {
        Iterator<T> iterator = this.iterator();
        Object[] array = new Object[this.size];
        int i = 0;
        while (iterator.hasNext()) {
            array[i++] = iterator.next();
        }

        return array;
    }

    @Override
    public <V> V[] toArray(V[] a) {
        if (a.length < size) {
            return (V[]) this.toArray();
        }

        LinkedList<Integer> l = new LinkedList<>();

        Iterator<T> iterator = this.iterator();
        Object[] result = a;
        int i = 0;
        while (iterator.hasNext()) {
            result[i++] = iterator.next();
        }

        if (this.size < a.length) {
            a[this.size] = null;
        }

        return a;
    }

    @Override
    public boolean add(T t) {
        // TODO grant AVL property
        if (this.root == null) {
            this.root = new AVLNode(t);
            this.size++;
            return true;
        }

        Triple<AVLNode, AVLNode, Integer> triple = this.root.traverse(t, false);
        triple.getMiddle().entail(t);
        this.size++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (this.root == null) {
            return false;
        }

        if (!this.clazz.isInstance(o)) {
            return false;
        }

        T val = this.clazz.cast(o);
        Triple<AVLNode, AVLNode, Integer> triple = this.root.traverse(val, true);
        if (triple.getMiddle().val.equals(val)) {
            // TODO grant AVL property
            triple.getLeft().gtr = triple.getMiddle().gtr;
            triple.getMiddle().gtr.leq = triple.getMiddle().leq;
            this.size--;
            return true;
        }

        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(element -> this.contains(element));
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T element: c) {
            changed |= this.add(element);
        }

        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object element: c) {
            changed |= this.remove(element);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false; // TODO
    }

    @Override
    public void clear() {
        this.root = null;
        this.size = 0;
    }

    private class AVLNode {
        private int level;

        private T val;

        private AVLNode leq;

        private AVLNode gtr;

        public AVLNode(T val) {
            this.val = val;
        }

        public boolean hasNext(T val) {
            return val.compareTo(val) <= 0 ? leq != null : gtr != null;
        }

        public AVLNode next(T val) {
            return val.compareTo(val) <= 0 ? leq : gtr;
        }

        public Triple<AVLNode, AVLNode, Integer> traverse(T val, boolean lookup) {
            int level = 0;

            AVLNode predecessor = null;
            AVLNode last = this;
            while (last.hasNext(val) || (lookup && last.val.equals(val))) {
                level++;
                predecessor = last;
                last = last.next(val);
            }

            return new ImmutableTriple<>(predecessor, last, level);
        }

        public AVLNode entail(T value) {
            AVLNode prev;
            if (this.val.compareTo(value) <= 0) {
                prev = this.leq;
                this.leq = new AVLNode(value);
            } else {
                prev = this.gtr;
                this.gtr = new AVLNode(value);
            }

            return prev;
        }
    }
}
