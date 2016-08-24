package org.aksw.twig.automaton.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WordSamplerTest {

    @Test
    public void simpleTest() {
        WordMatrix matrix = new WordMatrix();
        matrix.alterFrequency("a", "a", 1);
        WordSampler sampler = new WordSampler(matrix);
        Assert.assertEquals("a", sampler.getSuccessorDistribution("a").sample());
    }

    @Test
    public void complexTest() {
        WordMatrix matrix = new WordMatrix();
        String predecessor = "a";
        String[] successors = new String[] { "a", "b" };
        Map<String, Integer> occurrences = new HashMap<>();
        Arrays.stream(successors).forEach(successor -> {
            matrix.alterFrequency(predecessor, successor, 1);
            occurrences.put(successor, 0);
        });

        WordSampler sampler = new WordSampler(matrix);
        for (int i = 0; i < 10000; i++) {
            String sample = sampler.getSuccessorDistribution(predecessor).sample();
            occurrences.put(sample, occurrences.get(sample) + 1);
        }

        Assert.assertEquals(1d, (double) occurrences.get("a") / (double) occurrences.get("b"), 0.1);
    }

    @Test
    public void tweetLengthTest() {
        WordMatrix matrix = new WordMatrix();
        matrix.alterFrequency("a", "a", 10);
        matrix.alterFrequency("a", "", 1);
        matrix.alterFrequency("", "a", 1);
        WordSampler sampler = new WordSampler(matrix);

        String tweet = sampler.sample(new TestRandom());
        Assert.assertTrue(140 >= tweet.length());
    }

    private class TestRandom extends Random {

        @Override
        public double nextDouble() {
            return 0.6;
        }
    }
}
