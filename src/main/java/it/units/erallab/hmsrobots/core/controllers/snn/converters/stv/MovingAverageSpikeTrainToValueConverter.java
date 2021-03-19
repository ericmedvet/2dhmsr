package it.units.erallab.hmsrobots.core.controllers.snn.converters.stv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.SortedSet;
import java.util.stream.IntStream;

public class MovingAverageSpikeTrainToValueConverter extends AverageFrequencySpikeTrainToValueConverter {

  private static final int DEFAULT_NUMBER_OF_WINDOWS = 10;

  private final double[] windowSizes;
  private final int[] spikesPerWindow;
  private int currentPosition = 0;

  @JsonCreator
  public MovingAverageSpikeTrainToValueConverter(
          @JsonProperty("frequency") double frequency,
          @JsonProperty("numberOfWindows") int numberOfWindows
  ) {
    super(frequency);
    windowSizes = new double[numberOfWindows];
    spikesPerWindow = new int[numberOfWindows];
  }

  public MovingAverageSpikeTrainToValueConverter(int numberOfWindows) {
    this(DEFAULT_FREQUENCY, numberOfWindows);
  }

  public MovingAverageSpikeTrainToValueConverter(double frequency) {
    this(frequency, DEFAULT_NUMBER_OF_WINDOWS);
  }

  public MovingAverageSpikeTrainToValueConverter() {
    this(DEFAULT_FREQUENCY, DEFAULT_NUMBER_OF_WINDOWS);
  }

  @Override
  public double convert(SortedSet<Double> spikeTrain, double timeWindowSize) {
    windowSizes[currentPosition] = timeWindowSize;
    spikesPerWindow[currentPosition] = spikeTrain.size();
    currentPosition = (currentPosition == windowSizes.length - 1) ? 0 : (currentPosition + 1);
    int totalNumberOfSpikes = 0;
    double totalWindowSize = 0;
    for (int i = 0; i < windowSizes.length; i++) {
      if (windowSizes[i] > 0) {
        totalWindowSize += windowSizes[i];
        totalNumberOfSpikes += spikesPerWindow[i];
      }
    }
    return convert(totalNumberOfSpikes, totalWindowSize);
  }

  @Override
  public void reset() {
    IntStream.range(0, windowSizes.length).forEach(i -> {
      windowSizes[i] = 0;
      spikesPerWindow[i] = 0;
    });
    currentPosition = 0;
  }

}

