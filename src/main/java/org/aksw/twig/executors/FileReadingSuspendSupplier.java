package org.aksw.twig.executors;

import org.aksw.twig.files.FileHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public abstract class FileReadingSuspendSupplier<T extends Serializable> implements SuspendSupplier<T> {

    private static final Logger LOGGER = LogManager.getLogger(FileReadingSuspendSupplier.class);

    private final NavigableSet<File> filesToParse = new TreeSet<>();

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

    protected abstract Callable<T> getFileProcessor(File file);

    protected abstract T getMergedResult();

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
