package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.io.Serializable;

public interface MultivariateSpikingFunction extends Resettable, Serializable {

  int[][] apply(double t, int[][] inputs);

  int getInputDimension();

  int getOutputDimension();

  static MultivariateSpikingFunction build(SerializableFunction<int[][], int[][]> function, int inputDimension, int outputDimension) {
    return new MultivariateSpikingFunction() {
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
