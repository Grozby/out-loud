package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterInt64Method<T> extends FieldWriterInt64<T> {
   FieldWriterInt64Method(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Class fieldClass) {
      super(fieldName, ordinal, features, format, label, fieldClass, field, method);
   }

   @Override
   public Object getFieldValue(T object) {
      try {
         return this.method.invoke(object);
      } catch (InvocationTargetException var4) {
         Throwable cause = var4.getCause();
         throw new JSONException("invoke getter method error, " + this.fieldName, (Throwable)(cause != null ? cause : var4));
      } catch (IllegalAccessException | IllegalArgumentException var5) {
         throw new JSONException("invoke getter method error, " + this.fieldName, var5);
      }
   }
}
