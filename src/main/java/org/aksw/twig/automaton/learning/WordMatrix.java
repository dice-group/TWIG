package org.aksw.twig.automaton.learning;

import org.aksw.twig.files.FileHandler;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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

    private static final Logger LOGGER = LogManager.getLogger(WordMatrix.class);

    private Map<String, MutablePair<Long, Map<String, Long>>> matrix = new HashMap<>();

    /**
     * Adds a predecessor / successor pair to the matrix.
     * @param predecessor Predecessor to add.
     * @param successor Successor to add.
     */
    public void put(String predecessor, String successor) {
        MutablePair<Long, Map<String, Long>> mapping = matrix.computeIfAbsent(predecessor, key -> new MutablePair<>(0L, new HashMap<>()));

        mapping.setLeft(mapping.getLeft() + 1);
        Map<String, Long> columns = mapping.getRight();
        Long val = columns.get(successor);
        columns.put(successor, val == null ? 1 : ++val);
    }

    /**
     * Adds all pairs. Value of {@link Pair#getLeft()} will be taken as predecessor, value of {@link Pair#getRight()} will be taken as successor.
     * @param iterable Pairs to add.
     */
    public void putAll(Iterable<Pair<String, String>> iterable) {
        iterable.forEach(pair -> this.put(pair.getLeft(), pair.getRight()));
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
                        entry -> entry.getKey(),
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

    private static final Query TWITTER_CONTENT_QUERY = QueryFactory.create(
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX twig: <http://aksw.org/twig#> " +
            "SELECT ?c WHERE { ?x rdf:type twig:Tweet . ?x twig:tweetContent ?c . }"
    );

    /**
     * Creates a word matrix by reading given TWIG rdf data and writes it into a file.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments.
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
        File outputDirectory = parsedArgs.getLeft();
        Set<File> filesToRead = parsedArgs.getRight();

        WordMatrix matrix = new WordMatrix();
        filesToRead.stream()
                .forEach(file -> {
                    Model fileModel = RDFDataMgr.loadModel(file.getPath(), Lang.TURTLE);
                    try (QueryExecution execution = QueryExecutionFactory.create(TWITTER_CONTENT_QUERY, fileModel)) {
                        ResultSet resultSet = execution.execSelect();
                        while (resultSet.hasNext()) {
                            matrix.putAll(new TweetSplitter(resultSet.next().get("c").toString()));
                        }
                    }
                });

        File writeFile;
        try {
            writeFile = new FileHandler(outputDirectory, "wordMatrix", ".matrix").nextFile();
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
            return;
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(writeFile))) {
            outputStream.writeObject(matrix);
        } catch (IOException e) {
            LOGGER.error("Error: exception - {}", e.getMessage());
            return;
        }
    }
}
