package org.aksw.twig.statistics;

import org.apache.commons.math3.stat.regression.ModelSpecificationException;
import org.apache.commons.math3.stat.regression.SimpleRegression;

// TODO: implement UpdatingMultipleLinearRegression
public class SimpleExponentialRegression {

    private final SimpleRegression linearRegression = new SimpleRegression();

    public void addData(double x, double y) {
        this.linearRegression.addData(x, Math.log(y));
    }

    public void addData(double[][] data) throws ModelSpecificationException {
        double[][] logarithmized = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            logarithmized[i] = new double[data[i].length];
            for (int j = 1; j < data[i].length; j++) {
                logarithmized[i][j] = Math.log(data[i][j]);
            }
        }

        this.linearRegression.addData(logarithmized);
    }

    public double getAlpha() {
        return Math.exp(this.linearRegression.getIntercept());
    }

    public double getBeta() {
        return this.linearRegression.getSlope();
    }

    // TODO: add error calculation
}
