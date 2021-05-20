package it.units.erallab.hmsrobots.core.controllers.snndiscr.converters.vts;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="@class")
public interface QuantizedValueToSpikeTrainConverter extends Serializable, Resettable {

  double LOWER_BOUND = 0;
  double UPPER_BOUND = 1;
  double DEFAULT_FREQUENCY = 50;
  double MIN_FREQUENCY = 5;
  int ARRAY_SIZE = 16;

  int[] convert(double value, double timeWindowSize, double timeWindowEnd);

  void setFrequency(double frequency);

  @Override
  default void reset() {
  }

  default double clipInputValue(double value) {
    return Math.max(Math.min(UPPER_BOUND, value), LOWER_BOUND);
  }

}
