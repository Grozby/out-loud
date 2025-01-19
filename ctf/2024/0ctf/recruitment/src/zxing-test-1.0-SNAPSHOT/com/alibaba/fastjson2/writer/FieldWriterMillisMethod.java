package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterMillisMethod<T> extends FieldWriterDate<T> {
   FieldWriterMillisMethod(String fieldName, int ordinal, long features, String dateTimeFormat, String label, Class fieldClass, Field field, Method method) {
      super(fieldName, ordinal, features, dateTimeFormat, label, fieldClass, fieldClass, field, method);
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
   public boolean write(JSONWriter jsonWriter, T object) {
      long millis = (Long)this.getFieldValue(object);
      this.writeDate(jsonWriter, millis);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      long millis = (Long)this.getFieldValue(object);
      this.writeDate(jsonWriter, false, millis);
   }
}
