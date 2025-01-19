package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

public class PropertiesUtils {
   public static <T> T toJavaObject(Properties properties, Class<T> clazz) {
      return toJavaObject(properties, JSONFactory.getDefaultObjectReaderProvider(), clazz);
   }

   public static <T> T toJavaObject(Properties properties, ObjectReaderProvider provider, Class<T> clazz, JSONReader.Feature... features) {
      ObjectReader<T> reader = provider.getObjectReader(clazz);
      T instance = reader.createInstance(JSONReader.Feature.of(features));

      for (Entry<Object, Object> entry : properties.entrySet()) {
         String key = (String)entry.getKey();
         String value = (String)entry.getValue();
         int dotIndex = key.lastIndexOf(".");
         if (dotIndex == -1) {
            FieldReader<T> fieldReader = reader.getFieldReader(key);
            if (fieldReader != null) {
               Object fieldValue = TypeUtils.cast(value, fieldReader.fieldType);
               fieldReader.accept(instance, fieldValue);
            }
         } else {
            JSONPath.set(instance, key, value);
         }
      }

      return instance;
   }

   public static Properties toProperties(Object object) {
      return toProperties(JSONFactory.getDefaultObjectWriterProvider(), object);
   }

   public static Properties toProperties(ObjectWriterProvider provider, Object object, JSONWriter.Feature... features) {
      Map<Object, String> values = new IdentityHashMap<>();
      Properties properties = new Properties();
      paths(provider, values, properties, null, object);
      return properties;
   }

   static void paths(ObjectWriterProvider provider, Map<Object, String> values, Map paths, String parent, Object javaObject) {
      if (javaObject != null) {
         String p = values.put(javaObject, parent);
         if (p != null) {
            Class<?> type = javaObject.getClass();
            boolean basicType = type == String.class
               || type == Boolean.class
               || type == Character.class
               || type == UUID.class
               || javaObject instanceof Enum
               || javaObject instanceof Number
               || javaObject instanceof Date;
            if (!basicType) {
               return;
            }
         }

         if (javaObject instanceof Map) {
            Map map = (Map)javaObject;

            for (Object entryObj : map.entrySet()) {
               Entry entry = (Entry)entryObj;
               Object key = entry.getKey();
               if (key instanceof String) {
                  String strKey = (String)key;
                  boolean escape = strKey.isEmpty();
                  if (!escape) {
                     char c0 = strKey.charAt(0);
                     escape = (c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z') && c0 != '_';
                     if (!escape) {
                        for (int i = 1; i < strKey.length(); i++) {
                           char ch = strKey.charAt(i);
                           escape = (ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9') && ch != '_';
                           if (escape) {
                              break;
                           }
                        }
                     }
                  }

                  String path;
                  if (escape) {
                     path = parent + '[' + JSON.toJSONString(strKey, JSONWriter.Feature.UseSingleQuotes) + ']';
                  } else {
                     path = parent != null ? parent + "." + strKey : strKey;
                  }

                  paths(provider, values, paths, path, entry.getValue());
               }
            }
         } else if (javaObject instanceof Collection) {
            Collection collection = (Collection)javaObject;
            int ix = 0;

            for (Object item : collection) {
               String path = parent + "[" + ix + "]";
               paths(provider, values, paths, path, item);
               ix++;
            }
         } else {
            Class<?> clazz = javaObject.getClass();
            if (clazz.isArray()) {
               int len = Array.getLength(javaObject);

               for (int ix = 0; ix < len; ix++) {
                  Object item = Array.get(javaObject, ix);
                  String path = parent + "[" + ix + "]";
                  paths(provider, values, paths, path, item);
               }
            } else if (ObjectWriterProvider.isPrimitiveOrEnum(clazz)) {
               String propertyValue = javaObject == null ? "" : javaObject.toString();
               paths.put(parent, propertyValue);
            } else {
               ObjectWriter serializer = provider.getObjectWriter(clazz);
               if (serializer instanceof ObjectWriterAdapter) {
                  ObjectWriterAdapter javaBeanSerializer = (ObjectWriterAdapter)serializer;

                  try {
                     Map<String, Object> fieldValues = javaBeanSerializer.toMap(javaObject);

                     for (Entry<String, Object> entry : fieldValues.entrySet()) {
                        String key = entry.getKey();
                        if (key != null) {
                           String path = parent != null ? parent + "." + key : key;
                           paths(provider, values, paths, path, entry.getValue());
                        }
                     }
                  } catch (Exception var17) {
                     throw new JSONException("toJSON error", var17);
                  }
               }
            }
         }
      }
   }
}
