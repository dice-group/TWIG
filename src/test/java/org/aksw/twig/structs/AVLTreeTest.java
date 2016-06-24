package org.aksw.twig.structs;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AVLTreeTest {

    @Test
    public void readWriteTest() {
        Integer[] values = new Integer[] { 5, 4, 3, 1, 2 };
        List<Integer> valuesList = Arrays.asList(values);
        AVLTree<Integer> tree = new AVLTree<>();
        tree.addAll(valuesList);
        Assert.assertFalse(tree.isEmpty());
        Assert.assertEquals(values.length, tree.size());
        Assert.assertTrue(tree.containsAll(valuesList));

        Assert.assertTrue("Tree does not contain 1 (as object)", tree.contains((Object) 1));
        Assert.assertFalse("Tree does contain 0 (as object)", tree.contains((Object) 0));
        Assert.assertFalse("Tree does contain 0", tree.contains(0));

        Assert.assertTrue(tree.remove(4));
        Assert.assertFalse(tree.contains(4));

        Assert.assertFalse(tree.retainAll(valuesList));
        List<Integer> retain = new LinkedList<>();
        
        retain.add(1);
        Assert.assertTrue(tree.retainAll(retain));
        Assert.assertTrue(tree.containsAll(retain));

        tree.clear();
        Assert.assertEquals(0, tree.size());
        Assert.assertTrue(tree.isEmpty());
    }

    @Test
    public void iteratorAndArrayTest() {
        Integer[] values = new Integer[]{ 4, 2, 6, 1, 3, 5, 7 }; // this order will lead into no rebalancing
        AVLTree<Integer> tree = new AVLTree<>();
        tree.addAll(Arrays.asList(values));

        Integer[] depthSearchOrder = new Integer[] { 4, 2, 1, 3, 6, 5, 7 }; // this is the expected order for depth search
        Object[] treeArray = tree.toArray();
        Integer[] integerTreeArray = tree.toArray(new Integer[tree.size()]);
        Assert.assertEquals(depthSearchOrder.length, treeArray.length);
        Assert.assertEquals(depthSearchOrder.length, integerTreeArray.length);
        for (int i = 0; i < depthSearchOrder.length; i++) {
            Assert.assertEquals(depthSearchOrder[i], treeArray[i]);
            Assert.assertEquals(depthSearchOrder[i], integerTreeArray[i]);
        }
    }

    @Test
    public void equalsTest() {
        Integer[] values = new Integer[] { 5, 4, 3, 1, 2 };
        AVLTree<Integer> tree1 = new AVLTree<>();
        AVLTree<Integer> tree2 = new AVLTree<>();
        tree1.addAll(Arrays.asList(values));
        tree2.addAll(Arrays.asList(values));

        Assert.assertEquals(tree1, tree1);
        Assert.assertEquals(tree1, tree2);
        tree2.remove(1);
        Assert.assertNotEquals(tree1, tree2);
        Assert.assertNotEquals(tree1, 0);
        Assert.assertNotEquals(tree1, null);
    }
}
