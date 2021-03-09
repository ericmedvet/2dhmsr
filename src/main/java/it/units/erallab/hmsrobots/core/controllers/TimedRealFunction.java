package it.units.erallab.hmsrobots.core.controllers;

import java.io.Serializable;

/**
 * @author eric on 2021/03/09 for 2dhmsr
 */
public interface TimedRealFunction extends Serializable {
  double[] apply(double t, double[] input);

  int getInputDimension();

  int getOutputDimension();
}
