package it.units.erallab.hmsrobots.core.controllers;

import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.util.Grid;

public class BasicInteractiveController extends AbstractController<ControllableVoxel> {
  boolean keyPressed = false;
  @Override
  public Grid<Double> computeControlSignals(double t, Grid<? extends ControllableVoxel> voxels) {
    Grid<Double> values = Grid.create(voxels, v -> keyPressed?-1d:1d);
    return values;
  }

  @Override
  public void reset() {

  }

  public void setKeyPressed(boolean keyPressed) {
    this.keyPressed = keyPressed;
  }
}
