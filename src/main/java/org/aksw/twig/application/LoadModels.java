package org.aksw.twig.application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.aksw.twig.Const;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.files.FileHandler;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Merges models and reduces the size if needed, the size depends on the parameters of the VM, thus
 * it is important to specify the java heap size when running this class.
 *
 */
public class LoadModels {

  private static final Logger LOG = LogManager.getLogger(LoadModels.class);

  private final double defaultTruncateTo = Const.DEFAULT_TRUNCATE_TO;
  private double truncateTo = defaultTruncateTo;

  private final int maxMemory = Long.valueOf(Const.DEFAULT_MAX_MEMORY).intValue();

  public WordMatrix wordMatrixMerged = null;

  public LoadModels() {

  }

  /**
   *
   * @param files
   */
  public LoadModels(final Set<File> files) {

    for (final File file : files) {
      WordMatrix wordMatrix = loadWordMatrix(file);
      LOG.info("wordMatrix size: {}", wordMatrix.matrix.keySet().size());
      wordMatrix = truncate(wordMatrix);
      if (wordMatrixMerged == null) {
        wordMatrixMerged = wordMatrix;
      } else {
        wordMatrixMerged.merge(wordMatrix);
        wordMatrixMerged = truncate(wordMatrixMerged);
      }
    } // end for
  }

  protected WordMatrix truncate(final WordMatrix wordMatrix) {
    while (wordMatrix.matrix.keySet().size() > maxMemory && truncateTo <= 1) {
      wordMatrix.matrix = truncateTo(truncateTo, wordMatrix.matrix);
      truncateTo = truncateTo + defaultTruncateTo;
      LOG.info("runcated to: {}", truncateTo);
    }
    truncateTo = defaultTruncateTo;
    return wordMatrix;
  }

  public Map<String, MutablePair<Long, Map<String, Long>>> truncateTo(final double lowerBoundChance,
      final Map<String, MutablePair<Long, Map<String, Long>>> matrix) {

    // elements for median calculation
    final Queue<Long> predecessors = new ConcurrentLinkedQueue<>();
    matrix.entrySet().parallelStream().forEach(entry -> {
      predecessors.add(entry.getValue().getLeft().longValue());
    });

    // median calculation
    final long median =
        median(lowerBoundChance, predecessors.stream().collect(Collectors.toList()));

    // removes predecessor elements below median
    matrix.entrySet().removeIf(s -> s.getValue().getLeft() < median);

    matrix.entrySet().stream().forEach(entry -> {
      final String predecessor = entry.getKey();
      final MutablePair<Long, Map<String, Long>> v = entry.getValue();

      final long successorsMedian = median(lowerBoundChance, v.getRight().values());

      final Map<String, Long> successors = v.getRight();
      successors.entrySet().removeIf(s -> s.getValue().doubleValue() < successorsMedian);

      if (!successors.isEmpty()) {
        final long newCount =
            successors.entrySet().stream().map(Map.Entry::getValue).reduce(0L, Long::sum);
        matrix.put(predecessor, new MutablePair<>(newCount, successors));
      }
    });
    return matrix;
  }

  /**
   *
   * @param border
   * @param unsorted
   * @return list element
   */
  protected <T> T median(final double border, final Collection<T> unsorted) {
    final List<T> sorted = unsorted.stream().sorted().collect(Collectors.toList());
    final int index = Long.valueOf(Math.round(sorted.size() * border)).intValue();
    // LOG.info("size: {}, index: {}", unsorted.size(), index);
    return sorted.get(index >= sorted.size() ? sorted.size() - 1 : index);
  }

  /**
   *
   * @param file
   * @return
   */
  public WordMatrix loadWordMatrix(final File file) {
    ObjectInputStream stream = null;
    WordMatrix wordMatrix = null;
    try {
      LOG.info("reads ".concat(file.getName()));
      stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
      wordMatrix = (WordMatrix) stream.readObject();
      stream.close();
    } catch (IOException | ClassNotFoundException e) {
      LOG.error(e.getMessage(), e);
      try {
        stream.close();
      } catch (final IOException ee) {
        LOG.error(ee.getMessage(), ee);
      }
    }
    return wordMatrix;
  }

  /**
   *
   * @param file
   * @return
   */
  public WordMatrix loadWordMatrix(final String file) {
    return loadWordMatrix(Paths.get(file).toFile());
  }

  /**
   *
   * @param args
   */
  public static void main(final String[] args) {

    final Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);

    final LoadModels m = new LoadModels(parsedArgs.getRight());

    final File outputDirectory = parsedArgs.getLeft();
    final String outputFile = Paths.get(outputDirectory.getAbsolutePath()
        .concat(outputDirectory.getAbsolutePath().endsWith("/") ? "" : "/")
        .concat("word_matrix_merged.obj")).toFile().getAbsolutePath();

    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(//
        new BufferedOutputStream(new FileOutputStream(outputFile)))) {
      objectOutputStream.writeObject(m.wordMatrixMerged);
      objectOutputStream.flush();
    } catch (final IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
