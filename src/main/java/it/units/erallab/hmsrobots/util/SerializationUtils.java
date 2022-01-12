package it.units.erallab.hmsrobots.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author eric
 */
public class SerializationUtils {
  private static final Logger L = Logger.getLogger(Utils.class.getName());
  private static final Mode DEFAULT_SERIALIZATION_MODE = Mode.GZIPPED_JSON;
  private static final Mode DEFAULT_CLONE_MODE = Mode.JAVA;
  private static final ObjectMapper OM;
  private static final ObjectMapper PRETTY_OM;

  static {
    OM = new ObjectMapper();
    OM.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE);
    OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    PRETTY_OM = new ObjectMapper();
    PRETTY_OM.enable(SerializationFeature.INDENT_OUTPUT);
    PRETTY_OM.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    PRETTY_OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE);
    PRETTY_OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
  }

  private SerializationUtils() {
  }

  public enum Mode {JAVA, JSON, PRETTY_JSON, GZIPPED_JAVA, GZIPPED_JSON}

  public static class LambdaJsonDeserializer extends JsonDeserializer<SerializableFunction<?, ?>> {
    @Override
    public SerializableFunction<?, ?> deserialize(
        JsonParser jsonParser, DeserializationContext deserializationContext
    ) throws IOException {
      byte[] value = jsonParser.getBinaryValue();
      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value); ObjectInputStream inputStream = new ObjectInputStream(
          byteArrayInputStream)) {
        return (SerializableFunction<?, ?>) inputStream.readObject();
      } catch (ClassNotFoundException e) {
        throw new IOException(e);
      }
    }

    @Override
    public Object deserializeWithType(
        JsonParser jsonParser, DeserializationContext deserializationContext, TypeDeserializer typeDeserializer
    ) throws IOException {
      return deserialize(jsonParser, deserializationContext);
    }
  }

  public static class LambdaJsonSerializer extends JsonSerializer<SerializableFunction<?, ?>> {
    @Override
    public void serialize(
        SerializableFunction<?, ?> serializableFunction,
        JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider
    ) throws IOException {
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ObjectOutputStream outputStream = new ObjectOutputStream(
          byteArrayOutputStream)) {
        outputStream.writeObject(serializableFunction);
        jsonGenerator.writeBinary(byteArrayOutputStream.toByteArray());
      }
    }

    @Override
    public void serializeWithType(
        SerializableFunction<?, ?> serializableFunction,
        JsonGenerator jsonGenerator,
        SerializerProvider serializers,
        TypeSerializer typeSer
    ) throws IOException {
      //WritableTypeId typeId = typeSer.typeId(serializableFunction, JsonToken.START_OBJECT);
      //typeSer.writeTypePrefix(jsonGenerator, typeId);
      //jsonGenerator.writeFieldName("ser");
      serialize(serializableFunction, jsonGenerator, serializers);
      //typeSer.writeTypeSuffix(jsonGenerator, typeId);
    }
  }

  public static <T> T clone(T t) {
    return clone(t, DEFAULT_CLONE_MODE);
  }

  @SuppressWarnings("unchecked")
  public static <T> T clone(T t, Mode mode) {
    return (T) deserialize(serialize(t, mode), t.getClass(), mode);
  }

  private static byte[] decode(String string) {
    return Base64.getDecoder().decode(string);
  }

  public static <T> T deserialize(String string, Class<T> tClass) {
    return deserialize(string, tClass, DEFAULT_SERIALIZATION_MODE);
  }

  public static <T> T deserialize(String string, Class<T> tClass, Mode mode) {
    try {
      return switch (mode) {
        case JAVA -> javaDeserialize(decode(string), tClass);
        case JSON, PRETTY_JSON -> jsonDeserialize(string, tClass);
        case GZIPPED_JAVA -> javaDeserialize(ungzip(decode(string)), tClass);
        case GZIPPED_JSON -> jsonDeserialize(new String(ungzip(decode(string))), tClass);
      };
    } catch (IOException e) {
      L.log(Level.SEVERE, String.format("Cannot deserialize due to %s", e), e);
      return null;
    }
  }

  private static String encode(byte[] raw) {
    return Base64.getEncoder().encodeToString(raw);
  }

  private static byte[] gzip(byte[] raw) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gos = new GZIPOutputStream(
        baos,
        true
    );) {
      gos.write(raw);
      gos.flush();
      gos.close();
      return baos.toByteArray();
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T javaDeserialize(byte[] raw, Class<T> tClass) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(raw); ObjectInputStream ois = new ObjectInputStream(bais)) {
      Object o = ois.readObject();
      return (T) o;
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  private static byte[] javaSerialize(Object object) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
      return baos.toByteArray();
    }
  }

  private static <T> T jsonDeserialize(String string, Class<T> tClass) throws IOException {
    return OM.readValue(string, tClass);
  }

  private static String jsonSerialize(Object object, boolean pretty) throws IOException {
    return pretty ? PRETTY_OM.writeValueAsString(object) : OM.writeValueAsString(object);
  }

  public static String serialize(Object object) {
    return serialize(object, DEFAULT_SERIALIZATION_MODE);
  }

  public static String serialize(Object object, Mode mode) {
    try {
      return switch (mode) {
        case JAVA -> encode(javaSerialize(object));
        case JSON -> jsonSerialize(object, false);
        case PRETTY_JSON -> jsonSerialize(object, true);
        case GZIPPED_JAVA -> encode(gzip(javaSerialize(object)));
        case GZIPPED_JSON -> encode(gzip(jsonSerialize(object, false).getBytes()));
      };
    } catch (IOException e) {
      L.log(Level.SEVERE, String.format("Cannot serialize due to %s", e), e);
      return "";
    }
  }

  private static byte[] ungzip(byte[] raw) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(raw); GZIPInputStream gis = new GZIPInputStream(bais); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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
}
