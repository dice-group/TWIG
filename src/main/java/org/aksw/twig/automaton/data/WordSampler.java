package org.aksw.twig.automaton.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.aksw.twig.Const;
import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.aksw.twig.statistics.SamplingDiscreteTreeDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Transfers a word matrix into multiple {@link SamplingDiscreteTreeDistribution} objects in order
 * to be able to supply random words.
 */
public class WordSampler implements SamplingWordPredecessorSuccessorDistribution {

  private static final Logger LOGGER = LogManager.getLogger(WordSampler.class);

  private static final double TRUNCATE_CHANCE = Const.TRUNCATE_CHANCE;

  private static final double DISTRIBUTION_CHANCE_DELTA = Const.DISTRIBUTION_CHANCE_DELTA;

  // TODO: Add to config
  private static final int MAX_CHARS = 140;
  final int maxTries = 10;
  private final Map<String, SamplingDiscreteTreeDistribution<String>> distributionMap =
      new HashMap<>();

  private final Random r = new Random();

  public WordMatrix matrix = null;

  WordFilter filter = new WordFilter();

  /**
   * Creates a {@link WordSampler} of given {@link WordMatrix}.
   *
   * @param matrix Matrix to create the sampler of.
   */
  public WordSampler(final WordMatrix matrix) {

    this.matrix = matrix;

    matrix.getPredecessors().forEach(predecessor -> {
      final WordChanceMapping[] wordChanceMappings =
          matrix.getMappings(predecessor).entrySet().stream()//
              .map(entry -> new WordChanceMapping(entry.getKey(), entry.getValue()))//
              .toArray(WordChanceMapping[]::new);

      // Sort successors alphabetically
      Arrays.sort(wordChanceMappings, WordChanceMapping::compareTo);

      final SamplingDiscreteTreeDistribution<String> distribution;
      distribution = new SamplingDiscreteTreeDistribution<>(DISTRIBUTION_CHANCE_DELTA);

      for (int i = 0; i < wordChanceMappings.length; i++) {
        final WordChanceMapping mapping = wordChanceMappings[i];
        distribution.addDiscreteEvent(mapping.word, mapping.chance);
      }

      distributionMap.put(predecessor, distribution);
    });
  }

  /**
   * Sets a seed to the internal random number generator by {@link Random#setSeed(long)}.
   *
   * @param seed Seed.
   */
  @Override
  public void reseedRandomGenerator(final long seed) {
    r.setSeed(seed);
  }

  /**
   * Samples a successor to given predecessor.
   *
   * @param predecessor Predecessor.
   * @return Random successor.
   */
  @Override
  public SamplingDiscreteDistribution<String> getSuccessorDistribution(final String predecessor) {
    final SamplingDiscreteDistribution<String> returnValue = distributionMap.get(predecessor);

    if (returnValue == null) {
      return EMPTY_WORD_SAMPLER;
    }

    return returnValue;
  }

  /**
   * Samples a tweet.
   *
   * @return Random tweet.
   */
  @Override
  public String sample() {
    return sample(r);
  }

  @Override
  public String sample(final Random rand) {

    final StringBuffer sb = new StringBuffer();

    String pre = getSuccessorDistribution("").sample(rand);
    int charactersCount = pre.length();

    int tries = 0;
    while (!filter.isPredecessor(pre) && tries++ < maxTries) {
      pre = getSuccessorDistribution("").sample(rand);
    }
    sb.append(pre);
    String next = null;

    while (charactersCount < MAX_CHARS) {
      next = getSuccessorDistribution(pre).sample(rand);
      tries = 0;
      while (!filter.isSuccessor(next) && tries++ < maxTries) {
        next = getSuccessorDistribution(pre).sample(rand);
      }

      if (next.equals("")) {
        break;
      }

      if (filter.needsSpace(next)) {
        sb.append(" ");
        charactersCount++;// + 1 for space character
      }
      sb.append(next);
      pre = next;
      charactersCount += next.length();
    }

    if (next != null && charactersCount > MAX_CHARS) {
      sb.replace(sb.length() - next.length(), sb.length(), "");
    }

    return sb.toString().trim();
  }

  /**
   * Wrapper class for a word and it's chance to be a successor.
   */
  private static class WordChanceMapping implements Comparable<WordChanceMapping> {

    private final String word;

    private final double chance;

    WordChanceMapping(final String word, final double chance) {
      this.word = word;
      this.chance = chance;
    }

    @Override
    public int compareTo(final WordChanceMapping mapping) {
      return word.compareTo(mapping.word);
    }
  }

  private static final EmptyWordSampler EMPTY_WORD_SAMPLER = new EmptyWordSampler();

  /**
   * Is used as return value if there is no distribution for a predecessor.
   */
  private static class EmptyWordSampler implements SamplingDiscreteDistribution<String> {

    @Override
    public void reseedRandomGenerator(final long seed) {}

    @Override
    public String sample() {
      return "";
    }

    @Override
    public String sample(final Random r) {
      return "";
    }
  }

  /**
   * Creates a {@link WordSampler} by parsing a {@link WordMatrix} of file stated in {@code arg[0]}.
   * <br>
   * Then {@code arg[1]} tweets will be sampled and outputted by {@link Logger#info(String)}.
   *
   * @param args Arguments.
   */
  public static void main(final String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Insufficient arguments");
    }

    final int messages = Integer.parseInt(args[1]);
    try (ObjectInputStream objectInputStream =
        new ObjectInputStream(new FileInputStream(new File(args[0])))) {
      final WordMatrix matrix = (WordMatrix) objectInputStream.readObject();
      matrix.printInspection();
      matrix.truncateTo(TRUNCATE_CHANCE);
      final WordSampler sampler = new WordSampler(matrix);
      for (int i = 0; i < messages; i++) {
        LOGGER.info("Message: {}", sampler.sample());
      }
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
