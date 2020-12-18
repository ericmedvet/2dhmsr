package it.units.erallab.hmsrobots.core.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.Voxel;

import java.util.Arrays;
import java.util.Random;

/**
 * @author eric on 2020/12/18 for 2dhmsr
 */
public class Noisy implements Sensor {

  @JsonProperty
  private final Sensor sensor;
  @JsonProperty
  private final double sigma;

  private final double[] sigmas;
  private final Random random;

  @JsonCreator
  public Noisy(
      @JsonProperty("sensor") Sensor sensor,
      @JsonProperty("sigma") double sigma
  ) {
    this.sensor = sensor;
    this.sigma = sigma;
    random = new Random();
    sigmas = Arrays.stream(sensor.domains())
        .mapToDouble(d -> Math.abs(d.getMax() - d.getMin()) * sigma)
        .toArray();
  }

  @Override
  public Domain[] domains() {
    return sensor.domains();
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    double[] values = sensor.sense(voxel, t);
    for (int i = 0; i < values.length; i++) {
      values[i] = values[i] + random.nextGaussian() * sigmas[i];
    }
    return values;
  }
}
