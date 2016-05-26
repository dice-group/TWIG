package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Twitter7ReaderTest {

    private String sampleDataPath;
    private String brokenSampleDataPath;
    private String emptySampleDataPath;

    private Set<String> allBlocks = new HashSet<>();

    @Before
    public void setSampleDataPath() {
        this.sampleDataPath = getClass().getClassLoader().getResource("data/sample.txt").getPath();
        this.brokenSampleDataPath = getClass().getClassLoader().getResource("data/sample_broken.txt").getPath();
        this.emptySampleDataPath = getClass().getClassLoader().getResource("data/sample_empty.txt").getPath();
    }

    @Test
    public void testBlockReading() {
        try {
            Twitter7Reader reader = new Twitter7Reader(sampleDataPath, () -> new Callback() );
            Triple<String, String, String> triple = reader.readTwitter7Block();
            Assert.assertEquals("T       2009-09-30 23:55:53", triple.getLeft());
            Assert.assertEquals("U       http://twitter.com/andreavaleriac", triple.getMiddle());
            Assert.assertEquals("W       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)", triple.getRight());

            triple = reader.readTwitter7Block();
            Assert.assertEquals("T       2009-09-30 23:55:53", triple.getLeft());
            Assert.assertEquals("U       http://twitter.com/beautyismynam3", triple.getMiddle());
            Assert.assertEquals("W       soooo i got sum advice from the 1 i love most...he goes drop all those lame ass birds uno unot like them...lol here it goes", triple.getRight());
        } catch (IOException e) {
            Assert.fail("Class creation failed.");
        }
    }

    @Test
    public void testBrokenBlockReading() {
        try {
            Twitter7Reader reader = new Twitter7Reader(brokenSampleDataPath, () -> new Callback() );
            Triple<String, String, String> triple = reader.readTwitter7Block();
            Assert.assertEquals("T       2009-09-30 23:55:53", triple.getLeft());
            Assert.assertEquals("U       http://twitter.com/elektrap2", triple.getMiddle());
            Assert.assertEquals("W       I'm writing my first twitter!!", triple.getRight());
        } catch (IOException e) {
            Assert.fail("Class creation failed.");
        }
    }

    @Test
    public void testEmptyBlockReading() {
        try {
            Twitter7Reader reader = new Twitter7Reader(sampleDataPath, () -> new Callback() );
            reader.run();
            Assert.assertTrue(allBlocks.isEmpty());
        } catch (IOException e) {
            Assert.fail("Class creation failed.");
        }
    }


    @Test
    public void testReading() {
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/andreavaleriac\n" +
                "W       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/beautyismynam3\n" +
                "W       soooo i got sum advice from the 1 i love most...he goes drop all those lame ass birds uno unot like them...lol here it goes\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/bhightower1\n" +
                "W       @Audition_Portal PAAAUULLLL! I miss youuu! Lol I thought I sent you an email, but it was placed in my Drafts so I'll resend that to you! =]\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/bonita_bob16\n" +
                "W       I'm sad. I'm in pain. I want to cry.\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/burkehramsey\n" +
                "W       @klewis33 haha yeah I made my own Twitter client! :-)\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/chessiecat65\n" +
                "W       Get this-I caught my mom listening to \"Slippery When Wet\" by Bon Jovi...and she said she LIKES IT! Winger next? ;)\n");
        allBlocks.add("T       2009-09-30 23:55:53\n" +
                "U       http://twitter.com/elektrap2\n" +
                "W       I'm writing my first twitter!!\n");

        try {
            Twitter7Reader reader = new Twitter7Reader(emptySampleDataPath, () -> new Callback() );
            Triple<String, String, String> triple = reader.readTwitter7Block();
            Assert.assertNull(triple);
        } catch (IOException e) {
            Assert.fail("Class creation failed.");
        }
    }

    private class Callback implements FutureCallback<String> {

        @Override
        public synchronized void onSuccess(String result) {
            allBlocks.remove(result);
        }

        @Override
        public void onFailure(Throwable t) {
            Assert.fail(t.toString());
        }
    }
}
