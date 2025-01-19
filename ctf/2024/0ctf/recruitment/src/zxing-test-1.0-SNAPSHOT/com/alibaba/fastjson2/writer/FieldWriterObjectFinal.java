package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

abstract class FieldWriterObjectFinal<T> extends FieldWriterObject<T> {
   final Type fieldType;
   final Class fieldClass;
   volatile ObjectWriter objectWriter;
   final boolean refDetect;

   protected FieldWriterObjectFinal(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      this.fieldType = fieldType;
      this.fieldClass = fieldClass;
      this.refDetect = !ObjectWriterProvider.isNotReferenceDetect(fieldClass);
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      if (this.fieldClass != valueClass) {
         return super.getObjectWriter(jsonWriter, valueClass);
      } else {
         return this.objectWriter != null ? this.objectWriter : (this.objectWriter = super.getObjectWriter(jsonWriter, valueClass));
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Object value;
      try {
         value = this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            if (this.fieldClass.isArray()) {
               jsonWriter.writeArrayNull();
            } else if (this.fieldClass != StringBuffer.class && this.fieldClass != StringBuilder.class) {
               jsonWriter.writeNull();
            } else {
               jsonWriter.writeStringNull();
            }

            return true;
         }
      } else {
         ObjectWriter valueWriter = this.getObjectWriter(jsonWriter, this.fieldClass);
         if (this.unwrapped && this.writeWithUnwrapped(jsonWriter, value, this.features, this.refDetect, valueWriter)) {
            return true;
         } else {
            this.writeFieldName(jsonWriter);
            if (jsonWriter.jsonb) {
               valueWriter.writeJSONB(jsonWriter, value, this.fieldName, this.fieldType, this.features);
            } else {
               valueWriter.write(jsonWriter, value, this.fieldName, this.fieldType, this.features);
            }

            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Object value = this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         boolean refDetect = this.refDetect && jsonWriter.isRefDetect();
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

         ObjectWriter valueWriter = this.getObjectWriter(jsonWriter, this.fieldClass);
         boolean beanToArray = (jsonWriter.getFeatures(this.features) & JSONWriter.Feature.BeanToArray.mask) != 0L;
         if (jsonWriter.jsonb) {
            if (beanToArray) {
               valueWriter.writeArrayMappingJSONB(jsonWriter, value, this.fieldName, this.fieldType, this.features);
            } else {
               valueWriter.writeJSONB(jsonWriter, value, this.fieldName, this.fieldType, this.features);
            }
         } else if (beanToArray) {
            valueWriter.writeArrayMapping(jsonWriter, value, this.fieldName, this.fieldType, this.features);
         } else {
            valueWriter.write(jsonWriter, value, this.fieldName, this.fieldType, this.features);
         }

         if (refDetect) {
            jsonWriter.popPath(value);
         }
      }
   }
}
