package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AsymmetricSTDPLearningRule extends STDPLearningRule{

  @JsonProperty
  protected double tauPlus;
  @JsonProperty
  protected double tauMinus;

  public AsymmetricSTDPLearningRule() {
  }

  @JsonCreator
  public AsymmetricSTDPLearningRule(
      @JsonProperty("aPlus") double aPlus,
      @JsonProperty("aMinus") double aMinus,
      @JsonProperty("tauPlus") double tauPlus,
      @JsonProperty("tauMinus") double tauMinus
  ) {
    super(aPlus, aMinus);
    this.tauPlus = tauPlus;
    this.tauMinus = tauMinus;
  }

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
