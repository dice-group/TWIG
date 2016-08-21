package org.aksw.twig.statistics;

import org.aksw.twig.structs.AVLTree;

import java.util.Random;

public class DiscreteTreeDistribution<T> implements DiscreteDistribution<T> {

    private final Random random = new Random();

    private double aggregatedChance;

    private double aggregatedChanceDelta;

    public DiscreteTreeDistribution() {}

    public DiscreteTreeDistribution(double aggregatedChanceDelta) {
        this.aggregatedChanceDelta = aggregatedChanceDelta;
    }

    private final AVLTree<ChanceMapping> sampleTree = new AVLTree<>();

    public void addDiscreteEvent(T event, double chance) throws IllegalArgumentException {
        sampleTree.add(new ChanceMapping(event, chance));
    }

    @Override
    public void reseedRandomGenerator(long seed) {
        random.setSeed(seed);
    }

    @Override
    public T sample() {
        return sample(random);
    }

    @Override
    public T sample(Random r) {
        ChanceMapping closest = sampleTree.findGreater(new ChanceMapping(r.nextDouble()));
        return closest == null ? null : closest.val;
    }

    private class ChanceMapping implements Comparable<ChanceMapping> {

        T val;

        double chance;

        double aggregatedChanceToThis;

        ChanceMapping(T val, double chance) throws IllegalArgumentException {
            if (chance == 0) {
                throw new IllegalArgumentException("Chance must not be 0");
            }

            this.val = val;
            this.chance = chance;
            aggregatedChance += chance;

            if (aggregatedChance > 1 + aggregatedChanceDelta) {
                String exceptionMessage = "Aggregated chance was greater than (1 + delta) was ".concat(Double.toString(DiscreteTreeDistribution.this.aggregatedChance));
                aggregatedChance -= chance;
                throw new IllegalArgumentException(exceptionMessage);
            }

            aggregatedChanceToThis = aggregatedChance;
        }

        ChanceMapping(double chance) {
            aggregatedChanceToThis = chance;
        }

        @Override
        public int compareTo(ChanceMapping mapping) {
            return Double.compare(aggregatedChanceToThis, mapping.aggregatedChanceToThis);
        }
    }
}
