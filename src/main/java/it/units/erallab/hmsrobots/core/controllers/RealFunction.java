package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.util.SerializableFunction;

/**
 * @author eric on 2021/03/09 for 2dhmsr
 */
public interface RealFunction extends TimedRealFunction {
  double[] apply(double[] input);

  static RealFunction build(
      SerializableFunction<double[], double[]> function,
      int inputDimension,
      int outputDimension
  ) {
    return new RealFunction() {
      @Override
      public double[] apply(double[] input) {
        return function.apply(input);
      }

      @Override
      public int getInputDimension() {
        return inputDimension;
      }

      @Override
      public int getOutputDimension() {
        return outputDimension;
      }
    };
  }

  @Override
  default double[] apply(double t, double[] input) {
    return apply(input);
  }

}
