package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.snn.converters.AverageFrequencySpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.SpikeTrainToValueConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.UniformValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.ValueToSpikeTrainConverter;

import java.util.SortedSet;

public class Main {

  public static void main(String[] args) {
    testConverter();
  }

  public static void testConverter() {
    ValueToSpikeTrainConverter valueToSpikeTrainConverter = new UniformValueToSpikeTrainConverter();
    SpikeTrainToValueConverter spikeTrainToValueConverter = new AverageFrequencySpikeTrainToValueConverter();
    double frequency = 60;
    double timeWindow = 1/frequency;
    System.out.printf("Frequency: %f\nTime interval: %f\n",frequency,timeWindow);
    for(double i=-1; i<1; i+=0.1){
      SortedSet<Double> spikeTrain = valueToSpikeTrainConverter.convert(i,timeWindow);
      double j = spikeTrainToValueConverter.convert(spikeTrain,timeWindow);
      System.out.printf("Value: %.2f\nSpikes: %s\nReconverted value: %.2f\n\n",i,spikeTrain.toString(),j);
    }
  }

}
