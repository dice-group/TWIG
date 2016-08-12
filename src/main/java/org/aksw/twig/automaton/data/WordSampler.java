package org.aksw.twig.automaton.data;

import org.aksw.twig.structs.AVLTree;

import java.util.*;

public class WordSampler {

    private Map<String, AVLTree<WordChanceMapping>> sampleMap = new HashMap<>();

    private Random r = new Random();

    public String sample(String predecessor) {
        AVLTree<WordChanceMapping> tree = sampleMap.get(predecessor);
        if (tree == null) {
            throw new IllegalArgumentException("Predecessor not present");
        }

        WordChanceMapping random = tree.findGreater(new WordChanceMapping("", r.nextDouble()));
        return random == null ? null : random.word;
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

    public WordSampler(final IWordMatrix matrix) {
        matrix.getPredecessors().forEach(predecessor -> {

            AVLTree<WordChanceMapping> tree = new AVLTree<>();

            WordChanceMapping[] wordChanceMappings = matrix.getMappings(predecessor).entrySet().stream()
                    .map(entry -> new WordChanceMapping(entry.getKey(), entry.getValue()))
                    .toArray(WordChanceMapping[]::new);
            Arrays.sort(wordChanceMappings, WordChanceMapping::compareTo);

            double cumulativeChance = 0d;
            for (WordChanceMapping mapping: wordChanceMappings) {
                mapping.chance += cumulativeChance;
                cumulativeChance = mapping.chance;
                tree.add(mapping);
            }

            sampleMap.put(predecessor, tree);
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
            double delta = chance - mapping.chance;
            return delta < 0 ? -1 : (int) Math.ceil(delta);
        }
    }
}
