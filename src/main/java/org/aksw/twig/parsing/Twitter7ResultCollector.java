package org.aksw.twig.parsing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.aksw.twig.Const;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Collects {@link TWIGModelWrapper} and merges them into one. After the merged model has a size
 * over {@link #MODEL_MAX_SIZE} it will be printed into a gzip compressed file.
 */
class Twitter7ResultCollector implements FutureCallback<TWIGModelWrapper> {

  private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

  private final FileHandler fileHandler;

  private final TWIGModelWrapper currentModel = new TWIGModelWrapper();

  /**
   * Constructor setting class variables.
   *
   * @param fileName Basic file name for model printing.
   * @param outputDirectory Directory to print files into.
   */
  Twitter7ResultCollector(final String fileName, final File outputDirectory) {
    final String FILE_TYPE = ".ttl.gz";
    fileHandler = new FileHandler(outputDirectory, fileName, FILE_TYPE);
  }

  @Override
  public void onSuccess(final TWIGModelWrapper result) {
    synchronized (currentModel) {
      currentModel.getModel().add(result.getModel());

      if (currentModel.getModel().size() >= Const.MODEL_MAX_SIZE) {
        writeModel();
      }
    }
  }

  @Override
  public void onFailure(final Throwable t) {
    LOGGER.error(t.getMessage(), t);
  }

  /**
   * Writes the current collected model into a file.
   */
  public void writeModel() {
    synchronized (currentModel) {
      LOGGER.info("Writing result model {}.", currentModel);

      try (FileOutputStream fileOutputStream = new FileOutputStream(fileHandler.nextFile())) {
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(fileOutputStream))) {
          currentModel.write(writer);
          writer.flush();
        }
      } catch (final IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }
}
