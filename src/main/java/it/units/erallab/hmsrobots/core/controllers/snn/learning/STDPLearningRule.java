package it.units.erallab.hmsrobots.core.controllers.snn.learning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.util.Parametrized;

import java.io.Serializable;
import java.util.Arrays;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
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


  private static STDPLearningRule createFromReals(double[] parameters) {
    if (parameters.length != 6) {
      throw new IllegalArgumentException(String.format("Expected 6 parameters, received %d", parameters.length));
    }
    double[] stdpParams = Arrays.copyOfRange(parameters, 2, parameters.length);
    STDPLearningRule stdpLearningRule;
    // first parameter regulates symmetry, second regulates hebbian/antihebbian
    if (parameters[0] > 0) {
      SymmetricSTPDLearningRule.scaleParameters(stdpParams);
      if (parameters[1] > 0) {
        stdpLearningRule = new SymmetricHebbianLearningRule();
      } else {
        stdpLearningRule = new SymmetricAntiHebbianLearningRule();
      }
    } else {
      AsymmetricSTDPLearningRule.scaleParameters(stdpParams);
      if (parameters[1] > 0) {
        stdpLearningRule = new AsymmetricHebbianLearningRule();
      } else {
        stdpLearningRule = new AsymmetricAntiHebbianLearningRule();
      }
    }
    stdpLearningRule.setParams(stdpParams);
    return stdpLearningRule;
  }

  private static STDPLearningRule createFromReals(double[] parameters, double[] defaultSymmetricParams, double[] defaultAsymmetricParams) {
    if (parameters.length != 2) {
      throw new IllegalArgumentException(String.format("Expected 2 parameters, received %d", parameters.length));
    }
    double[] stdpParams;
    STDPLearningRule stdpLearningRule;
    // first parameter regulates symmetry, second regulates hebbian/antihebbian
    if (parameters[0] > 0) {
      stdpParams = SymmetricSTPDLearningRule.scaleParameters(defaultSymmetricParams);
      if (parameters[1] > 0) {
        stdpLearningRule = new SymmetricHebbianLearningRule();
      } else {
        stdpLearningRule = new SymmetricAntiHebbianLearningRule();
      }
    } else {
      stdpParams = AsymmetricSTDPLearningRule.scaleParameters(defaultAsymmetricParams);
      if (parameters[1] > 0) {
        stdpLearningRule = new AsymmetricHebbianLearningRule();
      } else {
        stdpLearningRule = new AsymmetricAntiHebbianLearningRule();
      }
    }
    stdpLearningRule.setParams(stdpParams);
    return stdpLearningRule;
  }

  public static STDPLearningRule[] createLearningRules(double[][] params) {
    return Arrays.stream(params).map(STDPLearningRule::createFromReals).toArray(STDPLearningRule[]::new);
  }

  public static STDPLearningRule[] createLearningRules(double[][] params, double[] defaultSymmetricParams, double[] defaultAsymmetricParams) {
    return Arrays.stream(params).map(p -> STDPLearningRule.createFromReals(p, defaultSymmetricParams, defaultAsymmetricParams)).toArray(STDPLearningRule[]::new);
  }

  public abstract double computeDeltaW(double deltaT);

  // might try using tanh instead of max and min
  protected static double scaleParameter(double param, double min, double max) {
    return (Math.max(Math.min(param, 1), -1) / 2 + 0.5) * (max - min) + min;
  }

  public static void main(String[] args) {
    System.out.println(scaleParameter(1, 0.1, 1));
  }

}
