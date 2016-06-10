package org.aksw.twig;

import org.aksw.twig.automaton.learning.IWordMatrix;
import org.aksw.twig.automaton.learning.WordMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class MiscTest {

    @Test
    public void test() {
        try {
            IWordMatrix matrix = WordMatrix.of(new File("C:/Git/TWIG/RDF/wordMatrix_0.matrix"));
            matrix.getPredecessors();
        } catch (IOException | ClassNotFoundException e) {
            Assert.fail();
        }
    }
}
