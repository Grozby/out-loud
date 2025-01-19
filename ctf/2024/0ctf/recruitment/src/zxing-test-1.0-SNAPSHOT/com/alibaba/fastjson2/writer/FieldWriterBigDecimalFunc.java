package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.function.Function;

final class FieldWriterBigDecimalFunc<T> extends FieldWriter<T> {
   final Function<T, BigDecimal> function;

   FieldWriterBigDecimalFunc(
      String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Function<T, BigDecimal> function
   ) {
      super(fieldName, ordinal, features, format, null, label, BigDecimal.class, BigDecimal.class, null, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      BigDecimal value = this.function.apply(object);
      jsonWriter.writeDecimal(value, this.features, this.decimalFormat);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      BigDecimal value;
      try {
         value = this.function.apply(object);
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

   @Override
   public Function getFunction() {
      return this.function;
   }
}
