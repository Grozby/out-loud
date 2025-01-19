package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.ToIntFunction;

final class FieldWriterInt32ValFunc extends FieldWriterInt32 {
   final ToIntFunction function;

   FieldWriterInt32ValFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, ToIntFunction function) {
      super(fieldName, ordinal, features, format, label, int.class, int.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.applyAsInt(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      int value;
      try {
         value = this.function.applyAsInt(object);
      } catch (RuntimeException var5) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var5;
      }

      this.writeInt32(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      int value = this.function.applyAsInt(object);
      jsonWriter.writeInt32(value);
   }
}
