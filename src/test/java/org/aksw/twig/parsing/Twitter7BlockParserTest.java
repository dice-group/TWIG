package org.aksw.twig.parsing;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class Twitter7BlockParserTest {

    /**
     * Tests parsing of twitter7 data information of one block.
     */
    @Test
    public void parseTest() {
        Twitter7BlockParser parser = new Twitter7BlockParser(new ImmutableTriple<>(
                "       2009-09-30 23:55:53",
                "       http://twitter.com/andreavaleriac",
                "       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)"
        ));

        try {
            parser.call();
        } catch (Twitter7BlockParseException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals("2009-09-30T23:55:53", parser.getMessageDateTime().toString());
        Assert.assertEquals("andreavaleriac", parser.getTwitterUserName());
        Assert.assertEquals("I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)", parser.getMessageContent());
    }
}
