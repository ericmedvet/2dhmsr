package it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.stv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.stream.IntStream;

public class QuantizedMovingAverageSpikeTrainToValueConverter extends QuantizedAverageFrequencySpikeTrainToValueConverter {

  private static final int DEFAULT_NUMBER_OF_WINDOWS = 10;

  @JsonProperty
  int numberOfWindows;
  private final double[] windowSizes;
  private final int[] spikesPerWindow;
  private int currentPosition = 0;

  @JsonCreator
  public QuantizedMovingAverageSpikeTrainToValueConverter(
          @JsonProperty("frequency") double frequency,
          @JsonProperty("numberOfWindows") int numberOfWindows
  ) {
    super(frequency);
    windowSizes = new double[numberOfWindows];
    spikesPerWindow = new int[numberOfWindows];
    this.numberOfWindows = numberOfWindows;
  }

  public QuantizedMovingAverageSpikeTrainToValueConverter(int numberOfWindows) {
    this(DEFAULT_FREQUENCY, numberOfWindows);
  }

  public QuantizedMovingAverageSpikeTrainToValueConverter(double frequency) {
    this(frequency, DEFAULT_NUMBER_OF_WINDOWS);
  }

  public QuantizedMovingAverageSpikeTrainToValueConverter() {
    this(DEFAULT_FREQUENCY, DEFAULT_NUMBER_OF_WINDOWS);
  }

  @Override
  public double convert(int[] spikeTrain, double timeWindowSize) {
    windowSizes[currentPosition] = timeWindowSize;
    spikesPerWindow[currentPosition] = Arrays.stream(spikeTrain).sum();
    currentPosition = (currentPosition == (windowSizes.length - 1)) ? 0 : (currentPosition + 1);
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

