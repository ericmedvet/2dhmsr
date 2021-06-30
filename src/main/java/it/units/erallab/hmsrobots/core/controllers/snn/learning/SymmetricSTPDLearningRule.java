package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.IntStream;

public abstract class SymmetricSTPDLearningRule extends STDPLearningRule {

  private static final double[] MIN_PARAMS = {1d, 1d, 3.5, 13.5};
  private static final double[] MAX_PARAMS = {10.6, 44d, 10d, 20d};

  @JsonProperty
  protected double sigmaPlus;
  @JsonProperty
  protected double sigmaMinus;

  public SymmetricSTPDLearningRule() {
  }

  @JsonCreator
  public SymmetricSTPDLearningRule(
      @JsonProperty("aPlus") double aPlus,
      @JsonProperty("aMinus") double aMinus,
      @JsonProperty("sigmaPlus") double sigmaPlus,
      @JsonProperty("sigmaMinus") double sigmaMinus
  ) {
    super(aPlus, aMinus);
    this.sigmaPlus = sigmaPlus;
    this.sigmaMinus = sigmaMinus;
  }

  protected double computeG(double deltaT) {
    return 1 / (sigmaPlus * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(deltaT / sigmaPlus, 2d))
        - 1 / (sigmaMinus * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(deltaT / sigmaMinus, 2d));
  }

  @Override
  public double[] getParams() {
    return new double[]{aPlus, aMinus, sigmaPlus, sigmaMinus};
  }

  @Override
  public void setParams(double[] params) {
    aPlus = params[0];
    aMinus = params[1];
    sigmaPlus = params[2];
    sigmaMinus = params[3];
  }

  protected static double[] scaleParameters(double[] params) {
    IntStream.range(0, 4).forEach(i -> params[i] = scaleParameter(params[i], MIN_PARAMS[i], MAX_PARAMS[i])
    );
    return params;
  }
}
