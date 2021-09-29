package it.units.erallab.hmsrobots.core.controllers.snn.learning;

public class AsymmetricAntiHebbianLearningRule extends AsymmetricSTDPLearningRule {

  @Override
  public double computeDeltaW(double deltaT) {
    if (deltaT > 0) {
      return -aPlus * Math.exp(-deltaT/tauPlus);
    } else if (deltaT < 0) {
      return aMinus * Math.exp(deltaT/tauMinus);
    }
    return 0;
  }

  @Override
  public String toString() {
    return "AntiHebbian" + super.toString();
  }
}
