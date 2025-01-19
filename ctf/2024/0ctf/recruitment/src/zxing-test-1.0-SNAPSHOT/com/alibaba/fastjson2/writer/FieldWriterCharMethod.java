package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterCharMethod<T> extends FieldWriter<T> {
   FieldWriterCharMethod(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Class fieldClass) {
      super(fieldName, ordinal, features, format, null, label, fieldClass, fieldClass, field, method);
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
      Character value = (Character)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         jsonWriter.writeChar(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Character value = (Character)this.getFieldValue(object);
      if (value == null) {
         if (((jsonWriter.context.getFeatures() | this.features) & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeChar(value);
         return true;
      }
   }
}
