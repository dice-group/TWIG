package org.aksw.twig.automaton.data;

import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.aksw.twig.statistics.SamplingDiscreteTreeDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * Transfers a word matrix into multiple {@link SamplingDiscreteTreeDistribution} objects in order to be able to supply random words.
 */
public class WordSampler implements SamplingWordPredecessorSuccessorDistribution, Serializable {

    private static final Logger LOGGER = LogManager.getLogger(WordSampler.class);

    private static final double TRUNCATE_CHANCE = 0.001;

    private static final double DISTRIBUTION_CHANCE_DELTA = 0.0001;

    private static final int MAX_CHARS = 140;

    private static final long serialVersionUID = -531197136476439196L;

    private Map<String, SamplingDiscreteTreeDistribution<String>> distributionMap = new HashMap<>();

    private Random r = new Random();

    /**
     * Sets a seed to the internal random number generator by {@link Random#setSeed(long)}.
     * @param seed Seed.
     */
    @Override
    public void reseedRandomGenerator(long seed) {
        r.setSeed(seed);
    }

    /**
     * Samples a successor to given predecessor.
     * @param predecessor Predecessor.
     * @return Random successor.
     */
    @Override
    public SamplingDiscreteDistribution<String> getSuccessorDistribution(String predecessor) {
        SamplingDiscreteDistribution<String> returnValue = distributionMap.get(predecessor);

        if (returnValue == null) {
            return EMPTY_WORD_SAMPLER;
        }

        return returnValue;
    }

    /**
     * Samples a tweet.
     * @return Random tweet.
     */
    @Override
    public String sample() {
        return sample(r);
    }

    @Override
    public String sample(Random randomSource) {
        String currentPredecessor = getSuccessorDistribution("").sample(randomSource);
        int charactersCount = currentPredecessor.length();

        LinkedList<String> tweet = new LinkedList<>();
        tweet.add(currentPredecessor);

        while (charactersCount < MAX_CHARS) {
            String next = getSuccessorDistribution(currentPredecessor).sample(randomSource);
            if (next.equals("")) {
                break;
            }

            tweet.add(next);
            currentPredecessor = next;
            charactersCount += next.length() + 1; // + 1 for space character
        }

        if (charactersCount > MAX_CHARS) {
            tweet.removeLast();
        }

        return tweet.stream().reduce("", (a, b) -> a.concat(" ").concat(b)).trim();
    }

    /**
     * Creates a {@link WordSampler} of given {@link WordMatrix}.
     * @param matrix Matrix to create the sampler of.
     */
    public WordSampler(final WordMatrix matrix) {

        matrix.truncateTo(TRUNCATE_CHANCE);
        matrix.getPredecessors().forEach(predecessor -> {

            WordChanceMapping[] wordChanceMappings = matrix.getMappings(predecessor).entrySet().stream()
                    .map(entry -> new WordChanceMapping(entry.getKey(), entry.getValue()))
                    .toArray(WordChanceMapping[]::new);
            Arrays.sort(wordChanceMappings, WordChanceMapping::compareTo); // Sort successors alphabetically

            SamplingDiscreteTreeDistribution<String> distribution = new SamplingDiscreteTreeDistribution<>(DISTRIBUTION_CHANCE_DELTA);
            for (int i = 0; i < wordChanceMappings.length; i++) {
                WordChanceMapping mapping = wordChanceMappings[i];
                distribution.addDiscreteEvent(mapping.word, mapping.chance);
            }

            distributionMap.put(predecessor, distribution);
        });
    }

    /**
     * Wrapper class for a word and it's chance to be a successor.
     */
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

    private static final EmptyWordSampler EMPTY_WORD_SAMPLER = new EmptyWordSampler();

    /**
     * Is used as return value if there is no distribution for a predecessor.
     */
    private static class EmptyWordSampler implements SamplingDiscreteDistribution<String> {

        @Override
        public void reseedRandomGenerator(long seed) {}

        @Override
        public String sample() {
            return "";
        }

        @Override
        public String sample(Random r) {
            return "";
        }
    }
}
