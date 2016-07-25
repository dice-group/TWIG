package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * This class will handle parsed models. It will do so by collecting results of {@link com.google.common.util.concurrent.ListenableFuture} to which this collector has been added.
 * Results will be merged into one {@link TwitterModelWrapper} that is then printed into a GZip compressed file.
 * Language of printed results will be {@link #WRITE_LANG}.
 * @author Felix Linker
 */
class Twitter7ResultCollector implements FutureCallback<TwitterModelWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    public static final String WRITE_LANG = "Turtle";

    public static final String FILE_TYPE = ".ttl.gz";

    private TwitterModelWrapper currentModel = new TwitterModelWrapper();

    private long modelMaxSize;

    private FileHandler fileHandler;

    /**
     * Creates a new instance and sets class variables.
     * @param modelMaxSize Max size of a {@link TwitterModelWrapper#model} to contain. If this size is exceeded by a {@link TwitterModelWrapper#model} it will be written into a file.
     * @param outputDirectory Directory to write results into.
     * @param fileName Base file name of result files. This filename will be accompanied by an ID and a file type ({@link #FILE_TYPE}).
     */
    Twitter7ResultCollector(long modelMaxSize, File outputDirectory, String fileName) {
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException();
        }

        this.modelMaxSize = modelMaxSize;
        this.fileHandler = new FileHandler(outputDirectory, fileName, FILE_TYPE);
    }

    @Override
    public synchronized void onSuccess(TwitterModelWrapper result) {

        this.currentModel.model.add(result.model);

        if (this.currentModel.model.size() >= this.modelMaxSize) {
            writeModel();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        if (t.getMessage() == null) {
            StackTraceElement[] stackTraceElements = t.getStackTrace();
            String stackTrace = "";
            for (StackTraceElement stackTraceElement: stackTraceElements) {
                stackTrace = stackTrace.concat(stackTraceElement.toString());
                stackTrace = stackTrace.concat("\n");
            }
            LOGGER.error(stackTrace);
        } else {
            LOGGER.error(t.getMessage(), t.getCause());
        }
    }

    /**
     * Writes the current collected model into a file.
     */
    synchronized void writeModel() {

        LOGGER.info("Writing result model {}.", this.currentModel);

        try (FileOutputStream fileOutputStream = new FileOutputStream(this.fileHandler.nextFile())) {
            try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(fileOutputStream))) {
                this.currentModel.model.write(writer, WRITE_LANG);
                writer.flush();
            }
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
            return;
        }

        this.currentModel = new TwitterModelWrapper();
    }
}
