package org.aksw.twig.parsing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * This class will only be used for small tests by author in order to test some jave constructs.
 */
public class MiscTest {

    private static final Logger LOGGER = LogManager.getLogger(MiscTest.class);

    @Test
    public void test() {
        try {
            URL twitterAccount = new URL("http://twitter.com/andreavaleriac");
            LOGGER.info(twitterAccount.getAuthority());
            Arrays.stream(twitterAccount.getPath().split("/"))
                    .forEach( pathPart -> LOGGER.info(pathPart) );

        } catch (MalformedURLException e) {
            Assert.fail();
        }
    }
}
