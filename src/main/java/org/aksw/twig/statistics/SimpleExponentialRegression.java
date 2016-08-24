package org.aksw.twig.statistics;

import org.apache.commons.math3.stat.regression.ModelSpecificationException;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Performs two dimensional exponential regression on a data set. Exponential regression will be done via linear regression on the same dat set with logarithmized y-axis.
 * The result of the linear regression is a function {@code f(x) = a + b * x}. This result will be translated into {@code f(x) = alpha + e^(beta * x)} with {@code alpha = e^a} and {@code beta = b}.
 */
public class SimpleExponentialRegression {

    private final SimpleRegression linearRegression = new SimpleRegression();

    /**
     * Adds data to the data set.
     * @param x x-axis value.
     * @param y y-axis value.
     */
    public void addData(double x, double y) {
        linearRegression.addData(x, Math.log(y));
    }

    /**
     * Adds multiple entries to the data set. Added values will be: {@code addData(data[0][0], data[0][1])}, {@code addData(data[1][0], data[1][1])} and so on.
     * @param data Data to add.
     * @throws ModelSpecificationException Exception on malformed data.
     */
    public void addData(double[][] data) throws ModelSpecificationException {
        double[][] logarithmized = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            logarithmized[i] = new double[data[i].length];
            logarithmized[i][0] = data[i][0];
            for (int j = 1; j < data[i].length; j++) {
                logarithmized[i][j] = Math.log(data[i][j]);
            }
        }

        this.linearRegression.addData(logarithmized);
    }

    /**
     * Returns the alpha value for the result of the exponential regression: {@code f(x) = alpha * e^(beta * x)}.
     * @return Alpha value of the exponential regression result.
     */
    public double getAlpha() {
        return Math.exp(this.linearRegression.getIntercept());
    }

    /**
     * Returns the beta value for the result of the exponential resgression: {@code f(x) = alpha * e^(beta * x)}.
     * @return Beta value of te exponential regression result.
     */
    public double getBeta() {
        return this.linearRegression.getSlope();
    }

    // TODO: add error calculation
}
