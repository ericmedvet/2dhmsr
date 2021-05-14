package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import it.units.erallab.hmsrobots.util.Parametrized;

public abstract class STDPLearningRule implements Parametrized {

  protected double aPlus;
  protected double aMinus;

  public abstract double computeDeltaW(double deltaT);

}
