package org.aksw.twig.statistics;

import org.junit.Assert;
import org.junit.Test;

public class ExponentialLikeDistributionTest {

    @Test
    public void sampleTest() {
        double lambda = Math.random() * -1d - 0.01;
        long seed = (long) (Long.MAX_VALUE * Math.random());
        ExponentialLikeDistribution d1 = new ExponentialLikeDistribution(lambda);
        d1.reseedRandomGenerator(seed);
        ExponentialLikeDistribution d2 = new ExponentialLikeDistribution(lambda);
        d2.reseedRandomGenerator(seed);

        Assert.assertEquals(d1.sample(), d2.sample());

        // Due to randomness code underneath is not testing perfectly and should only be run manually
        /*ExponentialLikeDistribution d = new ExponentialLikeDistribution(0.01);
        int steps = 1000;
        double sampleAverage = 0.0;
        for (int i = 0; i < steps; i++) {
            sampleAverage += (double) d.sample() / (double) steps;
        }
        Assert.assertEquals(d.getNumericalMean(), sampleAverage, 5d);*/
    }

    @Test
    public void probabilityTest() {
        ExponentialLikeDistribution d = new ExponentialLikeDistribution(Math.random() * -1d - 0.01);
        Assert.assertEquals(1.0, d.cumulativeProbability(-1, Integer.MAX_VALUE - 1), 0.0);

        int x0 = 0;
        int x1 = 10;
        double cumulativeP = 0.0;
        for (int k = x0 + 1; k <= x1; k++) {
            cumulativeP += d.probability(k);
        }
        Assert.assertEquals(d.cumulativeProbability(x0, x1), cumulativeP, 0.00001);
        Assert.assertEquals(d.cumulativeProbability(x0, x1), d.cumulativeProbability(x1) - d.cumulativeProbability(x0), 0.00001);
    }
}
