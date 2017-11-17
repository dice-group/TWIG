package org.aksw.twig.statistics;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.jena.ext.com.google.common.base.Function;
import org.junit.Assert;
import org.junit.Test;

public class SimpleExponentialRegressionTest {

  private double deviation = 0.001;

  private static final int DATA_SET_SIZE = 1000;

  @Test
  public void restoreFunctionTest() {
    double alpha = 2;
    double beta = 0.5;

    ExpFunction f = new ExpFunction(alpha, beta);

    SimpleExponentialRegression regression1 = new SimpleExponentialRegression();
    SimpleExponentialRegression regression2 = new SimpleExponentialRegression();
    double[][] data = new double[DATA_SET_SIZE][];
    for (int i = 0; i < DATA_SET_SIZE; i++) {
      double fValue = f.apply((double) i);
      regression1.addData(i, fValue);
      data[i] = new double[2];
      data[i][0] = i;
      data[i][1] = fValue;
    }
    regression2.addData(data);

    Assert.assertEquals(f.alpha, regression1.getAlpha(), this.deviation);
    Assert.assertEquals(f.beta, regression1.getBeta(), this.deviation);
    Assert.assertEquals(f.alpha, regression2.getAlpha(), this.deviation);
    Assert.assertEquals(f.beta, regression2.getBeta(), this.deviation);
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
