package it.units.erallab.hmsrobots.core.controllers.snn.learning;

public abstract class SymmetricSTPDLearningRule extends STDPLearningRule {

  protected double sigmaPlus;
  protected double sigmaMinus;

  protected double computeG(double deltaT) {
    return 1 / (sigmaPlus * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(deltaT / sigmaPlus, 2d))
        - 1 / (sigmaMinus * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(deltaT / sigmaMinus, 2d));
  }

  @Override
  public double[] getParams() {
    return new double[]{aPlus, aMinus, sigmaPlus,sigmaMinus};
  }

  @Override
  public void setParams(double[] params) {
    aPlus = params[0];
    aMinus = params[1];
    sigmaPlus = params[2];
    sigmaMinus = params[3];
  }
}
