package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class FieldWriterFloatMethod<T> extends FieldWriter<T> {
   protected FieldWriterFloatMethod(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
   }

   @Override
   public Object getFieldValue(Object object) {
      try {
         return this.method.invoke(object);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException var3) {
         throw new JSONException("invoke getter method error, " + this.fieldName, var3);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Float value;
      try {
         value = (Float)this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = jsonWriter.getFeatures(this.features);
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L && (features & JSONWriter.Feature.NotWriteDefaultValue.mask) == 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNumberNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeFieldName(jsonWriter);
         float floatValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeFloat(floatValue, this.decimalFormat);
         } else if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(floatValue);
         } else {
            jsonWriter.writeFloat(floatValue);
         }

         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Float value = (Float)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         float floatValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeFloat(floatValue, this.decimalFormat);
         } else {
            jsonWriter.writeFloat(floatValue);
         }
      }
   }
}
