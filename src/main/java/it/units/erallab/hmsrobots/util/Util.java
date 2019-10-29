/*
 * Copyright (C) 2019 Eric Medvet <eric.medvet@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.units.erallab.hmsrobots.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class Util {

  public static <T extends Serializable> String serialize(T t, boolean compress) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    if (compress) {
      oos = new ObjectOutputStream(new GZIPOutputStream(baos, true));
    } else {
      oos = new ObjectOutputStream(baos);
    }
    oos.writeObject(t);
    oos.flush();
    oos.close();
    return Base64.getEncoder().encodeToString(baos.toByteArray());
  }

  public static <T> T deserialize(String s, boolean compress) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(s));
    ObjectInputStream ois;
    if (compress) {
      ois = new ObjectInputStream(new GZIPInputStream(bais));
    } else {
      ois = new ObjectInputStream(bais);
    }
    return (T) ois.readObject();
  }

  public static <T extends Serializable> String lazilySerialize(T t) {
    try {
      return serialize(t, true);
    } catch (IOException e) {
      return e.getClass().getSimpleName();
    }
  }
  
}
