package it.units.erallab.hmsrobots;

import it.units.erallab.hmsrobots.core.controllers.*;
import it.units.erallab.hmsrobots.core.objects.ControllableVoxel;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.erallab.hmsrobots.util.Utils;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.dyn4j.dynamics.Settings;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static it.units.erallab.hmsrobots.util.Utils.params;

public class Pyworker implements Runnable{
    double[][] terrain;
    double duration;
    Function<List<Double>, Robot<?>> builder;
    Function<Robot<?>,List<Double>> fitness;
    List<List<Double>> genotypes = new ArrayList<>();
    List<List<Double>> results = new ArrayList<>();
    protected final ExecutorService executorService;

    public Pyworker(String terrain, String shape, String sensors, String controller, double duration) {
        Grid<? extends SensingVoxel> body = RobotUtils.buildSensorizingFunction(sensors).apply(RobotUtils.buildShape(shape));
        CentralizedSensing centralizedSensing = new CentralizedSensing(body);
        System.out.println("process "+Runtime.getRuntime().availableProcessors());
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.duration = duration;
        this.terrain = Locomotion.createTerrain(terrain);
        this.builder = i -> new Robot<>(
                SerializationUtils.clone(buildController(centralizedSensing, i, controller)),
                SerializationUtils.clone(body));
        this.fitness = robot -> {
            Locomotion locomotion = new Locomotion(duration, this.terrain, new Settings());
            Outcome outcome = locomotion.apply(robot, null);
            double vel = outcome.getVelocity();
            double[] desc = outcome.getSpectrumDescriptor();
            ArrayList<Double> fitness =  new ArrayList<>();
            fitness.add(vel);
            for(int i=0;i<desc.length;i++){
                fitness.add(desc[i]);
            }
            return fitness;
        };



    }

    /*public Outcome locomote() {
        Locomotion locomotion = new Locomotion(duration, terrain, new Settings());
        //System.out.println("pre apply");
        Outcome outcome = locomotion.apply(robot, null);
        //System.out.println("post apply");
        return outcome;
    }
*/
    public String getRobotSerialized(double[] genome) {
        return SerializationUtils.serialize(builder.apply(DoubleStream.of(genome).boxed().collect(Collectors.toList())));
    }

    private int[] innerNeurons(int nOfInputs, int nOfOutputs, int nOfInnerLayers, double innerLayerRatio) {
        int[] innerNeurons = new int[nOfInnerLayers];
        int centerSize = (int) Math.max(2, Math.round(nOfInputs * innerLayerRatio));
        if (nOfInnerLayers > 1) {
            for (int i = 0; i < nOfInnerLayers / 2; i++) {
                innerNeurons[i] = nOfInputs + (centerSize - nOfInputs) / (nOfInnerLayers / 2 + 1) * (i + 1);
            }
            for (int i = nOfInnerLayers / 2; i < nOfInnerLayers; i++) {
                innerNeurons[i] = centerSize + (nOfOutputs - centerSize) / (nOfInnerLayers / 2 + 1) * (i - nOfInnerLayers / 2);
            }
        } else if (nOfInnerLayers > 0) {
            innerNeurons[0] = centerSize;
        }
        return innerNeurons;
    }

    private CentralizedSensing buildController(CentralizedSensing centralizedSensing, List<Double> weights, String controller) {
        String mlp = "MLP-(?<ratio>\\d+(\\.\\d+)?)-(?<nLayers>\\d+)(-(?<actFun>(sin|tanh|sigmoid|relu)))?";
        Map<String, String> params;
        if ((params = params(mlp, controller)) != null) {
            //(MultiLayerPerceptron.ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput)

            MultiLayerPerceptron func = new MultiLayerPerceptron(
                    params.containsKey("actFun") ? MultiLayerPerceptron.ActivationFunction.valueOf(params.get("actFun").toUpperCase()) : MultiLayerPerceptron.ActivationFunction.TANH,
                    centralizedSensing.nOfInputs(),
                    innerNeurons(centralizedSensing.nOfInputs(), centralizedSensing.nOfOutputs(), Integer.parseInt(params.get("nLayers")), Double.parseDouble(params.get("ratio"))),
                    centralizedSensing.nOfOutputs(),
                    weights.stream().mapToDouble(Double::doubleValue).toArray()
            );
            centralizedSensing.setFunction(func);
            return centralizedSensing;
        }
        throw new IllegalArgumentException(String.format("Unknown mapper name: %s", controller));

    }

    public double[][] locomote(double[][] genos){
        genotypes = new ArrayList<>();
        for(int i=0;i<genos.length;i++){
            genotypes.add(DoubleStream.of(genos[i]).boxed().collect(Collectors.toList()));
        }

        run();

        double[][] res = new double[results.size()][5];

        for (int i=0;i<results.size();i++) {
            res[i] = results.get(i).stream().mapToDouble(Double::doubleValue).toArray();
        }

        return res;
    }

    public Outcome locomoteSerialized(String serialized){
        Locomotion locomotion = new Locomotion(duration, terrain, new Settings());
        Robot robot = SerializationUtils.deserialize(serialized, Robot.class, SerializationUtils.Mode.GZIPPED_JSON);
        //System.out.println("pre apply");
        Outcome outcome = locomotion.apply(robot, null);
        //System.out.println("post apply");
        return outcome;

    }

    protected static List<List<Double>> map(Collection<List<Double>> genotypes,
                                            Function<List<Double>, Robot<?>> solutionMapper,
                                            Function<Robot<?>, List<Double>> fitnessFunction,
                                            ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Callable<List<Double>>> callables = new ArrayList<>(genotypes.size());
        callables.addAll(genotypes.stream()
                .map(genotype -> (Callable<List<Double>>) () -> {
                    return fitnessFunction.apply(solutionMapper.apply(genotype));
                }).collect(Collectors.toList()));
        return getAll(executor.invokeAll(callables));
    }

    private static <T> List<T> getAll(List<Future<T>> futures) throws InterruptedException, ExecutionException {
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }

    @Override
    public void run() {
        try {
            results = map(this.genotypes, this.builder, this.fitness, executorService);
        } catch (Exception ex){
            System.err.println(ex.getMessage());
        }
    }
}
