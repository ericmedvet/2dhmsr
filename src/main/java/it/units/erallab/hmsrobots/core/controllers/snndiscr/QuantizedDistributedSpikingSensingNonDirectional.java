package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensingNonDirectional;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.stream.IntStream;

public class QuantizedDistributedSpikingSensingNonDirectional extends QuantizedDistributedSpikingSensing {

  @JsonCreator
  public QuantizedDistributedSpikingSensingNonDirectional(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<QuantizedMultivariateSpikingFunction> functions,
      @JsonProperty("outputConverters") Grid<QuantizedSpikeTrainToValueConverter> outputConverters,
      @JsonProperty("inputConverters") Grid<QuantizedValueToSpikeTrainConverter[]> inputConverters
  ) {
    super(signals,nOfInputGrid,nOfOutputGrid,functions,outputConverters,inputConverters);
  }

  public QuantizedDistributedSpikingSensingNonDirectional(Grid<? extends SensingVoxel> voxels, int stateSize, QuantizedSpikingFunction spikingFunction, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(
        stateSize,
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensingNonDirectional.nOfInputs(v, stateSize)),
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensingNonDirectional.nOfOutputs(v, stateSize)),
        Grid.create(
            voxels, v -> (v == null) ? null : new QuantizedMultilayerSpikingNetwork(DistributedSensingNonDirectional.nOfInputs(v, stateSize),
                new int[]{DistributedSensingNonDirectional.nOfInputs(v, stateSize), DistributedSensingNonDirectional.nOfInputs(v, stateSize)},
                DistributedSensingNonDirectional.nOfOutputs(v, stateSize), (x, y) -> spikingFunction)),
        Grid.create(voxels, v -> (v == null) ? null : SerializationUtils.clone(spikeTrainToValueConverter)),
        Grid.create(voxels, v -> (v == null) ? null : IntStream.range(0, v.getSensors().stream().mapToInt(s -> s.getDomains().length).sum()).mapToObj(i -> SerializationUtils.clone(valueToSpikeTrainConverter)).toArray(QuantizedValueToSpikeTrainConverter[]::new))
    );
  }

  @Override
  protected int[][] getLastSignals(int x, int y) {
    int[][] values = new int[signals * Dir.values().length][];
    if (signals <= 0) {
      return values;
    }
    int c = 0;
    for (Dir dir : Dir.values()) {
      int adjacentX = x + dir.dx;
      int adjacentY = y + dir.dy;
      int[][] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if (lastSignals != null) {
        System.arraycopy(lastSignals, 0, values, c, signals);
      }
      c = c + signals;
    }
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        values[i] = new int[QuantizedValueToSpikeTrainConverter.ARRAY_SIZE];
      }
    }
    return values;
  }

  @Override
  public String toString() {
    return super.toString().replace("DistributedSpikingSensing", "DistributedSpikingSensingNonDirectional");

  }

}
