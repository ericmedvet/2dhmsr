package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.AbstractController;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensingCALike;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv.QuantizedSpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts.QuantizedValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

public class QuantizedDistributedSpikingSensingCALike extends AbstractController<SensingVoxel> {

  private static final int ARRAY_SIZE = QuantizedValueToSpikeTrainConverter.ARRAY_SIZE;

  private enum Dir {

    N(0, -1),
    E(1, 0),
    S(0, 1),
    W(-1, 0);

    private final int dx;
    private final int dy;

    Dir(int dx, int dy) {
      this.dx = dx;
      this.dy = dy;
    }

    private static QuantizedDistributedSpikingSensingCALike.Dir adjacent(QuantizedDistributedSpikingSensingCALike.Dir dir) {
      return switch (dir) {
        case N -> Dir.S;
        case E -> Dir.W;
        case S -> Dir.N;
        case W -> Dir.E;
      };
    }
  }

  @JsonProperty
  private final int stateSize;
  @JsonProperty
  private final Grid<Integer> nOfInputGrid;
  @JsonProperty
  private final Grid<Integer> nOfOutputGrid;
  @JsonProperty
  private final Grid<QuantizedMultivariateSpikingFunction> functions;
  @JsonProperty
  private final Grid<QuantizedSpikeTrainToValueConverter> outputConverters;
  @JsonProperty
  private final Grid<QuantizedValueToSpikeTrainConverter[]> inputConverters;

  private double previousTime = 0;
  private final Grid<int[][]> lastSignalsGrid;
  private final Grid<int[][]> currentSignalsGrid;

  @JsonCreator
  public QuantizedDistributedSpikingSensingCALike(
      @JsonProperty("stateSize") int stateSize,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<QuantizedMultivariateSpikingFunction> functions,
      @JsonProperty("outputConverters") Grid<QuantizedSpikeTrainToValueConverter> outputConverters,
      @JsonProperty("inputConverters") Grid<QuantizedValueToSpikeTrainConverter[]> inputConverters
  ) {
    this.stateSize = stateSize;
    this.nOfInputGrid = nOfInputGrid;
    this.nOfOutputGrid = nOfOutputGrid;
    this.functions = functions;
    this.outputConverters = outputConverters;
    this.inputConverters = inputConverters;
    lastSignalsGrid = Grid.create(functions, f -> new int[stateSize][ARRAY_SIZE]);
    currentSignalsGrid = Grid.create(functions, f -> new int[stateSize][ARRAY_SIZE]);
    reset();
  }

  public QuantizedDistributedSpikingSensingCALike(Grid<? extends SensingVoxel> voxels, int stateSize, QuantizedSpikingFunction spikingFunction, QuantizedValueToSpikeTrainConverter valueToSpikeTrainConverter, QuantizedSpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(
        stateSize,
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensingCALike.nOfInputs(v, stateSize)),
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensingCALike.nOfOutputs(v, stateSize)),
        Grid.create(
            voxels, v -> (v == null) ? null : new QuantizedMultilayerSpikingNetwork(DistributedSensingCALike.nOfInputs(v, stateSize),
                new int[]{DistributedSensingCALike.nOfInputs(v, stateSize), DistributedSensingCALike.nOfInputs(v, stateSize)},
                DistributedSensingCALike.nOfOutputs(v, stateSize), (x, y) -> spikingFunction)),
        Grid.create(voxels, v -> (v == null) ? null : SerializationUtils.clone(spikeTrainToValueConverter)),
        Grid.create(voxels, v -> (v == null) ? null : IntStream.range(0, v.getSensors().stream().mapToInt(s -> s.getDomains().length).sum()).mapToObj(i -> SerializationUtils.clone(valueToSpikeTrainConverter)).toArray(QuantizedValueToSpikeTrainConverter[]::new))
    );
  }


  public Grid<QuantizedMultivariateSpikingFunction> getFunctions() {
    return functions;
  }

  public void reset() {
    previousTime = 0;
    for (int x = 0; x < lastSignalsGrid.getW(); x++) {
      for (int y = 0; y < lastSignalsGrid.getH(); y++) {
        lastSignalsGrid.set(x, y, new int[stateSize][ARRAY_SIZE]);
        currentSignalsGrid.set(x, y, new int[stateSize][ARRAY_SIZE]);
        if (outputConverters.get(x, y) != null) {
          outputConverters.get(x, y).reset();
        }
        if (inputConverters.get(x, y) != null) {
          Arrays.stream(inputConverters.get(x, y)).forEach(QuantizedValueToSpikeTrainConverter::reset);
        }
        if (functions.get(x, y) != null) {
          functions.get(x, y).reset();
        }
      }
    }
  }

  @Override
  public Grid<Double> computeControlSignals(double t, Grid<? extends SensingVoxel> voxels) {
    System.out.println("HHE");
    Grid<Double> controlSignals = Grid.create(voxels);
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      if (entry.getValue() == null) {
        continue;
      }
      //get inputs
      int[][] lastSignals = getLastSignals(entry.getX(), entry.getY());
      int[][] sensorValues = convertSensorReadings(entry.getValue().getSensorReadings(), inputConverters.get(entry.getX(), entry.getY()), t);
      int[][] inputs = ArrayUtils.addAll(lastSignals, sensorValues);
      //compute outputs
      QuantizedMultivariateSpikingFunction function = functions.get(entry.getX(), entry.getY());
      int[][] outputs = function != null ? function.apply(t, inputs) : new int[1 + stateSize][ARRAY_SIZE];
      //apply outputs
      double force = outputConverters.get(entry.getX(), entry.getY()).convert(outputs[0], t - previousTime);
      controlSignals.set(entry.getX(), entry.getY(), force);
      System.arraycopy(outputs, 1, currentSignalsGrid.get(entry.getX(), entry.getY()), 0, stateSize);
    }
    previousTime = t;
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      System.arraycopy(currentSignalsGrid.get(entry.getX(), entry.getY()), 0, lastSignalsGrid.get(entry.getX(), entry.getY()), 0, stateSize);
    }
    return controlSignals;
  }

  private int[][] getLastSignals(int x, int y) {
    int[][] values = new int[stateSize * Dir.values().length][];
    if (stateSize <= 0) {
      return values;
    }
    int c = 0;
    for (Dir dir : Dir.values()) {
      int adjacentX = x + dir.dx;
      int adjacentY = y + dir.dy;
      int[][] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if (lastSignals != null) {
        System.arraycopy(lastSignals, 0, values, c, stateSize);
      }
      c = c + stateSize;
    }
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        values[i] = new int[QuantizedValueToSpikeTrainConverter.ARRAY_SIZE];
      }
    }
    return values;
  }

  private int[][] convertSensorReadings(double[] sensorsReadings, QuantizedValueToSpikeTrainConverter[] valueToSpikeTrainConverters, double t) {
    int[][] convertedValues = new int[sensorsReadings.length][];
    IntStream.range(0, sensorsReadings.length).forEach(i -> convertedValues[i] = valueToSpikeTrainConverters[i].convert(sensorsReadings[i], t - previousTime, t));
    return convertedValues;
  }

  public int nOfInputs(int x, int y) {
    return nOfInputGrid.get(x, y);
  }

  public int nOfOutputs(int x, int y) {
    return nOfOutputGrid.get(x, y);
  }

  @Override
  public String toString() {
    return "DistributedSpikingSensing{" +
        "signals=" + stateSize +
        ", functions=" + functions +
        '}';
  }

}
