package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONSchemaValidException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FieldReaderObject<T> extends FieldReader<T> {
   protected ObjectReader initReader;
   protected final BiConsumer function;

   public FieldReaderObject(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      Field field,
      BiConsumer function
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, field);
      this.function = function;
   }

   @Override
   public ObjectReader getInitReader() {
      return this.initReader;
   }

   @Override
   public ObjectReader getObjectReader(JSONReader jsonReader) {
      if (this.initReader != null) {
         return this.initReader;
      } else if (this.reader != null) {
         return this.reader;
      } else {
         ObjectReader formattedObjectReader = createFormattedObjectReader(this.fieldType, this.fieldClass, this.format, this.locale);
         if (formattedObjectReader != null) {
            return this.reader = formattedObjectReader;
         } else if (this.fieldClass != null && Map.class.isAssignableFrom(this.fieldClass)) {
            return this.reader = ObjectReaderImplMap.of(this.fieldType, this.fieldClass, this.features);
         } else {
            return this.fieldClass != null && Collection.class.isAssignableFrom(this.fieldClass)
               ? (this.reader = ObjectReaderImplList.of(this.fieldType, this.fieldClass, this.features))
               : (this.reader = jsonReader.getObjectReader(this.fieldType));
         }
      }
   }

   @Override
   public ObjectReader getObjectReader(JSONReader.Context context) {
      if (this.reader != null) {
         return this.reader;
      } else {
         ObjectReader formattedObjectReader = createFormattedObjectReader(this.fieldType, this.fieldClass, this.format, this.locale);
         if (formattedObjectReader != null) {
            return this.reader = formattedObjectReader;
         } else if (Map.class.isAssignableFrom(this.fieldClass)) {
            return this.reader = ObjectReaderImplMap.of(this.fieldType, this.fieldClass, this.features);
         } else {
            return Collection.class.isAssignableFrom(this.fieldClass)
               ? (this.reader = ObjectReaderImplList.of(this.fieldType, this.fieldClass, this.features))
               : (this.reader = context.getObjectReader(this.fieldType));
         }
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (!this.fieldClassSerializable) {
         long contextFeatures = jsonReader.getContext().getFeatures();
         if ((contextFeatures & JSONReader.Feature.IgnoreNoneSerializable.mask) != 0L) {
            jsonReader.skipValue();
            return;
         }

         if ((contextFeatures & JSONReader.Feature.ErrorOnNoneSerializable.mask) != 0L) {
            throw new JSONException("not support none-Serializable");
         }
      }

      ObjectReader objectReader;
      if (this.initReader != null) {
         objectReader = this.initReader;
      } else {
         ObjectReader formattedObjectReader = createFormattedObjectReader(this.fieldType, this.fieldClass, this.format, this.locale);
         if (formattedObjectReader != null) {
            objectReader = this.initReader = formattedObjectReader;
         } else {
            objectReader = this.initReader = jsonReader.getContext().getObjectReader(this.fieldType);
         }
      }

      if (jsonReader.isReference()) {
         String reference = jsonReader.readReference();
         if ("..".equals(reference)) {
            this.accept(object, object);
         } else {
            this.addResolveTask(jsonReader, object, reference);
         }
      } else {
         Object value;
         try {
            if (jsonReader.nextIfNullOrEmptyString()) {
               if (this.defaultValue != null) {
                  value = this.defaultValue;
               } else if (this.fieldClass == OptionalInt.class) {
                  value = OptionalInt.empty();
               } else if (this.fieldClass == OptionalLong.class) {
                  value = OptionalLong.empty();
               } else if (this.fieldClass == OptionalDouble.class) {
                  value = OptionalDouble.empty();
               } else if (this.fieldClass == Optional.class) {
                  value = Optional.empty();
               } else {
                  value = null;
               }
            } else if (jsonReader.jsonb) {
               if (this.fieldClass == Object.class) {
                  ObjectReader autoTypeObjectReader = jsonReader.checkAutoType(Object.class, 0L, this.features);
                  if (autoTypeObjectReader != null) {
                     value = autoTypeObjectReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
                  } else {
                     value = jsonReader.readAny();
                  }
               } else {
                  value = objectReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
               }
            } else {
               value = objectReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
            }
         } catch (JSONSchemaValidException var8) {
            throw var8;
         } catch (IllegalAccessError | Exception var9) {
            if ((this.features & JSONReader.Feature.NullOnError.mask) == 0L) {
               Member member = (Member)(this.field != null ? this.field : this.method);
               String message;
               if (member != null) {
                  message = "read field '" + member.getDeclaringClass().getName() + "." + member.getName();
               } else {
                  message = "read field " + this.fieldName + " error";
               }

               throw new JSONException(jsonReader.info(message), var9);
            }

            value = null;
         }

         this.accept(object, value);
         if (this.noneStaticMemberClass && value != null) {
            BeanUtils.setNoneStaticMemberClassParent(value, object);
         }
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      if (!this.fieldClassSerializable && jsonReader.getType() != -110) {
         long contextFeatures = jsonReader.getContext().getFeatures();
         if ((contextFeatures & JSONReader.Feature.IgnoreNoneSerializable.mask) != 0L) {
            jsonReader.skipValue();
            return;
         }

         if ((contextFeatures & JSONReader.Feature.ErrorOnNoneSerializable.mask) != 0L) {
            throw new JSONException("not support none-Serializable");
         }
      }

      if (this.initReader == null) {
         this.initReader = jsonReader.getContext().getObjectReader(this.fieldType);
      }

      if (jsonReader.isReference()) {
         String reference = jsonReader.readReference();
         if ("..".equals(reference)) {
            this.accept(object, object);
         } else {
            this.addResolveTask(jsonReader, object, reference);
         }
      } else {
         Object value = this.initReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
         this.accept(object, value);
      }
   }

   @Override
   public void accept(T object, boolean value) {
      this.accept(object, Boolean.valueOf(value));
   }

   @Override
   public void accept(T object, byte value) {
      this.accept(object, Byte.valueOf(value));
   }

   @Override
   public void accept(T object, short value) {
      this.accept(object, Short.valueOf(value));
   }

   @Override
   public void accept(T object, int value) {
      this.accept(object, Integer.valueOf(value));
   }

   @Override
   public void accept(T object, long value) {
      this.accept(object, Long.valueOf(value));
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, Float.valueOf(value));
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, Double.valueOf(value));
   }

   @Override
   public void accept(T object, char value) {
      this.accept(object, Character.valueOf(value));
   }

   @Override
   public void accept(T object, Object value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (value != null || (this.features & JSONReader.Feature.IgnoreSetNullValue.mask) == 0L) {
         if (this.fieldClass == char.class && value instanceof String) {
            String str = (String)value;
            if (str.length() > 0) {
               value = str.charAt(0);
            } else {
               value = '\u0000';
            }
         }

         if (value != null && !this.fieldClass.isInstance(value)) {
            value = TypeUtils.cast(value, this.fieldType);
         }

         try {
            if (this.function != null) {
               this.function.accept(object, value);
            } else if (this.method != null) {
               this.method.invoke(object, value);
            } else {
               JDKUtils.UNSAFE.putObject(object, this.fieldOffset, value);
            }
         } catch (Exception var4) {
            throw new JSONException("set " + (this.function != null ? super.toString() : this.fieldName) + " error", var4);
         }
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      if (this.initReader == null) {
         this.initReader = this.getObjectReader(jsonReader);
      }

      Object object = jsonReader.jsonb
         ? this.initReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features)
         : this.initReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
      Function builder = this.initReader.getBuildFunction();
      if (builder != null) {
         object = builder.apply(object);
      }

      return object;
   }

   @Override
   public void processExtra(JSONReader jsonReader, Object object) {
      if (this.initReader == null) {
         this.initReader = this.getObjectReader(jsonReader);
      }

      if (this.initReader instanceof ObjectReaderBean && this.field != null) {
         String name = jsonReader.getFieldName();
         FieldReader extraField = this.initReader.getFieldReader(name);
         if (extraField != null) {
            try {
               Object unwrappedFieldValue = this.field.get(object);
               if (unwrappedFieldValue == null) {
                  unwrappedFieldValue = this.initReader.createInstance(this.features);
                  this.accept((T)object, unwrappedFieldValue);
               }

               extraField.readFieldValue(jsonReader, unwrappedFieldValue);
               return;
            } catch (Exception var6) {
               throw new JSONException("read unwrapped field error", var6);
            }
         }
      }

      jsonReader.skipValue();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }

   static void arrayToMap(
      Map object, Collection values, String keyName, PropertyNamingStrategy namingStrategy, ObjectReader valueReader, BiConsumer duplicateHandler
   ) {
      values.forEach(item -> {
         Object key;
         if (item instanceof Map) {
            key = ((Map)item).get(keyName);
         } else {
            if (item == null) {
               throw new JSONException("key not found " + keyName);
            }

            ObjectWriter itemWriter = JSONFactory.getObjectWriter(item.getClass(), 0L);
            key = itemWriter.getFieldValue(item, keyName);
         }

         if (namingStrategy != null && key instanceof String) {
            key = namingStrategy.fieldName((String)key);
         }

         Object mapValue;
         if (valueReader.getObjectClass().isInstance(item)) {
            mapValue = item;
         } else {
            if (!(item instanceof Map)) {
               throw new JSONException("can not accept " + JSON.toJSONString(item, JSONWriter.Feature.ReferenceDetection));
            }

            mapValue = valueReader.createInstance((Map)item);
         }

         Object origin = object.putIfAbsent(key, mapValue);
         if (origin != null & duplicateHandler != null) {
            duplicateHandler.accept(origin, mapValue);
         }
      });
   }
}
