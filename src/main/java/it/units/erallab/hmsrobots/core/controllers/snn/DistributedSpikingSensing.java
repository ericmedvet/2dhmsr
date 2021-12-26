package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensing;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.stv.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.vts.ValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;

public class DistributedSpikingSensing implements Controller<SensingVoxel> {

  private enum Dir {

    N(0, -1, 0),
    E(1, 0, 1),
    S(0, 1, 2),
    W(-1, 0, 3);

    private final int dx;
    private final int dy;
    private final int index;

    Dir(int dx, int dy, int index) {
      this.dx = dx;
      this.dy = dy;
      this.index = index;
    }

    private static DistributedSpikingSensing.Dir adjacent(DistributedSpikingSensing.Dir dir) {
      return switch (dir) {
        case N -> DistributedSpikingSensing.Dir.S;
        case E -> DistributedSpikingSensing.Dir.W;
        case S -> DistributedSpikingSensing.Dir.N;
        case W -> DistributedSpikingSensing.Dir.E;
      };
    }
  }

  @JsonProperty
  private final int signals;
  @JsonProperty
  private final Grid<Integer> nOfInputGrid;
  @JsonProperty
  private final Grid<Integer> nOfOutputGrid;
  @JsonProperty
  private final Grid<MultivariateSpikingFunction> functions;
  @JsonProperty
  private final Grid<SpikeTrainToValueConverter> outputConverters;
  @JsonProperty
  private final Grid<ValueToSpikeTrainConverter[]> inputConverters;

  private double previousTime = 0;
  private final Grid<SortedSet<Double>[]> lastSignalsGrid;
  private final Grid<SortedSet<Double>[]> currentSignalsGrid;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public DistributedSpikingSensing(
      @JsonProperty("signals") int signals,
      @JsonProperty("nOfInputGrid") Grid<Integer> nOfInputGrid,
      @JsonProperty("nOfOutputGrid") Grid<Integer> nOfOutputGrid,
      @JsonProperty("functions") Grid<MultivariateSpikingFunction> functions,
      @JsonProperty("outputConverters") Grid<SpikeTrainToValueConverter> outputConverters,
      @JsonProperty("inputConverters") Grid<ValueToSpikeTrainConverter[]> inputConverters
  ) {
    this.signals = signals;
    this.nOfInputGrid = nOfInputGrid;
    this.nOfOutputGrid = nOfOutputGrid;
    this.functions = functions;
    this.outputConverters = outputConverters;
    this.inputConverters = inputConverters;
    lastSignalsGrid = Grid.create(functions, f -> new SortedSet[signals * Dir.values().length]);
    lastSignalsGrid.forEach(entry -> {
      for (int i = 0; i < entry.getValue().length; i++) {
        entry.getValue()[i] = new TreeSet<>();
      }
    });
    currentSignalsGrid = Grid.create(functions, f -> new SortedSet[signals * Dir.values().length]);
    currentSignalsGrid.forEach(entry -> {
      for (int i = 0; i < entry.getValue().length; i++) {
        entry.getValue()[i] = new TreeSet<>();
      }
    });
    reset();
  }

