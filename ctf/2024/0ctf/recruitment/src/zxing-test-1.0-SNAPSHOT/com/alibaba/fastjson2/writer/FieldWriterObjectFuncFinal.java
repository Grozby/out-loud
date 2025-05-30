package com.alibaba.fastjson2.writer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

final class FieldWriterObjectFuncFinal<T> extends FieldWriterObjectFinal<T> {
   final Function function;
   final boolean isArray;

   FieldWriterObjectFuncFinal(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method, Function function
   ) {
      super(name, ordinal, features, format, label, fieldType, fieldClass, field, method);
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
}
