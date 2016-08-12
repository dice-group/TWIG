package org.aksw.twig.automaton.tweets;

import org.aksw.twig.executors.SelfSuspendingExecutor;
import org.aksw.twig.executors.SuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class WordMatrixHandler implements SuspendSupplier<WordMatrix> {

    private static final Logger LOGGER = LogManager.getLogger(WordMatrixHandler.class);

    private final WordMatrix mergedResult = new WordMatrix();

    private final NavigableSet<File> filesToParse = new TreeSet<>();

    public WordMatrixHandler(Collection<File> filesToParse) {
        this.filesToParse.addAll(filesToParse);
    }

    @Override
    public Callable<WordMatrix> next() {
        if (filesToParse.isEmpty()) {
            return null;
        }

        LOGGER.info("Supplying callable");
        synchronized (filesToParse) {
            File fileToParse = filesToParse.pollFirst();

            return () -> {
                WordMatrix matrix = new WordMatrix();
                matrix.addModel(Twitter7ModelWrapper.read(fileToParse).getModel());
                return matrix;
            };
        }
    }

    @Override
    public void addResult(WordMatrix result) {
        synchronized (mergedResult) {
            LOGGER.info("Merging result");
            mergedResult.merge(result);
        }
    }

    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        if (fileArgs.getLeft() == null) {
            LOGGER.error("No --out argument given");
            return;
        }

        final File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "word_matrix", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        WordMatrixHandler handler = new WordMatrixHandler(fileArgs.getRight());
        SelfSuspendingExecutor<WordMatrix> executor = new SelfSuspendingExecutor<>(handler);
        executor.addFinishedEventListeners(() -> {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                objectOutputStream.writeObject(handler.mergedResult);
                objectOutputStream.flush();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

    }
}
