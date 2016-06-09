package org.aksw.twig.automaton.learning;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An implementation to {@link IWordMatrix}. Allows writing predecessor / successor pairs and allows reading from files.
 */
public class WordMatrix implements IWordMatrix, Serializable {

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
}
