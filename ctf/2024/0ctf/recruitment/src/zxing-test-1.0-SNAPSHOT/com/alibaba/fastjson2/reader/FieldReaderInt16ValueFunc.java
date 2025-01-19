package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.function.ObjShortConsumer;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;

final class FieldReaderInt16ValueFunc<T> extends FieldReader<T> {
   final ObjShortConsumer<T> function;

   public FieldReaderInt16ValueFunc(
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Short defaultValue,
      JSONSchema schema,
      Method method,
      ObjShortConsumer<T> function
   ) {
      super(fieldName, short.class, short.class, ordinal, features, format, locale, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, short value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public void accept(T object, Object value) {
      short shortValue = TypeUtils.toShortValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)shortValue);
      }

      this.function.accept(object, shortValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      short fieldInt = (short)jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      this.function.accept(object, fieldInt);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (short)jsonReader.readInt32Value();
   }
}
