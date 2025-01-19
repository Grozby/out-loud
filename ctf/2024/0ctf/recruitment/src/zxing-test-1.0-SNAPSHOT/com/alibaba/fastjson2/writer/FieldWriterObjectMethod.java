package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

class FieldWriterObjectMethod<T> extends FieldWriterObject<T> {
   protected FieldWriterObjectMethod(
      String name, int ordinal, long features, String format, Locale locale, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, locale, label, fieldType, fieldClass, field, method);
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
