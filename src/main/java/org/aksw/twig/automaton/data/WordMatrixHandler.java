package org.aksw.twig.automaton.data;

import org.aksw.twig.executors.FileReadingSuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Creates multiple {@link WordMatrix} objects by parsing files as {@link TWIGModelWrapper} and
 * adding them to the matrix. Parsed objects will then be merged into one result.
 */
public class WordMatrixHandler extends FileReadingSuspendSupplier<WordMatrix> {

  private static final Logger LOGGER = LogManager.getLogger(WordMatrixHandler.class);

  private final WordMatrix mergedResult = new WordMatrix();

  public WordMatrixHandler(Collection<File> filesToParse) {
    super(filesToParse);
  }

  @Override
  public Callable<WordMatrix> getFileProcessor(File file) {
    return () -> {
      LOGGER.info("Parsing file {}", file.getName());
      WordMatrix matrix = new WordMatrix();
      matrix.addModel(TWIGModelWrapper.read(file).getModel());
      return matrix;
    };
  }

  @Override
  public void addResult(WordMatrix result) {
    synchronized (mergedResult) {
      LOGGER.info("Merging result");
      mergedResult.merge(result);
    }
  }

  @Override
  public WordMatrix getMergedResult() {
    return mergedResult;
  }

  /**
   * Runs a {@link org.aksw.twig.executors.SelfSuspendingExecutor} with a {@link WordMatrixHandler}
   * as {@link org.aksw.twig.executors.SuspendSupplier}. Arguments must state an output file to
   * serialize the resulting {@link WordMatrix}. Arguments should state files to parse and must be
   * formatted according to {@link FileHandler#readArgs(String[])}.
   * 
   * @param args Arguments.
   */
  public static void main(String[] args) {
    Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
    WordMatrixHandler handler = new WordMatrixHandler(fileArgs.getRight());
    FileReadingSuspendSupplier.start("word_matrix.obj", fileArgs.getLeft(), handler);
  }
}
