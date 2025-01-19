package com.alibaba.fastjson2.stream;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class JSONStreamReader<T> extends StreamReader<T> {
   protected ObjectReaderAdapter objectReader;

   public JSONStreamReader(Type[] types) {
      super(types);
   }

   public JSONStreamReader(ObjectReaderAdapter objectReader) {
      this.objectReader = objectReader;
   }

   public static JSONStreamReader of(File file) throws IOException {
      return of(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
   }

   public static JSONStreamReader of(InputStream in) throws IOException {
      return of(in, StandardCharsets.UTF_8);
   }

   public static JSONStreamReader of(InputStream in, Type... types) throws IOException {
      return of(in, StandardCharsets.UTF_8, types);
   }

   public static JSONStreamReader of(InputStream in, Charset charset, Type... types) {
      return (JSONStreamReader)(charset != StandardCharsets.UTF_16 && charset != StandardCharsets.UTF_16LE && charset != StandardCharsets.UTF_16BE
         ? new JSONStreamReaderUTF8(in, charset, types)
         : new JSONStreamReaderUTF16(new InputStreamReader(in, charset), types));
   }

   public static JSONStreamReader of(InputStream in, Class objectClass) {
      return of(in, StandardCharsets.UTF_8, objectClass);
   }

   public static JSONStreamReader of(InputStream in, Charset charset, Class objectClass) {
      JSONReader.Context context = JSONFactory.createReadContext();
      ObjectReaderAdapter objectReader = (ObjectReaderAdapter)context.getObjectReader(objectClass);
      return (JSONStreamReader)(charset != StandardCharsets.UTF_16 && charset != StandardCharsets.UTF_16LE && charset != StandardCharsets.UTF_16BE
         ? new JSONStreamReaderUTF8(in, charset, objectReader)
         : new JSONStreamReaderUTF16(new InputStreamReader(in, charset), objectReader));
   }

   public StreamReader.ColumnStat getColumnStat(String name) {
      if (this.columnStatsMap == null) {
         this.columnStatsMap = new LinkedHashMap<>();
      }

      if (this.columns == null) {
         this.columns = new ArrayList<>();
      }

      if (this.columnStats == null) {
         this.columnStats = new ArrayList<>();
      }

      StreamReader.ColumnStat stat = this.columnStatsMap.get(name);
      if (stat == null && this.columnStatsMap.size() <= 100) {
         stat = new StreamReader.ColumnStat(name);
         this.columnStatsMap.put(name, stat);
         this.columns.add(name);
         this.columnStats.add(stat);
      }

      return stat;
   }

   protected static void stat(StreamReader.ColumnStat stat, Object value) {
      if (stat != null) {
         if (value == null) {
            stat.nulls++;
         } else {
            stat.values++;
            if (!(value instanceof Number)) {
               if (value instanceof String) {
                  stat.stat((String)value);
               } else if (value instanceof Boolean) {
                  stat.booleans++;
               } else if (value instanceof Map) {
                  stat.maps++;
               } else {
                  if (value instanceof Collection) {
                     stat.arrays++;
                  }
               }
            } else {
               stat.numbers++;
               if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                  stat.integers++;
               } else if (value instanceof Float || value instanceof Double) {
                  stat.doubles++;
               }
            }
         }
      }
   }

   public void statAll() {
      this.columnStatsMap = new LinkedHashMap<>();
      this.columns = new ArrayList<>();
      this.columnStats = new ArrayList<>();

      while (true) {
         Object object = this.readLineObject();
         if (object == null) {
            return;
         }

         this.statLine(object);
      }
   }

   public void statLine(Object object) {
      if (object instanceof Map) {
         this.statMap(null, (Map)object, 0);
      } else if (object instanceof List) {
         this.statArray(null, (List)object, 0);
      }

      this.rowCount++;
   }

   private void statArray(String parentKey, List list, int level) {
      if (level <= 10) {
         if (list.size() <= 10) {
            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               String strKey = parentKey == null ? "[" + i + "]" : parentKey + "[" + i + "]";
               StreamReader.ColumnStat stat = this.getColumnStat(parentKey);
               stat(stat, item);
               if (item instanceof Map) {
                  this.statMap(strKey, (Map)item, level + 1);
               } else if (item instanceof List) {
                  this.statArray(strKey, (List)item, level + 1);
               }
            }
         }
      }
   }

   private void statMap(String parentKey, Map map, int level) {
      if (level <= 10) {
         for (Object o : map.entrySet()) {
            Entry entry = (Entry)o;
            Object key = entry.getKey();
            if (key instanceof String) {
               String strKey = parentKey == null ? (String)key : parentKey + "." + key;
               StreamReader.ColumnStat stat = this.getColumnStat(strKey);
               Object entryValue = entry.getValue();
               stat(stat, entryValue);
               if (entryValue instanceof Map) {
                  this.statMap(strKey, (Map)entryValue, level + 1);
               } else if (entryValue instanceof List) {
                  this.statArray(strKey, (List)entryValue, level + 1);
               }
            }
         }
      }
   }
}
