package org.aksw.twig.statistics;

import org.apache.commons.math3.exception.NumberIsTooLargeException;

import java.io.Serializable;
import java.util.Random;

/**
 * This class implements a discrete distribution using an exponential function for calculating probabilities.
 * The sample space consists of all natural numbers including 0. The probability mass function and cumulative probability function are defined as:<br>
 * Be lambda a negative real number.
 * <ul>
 *     <li>{@code p(x) = (1 - e^(lambda)) * e^(lambda * x)}</li>
 *     <li>{@code P(X <= x) = sum from 0 to x: p(x) = 1 - e^(lambda * (x + 1))}</li>
 * </ul>
 * Where {@code lambda} is the characteristic variable of the distribution. All methods supplied by this distribution run in {@code O(1)}.
 */
public class ExponentialLikeDistribution implements SamplingDiscreteDistribution<Integer>, Serializable {

    private Random r = new Random();

    private double multiplier;

    private double lambda;

    /**
     * Creates a new exponential like distribution with given lambda.
     * @param lambdaArg Characteristic variable to the distribution. Must be negative.
     */
    public ExponentialLikeDistribution(double lambdaArg) {
        if (lambdaArg >= 0) {
            throw new IllegalArgumentException("lambdaArg must be < 0");
        }

        lambda = lambdaArg;
        double expLambda = Math.exp(lambda);
        multiplier = (1d - expLambda);
    }

    /**
     * Returns the probability {@code P(X = x)}.
     * @param x Event to get the probability for.
     * @return Probability {@code P(X = x)}.
     */
    double probability(int x) {
        return multiplier * Math.exp(lambda * x);
    }

    /**
     * Returns the probability of {@code P(X <= x)}.
     * @param x Upper bound of events to get the accumulated probability for.
     * @return Probability {@code P(X <= x)}.
     */
    double cumulativeProbability(int x) {
        return 1 - Math.exp(lambda * (x + 1));
    }

    /**
     * Returns the probability of {@code P(x0 <= X <= x1)}.
     * @param x0 Lower bound of events to get the accumulated probability for.
     * @param x1 Upper bound of events to get the accumulated probability for.
     * @return Probability of {@code P(x0 <= X <= x1)}.
     * @throws NumberIsTooLargeException Thrown iff {@code x0 > x1}.
     */
    double cumulativeProbability(int x0, int x1) throws NumberIsTooLargeException {
        if (x0 > x1) {
            throw new NumberIsTooLargeException(x0, x1, true);
        }

        if (x0 == -1) {
            return cumulativeProbability(x1);
        }

        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    @Override
    public void reseedRandomGenerator(long seed) {
        r.setSeed(seed);
    }

    @Override
    public Integer sample() {
        return sample(r);
    }

    @Override
    public Integer sample(Random randomSource) {
        int lowerK = 0;
        int upperK = Integer.MAX_VALUE - 1;
        int pivot = upperK / 2;

        while (true) {
            if (randomSource.nextDouble() < cumulativeProbability(lowerK - 1, pivot) / cumulativeProbability(lowerK - 1, upperK)) {
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

    /**
     * Creates an exponential like distribution by taking the given exponential regression as frequency distribution.
     * @param regression Frequency distribution.
     * @return Exponential like distribution.
     */
    public static ExponentialLikeDistribution of(SimpleExponentialRegression regression) {
        if (regression.getBeta() >= 0) {
            throw new IllegalArgumentException("The regression's beta must be < 0.");
        }

        return new ExponentialLikeDistribution(regression.getBeta());
    }
}
