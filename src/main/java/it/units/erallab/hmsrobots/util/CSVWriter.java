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

import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Eric Medvet <eric.medvet@gmail.com>
 */
public class CSVWriter {
  
  public final static String SEPARATOR = ";";
  public final static String NULL = "NA";

  public static class Table {

    private final LinkedHashMap<String, String> columns;
    private final Object[][] data;

    public Table(LinkedHashMap<String, String> columns, Object[][] data) {
      this.columns = columns;
      this.data = data;
    }

    public LinkedHashMap<String, String> getColumns() {
      return columns;
    }

    public Object[][] getData() {
      return data;
    }
    
    public List<String> getColumnNames() {
      return columns.keySet().stream().collect(Collectors.toList());
    }

    public List<String> getColumnFormats() {
      return columns.values().stream().collect(Collectors.toList());
    }

    public static Table create(Map<String, List<?>> unshapedData) {
      LinkedHashMap<String, String> columns = new LinkedHashMap<>();
      for (String name : unshapedData.keySet()) {
        columns.put(name, "%s");
      }
      int maxLength = unshapedData.values().stream().mapToInt(List::size).max().orElse(0);
      Object[][] data = new Object[maxLength][];
      for (int i = 0; i < maxLength; i++) {
        data[i] = new Object[columns.size()];
        int j = 0;
        for (String name : columns.keySet()) {
          List<?> values = unshapedData.get(name);
          data[i][j] = (i < values.size()) ? values.get(i) : null;
          j = j+1;
        }
      }
      return new Table(columns, data);
    }

  }

  public static void write(Table table, PrintStream ps) {
    //header
    for (int i = 0; i<table.getColumnNames().size(); i++) {
      ps.print(table.getColumnNames().get(i));
        if (i<table.getColumnNames().size()-1) {
          ps.print(SEPARATOR);
        } else {
          ps.println();
        }
    }
    List<String> formats = table.getColumnFormats();
    //data
    for (Object[] row : table.getData()) {
      for (int i = 0; i<row.length; i++) {
        if (row[i]==null) {
          ps.print(NULL);
        } else {
          ps.printf(formats.get(i), row[i]);
        }
        if (i<row.length-1) {
          ps.print(SEPARATOR);
        } else {
          ps.println();
        }
      }
    }
  }
  
}
