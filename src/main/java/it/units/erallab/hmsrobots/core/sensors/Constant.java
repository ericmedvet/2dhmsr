package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * @author eric on 2020/12/11 for 2dhmsr
 */
public class Constant extends AbstractSensor {

  @JsonProperty
  private final double[] values;

  @JsonCreator
  public Constant(@JsonProperty("values") double... values) {
    super(computeDomains(values));
    this.values = values;
  }

  private static Domain[] computeDomains(double... values) {
    double max = Arrays.stream(values).max().orElse(1d);
    double min = Arrays.stream(values).min().orElse(0d);
    max = Math.max(1d, max);
    min = Math.min(0d, min);
    Domain[] domains = new Domain[values.length];
    for (int i = 0; i < values.length; i++) {
      domains[i] = Domain.of(min, max);
    }
    return domains;
  }

  @Override
  public double[] sense(double t) {
    return values;
  }

  @Override
  public String toString() {
    return "Constant{" +
        "values=" + Arrays.toString(values) +
        '}';
  }
}
