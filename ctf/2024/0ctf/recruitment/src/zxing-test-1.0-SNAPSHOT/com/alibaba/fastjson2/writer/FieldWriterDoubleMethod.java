package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class FieldWriterDoubleMethod<T> extends FieldWriter<T> {
   protected FieldWriterDoubleMethod(
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
      Double value;
      try {
         value = (Double)this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      this.writeFieldName(jsonWriter);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         double doubleValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeDouble(doubleValue, this.decimalFormat);
         } else if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(doubleValue);
         } else {
            jsonWriter.writeDouble(doubleValue);
         }
      }

      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Double value = (Double)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         double doubleValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeDouble(doubleValue, this.decimalFormat);
         } else {
            jsonWriter.writeDouble(doubleValue);
         }
      }
   }
}
