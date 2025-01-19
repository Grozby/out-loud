package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.BeanUtils;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class FieldWriterObject<T> extends FieldWriter<T> {
   volatile Class initValueClass;
   final boolean unwrapped;
   final boolean array;
   final boolean number;
   static final AtomicReferenceFieldUpdater<FieldWriterObject, Class> initValueClassUpdater = AtomicReferenceFieldUpdater.newUpdater(
      FieldWriterObject.class, Class.class, "initValueClass"
   );

   protected FieldWriterObject(
      String name, int ordinal, long features, String format, Locale locale, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, locale, label, fieldType, fieldClass, field, method);
      this.unwrapped = (features & 562949953421312L) != 0L;
      if (fieldClass == Currency.class) {
         this.initValueClass = fieldClass;
         this.initObjectWriter = ObjectWriterImplCurrency.INSTANCE_FOR_FIELD;
      }

      this.array = fieldClass.isArray()
         || Collection.class.isAssignableFrom(fieldClass)
         || fieldClass == AtomicLongArray.class
         || fieldClass == AtomicIntegerArray.class;
      this.number = Number.class.isAssignableFrom(fieldClass);
   }

   @Override
   public ObjectWriter getInitWriter() {
      return this.initObjectWriter;
   }

   @Override
   public boolean unwrapped() {
      return this.unwrapped;
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      Class initValueClass = this.initValueClass;
      if (initValueClass != null && this.initObjectWriter != ObjectWriterBaseModule.VoidObjectWriter.INSTANCE) {
         boolean typeMatch = initValueClass == valueClass
            || initValueClass.isAssignableFrom(valueClass) && !jsonWriter.isEnabled(JSONWriter.Feature.WriteClassName) && this.fieldType instanceof Class
            || initValueClass == Map.class && initValueClass.isAssignableFrom(valueClass)
            || initValueClass == List.class && initValueClass.isAssignableFrom(valueClass);
         if (!typeMatch && initValueClass.isPrimitive()) {
            typeMatch = typeMatch(initValueClass, valueClass);
         }

         if (typeMatch) {
            ObjectWriter objectWriter;
            if (this.initObjectWriter == null) {
               objectWriter = this.getObjectWriterTypeMatch(jsonWriter, valueClass);
            } else {
               objectWriter = this.initObjectWriter;
            }

            return objectWriter;
         } else {
            return this.getObjectWriterTypeNotMatch(jsonWriter, valueClass);
         }
      } else {
         return this.getObjectWriterVoid(jsonWriter, valueClass);
      }
   }

   private ObjectWriter getObjectWriterVoid(JSONWriter jsonWriter, Class valueClass) {
      ObjectWriter formattedWriter = null;
      if (BeanUtils.isExtendedMap(valueClass) && "$super$".equals(this.fieldName)) {
         JSONWriter.Context context = jsonWriter.context;
         boolean fieldBased = ((this.features | context.getFeatures()) & JSONWriter.Feature.FieldBased.mask) != 0L;
         formattedWriter = context.provider.getObjectWriter(this.fieldType, this.fieldClass, fieldBased);
         if (this.initObjectWriter == null) {
            boolean success = initValueClassUpdater.compareAndSet(this, null, valueClass);
            if (success) {
               initObjectWriterUpdater.compareAndSet(this, null, formattedWriter);
            }
         }

         return formattedWriter;
      } else {
         if (this.format == null) {
            JSONWriter.Context context = jsonWriter.context;
            boolean fieldBased = ((this.features | context.getFeatures()) & JSONWriter.Feature.FieldBased.mask) != 0L;
            formattedWriter = context.provider.getObjectWriterFromCache(valueClass, valueClass, fieldBased);
         }

         DecimalFormat decimalFormat = this.decimalFormat;
         if (valueClass == Float[].class) {
            if (decimalFormat != null) {
               formattedWriter = new ObjectWriterArrayFinal(Float.class, decimalFormat);
            } else {
               formattedWriter = ObjectWriterArrayFinal.FLOAT_ARRAY;
            }
         } else if (valueClass == Double[].class) {
            if (decimalFormat != null) {
               formattedWriter = new ObjectWriterArrayFinal(Double.class, decimalFormat);
            } else {
               formattedWriter = ObjectWriterArrayFinal.DOUBLE_ARRAY;
            }
         } else if (valueClass == float[].class) {
            if (decimalFormat != null) {
               formattedWriter = new ObjectWriterImplFloatValueArray(decimalFormat);
            } else {
               formattedWriter = ObjectWriterImplFloatValueArray.INSTANCE;
            }
         } else if (valueClass == double[].class) {
            if (decimalFormat != null) {
               formattedWriter = new ObjectWriterImplDoubleValueArray(decimalFormat);
            } else {
               formattedWriter = ObjectWriterImplDoubleValueArray.INSTANCE;
            }
         }

         if (formattedWriter == null) {
            formattedWriter = FieldWriter.getObjectWriter(this.fieldType, this.fieldClass, this.format, this.locale, valueClass);
         }

         if (formattedWriter == null) {
            boolean success = initValueClassUpdater.compareAndSet(this, null, valueClass);
            formattedWriter = jsonWriter.getObjectWriter(valueClass);
            if (success) {
               initObjectWriterUpdater.compareAndSet(this, null, formattedWriter);
            }
         } else if (this.initObjectWriter == null) {
            boolean success = initValueClassUpdater.compareAndSet(this, null, valueClass);
            if (success) {
               initObjectWriterUpdater.compareAndSet(this, null, formattedWriter);
            }
         }

         return formattedWriter;
      }
   }

   static boolean typeMatch(Class initValueClass, Class valueClass) {
      return initValueClass == int.class && valueClass == Integer.class
         || initValueClass == long.class && valueClass == Long.class
         || initValueClass == boolean.class && valueClass == Boolean.class
         || initValueClass == short.class && valueClass == Short.class
         || initValueClass == byte.class && valueClass == Byte.class
         || initValueClass == float.class && valueClass == Float.class
         || initValueClass == double.class && valueClass == Double.class
         || initValueClass == char.class && valueClass == Character.class;
   }

   private ObjectWriter getObjectWriterTypeNotMatch(JSONWriter jsonWriter, Class valueClass) {
      if (Map.class.isAssignableFrom(valueClass)) {
         return this.fieldClass.isAssignableFrom(valueClass) ? ObjectWriterImplMap.of(this.fieldType, valueClass) : ObjectWriterImplMap.of(valueClass);
      } else {
         ObjectWriter objectWriter = null;
         if (this.format != null) {
            objectWriter = FieldWriter.getObjectWriter(this.fieldType, this.fieldClass, this.format, null, valueClass);
         }

         if (objectWriter == null) {
            objectWriter = jsonWriter.getObjectWriter(valueClass);
         }

         return objectWriter;
      }
   }

   private ObjectWriter getObjectWriterTypeMatch(JSONWriter jsonWriter, Class valueClass) {
      ObjectWriter objectWriter;
      if (Map.class.isAssignableFrom(valueClass)) {
         if (this.fieldClass.isAssignableFrom(valueClass)) {
            objectWriter = ObjectWriterImplMap.of(this.fieldType, valueClass);
         } else {
            objectWriter = ObjectWriterImplMap.of(valueClass);
         }
      } else {
         objectWriter = jsonWriter.getObjectWriter(valueClass);
      }

      initObjectWriterUpdater.compareAndSet(this, null, objectWriter);
      return objectWriter;
   }

   @Override
   public void writeEnumJSONB(JSONWriter jsonWriter, Enum e) {
      if (e != null) {
         this.writeFieldName(jsonWriter);
         Class<?> valueClass = e.getClass();
         ObjectWriter valueWriter;
         if (this.initValueClass == null) {
            this.initValueClass = valueClass;
            valueWriter = jsonWriter.getObjectWriter(valueClass);
            initObjectWriterUpdater.compareAndSet(this, null, valueWriter);
         } else if (this.initValueClass == valueClass) {
            valueWriter = this.initObjectWriter;
         } else {
            valueWriter = jsonWriter.getObjectWriter(valueClass);
         }

         if (valueWriter == null) {
            throw new JSONException("get value writer error, valueType : " + valueClass);
         } else {
            valueWriter.writeJSONB(jsonWriter, e, this.fieldName, this.fieldType, this.features);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      long features = this.features | jsonWriter.getFeatures();
      if (!this.fieldClassSerializable && (features & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L) {
         return false;
      } else if (this.backReference && jsonWriter.containsReference(object)) {
         return false;
      } else {
         Object value;
         try {
            value = this.getFieldValue(object);
         } catch (RuntimeException var10) {
            if (jsonWriter.isIgnoreErrorGetter()) {
               return false;
            }

            throw var10;
         }

         if (value != null) {
            if (value == object && this.fieldClass == Throwable.class && this.field != null && this.field.getDeclaringClass() == Throwable.class) {
               return false;
            } else if ((features & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L && !(value instanceof Serializable)) {
               return false;
            } else {
               if ((features & JSONWriter.Feature.IgnoreEmpty.mask) != 0L) {
                  if (value instanceof Collection && ((Collection)value).isEmpty()) {
                     return false;
                  }

                  if (value instanceof Map && ((Map)value).isEmpty()) {
                     return false;
                  }
               }

               boolean refDetect = jsonWriter.isRefDetect(value);
               if (refDetect) {
                  if (value == object) {
                     this.writeFieldName(jsonWriter);
                     jsonWriter.writeReference("..");
                     return true;
                  }

                  String refPath = jsonWriter.setPath(this, value);
                  if (refPath != null) {
                     this.writeFieldName(jsonWriter);
                     jsonWriter.writeReference(refPath);
                     jsonWriter.popPath(value);
                     return true;
                  }
               }

               Class<?> valueClass = value.getClass();
               if (valueClass == byte[].class) {
                  this.writeBinary(jsonWriter, (byte[])value);
                  return true;
               } else {
                  ObjectWriter valueWriter = this.getObjectWriter(jsonWriter, valueClass);
                  if (valueWriter == null) {
                     throw new JSONException("get objectWriter error : " + valueClass);
                  } else if (this.unwrapped && this.writeWithUnwrapped(jsonWriter, value, features, refDetect, valueWriter)) {
                     return true;
                  } else {
                     this.writeFieldName(jsonWriter);
                     boolean jsonb = jsonWriter.jsonb;
                     if ((this.features & JSONWriter.Feature.BeanToArray.mask) != 0L) {
                        if (jsonb) {
                           valueWriter.writeArrayMappingJSONB(jsonWriter, value, this.fieldName, this.fieldType, this.features);
                        } else {
                           valueWriter.writeArrayMapping(jsonWriter, value, this.fieldName, this.fieldType, this.features);
                        }
                     } else if (jsonb) {
                        valueWriter.writeJSONB(jsonWriter, value, this.fieldName, this.fieldType, this.features);
                     } else {
                        valueWriter.write(jsonWriter, value, this.fieldName, this.fieldType, this.features);
                     }

                     if (refDetect) {
                        jsonWriter.popPath(value);
                     }

                     return true;
                  }
               }
            }
         } else if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L && (features & JSONWriter.Feature.NotWriteDefaultValue.mask) == 0L) {
            this.writeFieldName(jsonWriter);
            if (this.array) {
               jsonWriter.writeArrayNull();
            } else if (this.number) {
               jsonWriter.writeNumberNull();
            } else if (this.fieldClass != Appendable.class && this.fieldClass != StringBuffer.class && this.fieldClass != StringBuilder.class) {
               jsonWriter.writeNull();
            } else {
               jsonWriter.writeStringNull();
            }

            return true;
         } else if ((features & (JSONWriter.Feature.WriteNullNumberAsZero.mask | JSONWriter.Feature.NullAsDefaultValue.mask)) != 0L && this.number) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeInt32(0);
            return true;
         } else if ((features & (JSONWriter.Feature.WriteNullBooleanAsFalse.mask | JSONWriter.Feature.NullAsDefaultValue.mask)) == 0L
            || this.fieldClass != Boolean.class && this.fieldClass != AtomicBoolean.class) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeBool(false);
            return true;
         }
      }
   }

   protected final boolean writeWithUnwrapped(JSONWriter jsonWriter, Object value, long features, boolean refDetect, ObjectWriter valueWriter) {
      if (value instanceof Map) {
         boolean jsonb = jsonWriter.jsonb;

         for (Entry entry : ((Map)value).entrySet()) {
            String entryKey = entry.getKey().toString();
            Object entryValue = entry.getValue();
            if (entryValue != null || (features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
               jsonWriter.writeName(entryKey);
               if (!jsonb) {
                  jsonWriter.writeColon();
               }

               if (entryValue == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> entryValueClass = entryValue.getClass();
                  ObjectWriter entryValueWriter = jsonWriter.getObjectWriter(entryValueClass);
                  entryValueWriter.write(jsonWriter, entryValue);
               }
            }
         }

         if (refDetect) {
            jsonWriter.popPath(value);
         }

         return true;
      } else if (!(valueWriter instanceof ObjectWriterAdapter)) {
         return false;
      } else {
         ObjectWriterAdapter writerAdapter = (ObjectWriterAdapter)valueWriter;

         for (FieldWriter fieldWriter : writerAdapter.fieldWriters) {
            fieldWriter.write(jsonWriter, value);
         }

         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Object value = this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         Class<?> valueClass = value.getClass();
         ObjectWriter valueWriter;
         if (this.initValueClass == null) {
            this.initValueClass = valueClass;
            valueWriter = jsonWriter.getObjectWriter(valueClass);
            initObjectWriterUpdater.compareAndSet(this, null, valueWriter);
         } else if (this.initValueClass == valueClass) {
            valueWriter = this.initObjectWriter;
         } else {
            valueWriter = jsonWriter.getObjectWriter(valueClass);
         }

         if (valueWriter == null) {
            throw new JSONException("get value writer error, valueType : " + valueClass);
         } else {
            boolean refDetect = jsonWriter.isRefDetect() && !ObjectWriterProvider.isNotReferenceDetect(valueClass);
            if (refDetect) {
               if (value == object) {
                  jsonWriter.writeReference("..");
                  return;
               }

               String refPath = jsonWriter.setPath(this.fieldName, value);
               if (refPath != null) {
                  jsonWriter.writeReference(refPath);
                  jsonWriter.popPath(value);
                  return;
               }
            }

            if (jsonWriter.jsonb) {
               if (jsonWriter.isBeanToArray()) {
                  valueWriter.writeArrayMappingJSONB(jsonWriter, value, this.fieldName, this.fieldClass, this.features);
               } else {
                  valueWriter.writeJSONB(jsonWriter, value, this.fieldName, this.fieldClass, this.features);
               }
            } else {
               valueWriter.write(jsonWriter, value, this.fieldName, this.fieldClass, this.features);
            }

            if (refDetect) {
               jsonWriter.popPath(value);
            }
         }
      }
   }
}
