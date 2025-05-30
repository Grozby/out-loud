package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterBoolMethod extends FieldWriterBoolean {
   FieldWriterBoolMethod(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Class fieldClass) {
      super(fieldName, ordinal, features, format, label, fieldClass, fieldClass, field, method);
   }

   @Override
   public Object getFieldValue(Object object) {
      try {
         return this.method.invoke(object);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException var3) {
         throw new JSONException("invoke getter method error, " + this.fieldName, var3);
      }
   }
}
