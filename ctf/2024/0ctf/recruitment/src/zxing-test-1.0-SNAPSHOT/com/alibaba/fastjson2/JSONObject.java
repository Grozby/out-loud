package com.alibaba.fastjson2;

import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplEnum;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSONObject extends LinkedHashMap<String, Object> implements InvocationHandler {
   private static final long serialVersionUID = 1L;
   static ObjectReader<JSONArray> arrayReader;
   static final long NONE_DIRECT_FEATURES = JSONWriter.Feature.ReferenceDetection.mask
      | JSONWriter.Feature.PrettyFormat.mask
      | JSONWriter.Feature.NotWriteEmptyArray.mask
      | JSONWriter.Feature.NotWriteDefaultValue.mask;

   public JSONObject() {
   }

   public JSONObject(int initialCapacity) {
      super(initialCapacity);
   }

   public JSONObject(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
   }

   public JSONObject(int initialCapacity, float loadFactor, boolean accessOrder) {
      super(initialCapacity, loadFactor, accessOrder);
   }

   public JSONObject(Map map) {
      super(map);
   }

   public Object get(String key) {
      return super.get(key);
   }

   @Override
   public Object get(Object key) {
      if (key instanceof Number || key instanceof Character || key instanceof Boolean || key instanceof UUID) {
         Object value = super.get(key.toString());
         if (value != null) {
            return value;
         }
      }

      return super.get(key);
   }

   public Object getByPath(String jsonPath) {
      JSONPath path = JSONPath.of(jsonPath);
      if (path instanceof JSONPathSingleName) {
         String name = ((JSONPathSingleName)path).name;
         return this.get(name);
      } else {
         return path.eval(this);
      }
   }

   public boolean containsKey(String key) {
      return super.containsKey(key);
   }

   @Override
   public boolean containsKey(Object key) {
      return !(key instanceof Number) && !(key instanceof Character) && !(key instanceof Boolean) && !(key instanceof UUID)
         ? super.containsKey(key)
         : super.containsKey(key) || super.containsKey(key.toString());
   }

   public Object getOrDefault(String key, Object defaultValue) {
      return super.getOrDefault(key, defaultValue);
   }

   @Override
   public Object getOrDefault(Object key, Object defaultValue) {
      return !(key instanceof Number) && !(key instanceof Character) && !(key instanceof Boolean) && !(key instanceof UUID)
         ? super.getOrDefault(key, defaultValue)
         : super.getOrDefault(key.toString(), defaultValue);
   }

   public void forEchArrayObject(String key, Consumer<JSONObject> action) {
      JSONArray array = this.getJSONArray(key);
      if (array != null) {
         for (int i = 0; i < array.size(); i++) {
            action.accept(array.getJSONObject(i));
         }
      }
   }

   public JSONArray getJSONArray(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof JSONArray) {
         return (JSONArray)value;
      } else if (value instanceof JSONObject) {
         return JSONArray.of(value);
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            if (str.charAt(0) != '[') {
               return JSONArray.of(str);
            } else {
               JSONReader reader = JSONReader.of(str);
               if (arrayReader == null) {
                  arrayReader = reader.getObjectReader(JSONArray.class);
               }

               return arrayReader.readObject(reader, null, null, 0L);
            }
         } else {
            return null;
         }
      } else if (value instanceof Collection) {
         JSONArray array = new JSONArray((Collection<?>)value);
         this.put(key, array);
         return array;
      } else if (value instanceof Object[]) {
         return JSONArray.of((Object[])value);
      } else {
         Class<?> valueClass = value.getClass();
         if (!valueClass.isArray()) {
            return null;
         } else {
            int length = Array.getLength(value);
            JSONArray jsonArray = new JSONArray(length);

            for (int i = 0; i < length; i++) {
               Object item = Array.get(value, i);
               jsonArray.add(item);
            }

            return jsonArray;
         }
      }
   }

   public <T> List<T> getList(String key, Class<T> itemClass, JSONReader.Feature... features) {
      JSONArray jsonArray = this.getJSONArray(key);
      return jsonArray == null ? null : jsonArray.toList(itemClass, features);
   }

   public JSONObject getJSONObject(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof JSONObject) {
         return (JSONObject)value;
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            JSONReader reader = JSONReader.of(str);
            return JSONFactory.OBJECT_READER.readObject(reader, null, null, 0L);
         } else {
            return null;
         }
      } else if (value instanceof Map) {
         JSONObject object = new JSONObject((Map)value);
         this.put(key, object);
         return object;
      } else {
         Class valueClass = value.getClass();
         ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(valueClass);
         if (objectWriter instanceof ObjectWriterAdapter) {
            ObjectWriterAdapter writerAdapter = (ObjectWriterAdapter)objectWriter;
            return writerAdapter.toJSONObject(value);
         } else {
            return null;
         }
      }
   }

   public String getString(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof String) {
         return (String)value;
      } else if (value instanceof Date) {
         long timeMillis = ((Date)value).getTime();
         return DateUtils.toString(timeMillis, false, DateUtils.DEFAULT_ZONE_ID);
      } else {
         return !(value instanceof Boolean)
               && !(value instanceof Character)
               && !(value instanceof Number)
               && !(value instanceof UUID)
               && !(value instanceof Enum)
               && !(value instanceof TemporalAccessor)
            ? JSON.toJSONString(value)
            : value.toString();
      }
   }

   public Double getDouble(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Double) {
         return (Double)value;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Double.parseDouble(str) : null;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Double");
      }
   }

   public double getDoubleValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0.0;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Double.parseDouble(str) : 0.0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to double value");
      }
   }

   public Float getFloat(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Float) {
         return (Float)value;
      } else if (value instanceof Number) {
         return ((Number)value).floatValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Float.parseFloat(str) : null;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Float");
      }
   }

   public float getFloatValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0.0F;
      } else if (value instanceof Number) {
         return ((Number)value).floatValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Float.parseFloat(str) : 0.0F;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to float value");
      }
   }

   public Long getLong(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Long) {
         return (Long)value;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (long)Double.parseDouble(str) : Long.parseLong(str);
         } else {
            return null;
         }
      } else if (value instanceof Boolean) {
         return (Boolean)value ? 1L : 0L;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Long");
      }
   }

   public long getLongValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0L;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (long)Double.parseDouble(str) : Long.parseLong(str);
         } else {
            return 0L;
         }
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to long value");
      }
   }

   public long getLongValue(String key, long defaultValue) {
      Object value = super.get(key);
      if (value == null) {
         return defaultValue;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (long)Double.parseDouble(str) : Long.parseLong(str);
         } else {
            return defaultValue;
         }
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to long value");
      }
   }

   public Integer getInteger(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Integer) {
         return (Integer)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (int)Double.parseDouble(str) : Integer.parseInt(str);
         } else {
            return null;
         }
      } else if (value instanceof Boolean) {
         return (Boolean)value ? 1 : 0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Integer");
      }
   }

   public int getIntValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (int)Double.parseDouble(str) : Integer.parseInt(str);
         } else {
            return 0;
         }
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to int value");
      }
   }

   public int getIntValue(String key, int defaultValue) {
      Object value = super.get(key);
      if (value == null) {
         return defaultValue;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            return str.indexOf(46) != -1 ? (int)Double.parseDouble(str) : Integer.parseInt(str);
         } else {
            return defaultValue;
         }
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to int value");
      }
   }

   public Short getShort(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Short) {
         return (Short)value;
      } else if (value instanceof Number) {
         return ((Number)value).shortValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Short.parseShort(str) : null;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Short");
      }
   }

   public short getShortValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0;
      } else if (value instanceof Number) {
         return ((Number)value).shortValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Short.parseShort(str) : 0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to short value");
      }
   }

   public Byte getByte(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Number) {
         return ((Number)value).byteValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Byte.parseByte(str) : null;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Byte");
      }
   }

   public byte getByteValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return 0;
      } else if (value instanceof Number) {
         return ((Number)value).byteValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Byte.parseByte(str) : 0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to byte value");
      }
   }

   public byte[] getBytes(String key) {
      Object value = this.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof byte[]) {
         return (byte[])value;
      } else if (value instanceof String) {
         return Base64.getDecoder().decode((String)value);
      } else {
         throw new JSONException("can not cast to byte[], value : " + value);
      }
   }

   public Boolean getBoolean(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue() == 1;
      } else if (!(value instanceof String)) {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Boolean");
      } else {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? "true".equalsIgnoreCase(str) || "1".equals(str) : null;
      }
   }

   public boolean getBooleanValue(String key) {
      Object value = super.get(key);
      if (value == null) {
         return false;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue() == 1;
      } else if (!(value instanceof String)) {
         throw new JSONException("Can not cast '" + value.getClass() + "' to boolean value");
      } else {
         String str = (String)value;
         return "true".equalsIgnoreCase(str) || "1".equals(str);
      }
   }

   public boolean getBooleanValue(String key, boolean defaultValue) {
      Object value = super.get(key);
      if (value == null) {
         return defaultValue;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue() == 1;
      } else if (!(value instanceof String)) {
         throw new JSONException("Can not cast '" + value.getClass() + "' to boolean value");
      } else {
         String str = (String)value;
         return "true".equalsIgnoreCase(str) || "1".equals(str);
      }
   }

   public BigInteger getBigInteger(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof BigInteger) {
         return (BigInteger)value;
      } else if (value instanceof Number) {
         if (value instanceof BigDecimal) {
            return ((BigDecimal)value).toBigInteger();
         } else {
            long longValue = ((Number)value).longValue();
            return BigInteger.valueOf(longValue);
         }
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? new BigInteger(str) : null;
      } else if (value instanceof Boolean) {
         return (Boolean)value ? BigInteger.ONE : BigInteger.ZERO;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to BigInteger");
      }
   }

   public BigDecimal getBigDecimal(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Number) {
         if (value instanceof BigDecimal) {
            return (BigDecimal)value;
         } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger)value);
         } else if (value instanceof Float) {
            float floatValue = (Float)value;
            return TypeUtils.toBigDecimal(floatValue);
         } else if (value instanceof Double) {
            double doubleValue = (Double)value;
            return TypeUtils.toBigDecimal(doubleValue);
         } else {
            long longValue = ((Number)value).longValue();
            return BigDecimal.valueOf(longValue);
         }
      } else if (value instanceof String) {
         String str = (String)value;
         return TypeUtils.toBigDecimal(str);
      } else if (value instanceof Boolean) {
         return (Boolean)value ? BigDecimal.ONE : BigDecimal.ZERO;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to BigDecimal");
      }
   }

   public Date getDate(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Date) {
         return (Date)value;
      } else if (value instanceof String) {
         return DateUtils.parseDate((String)value);
      } else if (value instanceof Number) {
         long millis = ((Number)value).longValue();
         return new Date(millis);
      } else {
         return TypeUtils.toDate(value);
      }
   }

   public Date getDate(String key, Date defaultValue) {
      Date date = this.getDate(key);
      if (date == null) {
         date = defaultValue;
      }

      return date;
   }

   public Instant getInstant(String key) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (value instanceof Instant) {
         return (Instant)value;
      } else if (value instanceof Number) {
         long millis = ((Number)value).longValue();
         return millis == 0L ? null : Instant.ofEpochMilli(millis);
      } else {
         return TypeUtils.toInstant(value);
      }
   }

   @Override
   public String toString() {
      JSONWriter writer = JSONWriter.of();

      String var2;
      try {
         writer.setRootObject(this);
         writer.write(this);
         var2 = writer.toString();
      } catch (Throwable var5) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (writer != null) {
         writer.close();
      }

      return var2;
   }

   public String toString(JSONWriter.Feature... features) {
      JSONWriter writer = JSONWriter.of(features);

      String var3;
      try {
         writer.setRootObject(this);
         writer.write(this);
         var3 = writer.toString();
      } catch (Throwable var6) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (writer != null) {
         writer.close();
      }

      return var3;
   }

   public String toJSONString(JSONWriter.Feature... features) {
      return this.toString(features);
   }

   public static String toJSONString(Object object, JSONWriter.Feature... features) {
      return JSON.toJSONString(object, features);
   }

   public byte[] toJSONBBytes(JSONWriter.Feature... features) {
      JSONWriter writer = JSONWriter.ofJSONB(features);

      byte[] var3;
      try {
         writer.setRootObject(this);
         writer.write(this);
         var3 = writer.getBytes();
      } catch (Throwable var6) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (writer != null) {
         writer.close();
      }

      return var3;
   }

   public <T> T to(Function<JSONObject, T> function) {
      return function.apply(this);
   }

   public <T> T to(Type type, JSONReader.Feature... features) {
      long featuresValue = JSONFactory.defaultReaderFeatures;
      boolean fieldBased = false;

      for (JSONReader.Feature feature : features) {
         if (feature == JSONReader.Feature.FieldBased) {
            fieldBased = true;
         }

         featuresValue |= feature.mask;
      }

      if (type == String.class) {
         return (T)this.toString();
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
         return objectReader.createInstance(this, featuresValue);
      }
   }

   public <T> T to(TypeReference<T> typeReference, JSONReader.Feature... features) {
      return this.to(typeReference.getType(), features);
   }

   public <T> T to(Class<T> clazz, JSONReader.Feature... features) {
      long featuresValue = JSONFactory.defaultReaderFeatures | JSONReader.Feature.of(features);
      boolean fieldBased = JSONReader.Feature.FieldBased.isEnabled(featuresValue);
      if (clazz == String.class) {
         return (T)this.toString();
      } else if (clazz == JSON.class) {
         return (T)this;
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         ObjectReader<T> objectReader = provider.getObjectReader(clazz, fieldBased);
         return objectReader.createInstance(this, featuresValue);
      }
   }

   public void copyTo(Object object, JSONReader.Feature... features) {
      long featuresValue = JSONFactory.defaultReaderFeatures | JSONReader.Feature.of(features);
      boolean fieldBased = JSONReader.Feature.FieldBased.isEnabled(featuresValue);
      Class clazz = object.getClass();
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      ObjectReader objectReader = provider.getObjectReader(clazz, fieldBased);
      objectReader.accept(object, this, featuresValue);
   }

   public <T> T toJavaObject(Class<T> clazz, JSONReader.Feature... features) {
      return this.to(clazz, features);
   }

   /** @deprecated */
   public <T> T toJavaObject(Type type, JSONReader.Feature... features) {
      return this.to(type, features);
   }

   /** @deprecated */
   public <T> T toJavaObject(TypeReference<T> typeReference, JSONReader.Feature... features) {
      return this.to(typeReference, features);
   }

   public <T> T getObject(String key, Class<T> type, JSONReader.Feature... features) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (type == Object.class && features.length == 0) {
         return (T)value;
      } else {
         boolean fieldBased = false;

         for (JSONReader.Feature feature : features) {
            if (feature == JSONReader.Feature.FieldBased) {
               fieldBased = true;
               break;
            }
         }

         Class<?> valueClass = value.getClass();
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Function typeConvert = provider.getTypeConvert(valueClass, type);
         if (typeConvert != null) {
            return (T)typeConvert.apply(value);
         } else if (value instanceof Map) {
            ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
            return objectReader.createInstance((Map)value, features);
         } else if (value instanceof Collection) {
            ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
            return objectReader.createInstance((Collection)value, features);
         } else {
            Class clazz = TypeUtils.getMapping(type);
            if (clazz.isInstance(value)) {
               return (T)value;
            } else {
               ObjectReader objectReader = null;
               if (value instanceof String) {
                  String str = (String)value;
                  if (str.isEmpty() || "null".equals(str)) {
                     return null;
                  }

                  if (clazz.isEnum()) {
                     objectReader = provider.getObjectReader(clazz, fieldBased);
                     if (objectReader instanceof ObjectReaderImplEnum) {
                        long hashCode64 = Fnv.hashCode64(str);
                        ObjectReaderImplEnum enumReader = (ObjectReaderImplEnum)objectReader;
                        return (T)enumReader.getEnumByHashCode(hashCode64);
                     }
                  }
               }

               String json = JSON.toJSONString(value);
               JSONReader jsonReader = JSONReader.of(json);
               jsonReader.context.config(features);
               if (objectReader == null) {
                  objectReader = provider.getObjectReader(clazz, fieldBased);
               }

               T object = (T)objectReader.readObject(jsonReader, null, null, 0L);
               if (!jsonReader.isEnd()) {
                  throw new JSONException("not support input " + json);
               } else {
                  return object;
               }
            }
         }
      }
   }

   public <T> T getObject(String key, Type type, JSONReader.Feature... features) {
      Object value = super.get(key);
      if (value == null) {
         return null;
      } else if (type == Object.class && features.length == 0) {
         return (T)value;
      } else {
         boolean fieldBased = false;

         for (JSONReader.Feature feature : features) {
            if (feature == JSONReader.Feature.FieldBased) {
               fieldBased = true;
               break;
            }
         }

         Class<?> valueClass = value.getClass();
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Function typeConvert = provider.getTypeConvert(valueClass, type);
         if (typeConvert != null) {
            return (T)typeConvert.apply(value);
         } else if (value instanceof Map) {
            ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
            return objectReader.createInstance((Map)value, features);
         } else if (value instanceof Collection) {
            ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
            return objectReader.createInstance((Collection)value, features);
         } else {
            if (type instanceof Class) {
               Class clazz = (Class)type;
               if (clazz.isInstance(value)) {
                  return (T)value;
               }
            }

            if (value instanceof String) {
               String str = (String)value;
               if (str.isEmpty() || "null".equals(str)) {
                  return null;
               }
            }

            String json = JSON.toJSONString(value);
            JSONReader jsonReader = JSONReader.of(json);
            jsonReader.context.config(features);
            ObjectReader objectReader = provider.getObjectReader(type, fieldBased);
            return (T)objectReader.readObject(jsonReader, null, null, 0L);
         }
      }
   }

   public <T> T getObject(String key, TypeReference<T> typeReference, JSONReader.Feature... features) {
      return this.getObject(key, typeReference.type, features);
   }

   public <T> T getObject(String key, Function<JSONObject, T> creator) {
      JSONObject object = this.getJSONObject(key);
      return object == null ? null : creator.apply(object);
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();
      int parameterCount = method.getParameterCount();
      Class<?> returnType = method.getReturnType();
      if (parameterCount == 1) {
         if ("equals".equals(methodName)) {
            return this.equals(args[0]);
         } else {
            Class proxyInterface = null;
            Class<?>[] interfaces = proxy.getClass().getInterfaces();
            if (interfaces.length == 1) {
               proxyInterface = interfaces[0];
            }

            if (returnType != void.class && returnType != proxyInterface) {
               throw new JSONException("This method '" + methodName + "' is not a setter");
            } else {
               String name = this.getJSONFieldName(method);
               if (name == null) {
                  if (!methodName.startsWith("set")) {
                     throw new JSONException("This method '" + methodName + "' is not a setter");
                  }

                  name = methodName.substring(3);
                  if (name.length() == 0) {
                     throw new JSONException("This method '" + methodName + "' is an illegal setter");
                  }

                  name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
               }

               this.put(name, args[0]);
               return returnType != void.class ? proxy : null;
            }
         }
      } else if (parameterCount == 0) {
         if (returnType == void.class) {
            throw new JSONException("This method '" + methodName + "' is not a getter");
         } else {
            String name = this.getJSONFieldName(method);
            Object value;
            if (name == null) {
               boolean with = false;
               int prefix;
               if ((methodName.startsWith("get") || (with = methodName.startsWith("with"))) && methodName.length() > (prefix = with ? 4 : 3)) {
                  char[] chars = new char[methodName.length() - prefix];
                  methodName.getChars(prefix, methodName.length(), chars, 0);
                  if (chars[0] >= 'A' && chars[0] <= 'Z') {
                     chars[0] = (char)(chars[0] + ' ');
                  }

                  String fieldName = new String(chars);
                  if (fieldName.isEmpty()) {
                     throw new JSONException("This method '" + methodName + "' is an illegal getter");
                  }

                  value = this.get(fieldName);
                  if (value == null) {
                     return null;
                  }
               } else {
                  if (!methodName.startsWith("is")) {
                     if ("hashCode".equals(methodName)) {
                        return this.hashCode();
                     }

                     if ("toString".equals(methodName)) {
                        return this.toString();
                     }

                     if (methodName.startsWith("entrySet")) {
                        return this.entrySet();
                     }

                     if ("size".equals(methodName)) {
                        return this.size();
                     }

                     Class<?> declaringClass = method.getDeclaringClass();
                     if (declaringClass.isInterface() && !Modifier.isAbstract(method.getModifiers()) && !JDKUtils.ANDROID && !JDKUtils.GRAAL) {
                        Lookup lookup = JDKUtils.trustedLookup(declaringClass);
                        MethodHandle methodHandle = lookup.findSpecial(declaringClass, method.getName(), MethodType.methodType(returnType), declaringClass);
                        return (Object)methodHandle.invoke((Object)proxy);
                     }

                     throw new JSONException("This method '" + methodName + "' is not a getter");
                  }

                  if ("isEmpty".equals(methodName)) {
                     value = this.get("empty");
                     if (value == null) {
                        return this.isEmpty();
                     }
                  } else {
                     name = methodName.substring(2);
                     if (name.isEmpty()) {
                        throw new JSONException("This method '" + methodName + "' is an illegal getter");
                     }

                     name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                     value = this.get(name);
                     if (value == null) {
                        return false;
                     }
                  }
               }
            } else {
               value = this.get(name);
               if (value == null) {
                  return null;
               }
            }

            if (!returnType.isInstance(value)) {
               Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(value.getClass(), method.getGenericReturnType());
               if (typeConvert != null) {
                  value = typeConvert.apply(value);
               }
            }

            return value;
         }
      } else {
         throw new UnsupportedOperationException(method.toGenericString());
      }
   }

   private String getJSONFieldName(Method method) {
      String name = null;
      Annotation[] annotations = BeanUtils.getAnnotations(method);

      for (Annotation annotation : annotations) {
         Class<? extends Annotation> annotationType = annotation.annotationType();
         JSONField jsonField = BeanUtils.findAnnotation(annotation, JSONField.class);
         if (Objects.nonNull(jsonField)) {
            name = jsonField.name();
            if (name.isEmpty()) {
               name = null;
            }
         } else if ("com.alibaba.fastjson.annotation.JSONField".equals(annotationType.getName())) {
            JSONObject.NameConsumer nameConsumer = new JSONObject.NameConsumer(annotation);
            BeanUtils.annotationMethods(annotationType, nameConsumer);
            if (nameConsumer.name != null) {
               name = nameConsumer.name;
            }
         }
      }

      return name;
   }

   public JSONArray putArray(String name) {
      JSONArray array = new JSONArray();
      this.put(name, array);
      return array;
   }

   public JSONObject putObject(String name) {
      JSONObject object = new JSONObject();
      this.put(name, object);
      return object;
   }

   public JSONObject fluentPut(String key, Object value) {
      this.put(key, value);
      return this;
   }

   public boolean isValid(JSONSchema schema) {
      return schema.isValid(this);
   }

   static void nameFilter(Iterable<?> iterable, NameFilter nameFilter) {
      for (Object item : iterable) {
         if (item instanceof JSONObject) {
            ((JSONObject)item).nameFilter(nameFilter);
         } else if (item instanceof Iterable) {
            nameFilter((Iterable<?>)item, nameFilter);
         }
      }
   }

   static void nameFilter(Map map, NameFilter nameFilter) {
      JSONObject changed = null;
      Iterator<?> it = map.entrySet().iterator();

      while (it.hasNext()) {
         Entry entry = (Entry)it.next();
         Object entryKey = entry.getKey();
         Object entryValue = entry.getValue();
         if (entryValue instanceof JSONObject) {
            ((JSONObject)entryValue).nameFilter(nameFilter);
         } else if (entryValue instanceof Iterable) {
            nameFilter((Iterable<?>)entryValue, nameFilter);
         }

         if (entryKey instanceof String) {
            String key = (String)entryKey;
            String processName = nameFilter.process(map, key, entryValue);
            if (processName != null && !processName.equals(key)) {
               if (changed == null) {
                  changed = new JSONObject();
               }

               changed.put(processName, entryValue);
               it.remove();
            }
         }
      }

      if (changed != null) {
         map.putAll(changed);
      }
   }

   static void valueFilter(Iterable<?> iterable, ValueFilter valueFilter) {
      for (Object item : iterable) {
         if (item instanceof Map) {
            valueFilter((Map)item, valueFilter);
         } else if (item instanceof Iterable) {
            valueFilter((Iterable<?>)item, valueFilter);
         }
      }
   }

   static void valueFilter(Map map, ValueFilter valueFilter) {
      for (Object o : map.entrySet()) {
         Entry entry = (Entry)o;
         Object entryKey = entry.getKey();
         Object entryValue = entry.getValue();
         if (entryValue instanceof Map) {
            valueFilter((Map)entryValue, valueFilter);
         } else if (entryValue instanceof Iterable) {
            valueFilter((Iterable<?>)entryValue, valueFilter);
         }

         if (entryKey instanceof String) {
            String key = (String)entryKey;
            Object applyValue = valueFilter.apply(map, key, entryValue);
            if (applyValue != entryValue) {
               entry.setValue(applyValue);
            }
         }
      }
   }

   public void valueFilter(ValueFilter valueFilter) {
      valueFilter(this, valueFilter);
   }

   public void nameFilter(NameFilter nameFilter) {
      nameFilter(this, nameFilter);
   }

   public JSONObject clone() {
      return new JSONObject(this);
   }

   public Object eval(JSONPath path) {
      return path.eval(this);
   }

   public int getSize(String key) {
      Object value = this.get(key);
      if (value instanceof Map) {
         return ((Map)value).size();
      } else {
         return value instanceof Collection ? ((Collection)value).size() : 0;
      }
   }

   public static JSONObject of() {
      return new JSONObject();
   }

   public static JSONObject of(String key, Object value) {
      JSONObject object = new JSONObject(1, 1.0F);
      object.put(key, value);
      return object;
   }

   public static JSONObject of(String k1, Object v1, String k2, Object v2) {
      JSONObject object = new JSONObject(2, 1.0F);
      object.put(k1, v1);
      object.put(k2, v2);
      return object;
   }

   public static JSONObject of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
      JSONObject object = new JSONObject(3);
      object.put(k1, v1);
      object.put(k2, v2);
      object.put(k3, v3);
      return object;
   }

   public static JSONObject of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
      JSONObject object = new JSONObject(4, 1.0F);
      object.put(k1, v1);
      object.put(k2, v2);
      object.put(k3, v3);
      object.put(k4, v4);
      return object;
   }

   public static JSONObject of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
      JSONObject object = new JSONObject(5);
      object.put(k1, v1);
      object.put(k2, v2);
      object.put(k3, v3);
      object.put(k4, v4);
      object.put(k5, v5);
      return object;
   }

   public static JSONObject of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, Object... kvArray) {
      JSONObject object = new JSONObject(5);
      object.put(k1, v1);
      object.put(k2, v2);
      object.put(k3, v3);
      object.put(k4, v4);
      object.put(k5, v5);
      if (kvArray != null && kvArray.length > 0) {
         of(object, kvArray);
      }

      return object;
   }

   private static JSONObject of(JSONObject object, Object... kvArray) {
      if (kvArray != null && kvArray.length > 0) {
         int kvArrayLength = kvArray.length;
         if ((kvArrayLength & 1) == 1) {
            throw new JSONException("The length of kvArray cannot be odd");
         } else {
            List<Object> keyList = IntStream.range(0, kvArrayLength).filter(ix -> ix % 2 == 0).mapToObj(ix -> kvArray[ix]).collect(Collectors.toList());
            keyList.forEach(key -> {
               if (key == null || !(key instanceof String)) {
                  throw new JSONException("The value corresponding to the even bit index of kvArray is key, which cannot be null and must be of type string");
               }
            });
            List<Object> distinctKeyList = keyList.stream().distinct().collect(Collectors.toList());
            if (keyList.size() != distinctKeyList.size()) {
               throw new JSONException("The value corresponding to the even bit index of kvArray is key and cannot be duplicated");
            } else {
               List<Object> valueList = IntStream.range(0, kvArrayLength).filter(ix -> ix % 2 != 0).mapToObj(ix -> kvArray[ix]).collect(Collectors.toList());

               for (int i = 0; i < keyList.size(); i++) {
                  object.put(keyList.get(i).toString(), valueList.get(i));
               }

               return object;
            }
         }
      } else {
         throw new JSONException("The kvArray cannot be empty");
      }
   }

   public static <T> T parseObject(String text, Class<T> objectClass) {
      return JSON.parseObject(text, objectClass);
   }

   public static <T> T parseObject(String text, Class<T> objectClass, JSONReader.Feature... features) {
      return JSON.parseObject(text, objectClass, features);
   }

   public static <T> T parseObject(String text, Type objectType, JSONReader.Feature... features) {
      return JSON.parseObject(text, objectType, features);
   }

   public static <T> T parseObject(String text, TypeReference<T> typeReference, JSONReader.Feature... features) {
      return JSON.parseObject(text, typeReference, features);
   }

   public static JSONObject parseObject(String text) {
      return JSON.parseObject(text);
   }

   public static JSONObject parse(String text, JSONReader.Feature... features) {
      return JSON.parseObject(text, features);
   }

   public static JSONObject from(Object obj) {
      return (JSONObject)JSON.toJSON(obj);
   }

   public static JSONObject from(Object obj, JSONWriter.Feature... writeFeatures) {
      return (JSONObject)JSON.toJSON(obj, writeFeatures);
   }

   public boolean isArray(Object key) {
      Object object = super.get(key);
      return object instanceof JSONArray || object != null && object.getClass().isArray();
   }

   static class NameConsumer implements Consumer<Method> {
      final Annotation annotation;
      String name;

      NameConsumer(Annotation annotation) {
         this.annotation = annotation;
      }

      public void accept(Method method) {
         String methodName = method.getName();
         if ("name".equals(methodName)) {
            try {
               String result = (String)method.invoke(this.annotation);
               if (!result.isEmpty()) {
                  this.name = result;
               }
            } catch (InvocationTargetException | IllegalAccessException var4) {
            }
         }
      }
   }
}
