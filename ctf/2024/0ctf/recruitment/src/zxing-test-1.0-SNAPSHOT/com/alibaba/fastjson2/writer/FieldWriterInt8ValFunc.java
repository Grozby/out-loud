package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.function.ToByteFunction;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class FieldWriterInt8ValFunc extends FieldWriterInt8 {
   final ToByteFunction function;

   FieldWriterInt8ValFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, ToByteFunction function) {
      super(fieldName, ordinal, features, format, label, byte.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.applyAsByte(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      byte value = this.function.applyAsByte(object);
      jsonWriter.writeInt32(value);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      byte value;
      try {
         value = this.function.applyAsByte(object);
      } catch (RuntimeException var5) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var5;
      }

      this.writeInt8(jsonWriter, value);
      return true;
   }
}
