package org.aksw.twig.executors;

import org.aksw.twig.files.FileHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Navigates over a set of files polling {@link Callable} objects by {@link FileReadingSuspendSupplier#getFileProcessor(File)}.
 * Results of executed callables will be handed to {@link FileReadingSuspendSupplier#addResult(Object)} and should be merged into one result by implementing classes.
 * The merged result can be queried via {@link FileReadingSuspendSupplier#getMergedResult()}.<br>
 * Must be executed by a {@link SelfSuspendingExecutor}.
 * @param <T> Type of parsing results.
 */
public abstract class FileReadingSuspendSupplier<T extends Serializable> implements SuspendSupplier<T> {

    private static final Logger LOGGER = LogManager.getLogger(FileReadingSuspendSupplier.class);

    private final NavigableSet<File> filesToParse = new TreeSet<>();

    /**
     * Creates a new instance setting class variables.
     * @param filesToParse
     */
    public FileReadingSuspendSupplier(Collection<File> filesToParse) {
        this.filesToParse.addAll(filesToParse);
    }

    @Override
    public Callable<T> next() {
        if (filesToParse.isEmpty()) {
            return null;
        }

        synchronized (filesToParse) {
            LOGGER.info("Supplying callable");
            return getFileProcessor(filesToParse.pollFirst());
        }
    }

    /**
     * Returns a {@link Callable} parsing given file.
     * @param file File to parse.
     * @return Parsing callable.
     */
    protected abstract Callable<T> getFileProcessor(File file);

    /**
     * Returns the merged result of the executed callables.
     * @return Merged result.
     */
    protected abstract T getMergedResult();

    /**
     * Creates a {@link SelfSuspendingExecutor} and executes it.
     * @param fileName File name to serialize merged result.
     * @param outputDirectory Output directory for merged result.
     * @param suspendSupplier Suspend supplier to be executed.
     * @param <T> Type of parsing results.
     * @throws IllegalArgumentException Thrown if {@code outputDirectory} is {@code null}.
     */
    protected static <T extends Serializable> void start(String fileName, File outputDirectory, FileReadingSuspendSupplier<T> suspendSupplier) throws IllegalArgumentException {

        if (outputDirectory == null) {
            throw new IllegalArgumentException();
        }

        String[] split = fileName.split("\\.");
        File outputFile;
        try {
            outputFile = new FileHandler(outputDirectory, split[0], split.length > 1 ? ".".concat(split[1]) : ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        SelfSuspendingExecutor<T> executor = new SelfSuspendingExecutor<>(suspendSupplier);
        executor.addFinishedEventListeners(() -> {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                objectOutputStream.writeObject(suspendSupplier.getMergedResult());
                objectOutputStream.flush();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
        LOGGER.info("Starting executor");
        executor.start();
    }
}
