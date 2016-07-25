package org.aksw.twig.misc;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class MiscTest {

    private static final Logger LOGGER = LogManager.getLogger(MiscTest.class);

    public final Model model = ModelFactory.createDefaultModel();

    @Test
    public void test() {
        LOGGER.info("Still here");
    }
}
