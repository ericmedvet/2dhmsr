package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.snn.converters.UniformValueToSpikeTrainConverter;
import it.units.erallab.hmsrobots.core.controllers.snn.converters.ValueToSpikeTrainConverter;

import java.util.SortedSet;

public class Main {

  public static void main(String[] args) {
    ValueToSpikeTrainConverter valueToSpikeTrainConverter = new UniformValueToSpikeTrainConverter();
    double frequency = 60;
    double timeWindow = 1/frequency;
    System.out.printf("Frequency: %f\nTime interval: %f\n",frequency,timeWindow);
    for(double i=0; i<1; i+=0.1){
      SortedSet<Double> spikeTrain = valueToSpikeTrainConverter.convert(i,timeWindow);
      System.out.printf("Value: %.2f Spikes: %s\n",i,spikeTrain.toString());
    }
  }

}
