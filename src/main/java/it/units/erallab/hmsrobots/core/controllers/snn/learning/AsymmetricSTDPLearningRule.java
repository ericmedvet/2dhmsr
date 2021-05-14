package it.units.erallab.hmsrobots.core.controllers.snn.learning;

public abstract class AsymmetricSTDPLearningRule extends STDPLearningRule{

  protected double tauPlus;
  protected double tauMinus;

  @Override
  public double[] getParams() {
    return new double[]{aPlus, aMinus, tauPlus,tauMinus};
  }

  @Override
  public void setParams(double[] params) {
    aPlus = params[0];
    aMinus = params[1];
    tauPlus = params[2];
    tauMinus = params[3];
  }
}
