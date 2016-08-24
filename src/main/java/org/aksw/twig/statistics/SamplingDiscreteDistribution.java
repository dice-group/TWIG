package org.aksw.twig.statistics;

import java.util.Random;

/**
 * Samples events from a discrete sample space. A discrete sample space is a set of events that is countable, i. e. its cardinality is lower or equal to the cardinality of the natural numbers.
 * @param <T> Type of events to sample.
 */
public interface SamplingDiscreteDistribution<T> {

    /**
     * Reseeds the random generator that provides random numbers via {@link Random#setSeed(long)}.
     * After invoking this method sampled events will only be pseudo-random.
     * @param seed Seed to set.
     */
    void reseedRandomGenerator(long seed);

    /**
     * Samples an event from sample space. Sampled events will be random unless {@link #reseedRandomGenerator(long)} has been invoked.
     * @return Sampled event or {@code null} if the sample space is empty.
     */
    T sample();

    /**
     * Samples an event from samples space whilst using {@code r} as source of randomness.
     * @param r Source of randomness.
     * @return Sampled event or {@code null} if the sample space is empty.
     */
    T sample(Random r);
}
