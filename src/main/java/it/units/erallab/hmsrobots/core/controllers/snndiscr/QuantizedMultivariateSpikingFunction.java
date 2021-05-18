package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.io.Serializable;

public interface QuantizedMultivariateSpikingFunction extends Resettable, Serializable {

  int[][] apply(double t, int[][] inputs);

  int getInputDimension();

  int getOutputDimension();

  static QuantizedMultivariateSpikingFunction build(SerializableFunction<int[][], int[][]> function, int inputDimension, int outputDimension) {
    return new QuantizedMultivariateSpikingFunction() {
      @Override
      public int[][] apply(double t, int[][] inputs) {
        return function.apply(inputs);
      }

      @Override
      public int getInputDimension() {
        return inputDimension;
      }

      @Override
      public int getOutputDimension() {
        return outputDimension;
      }

      @Override
      public void reset() {
      }
    };
  }

}
