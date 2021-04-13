package it.units.erallab.hmsrobots.core.controllers.snn;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.units.erallab.hmsrobots.core.controllers.Resettable;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="@class")
public interface SpikingFunction extends Resettable, Serializable {

    SortedSet<Double> compute(SortedMap<Double,Double> weightedSpikes, double time);

    void setSumOfIncomingWeights(double sumOfIncomingWeights);

}
