package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

final class FieldWriterBigDecimalMethod<T> extends FieldWriter<T> {
   FieldWriterBigDecimalMethod(String fieldName, int ordinal, long features, String format, String label, Field field, Method method) {
      super(fieldName, ordinal, features, format, null, label, BigDecimal.class, BigDecimal.class, null, method);
   }

   @Override
   public Object getFieldValue(T object) {
      try {
         return this.method.invoke(object);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException var3) {
         throw new JSONException("invoke getter method error, " + this.fieldName, var3);
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      BigDecimal value = (BigDecimal)this.getFieldValue(object);
      jsonWriter.writeDecimal(value, this.features, this.decimalFormat);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      BigDecimal value;
      try {
         value = (BigDecimal)this.getFieldValue(object);
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
         }
      }

      this.writeFieldName(jsonWriter);
      jsonWriter.writeDecimal(value, this.features, this.decimalFormat);
      return true;
   }
}
