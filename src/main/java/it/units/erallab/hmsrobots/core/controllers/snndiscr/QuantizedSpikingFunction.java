package it.units.erallab.hmsrobots.core.controllers.snndiscr;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="@class")
public interface QuantizedSpikingFunction extends Resettable, Serializable {

    int[] compute(double[] weightedSpikes, double time);

    void setSumOfIncomingWeights(double sumOfIncomingWeights);

    void setPlotMode(boolean plotMode);

}
