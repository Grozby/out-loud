package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;

final class FieldWriterObjectArrayMethod<T> extends FieldWriter<T> {
   final Type itemType;
   final Class itemClass;
   ObjectWriter itemObjectWriter;

   FieldWriterObjectArrayMethod(
      String fieldName, Type itemType, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(fieldName, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      this.itemType = itemType;
      if (itemType instanceof Class) {
         this.itemClass = (Class)itemType;
      } else {
         this.itemClass = TypeUtils.getMapping(itemType);
      }
   }

   @Override
   public Object getFieldValue(Object object) {
      try {
         return this.method.invoke(object);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException var3) {
         throw new JSONException("field.get error, " + this.fieldName, var3);
      }
   }

   @Override
   public ObjectWriter getItemWriter(JSONWriter jsonWriter, Type itemType) {
      if (itemType != null && itemType != this.itemType) {
         return jsonWriter.getObjectWriter(itemType, null);
      } else if (this.itemObjectWriter != null) {
         return this.itemObjectWriter;
      } else if (itemType == Float[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Float.class, this.decimalFormat) : ObjectWriterArrayFinal.FLOAT_ARRAY;
      } else if (itemType == Double[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Double.class, this.decimalFormat) : ObjectWriterArrayFinal.DOUBLE_ARRAY;
      } else if (itemType == BigDecimal[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(BigDecimal.class, this.decimalFormat) : ObjectWriterArrayFinal.DECIMAL_ARRAY;
      } else if (itemType == Float.class) {
         return this.decimalFormat != null ? new ObjectWriterImplFloat(this.decimalFormat) : ObjectWriterImplFloat.INSTANCE;
      } else if (itemType == Double.class) {
         return this.decimalFormat != null ? new ObjectWriterImplDouble(this.decimalFormat) : ObjectWriterImplDouble.INSTANCE;
      } else if (itemType == BigDecimal.class) {
         return this.decimalFormat != null ? new ObjectWriterImplBigDecimal(this.decimalFormat, null) : ObjectWriterImplBigDecimal.INSTANCE;
      } else {
         return this.itemObjectWriter = jsonWriter.getObjectWriter(this.itemType, this.itemClass);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Object[] value = (Object[])this.getFieldValue(object);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask))
            != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeArray(jsonWriter, true, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Object[] value = (Object[])this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         this.writeArray(jsonWriter, false, value);
      }
   }

   public void writeArray(JSONWriter jsonWriter, boolean writeFieldName, Object[] array) {
      Class previousClass = null;
      ObjectWriter previousObjectWriter = null;
      if (writeFieldName) {
         this.writeFieldName(jsonWriter);
      }

      boolean refDetect = jsonWriter.isRefDetect();
      boolean previousItemRefDetect = refDetect;
      if (refDetect) {
         String path = jsonWriter.setPath(this.fieldName, array);
         if (path != null) {
            jsonWriter.writeReference(path);
            return;
         }
      }

      boolean writeAsString = (this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if (!jsonWriter.jsonb) {
         jsonWriter.startArray();

         for (int i = 0; i < array.length; i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            Object item = array[i];
            if (item == null) {
               jsonWriter.writeNull();
            } else if (writeAsString) {
               jsonWriter.writeString(item.toString());
            } else {
               Class<?> itemClass = item.getClass();
               ObjectWriter itemObjectWriter;
               if (itemClass == previousClass) {
                  itemObjectWriter = previousObjectWriter;
               } else {
                  itemObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                  previousClass = itemClass;
                  previousObjectWriter = itemObjectWriter;
               }

               itemObjectWriter.write(jsonWriter, item);
            }
         }

         jsonWriter.endArray();
      } else {
         Class arrayClass = array.getClass();
         if (arrayClass != this.fieldClass) {
            jsonWriter.writeTypeName(TypeUtils.getTypeName(arrayClass));
         }

         int size = array.length;
         jsonWriter.startArray(size);

         for (int i = 0; i < size; i++) {
            Object item = array[i];
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               Class<?> itemClass = item.getClass();
               boolean itemRefDetect;
               if (itemClass != previousClass) {
                  itemRefDetect = jsonWriter.isRefDetect();
                  previousObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                  previousClass = itemClass;
                  if (itemRefDetect) {
                     itemRefDetect = !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                  }

                  previousItemRefDetect = itemRefDetect;
               } else {
                  itemRefDetect = previousItemRefDetect;
               }

               if (itemRefDetect) {
                  String refPath = jsonWriter.setPath(i, item);
                  if (refPath != null) {
                     jsonWriter.writeReference(refPath);
                     jsonWriter.popPath(item);
                     continue;
                  }
               }

               previousObjectWriter.writeJSONB(jsonWriter, item, i, this.itemType, this.features);
               if (itemRefDetect) {
                  jsonWriter.popPath(item);
               }
            }
         }

         if (refDetect) {
            jsonWriter.popPath(array);
         }
      }
   }
}
