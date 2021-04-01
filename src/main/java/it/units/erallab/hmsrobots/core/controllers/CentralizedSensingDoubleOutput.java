package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.Sensor;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CentralizedSensingDoubleOutput extends CentralizedSensing {

  public CentralizedSensingDoubleOutput(
          @JsonProperty("nOfInputs") int nOfInputs,
          @JsonProperty("nOfOutputs") int nOfOutputs,
          @JsonProperty("function") TimedRealFunction function
  ) {
    super(nOfInputs, 2 * nOfOutputs, function);
  }

  public CentralizedSensingDoubleOutput(Grid<? extends SensingVoxel> voxels) {
    this(voxels, RealFunction.build(in -> new double[nOfOutputs(voxels)], nOfInputs(voxels), nOfOutputs(voxels)));
  }

  public CentralizedSensingDoubleOutput(Grid<? extends SensingVoxel> voxels, TimedRealFunction function) {
    this(nOfInputs(voxels), nOfOutputs(voxels), function);
  }

  @Override
  public void control(double t, Grid<? extends SensingVoxel> voxels) {
    //collect inputs
    double[] inputs = new double[nOfInputs];
    int c = 0;
    List<List<Pair<Sensor, double[]>>> allReadings = voxels.values().stream()
            .filter(Objects::nonNull)
            .map(SensingVoxel::getLastReadings)
            .collect(Collectors.toList());
    for (List<Pair<Sensor, double[]>> readings : allReadings) {
      for (Pair<Sensor, double[]> sensorPair : readings) {
        double[] sensorReadings = sensorPair.getValue();
        System.arraycopy(sensorReadings, 0, inputs, c, sensorReadings.length);
        c = c + sensorReadings.length;
      }
    }
    //compute outputs
    double[] outputs = function != null ? function.apply(t, inputs) : new double[nOfOutputs];
    //apply inputs
    c = 0;
    for (SensingVoxel voxel : voxels.values()) {
      if (voxel != null) {
        if (c < outputs.length) {
          double output = (outputs[c] - outputs[c + 1]) / 2d;  // apply force already deals with clipping
          voxel.applyForce(output);
          c = c + 2;
        }
      }
    }
  }


}
