package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;

import java.util.Arrays;

/**
 * @author eric on 2020/12/11 for 2dhmsr
 */
public class Constant implements Sensor {

  @JsonProperty
  private final double[] values;
  private final Domain[] domains;

  @JsonCreator
  public Constant(@JsonProperty("values") double... values) {
    this.values = values;
    double max = Arrays.stream(values).max().orElse(1d);
    double min = Arrays.stream(values).min().orElse(0d);
    max = Math.max(1d, max);
    min = Math.min(0d, min);
    domains = new Domain[values.length];
    for (int i = 0; i < values.length; i++) {
      domains[i] = Domain.of(min, max);
    }
  }

  @Override
  public Domain[] domains() {
    return domains;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    return values;
  }

  @Override
  public String toString() {
    return "Constant{" +
        "values=" + Arrays.toString(values) +
        '}';
  }
}
