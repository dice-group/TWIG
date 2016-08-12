package org.aksw.twig.automaton.data;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation to {@link IWordMatrix}. Allows writing predecessor / successor pairs and allows reading from files.
 */
public class WordMatrix implements IWordMatrix, Serializable {

    private static final long serialVersionUID = 2104488071228760278L;

    private static transient final Logger LOGGER = LogManager.getLogger(WordMatrix.class);

    private Map<String, MutablePair<Long, Map<String, Long>>> matrix = new HashMap<>();

    /**
     * Adds a predecessor / successor pair to the matrix.
     * @param predecessor Predecessor to add.
     * @param successor Successor to add.
     */
    public void put(String predecessor, String successor, long count) {
        MutablePair<Long, Map<String, Long>> mapping = matrix.computeIfAbsent(predecessor, key -> new MutablePair<>(0L, new HashMap<>()));

        mapping.setLeft(mapping.getLeft() + count);
        Map<String, Long> columns = mapping.getRight();
        Long val = columns.get(successor);
        columns.put(successor, val == null ? count : val + count);
    }

    /**
     * Adds all pairs. Value of {@link Pair#getLeft()} will be taken as predecessor, value of {@link Pair#getRight()} will be taken as successor.
     * @param iterable Pairs to add.
     */
    public void putAll(Iterable<Pair<String, String>> iterable) {
        iterable.forEach(pair -> this.put(pair.getLeft(), pair.getRight(), 1));
    }

    /**
     * Merges contents of given {@link WordMatrix} into this.
     * @param wordMatrix Matrix to merge.
     */
    public void merge(WordMatrix wordMatrix) {
        wordMatrix.matrix.entrySet().forEach(
                entry -> {
                    String predecessor = entry.getKey();
                    entry.getValue().getRight().entrySet().forEach(
                            mappedEntry -> {
                                put(predecessor, mappedEntry.getKey(), mappedEntry.getValue());
                            }
                    );
                }
        );
    }

    @Override
    public double getChance(String predecessor, String successor) throws IllegalArgumentException {
        MutablePair<Long, Map<String, Long>> mapping = matrix.get(predecessor);
        if (mapping == null) {
            throw new IllegalArgumentException("No mapping found.");
        }

        return (double) mapping.getRight().get(successor) / (double) mapping.getLeft();
    }

    @Override
    public Map<String, Double> getMappings(String predecessor) throws IllegalArgumentException {
        MutablePair<Long, Map<String, Long>> mapping = matrix.get(predecessor);
        if (mapping == null) {
            throw new IllegalArgumentException("No mapping found.");
        }

        long size = mapping.getLeft();
        return mapping.getRight().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / (double) size
                ));
    }

    @Override
    public Set<String> getPredecessors() {
        return matrix.keySet();
    }

    @Override
    public void saveToFile(File file) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
        outputStream.writeObject(this);
        outputStream.close();
    }

    /**
     * Adds all relevant statements from given model into the matrix.
     * @param model Model to add statements from.
     */
    public void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(Twitter7ModelWrapper.TWEET_CONTENT_PROPERTY_NAME)) {
                String tweet = statement.getObject().asLiteral().getString();
                putAll(new TweetSplitter(tweet));
            }
        });
    }

    /**
     * Reads a IWordMatrix of a file.
     * @param fileToParse File to parse.
     * @return Word matrix.
     * @throws IOException Thrown during file reading.
     * @throws ClassNotFoundException Thrown during de-serialization.
     */
    public static IWordMatrix of(File fileToParse) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileToParse));
        IWordMatrix matrix = (IWordMatrix) inputStream.readObject();
        inputStream.close();
        return matrix;
    }

    /**
     * Creates a word matrix by reading given TWIG rdf data and writes it into a file.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])} but {@code --out=} argument is mandatory.
     * @param args Arguments.
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
        File outputDirectory = parsedArgs.getLeft();
        Set<File> filesToRead = parsedArgs.getRight();

        if (outputDirectory == null) {
            throw new IllegalArgumentException("No --out argument given.");
        }

        WordMatrix matrix = new WordMatrix();
        filesToRead.forEach(file -> {
            try {
                matrix.addModel(Twitter7ModelWrapper.read(file).getModel());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        File writeFile;
        try {
            writeFile = new FileHandler(outputDirectory, "wordMatrix", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
            return;
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(writeFile))) {
            outputStream.writeObject(matrix);
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
        }
    }
}
