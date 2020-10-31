package it.units.erallab.hmsrobots.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import it.units.erallab.hmsrobots.core.controllers.CentralizedSensing;
import it.units.erallab.hmsrobots.core.controllers.DistributedSensing;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;

import java.io.*;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author eric
 * @created 2020/10/31
 * @project 2dhmsr
 */
public class SerializationUtils {
  private SerializationUtils() {
  }

  public enum Mode {JAVA, JSON, GZIPPED_JAVA, GZIPPED_JSON}

  private static final Logger L = Logger.getLogger(Utils.class.getName());
  private static final Mode DEFAULT_MODE = Mode.GZIPPED_JSON;
  private static final ObjectMapper OM;

  static {
    OM = new ObjectMapper();
    OM.enable(SerializationFeature.INDENT_OUTPUT);
    OM.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE);
    OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().build();
  }

  public static class LambdaJsonSerializer extends JsonSerializer<SerializableFunction<?, ?>> {
    @Override
    public void serialize(SerializableFunction<?, ?> serializableFunction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
      try (
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)
      ) {
        outputStream.writeObject(serializableFunction);
        jsonGenerator.writeBinary(byteArrayOutputStream.toByteArray());
      }
    }
  }

  public static class LambdaJsonDeserializer extends JsonDeserializer<SerializableFunction<?, ?>> {
    @Override
    public SerializableFunction<?, ?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      byte[] value = jsonParser.getBinaryValue();
      try (
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);
          ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream)
      ) {
        return (SerializableFunction<?, ?>) inputStream.readObject();
      } catch (ClassNotFoundException e) {
        throw new IOException(e);
      }
    }
  }

  public static String serialize(Object object) {
    return serialize(object, DEFAULT_MODE);
  }

  public static <T> T deserialize(String string, Class<T> tClass) {
    return deserialize(string, tClass, DEFAULT_MODE);
  }

  public static String serialize(Object object, Mode mode) {
    try {
      return switch (mode) {
        case JAVA -> encode(javaSerialize(object));
        case JSON -> jsonSerialize(object);
        case GZIPPED_JAVA -> encode(gzip(javaSerialize(object)));
        case GZIPPED_JSON -> encode(gzip(jsonSerialize(object).getBytes()));
      };
    } catch (IOException e) {
      L.log(Level.SEVERE, String.format("Cannot serialize due to %s", e), e);
      return "";
    }
  }

  public static <T> T deserialize(String string, Class<T> tClass, Mode mode) {
    try {
      return switch (mode) {
        case JAVA -> javaDeserialize(decode(string), tClass);
        case JSON -> jsonDeserialize(string, tClass);
        case GZIPPED_JAVA -> javaDeserialize(ungzip(decode(string)), tClass);
        case GZIPPED_JSON -> jsonDeserialize(new String(ungzip(decode(string))), tClass);
      };
    } catch (IOException e) {
      L.log(Level.SEVERE, String.format("Cannot deserialize due to %s", e), e);
      return null;
    }
  }

  private static byte[] javaSerialize(Object object) throws IOException {
    try (
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)
    ) {
      oos.writeObject(object);
      oos.flush();
      return baos.toByteArray();
    }
  }

  private static <T> T javaDeserialize(byte[] raw, Class<T> tClass) throws IOException {
    try (
        ByteArrayInputStream bais = new ByteArrayInputStream(raw);
        ObjectInputStream ois = new ObjectInputStream(bais)
    ) {
      Object o = ois.readObject();
      return (T) o;
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  private static String jsonSerialize(Object object) throws IOException {
    return OM.writeValueAsString(object);
  }

  private static <T> T jsonDeserialize(String string, Class<T> tClass) throws IOException {
    return OM.readValue(string, tClass);
  }

  private static byte[] gzip(byte[] raw) throws IOException {
    try (
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos, true);
    ) {
      gos.write(raw);
      gos.flush();
      gos.close();
      return baos.toByteArray();
    }
  }

  private static byte[] ungzip(byte[] raw) throws IOException {
    try (
        ByteArrayInputStream bais = new ByteArrayInputStream(raw);
        GZIPInputStream gis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ) {
      byte[] buf = new byte[1024];
      while (true) {
        int read = gis.read(buf);
        if (read == -1) {
          break;
        }
        baos.write(buf, 0, read);
      }
      return baos.toByteArray();
    }
  }

  private static String encode(byte[] raw) {
    return Base64.getEncoder().encodeToString(raw);
  }

  private static byte[] decode(String string) {
    return Base64.getDecoder().decode(string);
  }

  public static void main(String[] args) throws IOException {
    Random rnd = new Random();
    Grid<? extends SensingVoxel> body = it.units.erallab.hmsrobots.util.Utils.buildBody("biped-8x5-t-t");
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.RELU,
        CentralizedSensing.nOfInputs(body),
        new int[]{(int) Math.round((double) CentralizedSensing.nOfInputs(body) * 0.65d)},
        CentralizedSensing.nOfOutputs(body)
    );
    System.out.printf("weights=%d%n", mlp.getParams().length);
    double[] ws = new double[mlp.getParams().length];
    IntStream.range(0, ws.length).forEach(i -> ws[i] = rnd.nextDouble() * 2d - 1d);
    mlp.setParams(ws);
    Robot<SensingVoxel> r = new Robot<>(
        new CentralizedSensing(body, mlp),
        body
    );

    CentralizedSensing cs = new CentralizedSensing(2, 1, new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.SIGMOID, 2, new int[0], 1));

    DistributedSensing ds = new DistributedSensing(body, 1);
    body.stream().filter(e -> e.getValue() != null).forEach(e -> ds.getFunctions().set(e.getX(), e.getY(), new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        ds.nOfInputs(e.getX(), e.getY()),
        new int[5],
        ds.nOfOutputs(e.getX(), e.getY())
    )));

    for (Mode mode : Mode.values()) {
      System.out.printf("Body  with %12.12s: 1st=%6d 2nd=%6d%n",
          mode,
          serialize(body, mode).length(),
          serialize(deserialize(serialize(body, mode), Grid.class, mode), mode).length()
      );
      System.out.printf("MLP   with %12.12s: 1st=%6d 2nd=%6d%n",
          mode,
          serialize(mlp, mode).length(),
          serialize(deserialize(serialize(mlp, mode), MultiLayerPerceptron.class, mode), mode).length()
      );
      System.out.printf("Ctrl  with %12.12s: 1st=%6d 2nd=%6d%n",
          mode,
          serialize(r.getController(), mode).length(),
          serialize(deserialize(serialize(r.getController(), mode), CentralizedSensing.class, mode), mode).length()
      );
      System.out.printf("DS  with %12.12s: 1st=%6d 2nd=%6d%n",
          mode,
          serialize(ds, mode).length(),
          serialize(deserialize(serialize(ds, mode), DistributedSensing.class, mode), mode).length()
      );
      System.out.printf("Robot with %12.12s: 1st=%6d 2nd=%6d%n",
          mode,
          serialize(r, mode).length(),
          serialize(deserialize(serialize(r, mode), Robot.class, mode), mode).length()
      );
    }

  }

}
