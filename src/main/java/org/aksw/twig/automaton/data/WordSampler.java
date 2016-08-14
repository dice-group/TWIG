package org.aksw.twig.automaton.data;

import org.aksw.twig.statistics.DiscreteDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class WordSampler {

    private static final Logger LOGGER = LogManager.getLogger(WordSampler.class);

    private Map<String, DiscreteDistribution<String>> distributionMap = new HashMap<>();

    private Random r = new Random();

    public String sample(String predecessor) {
        DiscreteDistribution<String> distribution = distributionMap.get(predecessor);
        if (distribution == null) {
            throw new IllegalArgumentException("Predecessor not present");
        }

        return distribution.sample(r);
    }

    public String sampleTweet() {
        int charactersCount = 0;
        LinkedList<String> tweet = new LinkedList<>();
        String currentPredecessor = "";

        while (charactersCount < 140) {
            String next = sample(currentPredecessor);
            if (next.equals("")) {
                break;
            }

            tweet.add(next);
            currentPredecessor = next;
            charactersCount += next.length() + 1;
        }

        if (charactersCount > 140) {
            tweet.removeLast();
        }

        return tweet.stream().reduce("", String::concat);
    }

    public void reseed(long seed) {
        r.setSeed(seed);
    }

    public WordSampler(final WordMatrix matrix) {
        matrix.getPredecessors().forEach(predecessor -> {


            WordChanceMapping[] wordChanceMappings = matrix.getMappings(predecessor).entrySet().stream()
                    .map(entry -> new WordChanceMapping(entry.getKey(), entry.getValue()))
                    .toArray(WordChanceMapping[]::new);
            Arrays.sort(wordChanceMappings, WordChanceMapping::compareTo); // Sort successors alphabetically

            DiscreteDistribution<String> distribution = new DiscreteDistribution<>();
            for (WordChanceMapping mapping: wordChanceMappings) {
                distribution.addDiscreteEvent(mapping.word, mapping.chance);
            }

            distributionMap.put(predecessor, distribution);
        });
    }

    private static class WordChanceMapping implements Comparable<WordChanceMapping> {

        private String word;

        private double chance;

        WordChanceMapping(final String word, final double chance) {
            this.word = word;
            this.chance = chance;
        }

        @Override
        public int compareTo(WordChanceMapping mapping) {
            return word.compareTo(mapping.word);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Insufficient arguments");
        }

        int messages = Integer.parseInt(args[1]);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(new File(args[0])))) {
            WordMatrix matrix = (WordMatrix) objectInputStream.readObject();
            WordSampler sampler = new WordSampler(matrix);
            for (int i = 0; i < messages; i++) {
                LOGGER.info("Message: {}", sampler.sampleTweet());
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
