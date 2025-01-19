package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.ToDoubleFunction;

final class FieldWriterDoubleValueFunc extends FieldWriter {
   final ToDoubleFunction function;

   FieldWriterDoubleValueFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, ToDoubleFunction function) {
      super(fieldName, ordinal, features, format, null, label, double.class, double.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.applyAsDouble(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      double value = this.function.applyAsDouble(object);
      if (this.decimalFormat != null) {
         jsonWriter.writeDouble(value, this.decimalFormat);
      } else {
         jsonWriter.writeDouble(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      double value;
      try {
         value = this.function.applyAsDouble(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      this.writeFieldName(jsonWriter);
      if (this.decimalFormat != null) {
         jsonWriter.writeDouble(value, this.decimalFormat);
      } else {
         jsonWriter.writeDouble(value);
      }

      return true;
   }
}
