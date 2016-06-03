package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * This class will handle parsed models. It will do so by collecting results of {@link com.google.common.util.concurrent.ListenableFuture} to which this collector has been added.
 * Results will be merged into one {@link TwitterModelWrapper} that is then printed into a file.
 * Language of printed results will be {@link #WRITE_LANG}.
 * @author Felix Linker
 */
class Twitter7ResultCollector implements FutureCallback<TwitterModelWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    public static final String WRITE_LANG = "TURTLE";

    public static final String FILE_TYPE = ".rdf";

    public static final int WRITE_ATTEMPTS = 10;

    private TwitterModelWrapper currentModel = new TwitterModelWrapper();

    private long modelMaxSize;

    private int id = 0;

    private File outputDirectory;

    private String fileName;

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
        this.outputDirectory = outputDirectory;
        this.fileName = fileName;
    }

    @Override
    public synchronized void onSuccess(TwitterModelWrapper result) {

        LOGGER.info("Collected result model.");

        this.currentModel.model.add(result.model);

        if (this.currentModel.model.size() >= this.modelMaxSize) {
            writeModel();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage());
    }

    /**
     * Writes the current collected model into a file.
     */
    synchronized void writeModel() {

        LOGGER.info("Writing result model {}.", this.currentModel);

        try (PrintWriter writer = new PrintWriter(createNewWriteFile())) {
            this.currentModel.model.write(writer, WRITE_LANG);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
            return;
        }

        this.currentModel = new TwitterModelWrapper();
    }

    /**
     * Creates a new file to write into.
     * If creation of a new file fails, it will try {@link #WRITE_ATTEMPTS} times to create a new one.
     * @return New file.
     * @throws IOException Thrown if no new file could be created.
     */
    private File createNewWriteFile() throws IOException {

        int attempt = 0;
        File writeFile = nextFile();


        while (writeFile.exists() && attempt < WRITE_ATTEMPTS) {
            writeFile = nextFile();
            attempt++;
        }

        if (attempt >= WRITE_ATTEMPTS) {
            throw new FileNotFoundException("Couldn't resolve file to print parsing results in.");
        }

        while (!writeFile.exists() && attempt < WRITE_ATTEMPTS) {
            try {
                writeFile.createNewFile();
            } catch (IOException e) {
                continue;
            } finally {
                attempt++;
            }
        }

        if (attempt >= WRITE_ATTEMPTS) {
            throw new FileNotFoundException("Couldn't resolve file to print parsing results in.");
        }

        return writeFile;
    }

    /**
     * Creates a new file without any checks.
     * @return New file.
     */
    private File nextFile() {
        return new File(outputDirectory, this.fileName.concat("_").concat(Integer.toString(id++)).concat(FILE_TYPE));
    }
}
