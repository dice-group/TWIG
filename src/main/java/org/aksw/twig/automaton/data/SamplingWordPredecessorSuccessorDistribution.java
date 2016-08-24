package org.aksw.twig.automaton.data;

import org.aksw.twig.statistics.SamplingDiscreteDistribution;

/**
 * Samples whole sentences or returns a word predecessor-successor distribution by predecessor.
 */
public interface SamplingWordPredecessorSuccessorDistribution extends SamplingDiscreteDistribution<String> {

    /**
     * Returns the successor distribution for given predecessor.
     * @param predecessor Predecessor to get the distribution for.
     * @return Successor distribution.
     */
    SamplingDiscreteDistribution<String> getSuccessorDistribution(String predecessor);
}
