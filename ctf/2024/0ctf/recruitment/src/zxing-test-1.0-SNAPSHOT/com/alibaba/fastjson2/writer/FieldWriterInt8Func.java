package com.alibaba.fastjson2.writer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

final class FieldWriterInt8Func<T> extends FieldWriterInt8<T> {
   final Function<T, Byte> function;

   FieldWriterInt8Func(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Function<T, Byte> function) {
      super(fieldName, ordinal, features, format, label, Byte.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
