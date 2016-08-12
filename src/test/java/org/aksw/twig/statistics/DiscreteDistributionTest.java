package org.aksw.twig.statistics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class DiscreteDistributionTest {

    @Test
    public void emptyTest() {
        DiscreteDistribution<Integer> distribution = new DiscreteDistribution<>();
        Assert.assertNull(distribution.sample());
    }

    @Test
    public void simpleTest() {
        DiscreteDistribution<Integer> distribution = new DiscreteDistribution<>();
        distribution.addDiscreteEvent(1, 0.25); // will be aggregated 0.25
        distribution.addDiscreteEvent(2, 0.25); // will be aggregated 0.5
        distribution.addDiscreteEvent(3, 0.25); // will be aggregated 0.75
        distribution.addDiscreteEvent(4, 0.25); // will be aggregated 1

        Random testRandom = new TestRandom();

        Assert.assertEquals(new Integer(1), distribution.sample(testRandom));
        Assert.assertEquals(new Integer(2), distribution.sample(testRandom));
        Assert.assertEquals(new Integer(3), distribution.sample(testRandom));
        Assert.assertEquals(new Integer(4), distribution.sample(testRandom));
    }

    private class TestRandom extends Random {

        private double val;

        @Override
        public double nextDouble() {
            double ret = val;
            val += 0.25;
            val %= 1;
            return ret;
        }
    }
}
