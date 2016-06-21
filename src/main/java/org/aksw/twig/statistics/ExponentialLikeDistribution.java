package org.aksw.twig.statistics;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

import java.util.Random;

public class ExponentialLikeDistribution implements IntegerDistribution {

    private Random r = new Random();

    private double multiplier;

    private double beta;

    private double expBeta;

    private double mean;

    private ExponentialLikeDistribution(double betaArg) {
        beta = betaArg;
        expBeta = Math.exp(-beta);
        multiplier = (1 - expBeta);
        mean = multiplier * expBeta / Math.pow(expBeta - 1, 2);
    }

    @Override
    public double probability(int x) {
        return multiplier * Math.exp(-beta * x);
    }

    @Override
    public double cumulativeProbability(int x) {
        return multiplier * (1 - Math.exp(-beta * (x + 1))) / (1 - expBeta);
    }

    @Override
    public double cumulativeProbability(int x0, int x1) throws NumberIsTooLargeException {
        if (x0 > x1) {
            throw new NumberIsTooLargeException(x0, x1, true);
        }

        if (x0 < getSupportLowerBound() - 1 || x1 > getSupportUpperBound()) {
            throw new IllegalArgumentException("Arguments out of bonds");
        }

        if (x0 == -1) {
            return cumulativeProbability(x1);
        }

        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    @Override
    public int inverseCumulativeProbability(double p) throws OutOfRangeException {
        throw new UnsupportedOperationException("Method not implemented yet"); // TODO
    }

    @Override
    public double getNumericalMean() {
        return mean;
    }

    @Override
    public double getNumericalVariance() {
        throw new UnsupportedOperationException("Method not implemented yet"); // TODO
    }

    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isSupportConnected() {
        return true;
    }

    @Override
    public void reseedRandomGenerator(long seed) {
        r.setSeed(seed);
    }

    @Override
    public int sample() {
        return 0; // TODO
    }

    @Override
    public int[] sample(int sampleSize) {
        int[] randoms = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            randoms[i] = sample();
        }

        return randoms;
    }
}
