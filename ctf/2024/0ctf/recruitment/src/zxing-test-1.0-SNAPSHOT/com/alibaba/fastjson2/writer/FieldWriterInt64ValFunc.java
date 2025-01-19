package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.ToLongFunction;

final class FieldWriterInt64ValFunc<T> extends FieldWriterInt64<T> {
   final ToLongFunction function;

   FieldWriterInt64ValFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, ToLongFunction function) {
      super(fieldName, ordinal, features, format, label, long.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.applyAsLong(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      long value;
      try {
         value = this.function.applyAsLong(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      this.writeInt64(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      long value = this.function.applyAsLong(object);
      jsonWriter.writeInt64(value);
   }
}
