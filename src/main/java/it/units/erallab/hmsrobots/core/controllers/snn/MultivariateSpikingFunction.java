package it.units.erallab.hmsrobots.core.controllers.snn;

import it.units.erallab.hmsrobots.core.controllers.Resettable;
import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.io.Serializable;
import java.util.SortedSet;

public interface MultivariateSpikingFunction extends Resettable, Serializable {

  SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs);

  int getInputDimension();

  int getOutputDimension();

  static MultivariateSpikingFunction build(SerializableFunction<SortedSet<Double>[], SortedSet<Double>[]> function, int inputDimension, int outputDimension) {
    return new MultivariateSpikingFunction() {
      @Override
      public SortedSet<Double>[] apply(double t, SortedSet<Double>[] inputs) {
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
