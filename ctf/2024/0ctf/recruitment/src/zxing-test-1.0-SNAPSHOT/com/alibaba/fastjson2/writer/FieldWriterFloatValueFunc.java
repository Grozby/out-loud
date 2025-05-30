package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.function.ToFloatFunction;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class FieldWriterFloatValueFunc extends FieldWriter {
   final ToFloatFunction function;

   FieldWriterFloatValueFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, ToFloatFunction function) {
      super(fieldName, ordinal, features, format, null, label, float.class, float.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.applyAsFloat(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      float fieldValue = this.function.applyAsFloat(object);
      if (this.decimalFormat != null) {
         jsonWriter.writeDouble((double)fieldValue, this.decimalFormat);
      } else {
         jsonWriter.writeDouble((double)fieldValue);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      float value;
      try {
         value = this.function.applyAsFloat(object);
      } catch (RuntimeException var5) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var5;
      }

      this.writeFieldName(jsonWriter);
      if (this.decimalFormat != null) {
         jsonWriter.writeFloat(value, this.decimalFormat);
      } else {
         jsonWriter.writeFloat(value);
      }

      return true;
   }
}
