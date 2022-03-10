package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.snapshots.MLPState;
import it.units.erallab.hmsrobots.core.snapshots.Snapshot;
import it.units.erallab.hmsrobots.core.snapshots.Snapshottable;
import it.units.erallab.hmsrobots.util.DoubleRange;
import it.units.erallab.hmsrobots.util.Parametrized;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.Arrays;


public class SelfAttention implements Serializable, Parametrized, RealFunction, Snapshottable {

  @JsonProperty
  private final MultiLayerPerceptron downstream;
  @JsonProperty
  private final int n;
  @JsonProperty
  private final int din;
  @JsonProperty
  private final int dk;
  @JsonProperty
  private final double[][] wq;
  @JsonProperty
  private final double[][] wk;
  @JsonProperty
  private final double[] qbias;
  @JsonProperty
  private final double[] kbias;

  private final double[][] attention;
  private final double[][] latentCode;
  private final double[][] q;
  private final double[][] k;

  @JsonCreator
  public SelfAttention(@JsonProperty("dowstream") MultiLayerPerceptron downstream,
                       @JsonProperty("n") int n,
                       @JsonProperty("din") int din,
                       @JsonProperty("dk") int dk,
                       @JsonProperty("wq") double[][] wq,
                       @JsonProperty("wk") double[][] wk,
                       @JsonProperty("qbias") double[] qbias,
                       @JsonProperty("kbias") double[] kbias) {
    this.downstream = downstream;
    this.n = n;
    this.din = din;
    this.dk = dk;
    this.wq = wq;
    this.wk = wk;
    this.qbias = qbias;
    this.kbias = kbias;
    this.attention = new double[din][din];
    this.latentCode = new double[din][n];
    this.q = new double[din][dk];
    this.k = new double[din][dk];
  }

  public SelfAttention(MultiLayerPerceptron downstream, int n, int din, int dk) {
    this(downstream, n, din, dk, new double[1][dk], new double[1][dk], new double[dk], new double[dk]);
  }

  public static int countAttentionParams(int din, int dk) {
    return (din * dk) + dk + (din * dk) + dk;
  }

  public int countAttentionParams() { return countAttentionParams(this.din, this.dk); }

  public double[] getAttentionParams() { return concat(flat(this.wq), flat(this.wk), this.qbias, this.kbias); }

  public double[] getDownstreamParams() { return this.downstream.getParams(); }

  @Override
  public double[] getParams() { return concat(this.getAttentionParams(), this.getDownstreamParams()); }

  public void setAttentionParams(double[] params) {
    int s = 0;
    for (double[] row : this.wq) {
      System.arraycopy(params, s, row, 0, this.dk);
      s = s + this.dk;
    }
    System.arraycopy(params, s, this.qbias, 0, this.dk);
    s = s + this.dk;
    for (double[] row : this.wk) {
      System.arraycopy(params, s, row, 0, this.dk);
      s = s + this.dk;
    }
    System.arraycopy(params, s, this.kbias, 0, this.dk);
  }

  public void setDownstreamParams(double[] params) { this.downstream.setParams(params); }

  @Override
  public void setParams(double[] params) {
    this.setAttentionParams(Arrays.stream(params).limit(countAttentionParams(this.din, this.dk)).toArray());
    this.setDownstreamParams(Arrays.stream(params).skip(countAttentionParams(this.din, this.dk)).toArray());
  }

  public static double[] concat(double[]... arrays) {
    double[] values = new double[]{};
    for (double[] a : arrays) {
      values = ArrayUtils.addAll(values, a);
    }
    return values;
  }

  @Override
  public double[] apply(double[] inputs) {
    return this.downstream.apply(flat(this.applyAttention(inputs)));
  }

  public double[][] applyAttention(double[] inputs) {
    double[][] reshaped = reshapeVector(inputs, this.n, this.din);
    linearTransform(reshaped, this.wq, this.qbias, this.q);
    double[][] keys = matrixTranspose(linearTransform(reshaped, this.wk, this.kbias, this.k));
    matrixMult(this.q, keys, this.attention);
    matrixDiv(this.attention, Math.sqrt(this.dk));
    for (double[] row : this.attention) {
      tanh(row);
    }
    matrixMult(this.attention, matrixTranspose(reshaped), this.latentCode);
    return this.latentCode;
  }

  public static double[][] reshapeVector(double[] v, int p, int n) {
    if (v.length != p * n) {
      throw new RuntimeException(String.format("Cannot reshape vector of size %d into (%d,%d)", v.length, p, n));
    }
    double[][] reshaped = new double[p][n];
    int index = 0;
    for (int i = 0; i < p; ++i) {
      for (int j = 0; j < n; ++j) {
        reshaped[i][j] = v[index++];
      }
    }
    return reshaped;
  }

  public static double[] flat(double[][] input) {
    int dim = input[0].length;
    double[] flattened = new double[dim * input.length];
    for (int i = 0; i < input.length; ++i) {
      System.arraycopy(input[i], 0, flattened, i * dim, dim);
    }
    return flattened;
  }

  @Override
  public int getInputDimension() {
    return this.n * this.din;
  }

  @Override
  public int getOutputDimension() {
    return this.downstream.getOutputDimension();
  }

  public static void matrixMult(double[][] a, double[][] b, double[][] c) {
    for (int i = 0; i < a.length; ++i) {
      for (int j = 0; j < b[0].length; ++j) {
        double sum = 0.0;
        for (int k = 0; k < b.length; ++k) {
          sum += a[i][k] * b[k][j];
        }
        c[i][j] = sum;
      }
    }
  }

  public static double[][] linearTransform(double[][] x, double[][] a, double[] b, double[][] y) {
    matrixMult(x, a, y);
    for (int i = 0; i < y.length; ++i) {
      for (int j = 0; j < y[i].length; ++j) {
        y[i][j] += b[j];
      }
    }
    return y;
  }

  public static double[][] matrixTranspose(double[][] input) {
    double[][] act = new double[input[0].length][input.length];
    for (int i = 0; i < input.length; ++i) {
      for (int j = 0; j < input[0].length; ++j) {
        act[j][i] = input[i][j];
      }
    }
    return act;
  }

  public static void tanh(double[] v) {
    for (int i = 0; i < v.length; ++i) {
      v[i] = Math.tanh(v[i]);
    }
  }

  public static void matrixDiv(double[][] m, double value) {
    for (double[] row : m) {
      vectorDiv(row, value);
    }
  }

  public static void vectorDiv(double[] v, double value) {
    for (int j = 0; j < v.length; ++j) {
      v[j] /= value;
    }
  }

  @Override
  public Snapshot getSnapshot() {
    double[][][] weights = new double[1][][];
    weights[0] = reshapeVector(this.getAttentionParams(), 1, this.countAttentionParams());
    return new Snapshot(new MLPState(this.attention, weights, DoubleRange.of(-1d, 1d)), this.getClass());
  }

}