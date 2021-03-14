package it.units.erallab.hmsrobots.core.controllers.snn;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.util.*;

public abstract class SpikingNeuron implements SpikingFunction {

    protected final double restingPotential;
    private final double thresholdPotential;
    protected double membranePotential;
    protected double lastInputTime;
    private double lastEvaluatedTime;

    protected final SortedMap<Double, Double> membranePotentialValues;
    protected final SortedMap<Double, Double> inputSpikesValues;

    protected static final double PLOTTING_TIME_STEP = 0.000000000001;

    public SpikingNeuron(double restingPotential, double thresholdPotential) {
        this.restingPotential = restingPotential;
        this.thresholdPotential = thresholdPotential;
        membranePotential = restingPotential;
        membranePotentialValues = new TreeMap<>();
        inputSpikesValues = new TreeMap<>();
        membranePotentialValues.put(lastInputTime, membranePotential);
    }

    @Override
    public SortedSet<Double> compute(SortedMap<Double, Double> weightedSpikes, double t) {
        double timeWindowSize = t - lastEvaluatedTime;
        SortedSet<Double> spikes = new TreeSet<>();
        weightedSpikes.forEach((spikeTime, weightedSpike) -> {
                    double scaledSpikeTime = spikeTime * timeWindowSize + lastEvaluatedTime;
                    inputSpikesValues.put(scaledSpikeTime, weightedSpike);
                    acceptWeightedSpike(scaledSpikeTime, weightedSpike);
                    if (membranePotential > thresholdPotential) {
                        spikes.add(spikeTime);
                        resetAfterSpike();
                        membranePotentialValues.put(lastInputTime + 2 * PLOTTING_TIME_STEP, membranePotential);
                    }
                }
        );
        lastEvaluatedTime = t;
        return spikes;
    }

    protected abstract void acceptWeightedSpike(double spikeTime, double weightedSpike);

    protected abstract void resetAfterSpike();

    public XYChart getMembranePotentialEvolutionWithInputSpikesChart(int width, int height) {
        XYChart chart = getMembranePotentialEvolutionChart(width, height);

        if (!inputSpikesValues.isEmpty()) {
            XYSeries inputSpikesSeries = chart.addSeries("input", new ArrayList<>(inputSpikesValues.keySet()), new ArrayList<>(inputSpikesValues.values()));
            inputSpikesSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter).setMarker(SeriesMarkers.DIAMOND);
        }

        return chart;
    }

    public XYChart getMembranePotentialEvolutionChart(int width, int height) {
        XYChart chart = new XYChart(width, height);
        chart.getStyler().setXAxisMin(0.0).setXAxisMax(lastEvaluatedTime).setLegendPosition(Styler.LegendPosition.InsideNW);

        if (!membranePotentialValues.isEmpty()) {
            XYSeries membranePotentialSeries = chart.addSeries("membrane potential", new ArrayList<>(membranePotentialValues.keySet()), new ArrayList<>(membranePotentialValues.values()));
            membranePotentialSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line).setMarker(SeriesMarkers.NONE);
        }

        XYSeries thresholdSeries = chart.addSeries("threshold", List.of(0, lastEvaluatedTime), List.of(thresholdPotential, thresholdPotential));
        thresholdSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line).setMarker(SeriesMarkers.NONE);

        return chart;
    }

    public SortedMap<Double, Double> getMembranePotentialValues() {
        return membranePotentialValues;
    }

    public SortedMap<Double, Double> getInputSpikesValues() {
        return inputSpikesValues;
    }
}
