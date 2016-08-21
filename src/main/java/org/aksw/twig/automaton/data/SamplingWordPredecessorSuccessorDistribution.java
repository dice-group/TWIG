package org.aksw.twig.automaton.data;

import org.aksw.twig.statistics.SamplingDiscreteDistribution;

public interface SamplingWordPredecessorSuccessorDistribution extends SamplingDiscreteDistribution<String> {

    SamplingDiscreteDistribution<String> getSuccessorDistribution(String predecessor);
}
