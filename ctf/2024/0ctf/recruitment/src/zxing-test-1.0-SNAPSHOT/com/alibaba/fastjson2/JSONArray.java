package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplEnum;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class JSONArray extends ArrayList<Object> {
   private static final long serialVersionUID = 1L;
   static ObjectWriter<JSONArray> arrayWriter;

   public JSONArray() {
   }

   public JSONArray(int initialCapacity) {
      super(initialCapacity);
   }

   public JSONArray(Collection<?> collection) {
      super((Collection<? extends Object>)collection);
   }

   public JSONArray(Object... items) {
      super(items.length);
      super.addAll(Arrays.asList(items));
   }

   @Override
   public Object set(int index, Object element) {
      int size = super.size();
      if (index < 0) {
         index += size;
         if (index < 0) {
            super.add(0, element);
            return null;
         } else {
            return super.set(index, element);
         }
      } else if (index < size) {
         return super.set(index, element);
      } else {
         if (index < size + 4096) {
            while (index-- != size) {
               super.add(null);
            }

            super.add(element);
         }

         return null;
      }
   }

   public JSONArray getJSONArray(int index) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else if (value instanceof JSONArray) {
         return (JSONArray)value;
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
            if (str.charAt(0) != '[') {
               return of(str);
            } else {
               JSONReader reader = JSONReader.of(str);
               return JSONFactory.ARRAY_READER.readObject(reader, null, null, 0L);
            }
         } else {
            return null;
         }
      } else if (value instanceof Collection) {
         JSONArray array = new JSONArray((Collection<?>)value);
         this.set(index, array);
         return array;
      } else if (value instanceof Object[]) {
         return of((Object[])value);
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

   public JSONObject getJSONObject(int index) {
      Object value = this.get(index);
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
         this.set(index, object);
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

   public String getString(int index) {
      Object value = this.get(index);
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

   public Double getDouble(int index) {
      Object value = this.get(index);
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

   public double getDoubleValue(int index) {
      Object value = this.get(index);
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

   public Float getFloat(int index) {
      Object value = this.get(index);
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

   public float getFloatValue(int index) {
      Object value = this.get(index);
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

   public Long getLong(int index) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else if (value instanceof Long) {
         return (Long)value;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Long.parseLong(str) : null;
      } else if (value instanceof Boolean) {
         return (Boolean)value ? 1L : 0L;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Long");
      }
   }

   public long getLongValue(int index) {
      Object value = this.get(index);
      if (value == null) {
         return 0L;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Long.parseLong(str) : 0L;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to long value");
      }
   }

   public Integer getInteger(int index) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else if (value instanceof Integer) {
         return (Integer)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Integer.parseInt(str) : null;
      } else if (value instanceof Boolean) {
         return (Boolean)value ? 1 : 0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to Integer");
      }
   }

   public int getIntValue(int index) {
      Object value = this.get(index);
      if (value == null) {
         return 0;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Integer.parseInt(str) : 0;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to int value");
      }
   }

   public Short getShort(int index) {
      Object value = this.get(index);
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

   public short getShortValue(int index) {
      Object value = this.get(index);
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

   public Byte getByte(int index) {
      Object value = this.get(index);
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

   public byte getByteValue(int index) {
      Object value = this.get(index);
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

   public Boolean getBoolean(int index) {
      Object value = this.get(index);
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

   public boolean getBooleanValue(int index) {
      Object value = this.get(index);
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

   public BigInteger getBigInteger(int index) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else if (value instanceof Number) {
         if (value instanceof BigInteger) {
            return (BigInteger)value;
         } else if (value instanceof BigDecimal) {
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

   public BigDecimal getBigDecimal(int index) {
      Object value = this.get(index);
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
         return TypeUtils.toBigDecimal((String)value);
      } else if (value instanceof Boolean) {
         return (Boolean)value ? BigDecimal.ONE : BigDecimal.ZERO;
      } else {
         throw new JSONException("Can not cast '" + value.getClass() + "' to BigDecimal");
      }
   }

   public Date getDate(int index) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else if (value instanceof Date) {
         return (Date)value;
      } else if (value instanceof Number) {
         long millis = ((Number)value).longValue();
         return millis == 0L ? null : new Date(millis);
      } else {
         return TypeUtils.toDate(value);
      }
   }

   public Date getDate(int index, Date defaultValue) {
      Date date = this.getDate(index);
      if (date == null) {
         date = defaultValue;
      }

      return date;
   }

   public Instant getInstant(int index) {
      Object value = this.get(index);
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
         if ((writer.context.features & JSONObject.NONE_DIRECT_FEATURES) == 0L) {
            writer.write(this);
         } else {
            writer.setRootObject(this);
            if (arrayWriter == null) {
               arrayWriter = writer.getObjectWriter(JSONArray.class, JSONArray.class);
            }

            arrayWriter.write(writer, this, null, null, 0L);
         }

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

   public <T> T to(Type type) {
      return this.to(type, 0L);
   }

   public <T> T to(Type type, long features) {
      if (type == String.class) {
         return (T)this.toString();
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         ObjectReader<T> objectReader = provider.getObjectReader(type);
         return objectReader.createInstance(this, features);
      }
   }

   public <T> T to(Class<T> type) {
      if (type == String.class) {
         return (T)this.toString();
      } else if (type == JSON.class) {
         return (T)this;
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         ObjectReader<T> objectReader = provider.getObjectReader(type);
         return objectReader.createInstance(this);
      }
   }

   @Deprecated
   public <T> T toJavaObject(Type type) {
      return this.to(type);
   }

   public <T> List<T> toList(Class<T> itemClass, JSONReader.Feature... features) {
      boolean fieldBased = false;
      long featuresValue = JSONFactory.defaultReaderFeatures;

      for (JSONReader.Feature feature : features) {
         featuresValue |= feature.mask;
         if (feature == JSONReader.Feature.FieldBased) {
            fieldBased = true;
         }
      }

      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      ObjectReader<?> objectReader = provider.getObjectReader(itemClass, fieldBased);
      List<T> list = new ArrayList<>(this.size());

      for (int i = 0; i < this.size(); i++) {
         Object item = this.get(i);
         T classItem;
         if (item instanceof JSONObject) {
            classItem = (T)objectReader.createInstance((Map)item, featuresValue);
         } else if (item instanceof Map) {
            classItem = (T)objectReader.createInstance((Map)item, featuresValue);
         } else {
            if (item != null && !itemClass.isInstance(item)) {
               Class<?> currentItemClass = item.getClass();
               Function typeConvert = provider.getTypeConvert(currentItemClass, itemClass);
               if (typeConvert == null) {
                  throw new JSONException(currentItemClass + " cannot be converted to " + itemClass);
               }

               Object converted = typeConvert.apply(item);
               list.add((T)converted);
               continue;
            }

            classItem = (T)item;
         }

         list.add(classItem);
      }

      return list;
   }

   public <T> T[] toArray(Class<T> itemClass, JSONReader.Feature... features) {
      boolean fieldBased = false;
      long featuresValue = JSONFactory.defaultReaderFeatures;

      for (JSONReader.Feature feature : features) {
         featuresValue |= feature.mask;
         if (feature == JSONReader.Feature.FieldBased) {
            fieldBased = true;
         }
      }

      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      ObjectReader<?> objectReader = provider.getObjectReader(itemClass, fieldBased);
      T[] list = (T[])Array.newInstance(itemClass, this.size());

      for (int i = 0; i < this.size(); i++) {
         Object item = this.get(i);
         T classItem;
         if (item instanceof JSONObject) {
            classItem = (T)objectReader.createInstance((Map)item, featuresValue);
         } else if (item instanceof Map) {
            classItem = (T)objectReader.createInstance((Map)item, featuresValue);
         } else {
            if (item != null && !itemClass.isInstance(item)) {
               Class<?> currentItemClass = item.getClass();
               Function typeConvert = provider.getTypeConvert(currentItemClass, itemClass);
               if (typeConvert == null) {
                  throw new JSONException(currentItemClass + " cannot be converted to " + itemClass);
               }

               Object converted = typeConvert.apply(item);
               list[i] = (T)converted;
               continue;
            }

            classItem = (T)item;
         }

         list[i] = classItem;
      }

      return list;
   }

   public <T> List<T> toJavaList(Class<T> clazz, JSONReader.Feature... features) {
      return this.toList(clazz, features);
   }

   public <T> T getObject(int index, Type type, JSONReader.Feature... features) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else {
         Class<?> valueClass = value.getClass();
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Function typeConvert = provider.getTypeConvert(valueClass, type);
         if (typeConvert != null) {
            return (T)typeConvert.apply(value);
         } else {
            boolean fieldBased = false;
            long featuresValue = JSONFactory.defaultReaderFeatures;

            for (JSONReader.Feature feature : features) {
               featuresValue |= feature.mask;
               if (feature == JSONReader.Feature.FieldBased) {
                  fieldBased = true;
               }
            }

            if (value instanceof Map) {
               ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
               return objectReader.createInstance((Map)value, featuresValue);
            } else if (value instanceof Collection) {
               ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
               return objectReader.createInstance((Collection)value, featuresValue);
            } else {
               Class clazz = TypeUtils.getMapping(type);
               if (clazz.isInstance(value)) {
                  return (T)value;
               } else {
                  String json = JSON.toJSONString(value);
                  JSONReader jsonReader = JSONReader.of(json);
                  jsonReader.context.config(features);
                  ObjectReader objectReader = provider.getObjectReader(clazz, fieldBased);
                  return (T)objectReader.readObject(jsonReader, null, null, 0L);
               }
            }
         }
      }
   }

   public <T> T getObject(int index, Class<T> type, JSONReader.Feature... features) {
      Object value = this.get(index);
      if (value == null) {
         return null;
      } else {
         Class<?> valueClass = value.getClass();
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Function typeConvert = provider.getTypeConvert(valueClass, type);
         if (typeConvert != null) {
            return (T)typeConvert.apply(value);
         } else {
            boolean fieldBased = false;
            long featuresValue = JSONFactory.defaultReaderFeatures;

            for (JSONReader.Feature feature : features) {
               featuresValue |= feature.mask;
               if (feature == JSONReader.Feature.FieldBased) {
                  fieldBased = true;
               }
            }

            if (value instanceof Map) {
               ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
               return objectReader.createInstance((Map)value, featuresValue);
            } else if (value instanceof Collection) {
               ObjectReader<T> objectReader = provider.getObjectReader(type, fieldBased);
               return objectReader.createInstance((Collection)value, featuresValue);
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
   }

   public <T> T getObject(int index, Function<JSONObject, T> creator) {
      JSONObject object = this.getJSONObject(index);
      return object == null ? null : creator.apply(object);
   }

   public JSONObject addObject() {
      JSONObject object = new JSONObject();
      this.add(object);
      return object;
   }

   public JSONArray addArray() {
      JSONArray array = new JSONArray();
      this.add(array);
      return array;
   }

   public JSONArray fluentAdd(Object element) {
      this.add(element);
      return this;
   }

   public JSONArray fluentClear() {
      this.clear();
      return this;
   }

   public JSONArray fluentRemove(int index) {
      this.remove(index);
      return this;
   }

   public JSONArray fluentSet(int index, Object element) {
      this.set(index, element);
      return this;
   }

   public JSONArray fluentRemove(Object o) {
      this.remove(o);
      return this;
   }

   public JSONArray fluentRemoveAll(Collection<?> c) {
      this.removeAll(c);
      return this;
   }

   public JSONArray fluentAddAll(Collection<?> c) {
      this.addAll((Collection<? extends Object>)c);
      return this;
   }

   public boolean isValid(JSONSchema schema) {
      return schema.validate(this).isSuccess();
   }

   @Override
   public Object clone() {
      return new JSONArray(this);
   }

   public static JSONArray of(Object... items) {
      return new JSONArray(items);
   }

   public static JSONArray of(Object item) {
      JSONArray array = new JSONArray(1);
      array.add(item);
      return array;
   }

   public static JSONArray copyOf(Collection collection) {
      return new JSONArray(collection);
   }

   public static JSONArray of(Object first, Object second) {
      JSONArray array = new JSONArray(2);
      array.add(first);
      array.add(second);
      return array;
   }

   public static JSONArray of(Object first, Object second, Object third) {
      JSONArray array = new JSONArray(3);
      array.add(first);
      array.add(second);
      array.add(third);
      return array;
   }

   public static JSONArray parseArray(String text, JSONReader.Feature... features) {
      return JSON.parseArray(text, features);
   }

   public static <T> List<T> parseArray(String text, Class<T> type, JSONReader.Feature... features) {
      return JSON.parseArray(text, type, features);
   }

   public static JSONArray parse(String text, JSONReader.Feature... features) {
      return JSON.parseArray(text, features);
   }

   public static <T> List<T> parseArray(String input, Class<T> type) {
      return JSON.parseArray(input, type);
   }

   public static JSONArray from(Object obj) {
      return (JSONArray)JSON.toJSON(obj);
   }

   public static JSONArray from(Object obj, JSONWriter.Feature... writeFeatures) {
      return (JSONArray)JSON.toJSON(obj, writeFeatures);
   }
}
