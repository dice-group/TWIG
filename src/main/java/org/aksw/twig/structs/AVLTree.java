package org.aksw.twig.structs;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class AVLTree<T extends Comparable<T>> implements Collection<T> {

    private AVLNode root;

    private int size = 0;

    @Override
    public int size() {
        return size;
    }

    public int height() {
        return root == null ? 0 : root.height;
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
        if (node == null) {
            return false;
        }
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
        if (t == null) {
            throw new NullPointerException();
        }

        if (root == null) {
            root = new AVLNode(t, null);
            size++;
            return true;
        }

        root.traverse(t, false).add(t);
        size++;
        root = root.root();
        return true;
    }

    public boolean remove(final T t) {
        if (root == null || t == null) {
            return false;
        }

        AVLNode toRemove = root.traverse(t, true);
        if (toRemove.val.equals(t)) {
            toRemove.parent.remove(toRemove);
            root = root.root();
            return true;
        }

        return false;
    }

    @Override
    public boolean remove(final Object o) {
        if (o == null) {
            return false;
        }

        AVLIterator iterator = new AVLIterator();
        while (iterator.hasNext()) {
            AVLNode node = iterator.nextNode();
            if (node.val.equals(o)) {
                node.parent.remove(node);
                root = root.root();
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

        private AVLNode leq = null;

        private AVLNode gtr = null;

        private int balanceFactor = 0;

        private int height = 1;

        AVLNode(final T val, final AVLNode parent) {
            this.val = val;
            this.parent = parent;
        }

        AVLNode traverse(final T toFind, final boolean lookup) {
            for (AVLNode traversed = this;;) {
                AVLNode tmp = traversed.val.compareTo(toFind) > 0 ? traversed.leq : traversed.gtr;
                if (tmp == null) {
                    if (lookup) {
                        return null;
                    }
                    return traversed;
                }
                traversed = tmp;
                if (lookup && traversed.val.equals(toFind)) {
                    return traversed;
                }
            }
        }

        AVLNode root() {
            AVLNode node = this;
            while (node.parent != null) {
                node = node.parent;
            }
            return node;
        }

        void add(final T value) {
            addTree(new AVLNode(value, this));
        }

        void addTree(final AVLNode toAdd) {
            if (val.compareTo(toAdd.val) > 0) {
                if (leq != null) throw new IllegalStateException();
                leq = toAdd;
            } else {
                if (gtr != null) throw new IllegalStateException();
                gtr = toAdd;
            }

            checkBalance();
        }

        void remove(final AVLNode toRemove) {
            AVLNode removedLeq = toRemove.leq;
            AVLNode removedGtr = toRemove.gtr;

            if (leq == toRemove) {
                leq = null;
            } else if (gtr == toRemove) {
                gtr = null;
            } else {
                throw new IllegalArgumentException();
            }

            if (removedGtr != null) {
                removedGtr.parent = null;
                if (removedLeq != null) {
                    removedLeq.parent = null;
                    removedGtr.traverse(removedLeq.val, false).addTree(removedLeq);
                }

                this.addTree(removedGtr.root());
            } else if (removedLeq != null) {
                removedLeq.parent = null;
                this.addTree(removedLeq);
            }
        }

        void refreshBalance() {
            int gtrHeight = gtr == null ? 0 : gtr.height;
            int leqHeight = leq == null ? 0 : leq.height;
            height = Math.max(gtrHeight, leqHeight) + 1;
            balanceFactor = gtrHeight - leqHeight;
        }

        void checkBalance() {
            refreshBalance();
            if (Math.abs(balanceFactor) > 1) {
                rebalance();
            }

            if (balanceFactor != 0 && parent != null) {
                parent.checkBalance();
            }
        }

        void rebalance() {
            if (balanceFactor < 0) {
                if ((leq.gtr == null ? 0 : leq.gtr.height) <= (leq.leq == null ? 0 : leq.leq.height)) {
                    rotateRight();
                } else {
                    leq.rotateRight();
                    rotateLeft();
                }
            } else {
                if ((gtr.leq == null ? 0 : gtr.leq.height) <= (gtr.gtr == null ? 0 : gtr.gtr.height)) {
                    rotateLeft();
                } else {
                    gtr.rotateLeft();
                    rotateRight();
                }
            }
        }

        void rotateLeft() {
            gtr.parent = parent;
            if (parent != null) {
                if (parent.leq == this) {
                    parent.leq = gtr;
                } else {
                    parent.gtr = gtr;
                }
            }

            AVLNode gtrLeq = gtr.leq;
            gtr.leq = this;
            parent = gtr;

            gtr = gtrLeq;
            if (gtr != null) {
                gtr.parent = this;
            }

            refreshBalance();
            if (parent != null) {
                parent.refreshBalance();
            }
        }

        void rotateRight() {
            leq.parent = parent;
            if (parent != null) {
                if (parent.leq == this) {
                    parent.leq = leq;
                } else {
                    parent.gtr = leq;
                }
            }

            AVLNode leqGtr = leq.gtr;
            leq.gtr = this;
            parent = leq;

            leq = leqGtr;
            if (leq != null) {
                leq.parent = this;
            }

            refreshBalance();
            if (parent != null) {
                parent.refreshBalance();
            }
        }
    }

    private class AVLIterator implements Iterator<T> {

        private int traverseIndex = 0;

        private Object[] traverseArray = new Object[height()];

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
            return nextNode().val;
        }

        AVLNode nextNode() {
            AVLNode output = (AVLNode) traverseArray[traverseIndex++];
            if (traverseIndex == traverseArray.length) {
                branch();
            } else {
                setNext();
            }

            return output;
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
