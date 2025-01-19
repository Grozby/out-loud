package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderObjectFunc<T, V> extends FieldReaderObject<T> {
   FieldReaderObjectFunc(
      String fieldName,
      Type fieldType,
      Class<V> fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, V> function,
      ObjectReader fieldObjectReader
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null, function);
      this.initReader = fieldObjectReader;
   }

   @Override
   public void accept(T object, Object value) {
      if (this.fieldType == Float.class) {
         value = TypeUtils.toFloat(value);
      } else if (this.fieldType == Double.class) {
         value = TypeUtils.toDouble(value);
      }

      if (value != null || this.fieldClass != StackTraceElement[].class) {
         if (this.schema != null) {
            this.schema.assertValidate(value);
         }

         this.function.accept(object, value);
      }
   }
}
