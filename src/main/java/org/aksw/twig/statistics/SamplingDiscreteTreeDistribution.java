package org.aksw.twig.statistics;

import org.aksw.twig.structs.AVLTree;

import java.io.Serializable;
import java.util.Random;

/**
 * Samples discrete events by iterating through an AVL tree. Therefor sampling is in {@code O(log n)} with {@code n} being the size of the sample space.
 * @param <T> Type of the events to sample.
 */
public class SamplingDiscreteTreeDistribution<T> implements SamplingDiscreteDistribution<T>, Serializable {

    private static final long serialVersionUID = -1616372537342154522L;

    private final Random random = new Random();

    private double aggregatedChance;

    /**
     * Whenever a discrete event is added via {@link #addDiscreteEvent(Object, double)} its chance will be added to the aggregated chance of all events.
     * The optimal aggregated chance is obviously one. But since chances might be calculated inaccurate, events might sum up to aa aggregated chance being truly greater than one.
     * You can set a clearance for the aggregated chance in constructor. The clearance will allow the aggregated chance to be less or equal to {@code 1 + aggregatedChanceDelta}.
     * For example if your aggregated chance is 1.001 and {@code aggregatedChanceDelta} is 0.002 everything is fine.<br><br>
     *
     * Note that there will be small inaccuracies for random events since the event that lead to the aggregated chance being truly greater than 1 will be slightly less probable than it should be.
     * It is not possible to have events that can't occur at all.
     * An exception will be thrown if you add an element and the aggregated chance is greater than one although the aggregated chance was in clearance.<br><br>
     *
     * It is possible to have an aggregated chance truly smaller than one. If this is the case, then the last added event will be slightly more probable than it should be.
     * To be exact: Let {@code e} be the last added event of the sample space with an aggregated chance {@code P} of all events truly smaller than one.
     * Let {@code p} be the chance of {@code e}.
     * When sampling events from the sample space {@code e's} chance will be {@code p' := p + (1 - P)}.
     */
    public final double aggregatedChanceDelta;

    /**
     * Creates a new instance setting class variables. {@link #aggregatedChanceDelta} will be set to {@code 0.0}.
     */
    public SamplingDiscreteTreeDistribution() {
        this(0.0);
    }

    /**
     * Creates a new instance setting class variables. {@code aggregatedChanceDelta} sets the clearance to the aggregated chance. More information to be found here: {@link #aggregatedChanceDelta}.
     * @param aggregatedChanceDelta Clearance for the maximum aggregated chance.
     */
    public SamplingDiscreteTreeDistribution(double aggregatedChanceDelta) {
        this.aggregatedChanceDelta = aggregatedChanceDelta;
    }

    private final AVLTree<ChanceMapping> sampleTree = new AVLTree<>();

    /**
     * Adds a discrete event with its chance to the sample space.
     * @param event Event to add to the sample space.
     * @param chance Chance of the event.
     * @throws IllegalArgumentException Thrown if the aggregated chance of all events is truly greater than {@code 1 + }{@link #aggregatedChanceDelta} of if {@code chance} is {@code 0}.
     * @throws IllegalStateException Thrown if the aggregated chance of all events was {@code >= 1}.
     */
    public void addDiscreteEvent(T event, double chance) throws IllegalArgumentException, IllegalStateException {
        if (aggregatedChance >= 1) {
            throw new IllegalStateException("Aggregated chance was >= 1 - would lead into impossible event");
        }

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
        if (sampleTree.isEmpty()) {
            return null;
        }

        ChanceMapping closest = sampleTree.findGreater(new ChanceMapping(r.nextDouble()));
        if (closest == null) {
            return sampleTree.getGreatest().val;
        }

        return closest.val;
    }

    /**
     * Elements to {@link #sampleTree}.
     */
    private class ChanceMapping implements Comparable<ChanceMapping> {

        T val;

        /**
         * Chance of this event.
         */
        double chance;

        /**
         * Aggregated chance of this event plus chance of all events that have been added prior to this.
         */
        double aggregatedChanceToThis;

        /**
         * Creates a new instance setting class variables.
         * @param val Value of the event.
         * @param chance Chance of the event.
         * @throws IllegalArgumentException Thrown if the aggregated chance of all events is truly greater than {@code 1 + }{@link #aggregatedChanceDelta} of if {@code chance} is {@code 0}.
         */
        ChanceMapping(T val, double chance) throws IllegalArgumentException {
            if (chance == 0) {
                throw new IllegalArgumentException("Chance must not be 0");
            }

            this.val = val;
            this.chance = chance;
            aggregatedChance += chance;

            if (aggregatedChance > 1 + aggregatedChanceDelta) {
                String exceptionMessage = "Aggregated chance was greater than (1 + delta) was ".concat(Double.toString(SamplingDiscreteTreeDistribution.this.aggregatedChance));
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
