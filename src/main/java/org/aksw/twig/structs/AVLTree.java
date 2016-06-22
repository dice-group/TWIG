package org.aksw.twig.structs;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class AVLTree<T extends Comparable<T>> implements Collection<T> {

    private AVLNode root;

    private int size = 0;

    private int height = 0; // TODO: use and maintain

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Behaves like {@link Collection#contains(Object)} but is guaranteed to run in {@code O(log n)} with {@code n} being amount of values in this tree.
     * The default implementation of {@link Collection#contains(Object)} will run in {@code O(n)}.
     * @param value Value to check presence of.
     * @return {@code true} if and only if value is present in the tree.
     */
    public boolean contains(final T value) {
        if (root == null || value == null) {
            return false;
        }

        AVLNode node = root.traverse(value, true);
        return node.val.equals(value);
    }

    @Override
    public boolean contains(final Object o) {
        if (o == null) {
            return false;
        }

        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(o)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new AVLIterator();
    }

    @Override
    public Object[] toArray() {
        Iterator<T> iterator = iterator();
        Object[] array = new Object[size()];
        int i = 0;
        while (iterator.hasNext()) {
            array[i++] = iterator.next();
        }

        return array;
    }

    @Override
    public <V> V[] toArray(final V[] a) {
        if (a.length < size) {
            return (V[]) toArray();
        }

        LinkedList<Integer> l = new LinkedList<>();

        Iterator<T> iterator = iterator();
        Object[] result = a;
        int i = 0;
        while (iterator.hasNext()) {
            result[i++] = iterator.next();
        }

        if (size() < a.length) {
            a[size()] = null;
        }

        return a;
    }

    @Override
    public boolean add(final T t) {
        if (root == null) {
            root = new AVLNode(t, null);
            size++;
            return true;
        }

        // TODO grant AVL property
        root.traverse(t, false).entail(t);
        size++;
        return true;
    }

    public boolean remove(final T value) {
        if (root == null || value == null) {
            return false;
        }

        AVLNode toRemove = root.traverse(value, true);
        if (toRemove.val.equals(value)) {
            // TODO
            return true;
        }

        return false;
    }

    @Override
    public boolean remove(final Object o) {
        if (o == null) {
            return false;
        }

        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(iterator)) {
                // TODO
                return true;
            }
        }


        return false;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return c.stream().allMatch(element -> contains(element));
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        boolean changed = false;
        for (T element: c) {
            changed |= add(element);
        }

        return changed;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        for (Object element: c) {
            changed |= remove(element);
        }
        return changed;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    private class AVLNode {

        private T val;

        private AVLNode parent;

        private AVLNode leq;

        private AVLNode gtr;

        private int balanceFactor = 0;

        AVLNode(final T val, final AVLNode parent) {
            this.val = val;
            this.parent = parent;
        }

        boolean hasNext(final T val) {
            return val.compareTo(val) <= 0 ? leq != null : gtr != null;
        }

        AVLNode next(final T val) {
            return val.compareTo(val) <= 0 ? leq : gtr;
        }

        AVLNode traverse(final T val, final boolean lookup) {
            AVLNode last = this;
            while (last.hasNext(val) || (lookup && last.val.equals(val))) {
                last = last.next(val);
            }

            return last;
        }

        AVLNode entail(final T value) {
            AVLNode prev;
            if (val.compareTo(value) <= 0) {
                prev = leq;
                leq = new AVLNode(value, this);
            } else {
                prev = gtr;
                gtr = new AVLNode(value, this);
            }

            if (prev != null) {
                prev.parent = null;
            }
            return prev;
        }

        void add(final T value) {
            AVLNode newNode = new AVLNode(value, this);
            if (val.compareTo(value) <= 0) {
                if (leq != null) {
                    throw new IllegalStateException();
                }

                balanceFactor--;
                leq = newNode;
            } else {
                if (gtr != null) {
                    throw new IllegalStateException();
                }

                balanceFactor++;
                gtr = newNode;
            }

            /*if (balanceFactor!= 0) {
                for (AVLNode currentParent = parent; currentParent != null; currentParent = currentParent.parent) {
                    int oldDepth = currentParent.depth;
                    currentParent.refreshDepth();
                    if (currentParent.depth == oldDepth) {
                        break;
                    }
                }
            }*/
        }


    }

    private class AVLIterator implements Iterator<T> {

        private int traverseIndex = 0;

        private Object[] traverseArray = new Object[height];

        AVLIterator() {
            if (root != null) {
                traverseArray[traverseIndex] = root;
            }
        }

        @Override
        public boolean hasNext() {
            // other || (init check)
            return traverseIndex > -1 || (traverseIndex == 0 && traverseArray[traverseIndex] == null && root != null);
        }

        @Override
        public T next() {
            AVLNode output = (AVLNode) traverseArray[traverseIndex++];
            if (traverseIndex == traverseArray.length) {
                branch();
            } else {
                setNext();
            }

            return output.val;
        }

        private void setNext() {
            AVLNode output = (AVLNode) traverseArray[traverseIndex - 1];
            AVLNode next = (AVLNode) traverseArray[traverseIndex];
            if ((next == output.leq && output.gtr == null) || next == output.gtr) {
                branch();
            } else if (next == null && output.leq != null) {
                traverseArray[traverseIndex] = output.leq;
            } else {
                traverseArray[traverseIndex] = output.gtr;
            }
        }

        private void branch() {
            if (traverseIndex-- != traverseArray.length) {
                traverseArray[traverseIndex] = null;
            }

            setNext();
        }
    }
}
