package org.aksw.twig.statistics;

import org.aksw.twig.structs.AVLTree;

import java.util.Random;

public class DiscreteDistribution<T> {

    private final Random random = new Random();

    private double aggregatedChance;

    private final AVLTree<ChanceMapping> sampleTree = new AVLTree<>();

    public void addDiscreteEvent(T event, double chance) throws IllegalArgumentException {
        sampleTree.add(new ChanceMapping(event, chance));
    }

    public void reseed(long seed) {
        random.setSeed(seed);
    }

    public T sample() {
        return sample(random);
    }

    public T sample(Random r) {
        ChanceMapping closest = sampleTree.findClosest(new ChanceMapping(r.nextDouble()));
        return closest == null ? null : closest.val;
    }

    private class ChanceMapping implements Comparable<ChanceMapping> {

        T val;

        double chance;

        double aggregatedChance;

        ChanceMapping(T val, double chance) throws IllegalArgumentException {
            this.val = val;
            this.chance = chance;
            DiscreteDistribution.this.aggregatedChance += chance;
            if (DiscreteDistribution.this.aggregatedChance > 1) {
                throw new IllegalArgumentException("Aggregated chance was greater than 1");
            }
            this.aggregatedChance = DiscreteDistribution.this.aggregatedChance;
        }

        ChanceMapping(double chance) {
            aggregatedChance = chance;
        }

        @Override
        public int compareTo(ChanceMapping mapping) {
            double delta = aggregatedChance - mapping.aggregatedChance;
            return delta < 0 ? -1 : (int) delta;
        }
    }
}
