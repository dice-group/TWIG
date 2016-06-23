package org.aksw.twig.structs;

import org.junit.Assert;
import org.junit.Test;

public class AVLTreeTest {

    @Test
    public void readWriteTest() {
        AVLTree<Integer> tree = new AVLTree<>();
        tree.add(5);
        tree.add(4);
        tree.add(3);
        tree.add(1);
        tree.add(2);

//        Assert.assertTrue("Tree does not contain 1 (as object)", tree.contains((Object) 1));
        for (int i = 1; i <= 5; i++) {
            Assert.assertTrue("Tree does not contain ".concat(Integer.toString(i)), tree.contains(i));
        }
//        Assert.assertFalse("Tree does contain 0 (as object)", tree.contains((Object) 0));
        Assert.assertFalse("Tree does contain 0", tree.contains(0));
    }
}
