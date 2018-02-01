package org.aksw.twig.automaton.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class WordSamplerTest {
  private static final Logger LOGGER = LogManager.getLogger();

  @Test
  public void simpleTest() {
    final WordMatrix matrix = new WordMatrix();
    matrix.alterFrequency("a", "a", 1);
    final WordSampler sampler = new WordSampler(matrix);
    Assert.assertEquals("a", sampler.getSuccessorDistribution("a").sample());
  }

  @Test
  public void complexTest() {
    final WordMatrix matrix = new WordMatrix();
    final String predecessor = "a";
    final String[] successors = new String[] {"a", "b"};
    final Map<String, Integer> occurrences = new HashMap<>();
    Arrays.stream(successors).forEach(successor -> {
      matrix.alterFrequency(predecessor, successor, 1);
      occurrences.put(successor, 0);
    });

    LOGGER.info("occurrences: " + occurrences);
    final WordSampler sampler = new WordSampler(matrix);

    for (int i = 0; i < 10000; i++) {
      final String sample = sampler.getSuccessorDistribution(predecessor).sample();

      LOGGER.info("sample: " + sample + " value" + occurrences.get(sample));

      occurrences.put(sample, occurrences.get(sample) + 1);
    }

    Assert.assertEquals(1d, (double) occurrences.get("a") / (double) occurrences.get("b"), 0.1);
  }

  @Test
  public void tweetLengthTest() {
    final WordMatrix matrix = new WordMatrix();
    matrix.alterFrequency("a", "a", 10);
    matrix.alterFrequency("a", "", 1);
    matrix.alterFrequency("", "a", 1);
    final WordSampler sampler = new WordSampler(matrix);

    final String tweet = sampler.sample(new TestRandom());
    Assert.assertTrue(140 >= tweet.length());
  }

  private class TestRandom extends Random {

    @Override
    public double nextDouble() {
      return 0.6;
    }
  }
}
