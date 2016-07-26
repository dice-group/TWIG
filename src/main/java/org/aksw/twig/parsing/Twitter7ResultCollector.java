package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.GZIPOutputStream;

class Twitter7ResultCollector implements FutureCallback<Twitter7ModelWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    private static final int MODEL_MAX_SIZE = 1000000;

    private final FileHandler fileHandler;

    private final Twitter7ModelWrapper currentModel = new Twitter7ModelWrapper();

    private static final String FILE_TYPE = ".ttl.gz";

    Twitter7ResultCollector(String fileName, File outputDirectory) {
        this.fileHandler = new FileHandler(outputDirectory, fileName, FILE_TYPE);
    }

    @Override
    public void onSuccess(Twitter7ModelWrapper result) {
        synchronized (currentModel) {
            currentModel.getModel().add(result.getModel());

            if (currentModel.getModel().size() >= MODEL_MAX_SIZE) {
                writeModel();
            }
        }
    }

    @Override
    public void onFailure(Throwable t) {
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
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
