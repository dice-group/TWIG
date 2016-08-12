package org.aksw.twig.automaton.tweets;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WordSamplerTest {

    @Test
    public void simpleTest() {
        WordMatrix matrix = new WordMatrix();
        matrix.put("a", "a", 1);
        WordSampler sampler = new WordSampler(matrix);
        Assert.assertEquals("a", sampler.sample("a"));
    }

    @Test
    public void complexTest() {
        WordMatrix matrix = new WordMatrix();
        String predecessor = "a";
        String[] successors = new String[] { "a", "b" };
        Map<String, Integer> occurrences = new HashMap<>();
        Arrays.stream(successors).forEach(successor -> {
            matrix.put(predecessor, successor, 1);
            occurrences.put(successor, 0);
        });

        WordSampler sampler = new WordSampler(matrix);
        for (int i = 0; i < 10000; i++) {
            String sample = sampler.sample(predecessor);
            occurrences.put(sample, occurrences.get(sample) + 1);
        }

        Assert.assertEquals(1d, (double) occurrences.get("a") / (double) occurrences.get("b"), 0.1);
    }
}
