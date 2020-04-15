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
import java.util.Set;

import org.aksw.twig.Const;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.files.FileHandler;
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

  private final long maxMemory = Const.DEFAULT_MAX_MEMORY < 1 ? //
      RuntimeUtil.getMaxMemory() : Const.DEFAULT_MAX_MEMORY;

  private final long usedMemory = Double.valueOf(maxMemory * Const.MEMORY_USE).longValue();

  private final double defaultTruncateTo = Const.DEFAULT_TRUNCATE_TO;
  private double truncateTo = defaultTruncateTo;

  public WordMatrix wordMatrixMerged = null;

  public LoadModels(final Set<File> files) {

    LOG.info(//
        "max memory: {}, half: {}, defaultTruncateTo: {}, truncateTo: {} ", //
        maxMemory, usedMemory, defaultTruncateTo, truncateTo//
    );

    for (final File file : files) {
      final WordMatrix wordMatrix = loadWordMatrix(file);
      truncate(wordMatrix);
      if (wordMatrixMerged == null) {
        wordMatrixMerged = wordMatrix;
      } else {
        wordMatrixMerged.merge(wordMatrix);
        truncate(wordMatrixMerged);
      }
    } // end for

  }

  protected void truncate(final WordMatrix wordMatrix) {
    while (RuntimeUtil.getUsedMemory() > usedMemory) {
      wordMatrix.truncateTo(truncateTo);
      truncateTo = truncateTo + truncateTo;
      LOG.info("truncateTo: {}, used memory/max: {}/{}", //
          truncateTo, RuntimeUtil.getUsedMemoryInMiB(), RuntimeUtil.bytesToMiB(usedMemory));
    }
    truncateTo = defaultTruncateTo;
  }

  protected WordMatrix loadWordMatrix(final File file) {
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

  protected WordMatrix loadWordMatrix(final String file) {
    return loadWordMatrix(Paths.get(file).toFile());
  }

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
