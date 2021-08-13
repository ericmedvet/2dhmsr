package it.units.erallab.hmsrobots.core.sensors;

import it.units.erallab.hmsrobots.core.objects.BreakableVoxel;

/**
 * @author eric
 */
public class Malfunction extends AbstractSensor {

  private final static Domain[] DOMAINS = new Domain[]{
      Domain.of(0d, 1d)
  };

  public Malfunction() {
    super(DOMAINS);
  }

  @Override
  public double[] sense(double t) {
    if (voxel instanceof BreakableVoxel) {
      if (((BreakableVoxel) voxel).isBroken()) {
        return new double[]{1d};
      }
    }
    return new double[]{0d};
  }

}
