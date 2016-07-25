package org.aksw.twig.automaton;

import org.aksw.twig.automaton.tweets.IWordMatrix;
import org.aksw.twig.automaton.tweets.WordSampler;
import org.apache.commons.math3.distribution.IntegerDistribution;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Automaton {

    public Automaton(IWordMatrix wordMatrix, IntegerDistribution userToMessageCountDistribution) {
        wordSampler = new WordSampler(wordMatrix);
        this.userToMessageCountDistribution = userToMessageCountDistribution;
    }

    private WordSampler wordSampler;

    private IntegerDistribution userToMessageCountDistribution;

    private Duration atomicTimeUnit = Duration.ofDays(1);

    public void simulate(int userCount, Duration simulationTime) {

        Random r = new Random(simulationTime.hashCode() * userCount);

        double steps = (double) simulationTime.toHours() / (double) atomicTimeUnit.toHours();

        List<User> userList = new LinkedList<>();
        for (int i = 0; i < userCount; i++) {
            User user = new User(userToMessageCountDistribution.sample());
            userList.add(user);
        }


    }
}