  public DistributedSpikingSensing(Grid<? extends SensingVoxel> voxels, int signals, SpikingFunction spikingFunction, ValueToSpikeTrainConverter valueToSpikeTrainConverter, SpikeTrainToValueConverter spikeTrainToValueConverter) {
    this(
        signals,
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensing.nOfInputs(v, signals)),
        Grid.create(voxels, v -> (v == null) ? 0 : DistributedSensing.nOfOutputs(v, signals)),
        Grid.create(
            voxels, v -> (v == null) ? null : new MultilayerSpikingNetwork(DistributedSensing.nOfInputs(v, signals),
                new int[]{DistributedSensing.nOfInputs(v, signals), DistributedSensing.nOfInputs(v, signals)},
                DistributedSensing.nOfOutputs(v, signals), (x, y) -> spikingFunction)),
        Grid.create(voxels, v -> (v == null) ? null : SerializationUtils.clone(spikeTrainToValueConverter)),
        Grid.create(voxels, v -> (v == null) ? null : IntStream.range(0, v.getSensors().stream().mapToInt(s -> s.getDomains().length).sum()).mapToObj(i -> SerializationUtils.clone(valueToSpikeTrainConverter)).toArray(ValueToSpikeTrainConverter[]::new))
    );
  }


  public Grid<MultivariateSpikingFunction> getFunctions() {
    return functions;
  }

  @SuppressWarnings("unchecked")
  public void reset() {
    previousTime = 0;
    for (int x = 0; x < lastSignalsGrid.getW(); x++) {
      for (int y = 0; y < lastSignalsGrid.getH(); y++) {
        lastSignalsGrid.set(x, y, new SortedSet[signals * Dir.values().length]);
        currentSignalsGrid.set(x, y, new SortedSet[signals * Dir.values().length]);
        if (outputConverters.get(x, y) != null) {
          outputConverters.get(x, y).reset();
        }
        if (inputConverters.get(x, y) != null) {
          Arrays.stream(inputConverters.get(x, y)).forEach(ValueToSpikeTrainConverter::reset);
        }
        if (functions.get(x, y) != null) {
          functions.get(x, y).reset();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void control(double t, Grid<? extends SensingVoxel> voxels) {
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      if (entry.getValue() == null) {
        continue;
      }
      //get inputs
      SortedSet<Double>[] lastSignals = getLastSignals(entry.getX(), entry.getY());
      SortedSet<Double>[] sensorValues = convertSensorReadings(entry.getValue().getSensorReadings(), inputConverters.get(entry.getX(), entry.getY()), t);
      SortedSet<Double>[] inputs = ArrayUtils.addAll(lastSignals, sensorValues);
      //compute outputs
      MultivariateSpikingFunction function = functions.get(entry.getX(), entry.getY());
      SortedSet<Double>[] outputs = function != null ? function.apply(t, inputs) : new SortedSet[1 + signals * Dir.values().length];
      //apply outputs
      double force = outputConverters.get(entry.getX(), entry.getY()).convert(outputs[0], t - previousTime);
      entry.getValue().applyForce(force);
      System.arraycopy(outputs, 1, currentSignalsGrid.get(entry.getX(), entry.getY()), 0, signals * Dir.values().length);
    }
    previousTime = t;
    for (Grid.Entry<? extends SensingVoxel> entry : voxels) {
      System.arraycopy(currentSignalsGrid.get(entry.getX(), entry.getY()), 0, lastSignalsGrid.get(entry.getX(), entry.getY()), 0, signals * Dir.values().length);
    }
  }

  @SuppressWarnings("unchecked")
  private SortedSet<Double>[] getLastSignals(int x, int y) {
    SortedSet<Double>[] values = new SortedSet[signals * Dir.values().length];
    if (signals <= 0) {
      return values;
    }
    int c = 0;
    for (Dir dir : Dir.values()) {
      int adjacentX = x + dir.dx;
      int adjacentY = y + dir.dy;
      SortedSet<Double>[] lastSignals = lastSignalsGrid.get(adjacentX, adjacentY);
      if (lastSignals != null) {
        int index = Dir.adjacent(dir).index;
        System.arraycopy(lastSignals, index * signals, values, c, signals);
      }
      c = c + signals;
    }
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        values[i] = new TreeSet<>();
      }
    }
    return values;
  }

  @SuppressWarnings("unchecked")
  private SortedSet<Double>[] convertSensorReadings(double[] sensorsReadings, ValueToSpikeTrainConverter[] valueToSpikeTrainConverters, double t) {
    SortedSet<Double>[] convertedValues = new SortedSet[sensorsReadings.length];
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
        "signals=" + signals +
        ", functions=" + functions +
        '}';
  }

}
