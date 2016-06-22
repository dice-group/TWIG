package org.aksw.twig.statistics;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

import java.util.Random;

/**
 * This class implements a discrete distribution using an exponential function for calculating probabilities.
 * The sample space consists of all natural numbers including 0. The probability mass function and cumulative probability function are defined as:
 * <ul>
 *     <li>{@code p(x) = (1 - e^(-lambda)) * e^(-lambda * x)}</li>
 *     <li>{@code P(X <= x) = sum from 0 to x: p(x) = 1 - e^(-lambda * (x + 1))}</li>
 * </ul>
 * Where {@code lambda} is the characteristic variable of the distribution. All methods supplied by this distribution run in {@code O(1)}.
 */
public class ExponentialLikeDistribution implements IntegerDistribution {

    private Random r = new Random();

    private double multiplier;

    private double lambda;

    private double expLambda;

    private double mean;

    public ExponentialLikeDistribution(double lambdaArg) {
        lambda = lambdaArg;
        expLambda = Math.exp(-lambda);
        multiplier = (1 - expLambda);
        mean = multiplier * expLambda / Math.pow(expLambda - 1, 2);
    }

    @Override
    public double probability(int x) {
        return multiplier * Math.exp(-lambda * x);
    }

    @Override
    public double cumulativeProbability(int x) {
        return 1 - Math.exp(-lambda * (x + 1));
    }

    @Override
    public double cumulativeProbability(int x0, int x1) throws NumberIsTooLargeException {
        if (x0 > x1) {
            throw new NumberIsTooLargeException(x0, x1, true);
        }

        if (x0 < getSupportLowerBound() - 1 || x1 + 1 > getSupportUpperBound()) {
            throw new IllegalArgumentException("Arguments out of bonds");
        }

        if (x0 == -1) {
            return cumulativeProbability(x1);
        }

        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    @Override
    public int inverseCumulativeProbability(double p) throws OutOfRangeException {
        if (p == 0d) {
            return 0;
        }

        return (int) Math.ceil(- Math.log(1 - p) / lambda);
    }

    @Override
    public double getNumericalMean() {
        return mean;
    }

    /**
     * This method is currently unsupported.
     * @return None.
     */
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
        int lowerK = 0;
        int upperK = Integer.MAX_VALUE - 1;
        int pivot = upperK / 2;

        while (true) {
            if (r.nextDouble() < cumulativeProbability(lowerK - 1, pivot) / cumulativeProbability(lowerK - 1, upperK)) {
                upperK = pivot;
            } else {
                lowerK = pivot + 1;
            }
            pivot = lowerK + (upperK - lowerK) / 2;

            if (lowerK >= upperK) {
                return upperK;
            }
        }
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
