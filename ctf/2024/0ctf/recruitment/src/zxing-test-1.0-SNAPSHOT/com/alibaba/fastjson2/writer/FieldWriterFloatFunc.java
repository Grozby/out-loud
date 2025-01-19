package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

final class FieldWriterFloatFunc<T> extends FieldWriter<T> {
   final Function<T, Float> function;

   FieldWriterFloatFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Function<T, Float> function) {
      super(fieldName, ordinal, features, format, null, label, Float.class, Float.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Float value = this.function.apply(object);
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

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Float value;
      try {
         value = this.function.apply(object);
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
         } else {
            jsonWriter.writeFloat(floatValue);
         }

         return true;
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
