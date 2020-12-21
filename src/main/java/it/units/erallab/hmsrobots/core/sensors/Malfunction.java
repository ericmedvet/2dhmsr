package it.units.erallab.hmsrobots.core.sensors;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;

/**
 * @author eric
 */
public class Malfunction implements Sensor {

  private final static Domain[] DOMAINS = new Domain[]{
      Domain.of(0d, 1d)
  };

  @Override
  public Domain[] domains() {
    return DOMAINS;
  }

  @Override
  public double[] sense(Voxel voxel, double t) {
    if (voxel instanceof BreakableVoxel) {
      if (((BreakableVoxel) voxel).isBroken()) {
        return new double[]{1d};
      }
    }
    return new double[]{0d};
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
