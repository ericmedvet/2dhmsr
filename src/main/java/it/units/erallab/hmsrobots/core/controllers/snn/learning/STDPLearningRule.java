package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;

public abstract class STDPLearningRule implements Parametrized, Serializable {

  @JsonProperty
  protected double aPlus;
  @JsonProperty
  protected double aMinus;

  public STDPLearningRule() {
  }

  @JsonCreator
  public STDPLearningRule(
      @JsonProperty("aPlus") double aPlus,
      @JsonProperty("aMinus") double aMinus
  ) {
    this.aPlus = aPlus;
    this.aMinus = aMinus;
  }

  public abstract double computeDeltaW(double deltaT);

}
