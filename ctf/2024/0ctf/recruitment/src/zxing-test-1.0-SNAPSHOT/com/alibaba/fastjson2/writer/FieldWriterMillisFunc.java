package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.ToLongFunction;

final class FieldWriterMillisFunc<T> extends FieldWriterDate<T> {
   final ToLongFunction function;

   FieldWriterMillisFunc(String fieldName, int ordinal, long features, String dateTimeFormat, String label, Field field, Method method, ToLongFunction function) {
      super(fieldName, ordinal, features, dateTimeFormat, label, long.class, long.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.applyAsLong(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      long millis = this.function.applyAsLong(object);
      if (millis == 0L) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeDate(jsonWriter, millis);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      long millis = this.function.applyAsLong(object);
      this.writeDate(jsonWriter, false, millis);
   }
}
