package org.aksw.twig.statistics;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.jena.ext.com.google.common.base.Function;
import org.junit.Assert;
import org.junit.Test;

public class SimpleExponentialRegressionTest {

    private double deviation = 0.001;

    @Test
    public void restoreFunctionTest() {
        double alpha = 2;
        double beta = 0.5;

        ExpFunction f = new ExpFunction(alpha, beta);

        SimpleExponentialRegression regression = new SimpleExponentialRegression();
        for (int i = 0; i < 1000; i++) {
            regression.addData(i, f.apply((double) i));
        }

        Assert.assertEquals(f.alpha, regression.getAlpha(), this.deviation);
        Assert.assertEquals(f.beta, regression.getBeta(), this.deviation);
    }

    private class ExpFunction implements Function<Double, Double> {
        private double alpha, beta;

        ExpFunction(double alpha, double beta) {
            this.alpha = alpha;
            this.beta = beta;
        }

        @Override
        public Double apply(Double input) {
            if (input == null) {
                throw new NullArgumentException();
            }

            return alpha * Math.exp(beta * input);
        }
    }
}
