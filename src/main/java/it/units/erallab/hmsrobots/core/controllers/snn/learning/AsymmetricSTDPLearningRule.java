package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.IntStream;

public abstract class AsymmetricSTDPLearningRule extends STDPLearningRule{

  private static final double[] MIN_PARAMS = {0.1, 0.1, 1d, 1d};
  private static final double[] MAX_PARAMS = {1d, 1d, 10d, 10d};

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

  public static double[] scaleParameters(double[] params) {
    IntStream.range(0, 4).forEach(i -> params[i] = scaleParameter(params[i], MIN_PARAMS[i], MAX_PARAMS[i])
    );
    return params;
  }

}
