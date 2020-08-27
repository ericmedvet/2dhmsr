package it.units.erallab.hmsrobots.core.sensors;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;
import it.units.erallab.hmsrobots.core.objects.Voxel;

/**
 * @author eric
 * @created 2020/08/27
 * @project 2dhmsr
 */
public class Malfunction implements Sensor {

  @Override
  public Domain[] domains() {
    return new Domain[]{Domain.of(0, 1)};
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
}
