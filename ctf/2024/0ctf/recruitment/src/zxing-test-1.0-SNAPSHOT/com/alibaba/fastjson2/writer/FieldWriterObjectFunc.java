package com.alibaba.fastjson2.writer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

final class FieldWriterObjectFunc<T> extends FieldWriterObject<T> {
   final Function function;
   final boolean isArray;

   FieldWriterObjectFunc(
      String name,
      int ordinal,
      long features,
      String format,
      Locale locale,
      String label,
      Type fieldType,
      Class fieldClass,
      Field field,
      Method method,
      Function function
   ) {
      super(name, ordinal, features, format, locale, label, fieldType, fieldClass, field, method);
      this.function = function;
      this.isArray = fieldClass == AtomicIntegerArray.class
         || fieldClass == AtomicLongArray.class
         || fieldClass == AtomicReferenceArray.class
         || fieldClass.isArray();
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.apply(object);
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
