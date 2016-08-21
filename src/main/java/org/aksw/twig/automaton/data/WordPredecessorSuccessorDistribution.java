package org.aksw.twig.automaton.data;

import org.aksw.twig.statistics.DiscreteDistribution;

public interface WordPredecessorSuccessorDistribution extends DiscreteDistribution<String> {

    DiscreteDistribution<String> getSuccessorDistribution(String predecessor);
}
