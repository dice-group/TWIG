package org.aksw.twig.statistics;

import java.util.Random;

public interface DiscreteDistribution<T> {

    void reseedRandomGenerator(long seed);

    T sample();

    T sample(Random r);
}
