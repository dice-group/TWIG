package org.aksw.twig.automaton.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class calculates the probability of a word being succeeded by another one. Probabilities are
 * calculated by frequency distribution. The word that will be succeeded by another word will be
 * denoted as "predecessor" from now on. The word that is succeeding another word will be denoted as
 * "successor" from now on.<br>
 * <br>
 *
 * A word following "" (the empty word) denotes the word starting a sentence.<br>
 * A word followed by "" (the empty word) denotes the word ending a sentence.<br>
 * <br>
 *
 * By invocation of {@link #alterFrequency(String, String, long)} you alter the frequency
 * distribution of words being followed by one another.<br>
 * By invocation of {@link #getChance(String, String)} you get the transition chance between two
 * words.<br>
 * <br>
 *
 * For example: After invocation of: <br>
 *
 * <pre>
 *         {@code matrix.alterFrequency("a", "b", 3);}
 *         {@code matrix.alterFrequency("a", "c", 2);}
 * </pre>
 *
 * {@code matrix.getChance("a", "b");} will return {@code 0.6} whereas
 * {@code matrix.getChance("a", "c");} will return {@code 0.4};
 */
public class WordMatrix implements Serializable {

  private static final long serialVersionUID = 2104488071228760278L;

  private static final Logger LOGGER = LogManager.getLogger(WordMatrix.class);

  public final Map<Integer, String> index = new HashMap<>();
  public final Map<String, Integer> indexReverse = new HashMap<>();

  public final Map<Integer, MutablePair<Long, Map<Integer, Long>>> matrix = new HashMap<>();

  private boolean alteredSinceCached = true;

  private double cachedMeanChance;

  private double cachedChanceStdDeviation;

  private static final double[] INSPECTION_BOUNDS =
      new double[] {0.5, 0.1, 0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};

  /**
   * Adds {@link w} to index and gets its id.
   *
   * @param word
   * @return id
   */
  private Integer addAndGet(final String word) {
    if (word == null) {
      throw new NullPointerException("Parameter is Null!");
    }
    Integer key = null;
    if (!indexReverse.containsKey(word)) {
      key = index.size();
      index.put(key, word);
      indexReverse.put(word, key);
    } else {
      key = indexReverse.get(word);
    }
    return key;
  }

  /**
   * Alters the frequency distribution: You add {@code count} more occurences of the word
   * {@code predecessor} being followed by {@code successor}.
   *
   * @param predecessor Predecessor to add.
   * @param successor Successor to add.
   * @param count Count to alter the frequency distribution by.
   */
  public void alterFrequency(final String predecessor, final String successor, final long count) {

    alteredSinceCached = true;

    final Integer i = addAndGet(predecessor);
    final Integer ii = addAndGet(successor);

    final MutablePair<Long, Map<Integer, Long>> mapping;
    mapping = matrix.computeIfAbsent(i, key -> new MutablePair<>(0L, new HashMap<>()));
    mapping.setLeft(mapping.getLeft() + count);

    final Long val = mapping.getRight().get(ii);
    mapping.getRight().put(ii, val == null ? count : val + count);
  }

  /**
   * Adds all iterable elements as pairs of predecessors and successors to the frequency
   * distribution. Every {@link Pair} will be processed by:
   * {@code alterFrequency(pair.getLeft(), pair.getRight(), 1);}.
   *
   * @param iterable Pairs of succeeding words to add to the frequency distribution.
   */
  public void putAll(final Iterable<Pair<String, String>> iterable) {
    iterable.forEach(pair -> alterFrequency(pair.getLeft(), pair.getRight(), 1));
  }

  /**
   * Iterates over given {@link Model} looking for statements with
   * {@link TWIGModelWrapper#TWEET_CONTENT_PROPERTY_NAME} predicate. Once a sufficient statement is
   * found all words from the literal will be added to the frequency distribution.
   *
   * @param model Model to add statements from.
   */
  public void addModel(final Model model) {
    model.listStatements().forEachRemaining(statement -> {
      if (statement.getPredicate().getLocalName()
          .equals(TWIGModelWrapper.TWEET_CONTENT_PROPERTY_NAME)) {
        final String tweet = statement.getObject().asLiteral().getString();
        putAll(new TweetSplitterTwo(tweet));
      }
    });
  }

  /**
   * Merges the frequency distribution of given {@link wordMatrix} into this.
   *
   * @param wordMatrix Matrix to merge.
   */
  public void merge(final WordMatrix wordMatrix) {
    wordMatrix.matrix.entrySet().forEach(entry -> {
      final String predecessor = wordMatrix.index.get(entry.getKey());
      entry.getValue().getRight().entrySet().forEach(mappedEntry -> {
        final String successor = wordMatrix.index.get(mappedEntry.getKey());
        alterFrequency(predecessor, successor, mappedEntry.getValue());
      });
    });
  }

  /**
   * Returns the probability that {@code predecessor} will be followed by {@code successor}.
   *
   * @param predecessor Predecessor.
   * @param successor Successor.
   * @return Chance.
   * @throws IllegalArgumentException Thrown if there is no mapping for the {@code predecessor}.
   */
  public double getChance(final String predecessor, final String successor)
      throws IllegalArgumentException {

    final int i = addAndGet(predecessor);
    final int ii = addAndGet(successor);

    final MutablePair<Long, Map<Integer, Long>> mapping = matrix.get(i);
    if (mapping == null) {
      throw new IllegalArgumentException("No mapping found.");
    }

    return (double) mapping.getRight().get(ii) / (double) mapping.getLeft();
  }

  /**
   * Returns the set of all predecessors that can be queried as first argument in
   * {@link #getChance(String, String)} or {@link #getMappings(String)}.
   *
   * @return Set of predecessors.
   */
  public Set<String> getPredecessors() {
    return matrix.keySet().stream().map(key -> index.get(key)).collect(Collectors.toSet());
  }

  /**
   * Get all words that can be successor to the {@code predecessor}. Those successors are mapped to
   * their chance of succeeding.
   *
   * @param predecessor Predecessor.
   * @return Map of successor to succeeding chance.
   */
  public Map<Integer, Double> getMappings(final String predecessor)
      throws IllegalArgumentException {

    final Integer i = addAndGet(predecessor);

    final MutablePair<Long, Map<Integer, Long>> mapping = matrix.get(i);
    if (mapping == null) {
      throw new IllegalArgumentException("No mapping found.");
    }

    final long size = mapping.getLeft();
    return mapping.getRight().entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> (double) entry.getValue() / (double) size));
  }

  /**
   * Prints information about distriution of transition chances. For each element {@code x} in
   * {@link #INSPECTION_BOUNDS} you will see the average percentage of words that succeed with a
   * chance lower than or equal to {@code x}.<br>
   * Results will be printed by usage of {@link Logger#info(String, Object...)}.
   */
  public void printInspection() {
    final Map<Double, List<Double>> boundsMap = new HashMap<>(INSPECTION_BOUNDS.length);
    for (final double bound : INSPECTION_BOUNDS) {
      boundsMap.put(bound, new LinkedList<>());
    }

    matrix.entrySet().forEach(entry -> {
      Set<Map.Entry<Integer, Long>> entries = entry.getValue().getRight().entrySet();
      final double allEntries = entries.size();
      final double count = entry.getValue().getLeft();
      for (final double bound : INSPECTION_BOUNDS) {
        entries = entries.stream()
            .filter(successorEntry -> (double) successorEntry.getValue() / count <= bound)
            .collect(Collectors.toSet());
        boundsMap.get(bound).add(entries.size() / allEntries);
      }
    });

    for (final double bound : INSPECTION_BOUNDS) {
      final List<Double> inBounds = boundsMap.get(bound);
      final double averageInBounds =
          inBounds.stream().reduce(0d, (a, b) -> a + b) / inBounds.size();

      LOGGER.info("On average {}% of succeeding words succeed with a chance <= {}.",
          averageInBounds, bound);
    }
  }

  /**
   * Calculates {@link #cachedMeanChance} and {@link #cachedChanceStdDeviation} by inspection of all
   * transition chances present in {@link #matrix}.
   */
  private void calculateStatisticalValues() {

    alteredSinceCached = false;

    final List<Double> chances = new LinkedList<>();
    matrix.entrySet().forEach(entry -> {
      final double count = entry.getValue().getLeft();
      chances.addAll(entry.getValue().getRight().entrySet().stream()
          .map(successorEntry -> (double) successorEntry.getValue() / count)
          .collect(Collectors.toList()));
    });

    cachedMeanChance = chances.stream().reduce(0d, Double::sum) / chances.size();
    cachedChanceStdDeviation =
        Math.sqrt(chances.stream().reduce(0d, (a, b) -> a + b * b) / chances.size()
            - cachedMeanChance * cachedMeanChance);
  }

  /**
   * Returns the mean chance of a word to be successor to any other word.
   *
   * @return Mean chance.
   */
  public double getMeanChance() {
    if (alteredSinceCached) {
      calculateStatisticalValues();
    }

    return cachedMeanChance;
  }

  /**
   * Returns the standard deviation of the chance of a word to be successor to any other word.<br>
   *
   * @return Chance standard deviation.
   */
  public double getChanceStdDeviation() {
    if (alteredSinceCached) {
      calculateStatisticalValues();
    }

    return cachedChanceStdDeviation;
  }

  /**
   * Removes all successors from the matrix whose chance of succeeding is lower than given value.
   *
   * @param lowerBoundChance Lower bound for transition chances.
   */
  public void truncateTo(final double lowerBoundChance) {

    final Iterator<Map.Entry<Integer, MutablePair<Long, Map<Integer, Long>>>> i;
    i = matrix.entrySet().iterator();

    while (i.hasNext()) {
      final int predecessorIndex = i.next().getKey();

      final long lowerBound;
      lowerBound = Math.round((double) matrix.get(predecessorIndex).getLeft() * lowerBoundChance);

      final Iterator<Map.Entry<Integer, Long>> ii;
      ii = matrix.get(predecessorIndex).getRight().entrySet().iterator();

      long newSum = 0;
      while (ii.hasNext()) {
        final Map.Entry<Integer, Long> successorEntry = ii.next();
        if (successorEntry.getValue() < lowerBound) {
          ii.remove();
        } else {
          newSum += successorEntry.getValue();
        }
      }

      if (newSum == 0) {
        i.remove();
      } else {
        matrix.get(predecessorIndex).setLeft(newSum);
      }
    }

    /*
     * HashMap<String, MutablePair<Long, Map<String, Long>>> newMatrix = new HashMap<>();
     *
     * matrix.entrySet().forEach(entry -> {
     *
     * Set<Map.Entry<String, Long>> entrySet = entry.getValue().getRight().entrySet();
     *
     * final double count = (double) entry.getValue().getLeft();
     *
     * Map<String, Long> newSuccessorMap = entrySet.stream() .filter(successorEntry -> (double)
     * successorEntry.getValue() / count >= lowerBoundChance)
     * .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
     *
     * long newCount = newSuccessorMap.entrySet().stream().map(Map.Entry::getValue).reduce(0L,
     * Long::sum);
     *
     * if (!newSuccessorMap.isEmpty()) { newMatrix.put(entry.getKey(), new MutablePair<>(newCount,
     * newSuccessorMap)); } });
     *
     * matrix = newMatrix;
     */
  }
}
