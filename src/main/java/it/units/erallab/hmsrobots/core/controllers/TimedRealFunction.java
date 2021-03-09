package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.util.SerializableFunction;

import java.io.Serializable;
import java.util.function.Function;

/**
 * @author eric on 2021/03/09 for 2dhmsr
 */
public interface TimedRealFunction extends Serializable {
  double[] apply(double t, double[] input);
  int getInputDimension();
  int getOutputDimension();
}
