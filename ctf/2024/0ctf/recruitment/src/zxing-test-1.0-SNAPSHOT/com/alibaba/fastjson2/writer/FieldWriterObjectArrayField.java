package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;

final class FieldWriterObjectArrayField<T> extends FieldWriter<T> {
   final Type itemType;
   final Class itemClass;
   ObjectWriter itemObjectWriter;

   FieldWriterObjectArrayField(
      String fieldName, Type itemType, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field
   ) {
      super(fieldName, ordinal, features, format, null, label, fieldType, fieldClass, field, null);
      this.itemType = itemType;
      if (itemType instanceof Class) {
         this.itemClass = (Class)itemType;
      } else {
         this.itemClass = TypeUtils.getMapping(itemType);
      }
   }

   @Override
   public ObjectWriter getItemWriter(JSONWriter jsonWriter, Type itemType) {
      if (itemType != null && itemType != this.itemType) {
         return jsonWriter.getObjectWriter(itemType, TypeUtils.getClass(itemType));
      } else if (this.itemObjectWriter != null) {
         return this.itemObjectWriter;
      } else {
         if (itemType == Double.class) {
            this.itemObjectWriter = new ObjectWriterImplDouble(new DecimalFormat(this.format));
         } else if (itemType == Float.class) {
            this.itemObjectWriter = new ObjectWriterImplFloat(new DecimalFormat(this.format));
         } else if (itemType == BigDecimal.class && this.decimalFormat != null) {
            this.itemObjectWriter = new ObjectWriterImplBigDecimal(this.decimalFormat, null);
         } else {
            this.itemObjectWriter = jsonWriter.getObjectWriter(this.itemType, this.itemClass);
         }

         return this.itemObjectWriter;
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
      long features = jsonWriter.getFeatures();
      boolean refDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
      boolean previousItemRefDetect = refDetect;
      if (writeFieldName) {
         if (array.length == 0 && (features & JSONWriter.Feature.NotWriteEmptyArray.mask) != 0L) {
            return;
         }

         this.writeFieldName(jsonWriter);
      }

      if (refDetect) {
         String path = jsonWriter.setPath(this.fieldName, array);
         if (path != null) {
            jsonWriter.writeReference(path);
            return;
         }
      }

      if (!jsonWriter.jsonb) {
         jsonWriter.startArray();

         for (int i = 0; i < array.length; i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            Object item = array[i];
            if (item == null) {
               jsonWriter.writeNull();
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

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      if (valueClass == String[].class) {
         return ObjectWriterImplStringArray.INSTANCE;
      } else if (valueClass == Float[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Float.class, this.decimalFormat) : ObjectWriterArrayFinal.FLOAT_ARRAY;
      } else if (valueClass == Double[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Double.class, this.decimalFormat) : ObjectWriterArrayFinal.DOUBLE_ARRAY;
      } else if (valueClass == BigDecimal[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(BigDecimal.class, this.decimalFormat) : ObjectWriterArrayFinal.DECIMAL_ARRAY;
      } else {
         return jsonWriter.getObjectWriter(valueClass);
      }
   }
}
