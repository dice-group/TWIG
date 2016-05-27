package org.aksw.twig.parsing;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class Twitter7BlockParserTest {

    private Twitter7BlockParser parser;

    @Test
    public void parseTest() {
        this.parser = new Twitter7BlockParser(new ImmutableTriple<>(
                "       2009-09-30 23:55:53",
                "       http://twitter.com/andreavaleriac",
                "       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)"
        ));

        try {
            this.parser.call();
        } catch (Twitter7BlockParseException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals("2009-09-30T23:55:53", this.parser.getMessageDateTime().toString());
        Assert.assertEquals("andreavaleriac", this.parser.getTwitterUserName());
        Assert.assertEquals("I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)", this.parser.getMessageContent());
    }
}
