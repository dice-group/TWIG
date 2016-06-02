package org.aksw.twig.parsing;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class will only be used for small tests by author in order to test some jave constructs.
 */
public class MiscTest {

    private static final Logger LOGGER = LogManager.getLogger(MiscTest.class);

    @Test
    public void test() {
        Model model = ModelFactory.createDefaultModel();
        Literal stringLiteral = model.createTypedLiteral(LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse("2016-12-01 00:00:00")).toString(), XSDDatatype.XSDdateTime);

        LOGGER.info(stringLiteral.getDatatypeURI());
    }
}
